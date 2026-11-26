package com.example.fastfilesearch.desktop.engine

import com.example.fastfilesearch.desktop.database.DesktopDatabaseHelper
import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchHistoryCodec
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.SyncSummary
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.model.WorkspaceDeleteStats
import com.example.fastfilesearch.desktop.parser.DesktopFileParserDispatcher
import com.example.fastfilesearch.desktop.storage.DesktopAppStorage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * Offline desktop search and differential indexing engine.
 * Stores SQLite FTS5 index and preferences under %LOCALAPPDATA%\SafeSearch\.
 */
class DesktopSearchEngine(
    private val baseDir: File = DesktopAppStorage.defaultAppDataDir()
) : Closeable {

    companion object {
        private const val KEY_THEME = "app_theme"
        private const val KEY_DEFAULT_METHOD = "default_search_method"
        private const val KEY_SHOW_ALL_ROWS = "show_all_rows"
        private const val KEY_STRETCH_SINGLE_COLUMN = "stretch_single_column"
        private const val KEY_VERTICAL_COPY_FORMAT = "vertical_copy_format"
        private const val KEY_EXPAND_FULL_CELL_TEXT = "expand_full_cell_text"
        private const val KEY_SEARCH_HISTORY_ENABLED = "search_history_enabled"
        private const val KEY_SEARCH_HISTORY = "search_history_list"
        private const val KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled"
        private const val KEY_ACTIVE_WORKSPACE_ID = "active_workspace_id"
        private const val PARALLEL_WORKERS = 4
    }

    private val dbHelper = DesktopDatabaseHelper(DesktopAppStorage.getIndexDatabaseFile(baseDir))
    private val settingsFile = DesktopAppStorage.getSettingsFile(baseDir)
    private val prefsLock = Any()
    private val syncMutex = Mutex()

    // --- Workspaces ---

    fun getAllWorkspaces(): List<Workspace> = dbHelper.getAllWorkspaces()

    fun getWorkspace(id: String): Workspace? = dbHelper.getWorkspace(id)

    fun addWorkspace(folder: File): Workspace {
        val canonical = folder.canonicalFile
        val existing = dbHelper.getAllWorkspaces().firstOrNull {
            File(it.path).canonicalPath.equals(canonical.path, ignoreCase = true)
        }
        if (existing != null) return existing

        val ws = Workspace(
            id = UUID.randomUUID().toString(),
            name = canonical.name.ifBlank { canonical.path },
            path = canonical.absolutePath,
            addedAt = System.currentTimeMillis()
        )
        dbHelper.insertWorkspace(ws)
        return ws
    }

    fun renameWorkspace(id: String, newName: String): Boolean = dbHelper.renameWorkspace(id, newName)

    fun deleteWorkspace(workspaceId: String): Boolean = deleteWorkspaceWithStats(workspaceId).deleted

    fun deleteWorkspaceWithStats(workspaceId: String): WorkspaceDeleteStats {
        val stats = dbHelper.deleteWorkspaceWithStats(workspaceId)
        if (getActiveWorkspaceId() == workspaceId) {
            setActiveWorkspaceId(null)
        }
        return stats
    }

    fun getActiveWorkspaceId(): String? = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        val active = props.getProperty(KEY_ACTIVE_WORKSPACE_ID)?.takeIf { it.isNotBlank() } ?: return@synchronized null
        if (dbHelper.getWorkspace(active) == null) {
            props.remove(KEY_ACTIVE_WORKSPACE_ID)
            DesktopAppStorage.saveProperties(settingsFile, props)
            return@synchronized null
        }
        active
    }

    fun setActiveWorkspaceId(id: String?) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        if (id.isNullOrBlank()) {
            props.remove(KEY_ACTIVE_WORKSPACE_ID)
        } else {
            props.setProperty(KEY_ACTIVE_WORKSPACE_ID, id)
        }
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun getActiveWorkspace(): Workspace? {
        val id = getActiveWorkspaceId() ?: return null
        return dbHelper.getWorkspace(id)
    }

    // --- Settings ---

    fun getThemeMode(): String = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_THEME, "system")
    }

    fun setThemeMode(theme: String) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_THEME, theme)
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun getDefaultSearchMethod(): SearchMethod = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        SearchMethod.fromStoredName(props.getProperty(KEY_DEFAULT_METHOD))
    }

    fun setDefaultSearchMethod(method: SearchMethod) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_DEFAULT_METHOD, method.name)
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isShowAllRows(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_SHOW_ALL_ROWS, "true").toBoolean()
    }

    fun setShowAllRows(showAll: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_SHOW_ALL_ROWS, showAll.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isStretchSingleColumn(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_STRETCH_SINGLE_COLUMN, "true").toBoolean()
    }

    fun setStretchSingleColumn(stretch: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_STRETCH_SINGLE_COLUMN, stretch.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isVerticalCopyFormat(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_VERTICAL_COPY_FORMAT, "true").toBoolean()
    }

    fun setVerticalCopyFormat(vertical: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_VERTICAL_COPY_FORMAT, vertical.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isExpandFullCellText(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_EXPAND_FULL_CELL_TEXT, "true").toBoolean()
    }

    fun setExpandFullCellText(expand: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_EXPAND_FULL_CELL_TEXT, expand.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isBackgroundSyncEnabled(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_BACKGROUND_SYNC_ENABLED, "true").toBoolean()
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_BACKGROUND_SYNC_ENABLED, enabled.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun isSearchHistoryEnabled(): Boolean = synchronized(prefsLock) {
        DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_SEARCH_HISTORY_ENABLED, "true").toBoolean()
    }

    fun setSearchHistoryEnabled(enabled: Boolean) = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.setProperty(KEY_SEARCH_HISTORY_ENABLED, enabled.toString())
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    fun getSearchHistory(): List<String> = synchronized(prefsLock) {
        val raw = DesktopAppStorage.loadProperties(settingsFile).getProperty(KEY_SEARCH_HISTORY)
        SearchHistoryCodec.decode(raw)
    }

    fun addSearchHistory(query: String, minLength: Int = 2): List<String> = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        val current = SearchHistoryCodec.decode(props.getProperty(KEY_SEARCH_HISTORY))
        val updated = SearchHistoryCodec.record(current, query, minLength = minLength)
        if (updated != current) {
            props.setProperty(KEY_SEARCH_HISTORY, SearchHistoryCodec.encode(updated))
            DesktopAppStorage.saveProperties(settingsFile, props)
        }
        updated
    }

    fun removeSearchHistoryItem(query: String): List<String> = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        val updated = SearchHistoryCodec.decode(props.getProperty(KEY_SEARCH_HISTORY)).filterNot { it == query }
        props.setProperty(KEY_SEARCH_HISTORY, SearchHistoryCodec.encode(updated))
        DesktopAppStorage.saveProperties(settingsFile, props)
        updated
    }

    fun clearSearchHistory() = synchronized(prefsLock) {
        val props = DesktopAppStorage.loadProperties(settingsFile)
        props.remove(KEY_SEARCH_HISTORY)
        DesktopAppStorage.saveProperties(settingsFile, props)
    }

    // --- Indexing & Search ---

    fun getIndexStats(workspaceId: String? = getActiveWorkspaceId()): Pair<Int, Int> {
        return dbHelper.getStats(workspaceId)
    }

    fun getAllIndexedFiles(workspaceId: String? = getActiveWorkspaceId()): List<IndexedFileRecord> {
        return dbHelper.getAllIndexedFiles(workspaceId)
    }

    fun search(query: String, filter: SearchFilter = SearchFilter()): List<SearchResult> {
        val activeId = getActiveWorkspaceId()
        val effectiveFilter = if (activeId != null) {
            filter.copy(selectedWorkspaceIds = setOf(activeId))
        } else {
            filter
        }
        return dbHelper.search(query, effectiveFilter)
    }

    suspend fun syncWorkspaces(
        workspaces: List<Workspace> = getAllWorkspaces(),
        onProgress: (IndexProgress) -> Unit = {}
    ): SyncSummary = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            var totalSummary = SyncSummary()
            try {
                for (ws in workspaces) {
                    currentCoroutineContext().ensureActive()
                    val wsSummary = syncSingleWorkspace(ws, onProgress)
                    totalSummary = SyncSummary(
                        totalFound = totalSummary.totalFound + wsSummary.totalFound,
                        indexedCount = totalSummary.indexedCount + wsSummary.indexedCount,
                        skippedCount = totalSummary.skippedCount + wsSummary.skippedCount,
                        deletedCount = totalSummary.deletedCount + wsSummary.deletedCount
                    )
                }
                totalSummary
            } catch (_: CancellationException) {
                totalSummary.copy(cancelled = true)
            } finally {
                onProgress(IndexProgress(isIndexing = false))
            }
        }
    }

    private suspend fun syncSingleWorkspace(
        workspace: Workspace,
        onProgress: (IndexProgress) -> Unit
    ): SyncSummary {
        val rootDir = File(workspace.path)
        if (!rootDir.exists() || !rootDir.isDirectory) return SyncSummary()

        onProgress(
            IndexProgress(
                isIndexing = true,
                statusMessage = "جاري فحص المجلد: ${workspace.name}"
            )
        )

        val discovered = discoverSupportedFiles(rootDir)
        val existing = dbHelper.getIndexedFilesForWorkspace(workspace.id)
        val discoveredPaths = discovered.map { it.absolutePath }.toHashSet()

        val deletedPaths = existing.keys.filterNot { it in discoveredPaths }
        for (path in deletedPaths) {
            currentCoroutineContext().ensureActive()
            dbHelper.deleteFileByUri(path)
        }

        val toIndex = discovered.filter { file ->
            val old = existing[file.absolutePath]
            old == null || old.size != file.length() || old.lastModified != file.lastModified()
        }

        val skippedCount = discovered.size - toIndex.size
        val totalToIndex = toIndex.size

        if (totalToIndex > 0) {
            val semaphore = Semaphore(PARALLEL_WORKERS)
            val processedCounter = AtomicInteger(0)
            coroutineScope {
                toIndex.map { file ->
                    async(Dispatchers.IO) {
                        semaphore.withPermit {
                            currentCoroutineContext().ensureActive()
                            val idx = processedCounter.incrementAndGet()
                            onProgress(
                                IndexProgress(
                                    current = idx,
                                    total = totalToIndex,
                                    currentFileName = file.name,
                                    isIndexing = true,
                                    statusMessage = "فهرسة ($idx/$totalToIndex): ${file.name}"
                                )
                            )
                            indexSingleFile(workspace.id, file)
                        }
                    }
                }.awaitAll()
            }
        }

        return SyncSummary(
            totalFound = discovered.size,
            indexedCount = totalToIndex,
            skippedCount = skippedCount,
            deletedCount = deletedPaths.size
        )
    }

    private fun indexSingleFile(workspaceId: String, file: File) {
        try {
            val segments = mutableListOf<FileSegment>()
            val sheetHeaders = mutableMapOf<String, List<String>>()

            DesktopFileParserDispatcher.parseFile(
                file = file,
                fileUri = file.absolutePath,
                fileName = file.name,
                onHeaderFound = { sheetName, headers ->
                    sheetHeaders[sheetName] = headers
                },
                onSegmentFound = { seg ->
                    segments.add(seg.copy(workspaceId = workspaceId))
                }
            )

            val record = IndexedFileRecord(
                uri = file.absolutePath,
                workspaceId = workspaceId,
                fileName = file.name,
                size = file.length(),
                lastModified = file.lastModified()
            )
            dbHelper.saveFileAndSegments(record, segments, sheetHeaders)
        } catch (_: Throwable) {
            // Skip unreadable individual files safely
        }
    }

    private fun discoverSupportedFiles(rootDir: File): List<File> {
        val result = mutableListOf<File>()
        try {
            Files.walkFileTree(rootDir.toPath(), object : SimpleFileVisitor<Path>() {
                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val f = file.toFile()
                    if (attrs.isRegularFile && DesktopFileParserDispatcher.isSupportedFile(f.name)) {
                        result.add(f)
                    }
                    return FileVisitResult.CONTINUE
                }

                override fun visitFileFailed(file: Path, exc: java.io.IOException?): FileVisitResult {
                    return FileVisitResult.CONTINUE
                }

                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                    val name = dir.fileName?.toString() ?: ""
                    if (name.equals("\$RECYCLE.BIN", ignoreCase = true) ||
                        name.equals("System Volume Information", ignoreCase = true)
                    ) {
                        return FileVisitResult.SKIP_SUBTREE
                    }
                    return FileVisitResult.CONTINUE
                }
            })
        } catch (_: Throwable) {
        }
        return result
    }

    override fun close() {
        dbHelper.close()
    }
}
