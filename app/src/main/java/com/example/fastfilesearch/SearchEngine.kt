package com.example.fastfilesearch

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.database.SearchDatabaseHelper
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.model.Workspace
import com.example.fastfilesearch.parser.FileParserDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

data class IndexProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFileName: String = "",
    val isIndexing: Boolean = false,
    val statusMessage: String = ""
)

data class SyncSummary(
    val totalFound: Int = 0,
    val indexedCount: Int = 0,
    val skippedCount: Int = 0,
    val deletedCount: Int = 0
) {
    operator fun plus(other: SyncSummary): SyncSummary {
        return SyncSummary(
            totalFound = this.totalFound + other.totalFound,
            indexedCount = this.indexedCount + other.indexedCount,
            skippedCount = this.skippedCount + other.skippedCount,
            deletedCount = this.deletedCount + other.deletedCount
        )
    }
}

data class DiscoveredFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?
)

class SearchEngine(private val context: Context) {

    companion object {
        private const val TAG = "SearchEngine"
        private const val PREFS_NAME = "fast_file_search_prefs"
        private const val KEY_FOLDER_URI = "saved_folder_uri"
        private const val KEY_FOLDER_NAME = "saved_folder_name"
        private const val KEY_ACTIVE_WORKSPACE_ID = "active_workspace_id"
        private const val KEY_BACKGROUND_SYNC_ENABLED = "background_sync_enabled"
        private const val KEY_STRETCH_SINGLE_COLUMN = "stretch_single_column"
        private const val KEY_SHOW_ALL_ROWS = "show_all_rows"
        private const val KEY_VERTICAL_COPY_FORMAT = "vertical_copy_format"
        private const val KEY_EXPAND_FULL_CELL_TEXT = "expand_full_cell_text"
        private const val KEY_APP_THEME = "app_theme"
        private const val PARALLEL_WORKERS = 4
    }

    private val dbHelper = SearchDatabaseHelper.getInstance(context)
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val syncMutex = Mutex()

    init {
        migrateLegacyFolderIfNeeded()
    }

