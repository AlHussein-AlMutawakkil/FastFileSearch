package com.example.fastfilesearch.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.model.Workspace
import com.example.fastfilesearch.util.ArabicNormalizer

class SearchDatabaseHelper(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {

    companion object {
        private const val TAG = "SearchDatabaseHelper"
        private const val DATABASE_NAME = "fast_file_search.db"
        // Upgraded to version 4 for Multi-Workspaces support and differential indexing
        private const val DATABASE_VERSION = 4

        const val TABLE_WORKSPACES = "workspaces"
        const val COL_WS_ID = "id"
        const val COL_WS_NAME = "name"
        const val COL_WS_URI = "uri"
        const val COL_WS_ADDED_AT = "added_at"

        const val TABLE_FILES = "indexed_files"
        const val COL_FILE_URI = "file_uri"
        const val COL_FILE_WS_ID = "workspace_id"
        const val COL_FILE_NAME = "file_name"
        const val COL_MIME_TYPE = "mime_type"
        const val COL_FILE_SIZE = "file_size"
        const val COL_LAST_MODIFIED = "last_modified"
        const val COL_INDEXED_AT = "indexed_at"

        const val TABLE_SHEET_HEADERS = "sheet_headers"
        const val COL_SH_FILE_URI = "file_uri"
        const val COL_SH_SHEET_NAME = "sheet_name"
        const val COL_SH_HEADERS_TEXT = "headers_text"

        const val TABLE_FTS = "file_segments_fts"
        const val FTS_COL_WS_ID = "workspace_id"
        const val FTS_COL_URI = "file_uri"
        const val FTS_COL_NAME = "file_name"
        const val FTS_COL_LOCATION = "location"
        const val FTS_COL_CONTENT = "content"
        const val FTS_COL_NORMALIZED = "normalized_content"

        @Volatile
        private var instance: SearchDatabaseHelper? = null

        fun getInstance(context: Context): SearchDatabaseHelper {
            return instance ?: synchronized(this) {
                instance ?: SearchDatabaseHelper(context).also { instance = it }
            }
        }
    }

    @Volatile
    private var isUsingFts5: Boolean? = null

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        try {
            db.enableWriteAheadLogging()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to enable write-ahead logging (WAL)", e)
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        createTables(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        Log.i(TAG, "Upgrading database from $oldVersion to $newVersion")
        try {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_WORKSPACES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SHEET_HEADERS")
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping tables during upgrade", e)
        }
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        // 1. Workspaces table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_WORKSPACES (
                $COL_WS_ID TEXT PRIMARY KEY,
                $COL_WS_NAME TEXT NOT NULL,
                $COL_WS_URI TEXT UNIQUE NOT NULL,
                $COL_WS_ADDED_AT INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // 2. Files metadata table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FILES (
                $COL_FILE_URI TEXT PRIMARY KEY,
                $COL_FILE_WS_ID TEXT NOT NULL,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_MIME_TYPE TEXT,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_LAST_MODIFIED INTEGER NOT NULL,
                $COL_INDEXED_AT INTEGER NOT NULL
            );
            """.trimIndent()
        )

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_files_ws ON $TABLE_FILES ($COL_FILE_WS_ID);")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_files_uri ON $TABLE_FILES ($COL_FILE_URI);")

        // 3. Sheet headers table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_SHEET_HEADERS (
                $COL_SH_FILE_URI TEXT NOT NULL,
                $COL_SH_SHEET_NAME TEXT NOT NULL,
                $COL_SH_HEADERS_TEXT TEXT,
                PRIMARY KEY ($COL_SH_FILE_URI, $COL_SH_SHEET_NAME)
            );
            """.trimIndent()
        )

        // 4. FTS Virtual Table
        val fts5Available = probeFts5(db)

        if (fts5Available) {
            Log.i(TAG, "FTS5 is supported. Creating FTS5 virtual table with workspace_id.")
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts5(
                        $FTS_COL_WS_ID UNINDEXED,
                        $FTS_COL_URI UNINDEXED,
                        $FTS_COL_NAME,
                        $FTS_COL_LOCATION UNINDEXED,
                        $FTS_COL_CONTENT UNINDEXED,
                        $FTS_COL_NORMALIZED,
                        tokenize = 'unicode61'
                    );
                    """.trimIndent()
                )
                isUsingFts5 = true
                return
            } catch (e: Exception) {
                Log.w(TAG, "Failed creating FTS5 table, falling back to FTS4", e)
            }
        }

        // Fallback to FTS4
        Log.i(TAG, "Using FTS4 virtual table with workspace_id.")
        val fts4Unicode61 = probeFts4Unicode61(db)
        val tokenizeClause = if (fts4Unicode61) "tokenize=unicode61" else ""

        val fts4Sql = buildString {
            append("CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts4(")
            append("$FTS_COL_WS_ID, ")
            append("$FTS_COL_URI, ")
            append("$FTS_COL_NAME, ")
            append("$FTS_COL_LOCATION, ")
            append("$FTS_COL_CONTENT, ")
            append("$FTS_COL_NORMALIZED, ")
            append("notindexed=$FTS_COL_WS_ID, ")
            append("notindexed=$FTS_COL_URI, ")
            append("notindexed=$FTS_COL_LOCATION, ")
            append("notindexed=$FTS_COL_CONTENT")
            if (tokenizeClause.isNotEmpty()) {
                append(", $tokenizeClause")
            }
            append(");")
        }

        db.execSQL(fts4Sql)
        isUsingFts5 = false
    }

    private fun probeFts5(db: SQLiteDatabase): Boolean {
        return try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS _probe_fts5 USING fts5(x);")
            db.execSQL("DROP TABLE IF EXISTS _probe_fts5;")
            true
        } catch (e: Exception) {
            Log.d(TAG, "FTS5 probe failed: ${e.message}")
            false
        }
    }

    private fun probeFts4Unicode61(db: SQLiteDatabase): Boolean {
        return try {
            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS _probe_fts4 USING fts4(x, tokenize=unicode61);")
            db.execSQL("DROP TABLE IF EXISTS _probe_fts4;")
            true
        } catch (e: Exception) {
            Log.d(TAG, "FTS4 unicode61 probe failed: ${e.message}")
            false
        }
    }

    private fun checkIsFts5(db: SQLiteDatabase): Boolean {
        isUsingFts5?.let { return it }

        var usingFts5 = false
        try {
            val cursor = db.rawQuery(
                "SELECT sql FROM sqlite_master WHERE type='table' AND name='$TABLE_FTS'",
                null
            )
            cursor.use {
                if (it.moveToFirst()) {
                    val sql = it.getString(0) ?: ""
                    usingFts5 = sql.contains("fts5", ignoreCase = true)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not determine FTS type from sqlite_master", e)
        }

        isUsingFts5 = usingFts5
        return usingFts5
    }

    // ==========================================
    // WORKSPACES CRUD
    // ==========================================

    fun getAllWorkspaces(): List<Workspace> {
        val list = mutableListOf<Workspace>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_WS_ID, $COL_WS_NAME, $COL_WS_URI, $COL_WS_ADDED_AT FROM $TABLE_WORKSPACES ORDER BY $COL_WS_ADDED_AT ASC",
                null
            )
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow(COL_WS_ID)
                val nameIdx = it.getColumnIndexOrThrow(COL_WS_NAME)
                val uriIdx = it.getColumnIndexOrThrow(COL_WS_URI)
                val timeIdx = it.getColumnIndexOrThrow(COL_WS_ADDED_AT)

                while (it.moveToNext()) {
                    list.add(
                        Workspace(
                            id = it.getString(idIdx),
                            name = it.getString(nameIdx),
                            uri = it.getString(uriIdx),
                            addedAt = it.getLong(timeIdx)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading workspaces", e)
        }
        return list
    }

    fun getWorkspace(id: String): Workspace? {
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_WS_ID, $COL_WS_NAME, $COL_WS_URI, $COL_WS_ADDED_AT FROM $TABLE_WORKSPACES WHERE $COL_WS_ID = ?",
                arrayOf(id)
            )
            cursor.use {
                if (it.moveToFirst()) {
                    return Workspace(
                        id = it.getString(0),
                        name = it.getString(1),
                        uri = it.getString(2),
                        addedAt = it.getLong(3)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed getting workspace $id", e)
        }
        return null
    }

    fun insertWorkspace(workspace: Workspace): Boolean {
        val db = writableDatabase
        return try {
            val cv = ContentValues().apply {
                put(COL_WS_ID, workspace.id)
                put(COL_WS_NAME, workspace.name)
                put(COL_WS_URI, workspace.uri)
                put(COL_WS_ADDED_AT, workspace.addedAt)
            }
            db.insertWithOnConflict(TABLE_WORKSPACES, null, cv, SQLiteDatabase.CONFLICT_REPLACE) >= 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed inserting workspace ${workspace.name}", e)
            false
        }
    }

    fun renameWorkspace(id: String, newName: String): Boolean {
        val db = writableDatabase
        return try {
            val cv = ContentValues().apply {
                put(COL_WS_NAME, newName)
            }
            db.update(TABLE_WORKSPACES, cv, "$COL_WS_ID = ?", arrayOf(id)) > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed renaming workspace $id", e)
            false
        }
    }

    fun deleteWorkspace(workspaceId: String): Boolean {
        val db = writableDatabase
        db.beginTransaction()
        return try {
            // Delete FTS entries
            db.delete(TABLE_FTS, "$FTS_COL_WS_ID = ?", arrayOf(workspaceId))

            // Delete Sheet Headers for files in this workspace
            db.execSQL(
                "DELETE FROM $TABLE_SHEET_HEADERS WHERE $COL_SH_FILE_URI IN (SELECT $COL_FILE_URI FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?)",
                arrayOf(workspaceId)
            )

            // Delete indexed files
            db.delete(TABLE_FILES, "$COL_FILE_WS_ID = ?", arrayOf(workspaceId))

            // Delete workspace
            val count = db.delete(TABLE_WORKSPACES, "$COL_WS_ID = ?", arrayOf(workspaceId))
            db.setTransactionSuccessful()
            count > 0
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting workspace $workspaceId", e)
            false
        } finally {
            db.endTransaction()
        }
    }

    // ==========================================
    // FILES & CHUNKS MANAGEMENT
    // ==========================================

    fun getIndexedFiles(workspaceId: String? = null): Map<String, IndexedFileRecord> {
        val result = mutableMapOf<String, IndexedFileRecord>()
        try {
            val db = readableDatabase
            val sql = if (workspaceId != null) {
                "SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?"
            } else {
                "SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT FROM $TABLE_FILES"
            }
            val args = if (workspaceId != null) arrayOf(workspaceId) else null
            val cursor = db.rawQuery(sql, args)
            cursor.use {
                val uriIdx = it.getColumnIndexOrThrow(COL_FILE_URI)
                val wsIdx = it.getColumnIndexOrThrow(COL_FILE_WS_ID)
                val nameIdx = it.getColumnIndexOrThrow(COL_FILE_NAME)
                val mimeIdx = it.getColumnIndexOrThrow(COL_MIME_TYPE)
                val sizeIdx = it.getColumnIndexOrThrow(COL_FILE_SIZE)
                val modIdx = it.getColumnIndexOrThrow(COL_LAST_MODIFIED)
                val timeIdx = it.getColumnIndexOrThrow(COL_INDEXED_AT)

                while (it.moveToNext()) {
                    val uri = it.getString(uriIdx)
                    result[uri] = IndexedFileRecord(
                        uri = uri,
                        workspaceId = it.getString(wsIdx),
                        fileName = it.getString(nameIdx),
                        mimeType = if (it.isNull(mimeIdx)) null else it.getString(mimeIdx),
                        size = it.getLong(sizeIdx),
                        lastModified = it.getLong(modIdx),
                        indexedAt = it.getLong(timeIdx)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading indexed files", e)
        }
        return result
    }

    /**
     * Deletes only the search chunks/FTS records and sheet headers for a single file (for re-indexing).
     */
    fun deleteFileChunks(fileUri: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FTS, "$FTS_COL_URI = ?", arrayOf(fileUri))
            db.delete(TABLE_SHEET_HEADERS, "$COL_SH_FILE_URI = ?", arrayOf(fileUri))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed deleting file chunks for $fileUri", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Removes all indexing records, segments, and sheet headers for a specific file URI.
     */
    fun deleteFileIndex(fileUri: String) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FILES, "$COL_FILE_URI = ?", arrayOf(fileUri))
            db.delete(TABLE_FTS, "$FTS_COL_URI = ?", arrayOf(fileUri))
            db.delete(TABLE_SHEET_HEADERS, "$COL_SH_FILE_URI = ?", arrayOf(fileUri))
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete file index for $fileUri", e)
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Saves a file record and batch-inserts all its extracted segments and sheet headers in a single fast transaction.
     */
    fun saveFileAndSegments(
        record: IndexedFileRecord,
        segments: List<FileSegment>,
        sheetHeaders: Map<String, List<String>> = emptyMap()
    ) {
        val db = writableDatabase
        db.beginTransaction()
        var statement: SQLiteStatement? = null
        var shStatement: SQLiteStatement? = null
        try {
            // 1. Delete previous records for this file if existing
            db.delete(TABLE_FILES, "$COL_FILE_URI = ?", arrayOf(record.uri))
            db.delete(TABLE_FTS, "$FTS_COL_URI = ?", arrayOf(record.uri))
            db.delete(TABLE_SHEET_HEADERS, "$COL_SH_FILE_URI = ?", arrayOf(record.uri))

            // 2. Insert into indexed_files
            val cv = ContentValues().apply {
                put(COL_FILE_URI, record.uri)
                put(COL_FILE_WS_ID, record.workspaceId)
                put(COL_FILE_NAME, record.fileName)
                put(COL_MIME_TYPE, record.mimeType)
                put(COL_FILE_SIZE, record.size)
                put(COL_LAST_MODIFIED, record.lastModified)
                put(COL_INDEXED_AT, record.indexedAt)
            }
            db.insertWithOnConflict(TABLE_FILES, null, cv, SQLiteDatabase.CONFLICT_REPLACE)

            // 3. Insert sheet headers
            if (sheetHeaders.isNotEmpty()) {
                val shInsertSql = """
                    INSERT OR REPLACE INTO $TABLE_SHEET_HEADERS ($COL_SH_FILE_URI, $COL_SH_SHEET_NAME, $COL_SH_HEADERS_TEXT)
                    VALUES (?, ?, ?)
                """.trimIndent()
                shStatement = db.compileStatement(shInsertSql)
                for ((sheetName, headersList) in sheetHeaders) {
                    shStatement.clearBindings()
                    shStatement.bindString(1, record.uri)
                    shStatement.bindString(2, sheetName)
                    shStatement.bindString(3, headersList.joinToString("\t"))
                    shStatement.executeInsert()
                }
            }

            // 4. Batch insert segments into FTS
            val normalizedFileName = ArabicNormalizer.normalize(record.fileName)
            val insertSql = """
                INSERT INTO $TABLE_FTS ($FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT, $FTS_COL_NORMALIZED)
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
            statement = db.compileStatement(insertSql)

            if (segments.isNotEmpty()) {
                for (segment in segments) {
                    statement.clearBindings()
                    statement.bindString(1, record.workspaceId)
                    statement.bindString(2, segment.fileUri)
                    statement.bindString(3, segment.fileName)
                    statement.bindString(4, segment.location)
                    statement.bindString(5, segment.content)

                    val combinedNormalized = if (normalizedFileName.isNotEmpty()) {
                        "$normalizedFileName ${segment.normalizedContent}"
                    } else {
                        segment.normalizedContent
                    }
                    statement.bindString(6, combinedNormalized)
                    statement.executeInsert()
                }
            } else {
                // File with 0 segments: insert file record entry
                statement.clearBindings()
                statement.bindString(1, record.workspaceId)
                statement.bindString(2, record.uri)
                statement.bindString(3, record.fileName)
                statement.bindString(4, "ملف")
                statement.bindString(5, record.fileName)
                statement.bindString(6, normalizedFileName)
                statement.executeInsert()
            }

            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file and segments for ${record.fileName}", e)
        } finally {
            statement?.close()
            shStatement?.close()
            db.endTransaction()
        }
    }

    private data class SearchHit(
        val id: Long,
        val workspaceId: String,
        val fileUri: String,
        val fileName: String,
        val location: String,
        val content: String
    )

    private fun parseSheetAndRow(location: String): Pair<String?, Int?> {
        var sheet: String? = null
        var row: Int? = null

        if (location.contains("ورقة:")) {
            val parts = location.split("،")
            sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim()
            val rowPart = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim()
            row = rowPart?.toIntOrNull()
        } else if (location.contains("سطر")) {
            row = location.replace("سطر", "").replace(":", "").trim().toIntOrNull()
        } else if (location.contains("فقرة")) {
            row = location.replace("فقرة", "").trim().toIntOrNull()
        }

        return Pair(sheet, row)
    }

    private fun getSheetHeadersForFiles(
        db: SQLiteDatabase,
        fileUris: Set<String>
    ): Map<Pair<String, String>, List<String>> {
        if (fileUris.isEmpty()) return emptyMap()
        val headersMap = mutableMapOf<Pair<String, String>, List<String>>()

        fileUris.chunked(500).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val sql = """
                SELECT $COL_SH_FILE_URI, $COL_SH_SHEET_NAME, $COL_SH_HEADERS_TEXT 
                FROM $TABLE_SHEET_HEADERS 
                WHERE $COL_SH_FILE_URI IN ($placeholders)
            """.trimIndent()
            val cursor = db.rawQuery(sql, chunk.toTypedArray())
            cursor.use {
                val uriIdx = it.getColumnIndexOrThrow(COL_SH_FILE_URI)
                val sheetIdx = it.getColumnIndexOrThrow(COL_SH_SHEET_NAME)
                val textIdx = it.getColumnIndexOrThrow(COL_SH_HEADERS_TEXT)

                while (it.moveToNext()) {
                    val uri = it.getString(uriIdx)
                    val sheet = it.getString(sheetIdx)
                    val text = if (it.isNull(textIdx)) "" else it.getString(textIdx)
                    val list = if (text.isNotEmpty()) text.split("\t") else emptyList()
                    headersMap[Pair(uri, sheet)] = list
                }
            }
        }

        return headersMap
    }

    private fun enrichSearchResults(
        db: SQLiteDatabase,
        hits: List<SearchHit>,
        rawQuery: String
    ): List<SearchResult> {
        if (hits.isEmpty()) return emptyList()

        val fileUris = hits.map { it.fileUri }.toSet()
        val headersMap = getSheetHeadersForFiles(db, fileUris)

        return hits.map { hit ->
            val (sheet, row) = parseSheetAndRow(hit.location)
            val headers = if (sheet != null) {
                headersMap[Pair(hit.fileUri, sheet)] ?: emptyList()
            } else {
                emptyList()
            }

            SearchResult(
                id = hit.id,
                fileUri = hit.fileUri,
                workspaceId = hit.workspaceId,
                fileName = hit.fileName,
                location = hit.location,
                snippet = hit.content,
                query = rawQuery.trim(),
                sheetName = sheet,
                rowNumber = row,
                columnHeaders = headers
            )
        }
    }

    /**
     * Executes search with filtering by Workspace IDs, File URIs, Extensions, and Search Method.
     */
    fun search(
        rawQuery: String,
        filter: SearchFilter = SearchFilter(),
        limit: Int = 150
    ): List<SearchResult> {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery).trim()
        if (normalizedQuery.isBlank()) return emptyList()

        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val db = readableDatabase
        val fts5 = checkIsFts5(db)

        val additionalWhere = mutableListOf<String>()
        val additionalArgs = mutableListOf<String>()

        // 1. Workspace filter
        if (filter.selectedWorkspaceIds.isNotEmpty()) {
            val placeholders = filter.selectedWorkspaceIds.joinToString(",") { "?" }
            additionalWhere.add("$FTS_COL_WS_ID IN ($placeholders)")
            additionalArgs.addAll(filter.selectedWorkspaceIds)
        }

        // 2. File URI filter
        if (filter.selectedFileUris.isNotEmpty()) {
            val placeholders = filter.selectedFileUris.joinToString(",") { "?" }
            additionalWhere.add("$FTS_COL_URI IN ($placeholders)")
            additionalArgs.addAll(filter.selectedFileUris)
        }

        // 3. Extension filter
        if (filter.selectedExtensions.isNotEmpty()) {
            val extConditions = filter.selectedExtensions.joinToString(" OR ") {
                "$FTS_COL_NAME LIKE ?"
            }
            additionalWhere.add("($extConditions)")
            filter.selectedExtensions.forEach { ext ->
                additionalArgs.add("%.$ext")
            }
        }

        val rawHits = mutableListOf<SearchHit>()

        if (filter.method == SearchMethod.CONTAINS) {
            val instrConditions = tokens.map { "instr($FTS_COL_NORMALIZED, ?) > 0" }
            val allWhereClauses = (instrConditions + additionalWhere).joinToString(" AND ")
            val sql = """
                SELECT rowid, $FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                FROM $TABLE_FTS
                WHERE $allWhereClauses
                LIMIT ?
            """.trimIndent()
            val queryArgs = (tokens + additionalArgs + listOf(limit.toString())).toTypedArray()

            try {
                val cursor = db.rawQuery(sql, queryArgs)
                cursor.use {
                    val idIdx = it.getColumnIndexOrThrow("rowid")
                    val wsIdx = it.getColumnIndexOrThrow(FTS_COL_WS_ID)
                    val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                    val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                    val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                    val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                    while (it.moveToNext()) {
                        rawHits.add(
                            SearchHit(
                                id = it.getLong(idIdx),
                                workspaceId = it.getString(wsIdx),
                                fileUri = it.getString(uriIdx),
                                fileName = it.getString(nameIdx),
                                location = it.getString(locIdx),
                                content = it.getString(contentIdx)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "CONTAINS query failed", e)
            }
        } else {
            val matchExpr = when (filter.method) {
                SearchMethod.EXACT_PHRASE -> "\"${tokens.joinToString(" ")}\""
                SearchMethod.WHOLE_WORD -> if (fts5) tokens.joinToString(" ") { "\"$it\"" } else tokens.joinToString(" ") { it }
                SearchMethod.ALL_WORDS, SearchMethod.CONTAINS -> if (fts5) tokens.joinToString(" ") { "\"$it\"*" } else tokens.joinToString(" ") { "$it*" }
            }

            val ftsWhereClauses = mutableListOf<String>()
            val ftsArgs = mutableListOf<String>()

            ftsWhereClauses.add("$TABLE_FTS MATCH ?")
            ftsArgs.add(matchExpr)

            ftsWhereClauses.addAll(additionalWhere)
            ftsArgs.addAll(additionalArgs)

            val whereClause = ftsWhereClauses.joinToString(" AND ")
            val orderClause = if (fts5) "ORDER BY bm25($TABLE_FTS)" else ""

            val sql = """
                SELECT rowid, $FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                FROM $TABLE_FTS
                WHERE $whereClause
                $orderClause
                LIMIT ?
            """.trimIndent()

            ftsArgs.add(limit.toString())

            try {
                val cursor = db.rawQuery(sql, ftsArgs.toTypedArray())
                cursor.use {
                    val idIdx = it.getColumnIndexOrThrow("rowid")
                    val wsIdx = it.getColumnIndexOrThrow(FTS_COL_WS_ID)
                    val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                    val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                    val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                    val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                    while (it.moveToNext()) {
                        rawHits.add(
                            SearchHit(
                                id = it.getLong(idIdx),
                                workspaceId = it.getString(wsIdx),
                                fileUri = it.getString(uriIdx),
                                fileName = it.getString(nameIdx),
                                location = it.getString(locIdx),
                                content = it.getString(contentIdx)
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Primary FTS query failed for expr [$matchExpr], trying fallback", e)
                try {
                    val fallbackExpr = tokens.joinToString(" ")
                    val fallbackWhere = (listOf("$TABLE_FTS MATCH ?") + additionalWhere).joinToString(" AND ")
                    val fallbackArgs = (listOf(fallbackExpr) + additionalArgs + listOf(limit.toString())).toTypedArray()
                    val fallbackSql = """
                        SELECT rowid, $FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                        FROM $TABLE_FTS
                        WHERE $fallbackWhere
                        LIMIT ?
                    """.trimIndent()

                    val cursor = db.rawQuery(fallbackSql, fallbackArgs)
                    cursor.use {
                        val idIdx = it.getColumnIndexOrThrow("rowid")
                        val wsIdx = it.getColumnIndexOrThrow(FTS_COL_WS_ID)
                        val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                        val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                        val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                        val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                        while (it.moveToNext()) {
                            rawHits.add(
                                SearchHit(
                                    id = it.getLong(idIdx),
                                    workspaceId = it.getString(wsIdx),
                                    fileUri = it.getString(uriIdx),
                                    fileName = it.getString(nameIdx),
                                    location = it.getString(locIdx),
                                    content = it.getString(contentIdx)
                                )
                            )
                        }
                    }
                } catch (fallbackEx: Exception) {
                    Log.e(TAG, "Search fallback failed", fallbackEx)
                }
            }
        }

        var processedHits: List<SearchHit> = rawHits

        if (filter.method == SearchMethod.EXACT_PHRASE) {
            processedHits = processedHits.filter { hit ->
                val normContent = ArabicNormalizer.normalize(hit.content)
                val normName = ArabicNormalizer.normalize(hit.fileName)
                normContent.contains(normalizedQuery) || normName.contains(normalizedQuery)
            }
        } else if (filter.method == SearchMethod.WHOLE_WORD) {
            processedHits = processedHits.filter { hit ->
                val normContent = ArabicNormalizer.normalize(hit.content)
                val normName = ArabicNormalizer.normalize(hit.fileName)
                tokens.all { token ->
                    val regex = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(token)}(?![\\p{L}\\p{Nd}])")
                    regex.containsMatchIn(normContent) || regex.containsMatchIn(normName)
                }
            }
        }

        if (!filter.ignoreDiacritics) {
            val rawTrimmed = rawQuery.trim()
            val rawTokens = rawTrimmed.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
            processedHits = processedHits.filter { hit ->
                when (filter.method) {
                    SearchMethod.EXACT_PHRASE -> {
                        hit.content.contains(rawTrimmed) || hit.fileName.contains(rawTrimmed)
                    }
                    SearchMethod.WHOLE_WORD -> {
                        rawTokens.all { token ->
                            val regex = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(token)}(?![\\p{L}\\p{Nd}])")
                            regex.containsMatchIn(hit.content) || regex.containsMatchIn(hit.fileName)
                        }
                    }
                    SearchMethod.CONTAINS, SearchMethod.ALL_WORDS -> {
                        rawTokens.all { hit.content.contains(it) || hit.fileName.contains(it) }
                    }
                }
            }
        }

        return enrichSearchResults(db, processedHits, rawQuery)
    }

    /**
     * Clears all indexed data, workspaces, and sheet headers.
     */
    fun clearAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_WORKSPACES, null, null)
            db.delete(TABLE_FILES, null, null)
            db.delete(TABLE_FTS, null, null)
            db.delete(TABLE_SHEET_HEADERS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Returns the count of indexed files and segments for a specific workspace or all.
     */
    fun getStats(workspaceId: String? = null): Pair<Int, Int> {
        var filesCount = 0
        var segmentsCount = 0

        try {
            val db = readableDatabase
            val filesSql = if (workspaceId != null) {
                "SELECT COUNT(*) FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?"
            } else {
                "SELECT COUNT(*) FROM $TABLE_FILES"
            }
            val segmentsSql = if (workspaceId != null) {
                "SELECT COUNT(*) FROM $TABLE_FTS WHERE $FTS_COL_WS_ID = ?"
            } else {
                "SELECT COUNT(*) FROM $TABLE_FTS"
            }
            val args = if (workspaceId != null) arrayOf(workspaceId) else null

            db.rawQuery(filesSql, args).use {
                if (it.moveToFirst()) filesCount = it.getInt(0)
            }
            db.rawQuery(segmentsSql, args).use {
                if (it.moveToFirst()) segmentsCount = it.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading stats", e)
        }

        return Pair(filesCount, segmentsCount)
    }
}
