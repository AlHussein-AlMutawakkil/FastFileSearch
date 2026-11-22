package com.example.fastfilesearch.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchResult
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
        // Upgraded to version 3 for WAL mode, sheet_headers table, and Arabic filename indexing
        private const val DATABASE_VERSION = 3

        private const val TABLE_FILES = "indexed_files"
        private const val COL_FILE_URI = "file_uri"
        private const val COL_FILE_NAME = "file_name"
        private const val COL_MIME_TYPE = "mime_type"
        private const val COL_FILE_SIZE = "file_size"
        private const val COL_LAST_MODIFIED = "last_modified"
        private const val COL_INDEXED_AT = "indexed_at"

        private const val TABLE_SHEET_HEADERS = "sheet_headers"
        private const val COL_SH_FILE_URI = "file_uri"
        private const val COL_SH_SHEET_NAME = "sheet_name"
        private const val COL_SH_HEADERS_TEXT = "headers_text"

        private const val TABLE_FTS = "file_segments_fts"
        private const val FTS_COL_URI = "file_uri"
        private const val FTS_COL_NAME = "file_name"
        private const val FTS_COL_LOCATION = "location"
        private const val FTS_COL_CONTENT = "content"
        private const val FTS_COL_NORMALIZED = "normalized_content"

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
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FILES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FTS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_SHEET_HEADERS")
        } catch (e: Exception) {
            Log.e(TAG, "Error dropping tables during upgrade", e)
        }
        createTables(db)
    }

    private fun createTables(db: SQLiteDatabase) {
        // 1. Files metadata table
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_FILES (
                $COL_FILE_URI TEXT PRIMARY KEY,
                $COL_FILE_NAME TEXT NOT NULL,
                $COL_MIME_TYPE TEXT,
                $COL_FILE_SIZE INTEGER NOT NULL,
                $COL_LAST_MODIFIED INTEGER NOT NULL,
                $COL_INDEXED_AT INTEGER NOT NULL
            );
            """.trimIndent()
        )

        // 2. Sheet headers table
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

        // 3. Determine FTS module availability
        val fts5Available = probeFts5(db)

        if (fts5Available) {
            Log.i(TAG, "FTS5 is supported. Creating FTS5 virtual table.")
            try {
                db.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts5(
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
                Log.w(TAG, "Failed creating FTS5 table despite probe, falling back to FTS4", e)
            }
        }

        // Fallback to FTS4
        Log.i(TAG, "Using FTS4 virtual table for maximum device compatibility.")
        val fts4Unicode61 = probeFts4Unicode61(db)
        val tokenizeClause = if (fts4Unicode61) "tokenize=unicode61" else ""

        val fts4Sql = buildString {
            append("CREATE VIRTUAL TABLE IF NOT EXISTS $TABLE_FTS USING fts4(")
            append("$FTS_COL_URI, ")
            append("$FTS_COL_NAME, ")
            append("$FTS_COL_LOCATION, ")
            append("$FTS_COL_CONTENT, ")
            append("$FTS_COL_NORMALIZED, ")
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

    /**
     * Retrieves all indexed files to compare file footprint (uri, size, lastModified) for Delta Sync.
     */
    fun getAllIndexedFiles(): Map<String, IndexedFileRecord> {
        val result = mutableMapOf<String, IndexedFileRecord>()
        try {
            val db = readableDatabase
            val cursor = db.rawQuery(
                "SELECT $COL_FILE_URI, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT FROM $TABLE_FILES",
                null
            )
            cursor.use {
                val uriIdx = it.getColumnIndexOrThrow(COL_FILE_URI)
                val nameIdx = it.getColumnIndexOrThrow(COL_FILE_NAME)
                val mimeIdx = it.getColumnIndexOrThrow(COL_MIME_TYPE)
                val sizeIdx = it.getColumnIndexOrThrow(COL_FILE_SIZE)
                val modIdx = it.getColumnIndexOrThrow(COL_LAST_MODIFIED)
                val timeIdx = it.getColumnIndexOrThrow(COL_INDEXED_AT)

                while (it.moveToNext()) {
                    val uri = it.getString(uriIdx)
                    result[uri] = IndexedFileRecord(
                        uri = uri,
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
     * Normalized filename is indexed along with segment contents so that Arabic queries match filenames uniformly.
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
                INSERT INTO $TABLE_FTS ($FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT, $FTS_COL_NORMALIZED)
                VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
            statement = db.compileStatement(insertSql)

            if (segments.isNotEmpty()) {
                for (segment in segments) {
                    statement.clearBindings()
                    statement.bindString(1, segment.fileUri)
                    statement.bindString(2, segment.fileName)
                    statement.bindString(3, segment.location)
                    statement.bindString(4, segment.content)

                    // Normalize filename inside normalized_content so Arabic search matches filenames
                    val combinedNormalized = if (normalizedFileName.isNotEmpty()) {
                        "$normalizedFileName ${segment.normalizedContent}"
                    } else {
                        segment.normalizedContent
                    }
                    statement.bindString(5, combinedNormalized)
                    statement.executeInsert()
                }
            } else {
                // File with 0 segments (e.g. empty document): insert file record segment
                statement.clearBindings()
                statement.bindString(1, record.uri)
                statement.bindString(2, record.fileName)
                statement.bindString(3, "ملف")
                statement.bindString(4, record.fileName)
                statement.bindString(5, normalizedFileName)
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
     * Executes FTS search using normalized query terms.
     * Uses BM25 ranking when FTS5 is active, or standard FTS matching when FTS4 is active.
     * Enriches results with sheet headers from sheet_headers table and extracts sheetName and rowNumber.
     */
    fun search(rawQuery: String, limit: Int = 150): List<SearchResult> {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery)
        if (normalizedQuery.isBlank()) return emptyList()

        // Clean tokens from any punctuation to guarantee valid FTS query syntax
        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        val db = readableDatabase
        val fts5 = checkIsFts5(db)

        val matchExpr = if (fts5) {
            tokens.joinToString(" ") { "\"$it\"*" }
        } else {
            tokens.joinToString(" ") { "$it*" }
        }

        val sql = if (fts5) {
            """
            SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
            FROM $TABLE_FTS
            WHERE $TABLE_FTS MATCH ?
            ORDER BY bm25($TABLE_FTS)
            LIMIT ?
            """.trimIndent()
        } else {
            """
            SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
            FROM $TABLE_FTS
            WHERE $TABLE_FTS MATCH ?
            LIMIT ?
            """.trimIndent()
        }

        val hits = mutableListOf<SearchHit>()
        try {
            val cursor = db.rawQuery(sql, arrayOf(matchExpr, limit.toString()))
            cursor.use {
                val idIdx = it.getColumnIndexOrThrow("rowid")
                val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                while (it.moveToNext()) {
                    hits.add(
                        SearchHit(
                            id = it.getLong(idIdx),
                            fileUri = it.getString(uriIdx),
                            fileName = it.getString(nameIdx),
                            location = it.getString(locIdx),
                            content = it.getString(contentIdx)
                        )
                    )
                }
            }
            return enrichSearchResults(db, hits, rawQuery)
        } catch (e: Exception) {
            Log.w(TAG, "Primary FTS query failed for expr [$matchExpr], trying simple token match", e)
            try {
                // Fallback: simple bare token without asterisk
                val fallbackExpr = tokens.joinToString(" ")
                val fallbackSql = """
                    SELECT rowid, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                    FROM $TABLE_FTS
                    WHERE $TABLE_FTS MATCH ?
                    LIMIT ?
                """.trimIndent()

                val fallbackHits = mutableListOf<SearchHit>()
                val cursor = db.rawQuery(fallbackSql, arrayOf(fallbackExpr, limit.toString()))
                cursor.use {
                    val idIdx = it.getColumnIndexOrThrow("rowid")
                    val uriIdx = it.getColumnIndexOrThrow(FTS_COL_URI)
                    val nameIdx = it.getColumnIndexOrThrow(FTS_COL_NAME)
                    val locIdx = it.getColumnIndexOrThrow(FTS_COL_LOCATION)
                    val contentIdx = it.getColumnIndexOrThrow(FTS_COL_CONTENT)

                    while (it.moveToNext()) {
                        fallbackHits.add(
                            SearchHit(
                                id = it.getLong(idIdx),
                                fileUri = it.getString(uriIdx),
                                fileName = it.getString(nameIdx),
                                location = it.getString(locIdx),
                                content = it.getString(contentIdx)
                            )
                        )
                    }
                }
                return enrichSearchResults(db, fallbackHits, rawQuery)
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Search fallback failed", fallbackEx)
            }
        }

        return emptyList()
    }

    /**
     * Clears all indexed data and sheet headers.
     */
    fun clearAll() {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete(TABLE_FILES, null, null)
            db.delete(TABLE_FTS, null, null)
            db.delete(TABLE_SHEET_HEADERS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /**
     * Returns the count of indexed files and segments safely.
     */
    fun getStats(): Pair<Int, Int> {
        var filesCount = 0
        var segmentsCount = 0

        try {
            val db = readableDatabase
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_FILES", null).use {
                if (it.moveToFirst()) filesCount = it.getInt(0)
            }
            db.rawQuery("SELECT COUNT(*) FROM $TABLE_FTS", null).use {
                if (it.moveToFirst()) segmentsCount = it.getInt(0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed reading stats", e)
        }

        return Pair(filesCount, segmentsCount)
    }
}