    /**
     * Requirement 1: Migration: If user previously had a saved folder, migrate it safely to the first Workspace.
     */
    private fun migrateLegacyFolderIfNeeded() {
        try {
            val existingWorkspaces = dbHelper.getAllWorkspaces()
            if (existingWorkspaces.isEmpty()) {
                val legacyUriStr = prefs.getString(KEY_FOLDER_URI, null)
                val legacyName = prefs.getString(KEY_FOLDER_NAME, "") ?: ""
                if (!legacyUriStr.isNullOrBlank()) {
                    val uri = Uri.parse(legacyUriStr)
                    val name = if (legacyName.isNotBlank()) legacyName else (uri.lastPathSegment ?: "مجلد المستندات")
                    val workspace = Workspace(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        uri = legacyUriStr,
                        addedAt = System.currentTimeMillis()
                    )
                    dbHelper.insertWorkspace(workspace)
                    setActiveWorkspaceId(workspace.id)
                    Log.i(TAG, "Successfully migrated legacy folder to Workspace: ${workspace.name}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during legacy folder migration", e)
        }
    }

    // ==========================================
    // WORKSPACES API
    // ==========================================

    fun getAllWorkspaces(): List<Workspace> {
        return dbHelper.getAllWorkspaces()
    }

    fun getWorkspace(id: String): Workspace? {
        return dbHelper.getWorkspace(id)
    }

    fun addWorkspace(uri: Uri, folderName: String = ""): Workspace {
        // Persist SAF permissions
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to take persistable URI permission for $uri", e)
        }

        val resolvedName = if (folderName.isNotBlank()) {
            folderName
        } else {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name ?: uri.lastPathSegment ?: "مجلد محدد"
        }

        val workspace = Workspace(
            id = UUID.randomUUID().toString(),
            name = resolvedName,
            uri = uri.toString(),
            addedAt = System.currentTimeMillis()
        )
        dbHelper.insertWorkspace(workspace)

        // Set as active workspace if none was active
        if (getActiveWorkspaceId() == null) {
            setActiveWorkspaceId(workspace.id)
        }

        return workspace
    }

    fun renameWorkspace(id: String, newName: String): Boolean {
        return dbHelper.renameWorkspace(id, newName)
    }

    fun deleteWorkspace(id: String): Boolean {
        val success = dbHelper.deleteWorkspace(id)
        if (getActiveWorkspaceId() == id) {
            val remaining = dbHelper.getAllWorkspaces()
            setActiveWorkspaceId(remaining.firstOrNull()?.id)
        }
        return success
    }

    fun getActiveWorkspaceId(): String? {
        val active = prefs.getString(KEY_ACTIVE_WORKSPACE_ID, null)
        if (active != null) {
            val exists = dbHelper.getWorkspace(active) != null
            if (!exists) {
                val first = dbHelper.getAllWorkspaces().firstOrNull()
                setActiveWorkspaceId(first?.id)
                return first?.id
            }
        }
        return active
    }

    fun setActiveWorkspaceId(id: String?) {
        if (id == null) {
            prefs.edit().remove(KEY_ACTIVE_WORKSPACE_ID).apply()
        } else {
            prefs.edit().putString(KEY_ACTIVE_WORKSPACE_ID, id).apply()
        }
    }

    fun getActiveWorkspace(): Workspace? {
        val id = getActiveWorkspaceId() ?: return null
        return dbHelper.getWorkspace(id)
    }

    fun isBackgroundSyncEnabled(): Boolean {
        return prefs.getBoolean(KEY_BACKGROUND_SYNC_ENABLED, true)
    }

    fun setBackgroundSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BACKGROUND_SYNC_ENABLED, enabled).apply()
    }

    // ==========================================
    // PREFERENCES
    // ==========================================

    fun isStretchSingleColumn(): Boolean {
        return prefs.getBoolean(KEY_STRETCH_SINGLE_COLUMN, true)
    }

    fun setStretchSingleColumn(stretch: Boolean) {
        prefs.edit().putBoolean(KEY_STRETCH_SINGLE_COLUMN, stretch).apply()
    }

    fun isShowAllRows(): Boolean {
        return prefs.getBoolean(KEY_SHOW_ALL_ROWS, true)
    }

    fun setShowAllRows(showAll: Boolean) {
        prefs.edit().putBoolean(KEY_SHOW_ALL_ROWS, showAll).apply()
    }

    fun isVerticalCopyFormat(): Boolean {
        return prefs.getBoolean(KEY_VERTICAL_COPY_FORMAT, true)
    }

    fun setVerticalCopyFormat(vertical: Boolean) {
        prefs.edit().putBoolean(KEY_VERTICAL_COPY_FORMAT, vertical).apply()
    }

    fun isExpandFullCellText(): Boolean {
        return prefs.getBoolean(KEY_EXPAND_FULL_CELL_TEXT, true)
    }

    fun setExpandFullCellText(expand: Boolean) {
        prefs.edit().putBoolean(KEY_EXPAND_FULL_CELL_TEXT, expand).apply()
    }

    fun getAppTheme(): String {
        return prefs.getString(KEY_APP_THEME, "system") ?: "system"
    }

    fun setAppTheme(theme: String) {
        prefs.edit().putString(KEY_APP_THEME, theme).apply()
    }

    // ==========================================
    // INCREMENTAL DIFFERENTIAL INDEXING
    // ==========================================

    /**
     * Requirement 2: Smart incremental indexing for a single workspace.
     * Uses Mutex to prevent concurrent sync operations.
     */
    suspend fun syncWorkspaceIncremental(
        workspace: Workspace,
        onProgress: (IndexProgress) -> Unit = {}
    ): SyncSummary = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val uri = Uri.parse(workspace.uri)

            // Validate SAF permission
            val hasPermission = context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
            if (!hasPermission) {
                Log.w(TAG, "Workspace permission not persisted or revoked for: ${workspace.name}")
                return@withContext SyncSummary()
            }

            onProgress(
                IndexProgress(
                    current = 0,
                    total = 0,
                    isIndexing = true,
                    statusMessage = "فحص التغييرات في ${workspace.name}..."
                )
            )

            // 1. Discover current files in folder
            val discoveredFiles = scanDirectoryTree(uri)
            val discoveredMap = discoveredFiles.associateBy { it.uri.toString() }

            // 2. Load existing records for this workspace
            val existingMap = dbHelper.getIndexedFiles(workspace.id)

            // 3. Differential comparison
            val toAdd = mutableListOf<DiscoveredFile>()
            val toUpdate = mutableListOf<DiscoveredFile>()
            val toDelete = mutableListOf<String>()
            var skippedCount = 0

            for (discovered in discoveredFiles) {
                val exist = existingMap[discovered.uri.toString()]
                if (exist == null) {
                    toAdd.add(discovered)
                } else if (exist.size != discovered.size || exist.lastModified != discovered.lastModified) {
                    toUpdate.add(discovered)
                } else {
                    skippedCount++
                }
            }

            for (existUri in existingMap.keys) {
                if (!discoveredMap.containsKey(existUri)) {
                    toDelete.add(existUri)
                }
            }

            // 4. Delete removed files from index
            for (delUri in toDelete) {
                dbHelper.deleteFileIndex(delUri)
            }

            // 5. Index new and modified files
            val filesToIndex = toAdd + toUpdate
            val totalToIndex = filesToIndex.size
            val processedCounter = AtomicInteger(0)

            if (totalToIndex > 0) {
                onProgress(
                    IndexProgress(
                        current = 0,
                        total = totalToIndex,
                        isIndexing = true,
                        statusMessage = "مزامنة $totalToIndex ملف في ${workspace.name}..."
                    )
                )

                val semaphore = Semaphore(PARALLEL_WORKERS)
                coroutineScope {
                    filesToIndex.map { file ->
                        async(Dispatchers.IO) {
                            semaphore.withPermit {
                                val count = processedCounter.incrementAndGet()
                                onProgress(
                                    IndexProgress(
                                        current = count,
                                        total = totalToIndex,
                                        currentFileName = file.name,
                                        isIndexing = true,
                                        statusMessage = "فهرسة (${count}/$totalToIndex): ${file.name}"
                                    )
                                )
                                // Delete old chunks if updating
                                if (existingMap.containsKey(file.uri.toString())) {
                                    dbHelper.deleteFileChunks(file.uri.toString())
                                }
                                indexSingleFile(workspace.id, file)
                            }
                        }
                    }.awaitAll()
                }
            }

            onProgress(
                IndexProgress(
                    current = totalToIndex,
                    total = totalToIndex,
                    isIndexing = false,
                    statusMessage = "اكتملت مزامنة ${workspace.name}"
                )
            )

            SyncSummary(
                totalFound = discoveredFiles.size,
                indexedCount = totalToIndex,
                skippedCount = skippedCount,
                deletedCount = toDelete.size
            )
        }
    }

    /**
     * Requirement 2 & 3: Incremental differential indexing for ALL workspaces.
     */
    suspend fun syncAllWorkspacesIncremental(
        onProgress: (IndexProgress) -> Unit = {}
    ): SyncSummary = syncMutex.withLock {
        withContext(Dispatchers.IO) {
            val workspaces = dbHelper.getAllWorkspaces()
            var totalSummary = SyncSummary()

            for ((index, ws) in workspaces.withIndex()) {
                onProgress(
                    IndexProgress(
                        current = index,
                        total = workspaces.size,
                        isIndexing = true,
                        statusMessage = "مزامنة [${index + 1}/${workspaces.size}]: ${ws.name}"
                    )
                )

                val uri = Uri.parse(ws.uri)
                val hasPermission = context.contentResolver.persistedUriPermissions.any {
                    it.uri == uri && it.isReadPermission
                }
                if (!hasPermission) {
                    Log.w(TAG, "Skipping workspace without permission: ${ws.name}")
                    continue
                }

                // Call inner logic directly (since mutex is already locked)
                val summary = internalSyncWorkspace(ws, onProgress)
                totalSummary += summary
            }

            onProgress(
                IndexProgress(
                    current = workspaces.size,
                    total = workspaces.size,
                    isIndexing = false,
                    statusMessage = "اكتملت المزامنة لجميع المجلدات"
                )
            )

            totalSummary
        }
    }

    private fun internalSyncWorkspace(
        workspace: Workspace,
        onProgress: (IndexProgress) -> Unit
    ): SyncSummary {
        val uri = Uri.parse(workspace.uri)
        val discoveredFiles = scanDirectoryTree(uri)
        val discoveredMap = discoveredFiles.associateBy { it.uri.toString() }
        val existingMap = dbHelper.getIndexedFiles(workspace.id)

        val toAdd = mutableListOf<DiscoveredFile>()
        val toUpdate = mutableListOf<DiscoveredFile>()
        val toDelete = mutableListOf<String>()
        var skippedCount = 0

        for (discovered in discoveredFiles) {
            val exist = existingMap[discovered.uri.toString()]
            if (exist == null) {
                toAdd.add(discovered)
            } else if (exist.size != discovered.size || exist.lastModified != discovered.lastModified) {
                toUpdate.add(discovered)
            } else {
                skippedCount++
            }
        }

        for (existUri in existingMap.keys) {
            if (!discoveredMap.containsKey(existUri)) {
                toDelete.add(existUri)
            }
        }

        for (delUri in toDelete) {
            dbHelper.deleteFileIndex(delUri)
        }

        val filesToIndex = toAdd + toUpdate
        for ((idx, file) in filesToIndex.withIndex()) {
            onProgress(
                IndexProgress(
                    current = idx + 1,
                    total = filesToIndex.size,
                    currentFileName = file.name,
                    isIndexing = true,
                    statusMessage = "فهرسة (${idx + 1}/${filesToIndex.size}): ${file.name}"
                )
            )
            if (existingMap.containsKey(file.uri.toString())) {
                dbHelper.deleteFileChunks(file.uri.toString())
            }
            indexSingleFile(workspace.id, file)
        }

        return SyncSummary(
            totalFound = discoveredFiles.size,
            indexedCount = filesToIndex.size,
            skippedCount = skippedCount,
            deletedCount = toDelete.size
        )
    }

    private fun indexSingleFile(workspaceId: String, file: DiscoveredFile) {
        try {
            val segments = mutableListOf<FileSegment>()
            val sheetHeaders = mutableMapOf<String, List<String>>()

            context.contentResolver.openInputStream(file.uri)?.use { inputStream ->
                FileParserDispatcher.parseFile(
                    context = context,
                    inputStream = inputStream,
                    fileUri = file.uri.toString(),
                    fileName = file.name,
                    onHeaderFound = { sheetName, headers ->
                        sheetHeaders[sheetName] = headers
                    }
                ) { segment ->
                    segments.add(segment.copy(workspaceId = workspaceId))
                }
            }

            val record = IndexedFileRecord(
                uri = file.uri.toString(),
                workspaceId = workspaceId,
                fileName = file.name,
                mimeType = file.mimeType,
                size = file.size,
                lastModified = file.lastModified
            )

            dbHelper.saveFileAndSegments(record, segments, sheetHeaders)
        } catch (t: Throwable) {
            Log.e(TAG, "Failed indexing file ${file.name} at ${file.uri}", t)
        }
    }

    private fun scanDirectoryTree(treeUri: Uri): List<DiscoveredFile> {
        val results = mutableListOf<DiscoveredFile>()

        try {
            val rootDocId = DocumentsContract.getTreeDocumentId(treeUri)
            scanFolderRecursive(treeUri, rootDocId, results)
        } catch (e: Exception) {
            Log.w(TAG, "Direct DocumentsContract tree scan failed, falling back to DocumentFile", e)
            val docFile = DocumentFile.fromTreeUri(context, treeUri)
            if (docFile != null && docFile.isDirectory) {
                scanDocumentFileRecursive(docFile, results)
            }
        }

        return results
    }

    private fun scanFolderRecursive(
        treeUri: Uri,
        folderDocId: String,
        results: MutableList<DiscoveredFile>
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, folderDocId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )

        try {
            val cursor = context.contentResolver.query(
                childrenUri,
                projection,
                null,
                null,
                null
            )
            cursor?.use {
                val idIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_SIZE)
                val modIdx = it.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED)

                while (it.moveToNext()) {
                    val docId = it.getString(idIdx)
                    val name = it.getString(nameIdx) ?: continue
                    val mime = it.getString(mimeIdx)

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        scanFolderRecursive(treeUri, docId, results)
                    } else {
                        if (FileParserDispatcher.isSupportedFile(name)) {
                            val size = if (it.isNull(sizeIdx)) 0L else it.getLong(sizeIdx)
                            val modified = if (it.isNull(modIdx)) 0L else it.getLong(modIdx)
                            val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                            results.add(
                                DiscoveredFile(
                                    uri = fileUri,
                                    name = name,
                                    size = size,
                                    lastModified = modified,
                                    mimeType = mime
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning folder with docId: $folderDocId", e)
        }
    }

    private fun scanDocumentFileRecursive(
        directory: DocumentFile,
        results: MutableList<DiscoveredFile>
    ) {
        val files = directory.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                scanDocumentFileRecursive(file, results)
            } else {
                val name = file.name ?: continue
                if (FileParserDispatcher.isSupportedFile(name)) {
                    results.add(
                        DiscoveredFile(
                            uri = file.uri,
                            name = name,
                            size = file.length(),
                            lastModified = file.lastModified(),
                            mimeType = file.type
                        )
                    )
                }
            }
        }
    }

    // ==========================================
    // SEARCH & STATS
    // ==========================================

    /**
     * Executes instant FTS search with filter options.
     * Automatically applies active workspace scoping if no specific workspace filter is selected.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter()
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        val activeId = getActiveWorkspaceId()
        val effectiveFilter = if (activeId != null) {
            filter.copy(selectedWorkspaceIds = setOf(activeId))
        } else {
            filter
        }

        dbHelper.search(query, effectiveFilter)
    }

    fun getAllIndexedFiles(workspaceId: String? = null): List<IndexedFileRecord> {
        val targetWsId = workspaceId ?: getActiveWorkspaceId()
        return dbHelper.getIndexedFiles(targetWsId).values.toList()
    }

    fun getIndexStats(workspaceId: String? = null): Pair<Int, Int> {
        val targetWsId = workspaceId ?: getActiveWorkspaceId()
        return dbHelper.getStats(targetWsId)
    }

    fun clearAll() {
        prefs.edit().remove(KEY_FOLDER_URI).remove(KEY_FOLDER_NAME).remove(KEY_ACTIVE_WORKSPACE_ID).apply()
        dbHelper.clearAll()
    }

    fun openFile(fileUriString: String): Result<Unit> {
        return try {
            val uri = Uri.parse(fileUriString)
            val mimeType = context.contentResolver.getType(uri) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Result.success(Unit)
        } catch (e: Exception) {
            try {
                val uri = Uri.parse(fileUriString)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                Result.success(Unit)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Failed to open file: $fileUriString", fallbackEx)
                Result.failure(fallbackEx)
            }
        }
    }
}
