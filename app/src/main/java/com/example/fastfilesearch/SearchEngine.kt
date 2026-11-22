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
import com.example.fastfilesearch.parser.FileParserDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

data class IndexProgress(
    val current: Int = 0,
    val total: Int = 0,
    val currentFileName: String = "",
    val isIndexing: Boolean = false,
    val statusMessage: String = ""
)

data class SyncSummary(
    val totalFound: Int,
    val indexedCount: Int,
    val skippedCount: Int,
    val deletedCount: Int
)

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

    fun getSavedFolderUri(): Uri? {
        val uriStr = prefs.getString(KEY_FOLDER_URI, null) ?: return null
        return try {
            Uri.parse(uriStr)
        } catch (e: Exception) {
            null
        }
    }

    fun getSavedFolderName(): String {
        return prefs.getString(KEY_FOLDER_NAME, "") ?: ""
    }

    fun saveFolderUri(uri: Uri, folderName: String = "") {
        // If folder changed to a completely different URI, clear the old index at once
        val previousUriStr = prefs.getString(KEY_FOLDER_URI, null)
        if (previousUriStr != null && previousUriStr != uri.toString()) {
            Log.i(TAG, "Target folder changed from $previousUriStr to $uri. Clearing entire index cache.")
            dbHelper.clearAll()
        }

        // Persist SAF permissions
        try {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri, flags)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to take persistable URI permission", e)
        }

        val resolvedName = if (folderName.isNotBlank()) {
            folderName
        } else {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            docFile?.name ?: uri.lastPathSegment ?: "مجلد محدد"
        }

        prefs.edit()
            .putString(KEY_FOLDER_URI, uri.toString())
            .putString(KEY_FOLDER_NAME, resolvedName)
            .apply()
    }

    fun clearSavedFolder() {
        prefs.edit().remove(KEY_FOLDER_URI).remove(KEY_FOLDER_NAME).apply()
        dbHelper.clearAll()
    }

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

    /**
     * Executes Delta Sync:
     * 1. Scans folder tree.
     * 2. Compares signature (uri, size, lastModified) against DB.
     * 3. Deletes records of removed files (clears all at once if no overlap).
     * 4. Indexes new or modified files in parallel (4 files concurrently).
     * 5. Skips unchanged files (0 I/O).
     */
    suspend fun syncFolder(
        treeUri: Uri,
        onProgress: (IndexProgress) -> Unit = {}
    ): SyncSummary = withContext(Dispatchers.IO) {
        onProgress(IndexProgress(isIndexing = true, statusMessage = "جاري مسح المجلد..."))

        // 1. Discover all candidate files
        val discoveredFiles = scanDirectoryTree(treeUri)
        val discoveredMap = discoveredFiles.associateBy { it.uri.toString() }

        // 2. Fetch existing indexed files footprint from SQLite
        val indexedMap = dbHelper.getAllIndexedFiles()

        // 3. Remove deleted files from DB (clear all at once if completely different folder)
        var deletedCount = 0
        if (indexedMap.isNotEmpty() && discoveredFiles.isNotEmpty() && discoveredMap.keys.none { indexedMap.containsKey(it) }) {
            Log.i(TAG, "No overlap with previous indexed files. Clearing old index all at once.")
            deletedCount = indexedMap.size
            dbHelper.clearAll()
        } else {
            for ((existingUri, _) in indexedMap) {
                if (!discoveredMap.containsKey(existingUri)) {
                    dbHelper.deleteFileIndex(existingUri)
                    deletedCount++
                }
            }
        }

        // 4. Identify new and modified files
        val filesToIndex = mutableListOf<DiscoveredFile>()
        var skippedCount = 0

        for (file in discoveredFiles) {
            val existing = indexedMap[file.uri.toString()]
            if (existing != null && existing.size == file.size && existing.lastModified == file.lastModified) {
                skippedCount++
            } else {
                filesToIndex.add(file)
            }
        }

        val totalToIndex = filesToIndex.size
        val processedCounter = AtomicInteger(0)

        if (totalToIndex > 0) {
            onProgress(
                IndexProgress(
                    current = 0,
                    total = totalToIndex,
                    currentFileName = "",
                    isIndexing = true,
                    statusMessage = "فهرسة $totalToIndex ملف..."
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

                            indexSingleFile(file)
                        }
                    }
                }.awaitAll()
            }
        }

        onProgress(
            IndexProgress(
                current = totalToIndex,
                total = totalToIndex,
                currentFileName = "",
                isIndexing = false,
                statusMessage = "اكتملت المزامنة"
            )
        )

        SyncSummary(
            totalFound = discoveredFiles.size,
            indexedCount = totalToIndex,
            skippedCount = skippedCount,
            deletedCount = deletedCount
        )
    }

    private fun indexSingleFile(file: DiscoveredFile) {
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
                    segments.add(segment)
                }
            }

            val record = IndexedFileRecord(
                uri = file.uri.toString(),
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
                        // Ignore temporary Office files (~$) and unsupported extensions
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

    /**
     * Executes instant FTS search with filter options.
     */
    suspend fun search(
        query: String,
        filter: SearchFilter = SearchFilter()
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        dbHelper.search(query, filter)
    }

    /**
     * Returns all currently indexed files.
     */
    fun getAllIndexedFiles(): List<IndexedFileRecord> {
        return dbHelper.getAllIndexedFiles().values.toList()
    }

    /**
     * Opens the file in its default external application.
     */
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
            // Fallback to generic */* type
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

    fun getIndexStats(): Pair<Int, Int> {
        return dbHelper.getStats()
    }
}
