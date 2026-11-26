package com.example.fastfilesearch.desktop.database

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.model.WorkspaceDeleteStats
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import java.io.Closeable
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Local offline SQLite + FTS5 search database powered by org.xerial:sqlite-jdbc.
 * Mirrors :app SearchDatabaseHelper schema and Arabic search behavior.
 */
class DesktopDatabaseHelper(private val dbFile: File) : Closeable {

    companion object {
        const val TABLE_WORKSPACES = "workspaces"
        const val COL_WS_ID = "id"
        const val COL_WS_NAME = "name"
        const val COL_WS_PATH = "path"
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
    }

    private val lock = Any()
    private var connection: Connection? = null

    init {
        dbFile.parentFile?.mkdirs()
        synchronized(lock) {
            ensureInitialized()
        }
    }

    private fun getConnection(): Connection {
        val current = connection
        if (current != null && !current.isClosed) return current
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        conn.createStatement().use { st ->
            st.execute("PRAGMA journal_mode=WAL;")
            st.execute("PRAGMA synchronous=NORMAL;")
            st.execute("PRAGMA busy_timeout=5000;")
        }
        connection = conn
        return conn
    }

    private fun ensureInitialized() {
        val conn = getConnection()
        conn.createStatement().use { st ->
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_WORKSPACES (
                    $COL_WS_ID TEXT PRIMARY KEY,
                    $COL_WS_NAME TEXT NOT NULL,
                    $COL_WS_PATH TEXT UNIQUE NOT NULL,
                    $COL_WS_ADDED_AT INTEGER NOT NULL
                );
                """.trimIndent()
            )
            st.execute(
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
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_ws ON $TABLE_FILES ($COL_FILE_WS_ID);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_uri ON $TABLE_FILES ($COL_FILE_URI);")
            st.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_SHEET_HEADERS (
                    $COL_SH_FILE_URI TEXT NOT NULL,
                    $COL_SH_SHEET_NAME TEXT NOT NULL,
                    $COL_SH_HEADERS_TEXT TEXT,
                    PRIMARY KEY ($COL_SH_FILE_URI, $COL_SH_SHEET_NAME)
                );
                """.trimIndent()
            )
            st.execute(
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
        }
    }

    // --- Workspaces ---

    fun insertWorkspace(workspace: Workspace) = synchronized(lock) {
        val conn = getConnection()
        conn.prepareStatement(
            """
            INSERT OR REPLACE INTO $TABLE_WORKSPACES ($COL_WS_ID, $COL_WS_NAME, $COL_WS_PATH, $COL_WS_ADDED_AT)
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, workspace.id)
            ps.setString(2, workspace.name)
            ps.setString(3, workspace.path)
            ps.setLong(4, workspace.addedAt)
            ps.executeUpdate()
        }
    }

    fun getAllWorkspaces(): List<Workspace> = synchronized(lock) {
        val list = mutableListOf<Workspace>()
        val conn = getConnection()
        conn.prepareStatement(
            "SELECT $COL_WS_ID, $COL_WS_NAME, $COL_WS_PATH, $COL_WS_ADDED_AT FROM $TABLE_WORKSPACES ORDER BY $COL_WS_ADDED_AT ASC"
        ).use { ps ->
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        Workspace(
                            id = rs.getString(1),
                            name = rs.getString(2),
                            path = rs.getString(3),
                            addedAt = rs.getLong(4)
                        )
                    )
                }
            }
        }
        list
    }

    fun getWorkspace(id: String): Workspace? = synchronized(lock) {
        val conn = getConnection()
        conn.prepareStatement(
            "SELECT $COL_WS_ID, $COL_WS_NAME, $COL_WS_PATH, $COL_WS_ADDED_AT FROM $TABLE_WORKSPACES WHERE $COL_WS_ID = ?"
        ).use { ps ->
            ps.setString(1, id)
            ps.executeQuery().use { rs ->
                if (rs.next()) {
                    Workspace(
                        id = rs.getString(1),
                        name = rs.getString(2),
                        path = rs.getString(3),
                        addedAt = rs.getLong(4)
                    )
                } else {
                    null
                }
            }
        }
    }

    fun renameWorkspace(id: String, newName: String): Boolean = synchronized(lock) {
        val clean = newName.trim()
        if (clean.isEmpty()) return@synchronized false
        val conn = getConnection()
        conn.prepareStatement(
            "UPDATE $TABLE_WORKSPACES SET $COL_WS_NAME = ? WHERE $COL_WS_ID = ?"
        ).use { ps ->
            ps.setString(1, clean)
            ps.setString(2, id)
            ps.executeUpdate() > 0
        }
    }

    fun deleteWorkspace(workspaceId: String): Boolean {
        return deleteWorkspaceWithStats(workspaceId).deleted
    }

    fun deleteWorkspaceWithStats(workspaceId: String): WorkspaceDeleteStats = synchronized(lock) {
        val conn = getConnection()
        val prevAuto = conn.autoCommit
        conn.autoCommit = false
        try {
            val segmentsRemoved = conn.prepareStatement("DELETE FROM $TABLE_FTS WHERE $FTS_COL_WS_ID = ?").use { ps ->
                ps.setString(1, workspaceId)
                ps.executeUpdate()
            }
            conn.prepareStatement(
                "DELETE FROM $TABLE_SHEET_HEADERS WHERE $COL_SH_FILE_URI IN (SELECT $COL_FILE_URI FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?)"
            ).use { ps ->
                ps.setString(1, workspaceId)
                ps.executeUpdate()
            }
            val filesRemoved = conn.prepareStatement("DELETE FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?").use { ps ->
                ps.setString(1, workspaceId)
                ps.executeUpdate()
            }
            val deleted = conn.prepareStatement("DELETE FROM $TABLE_WORKSPACES WHERE $COL_WS_ID = ?").use { ps ->
                ps.setString(1, workspaceId)
                ps.executeUpdate() > 0
            }
            conn.commit()
            WorkspaceDeleteStats(
                deleted = deleted,
                deletedFilesCount = filesRemoved,
                deletedSegmentsCount = segmentsRemoved
            )
        } catch (t: Throwable) {
            try { conn.rollback() } catch (_: Throwable) {}
            throw t
        } finally {
            conn.autoCommit = prevAuto
        }
    }

    // --- Indexed Files & Segments ---

    fun getIndexedFilesForWorkspace(workspaceId: String): Map<String, IndexedFileRecord> = synchronized(lock) {
        val map = mutableMapOf<String, IndexedFileRecord>()
        val conn = getConnection()
        conn.prepareStatement(
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT
            FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?
            """.trimIndent()
        ).use { ps ->
            ps.setString(1, workspaceId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val rec = IndexedFileRecord(
                        uri = rs.getString(1),
                        workspaceId = rs.getString(2),
                        fileName = rs.getString(3),
                        mimeType = rs.getString(4),
                        size = rs.getLong(5),
                        lastModified = rs.getLong(6),
                        indexedAt = rs.getLong(7)
                    )
                    map[rec.uri] = rec
                }
            }
        }
        map
    }

    fun getAllIndexedFiles(workspaceId: String? = null): List<IndexedFileRecord> = synchronized(lock) {
        val list = mutableListOf<IndexedFileRecord>()
        val conn = getConnection()
        val sql = if (workspaceId != null) {
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT
            FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?
            """.trimIndent()
        } else {
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT
            FROM $TABLE_FILES
            """.trimIndent()
        }
        conn.prepareStatement(sql).use { ps ->
            if (workspaceId != null) ps.setString(1, workspaceId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    list.add(
                        IndexedFileRecord(
                            uri = rs.getString(1),
                            workspaceId = rs.getString(2),
                            fileName = rs.getString(3),
                            mimeType = rs.getString(4),
                            size = rs.getLong(5),
                            lastModified = rs.getLong(6),
                            indexedAt = rs.getLong(7)
                        )
                    )
                }
            }
        }
        list
    }

    fun deleteFileByUri(fileUri: String) = synchronized(lock) {
        val conn = getConnection()
        val prevAuto = conn.autoCommit
        conn.autoCommit = false
        try {
            conn.prepareStatement("DELETE FROM $TABLE_SHEET_HEADERS WHERE $COL_SH_FILE_URI = ?").use { ps ->
                ps.setString(1, fileUri)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM $TABLE_FTS WHERE $FTS_COL_URI = ?").use { ps ->
                ps.setString(1, fileUri)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM $TABLE_FILES WHERE $COL_FILE_URI = ?").use { ps ->
                ps.setString(1, fileUri)
                ps.executeUpdate()
            }
            conn.commit()
        } catch (t: Throwable) {
            try { conn.rollback() } catch (_: Throwable) {}
            throw t
        } finally {
            conn.autoCommit = prevAuto
        }
    }

    fun saveFileAndSegments(
        record: IndexedFileRecord,
        segments: List<FileSegment>,
        sheetHeaders: Map<String, List<String>> = emptyMap()
    ) = synchronized(lock) {
        val conn = getConnection()
        val prevAuto = conn.autoCommit
        conn.autoCommit = false
        try {
            conn.prepareStatement("DELETE FROM $TABLE_FTS WHERE $FTS_COL_URI = ?").use { ps ->
                ps.setString(1, record.uri)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM $TABLE_SHEET_HEADERS WHERE $COL_SH_FILE_URI = ?").use { ps ->
                ps.setString(1, record.uri)
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO $TABLE_FILES
                ($COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, record.uri)
                ps.setString(2, record.workspaceId)
                ps.setString(3, record.fileName)
                ps.setString(4, record.mimeType)
                ps.setLong(5, record.size)
                ps.setLong(6, record.lastModified)
                ps.setLong(7, record.indexedAt)
                ps.executeUpdate()
            }

            if (sheetHeaders.isNotEmpty()) {
                conn.prepareStatement(
                    """
                    INSERT OR REPLACE INTO $TABLE_SHEET_HEADERS
                    ($COL_SH_FILE_URI, $COL_SH_SHEET_NAME, $COL_SH_HEADERS_TEXT)
                    VALUES (?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    for ((sheetName, headers) in sheetHeaders) {
                        ps.setString(1, record.uri)
                        ps.setString(2, sheetName)
                        ps.setString(3, headers.joinToString("\t"))
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
            }

            val normalizedFileName = ArabicNormalizer.normalize(record.fileName)
            conn.prepareStatement(
                """
                INSERT INTO $TABLE_FTS
                ($FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT, $FTS_COL_NORMALIZED)
                VALUES (?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                if (segments.isNotEmpty()) {
                    for (seg in segments) {
                        val combinedNormalized = if (normalizedFileName.isNotEmpty()) {
                            "$normalizedFileName ${seg.normalizedContent}"
                        } else {
                            seg.normalizedContent
                        }
                        ps.setString(1, record.workspaceId)
                        ps.setString(2, seg.fileUri)
                        ps.setString(3, seg.fileName)
                        ps.setString(4, seg.location)
                        ps.setString(5, seg.content)
                        ps.setString(6, combinedNormalized)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                } else {
                    ps.setString(1, record.workspaceId)
                    ps.setString(2, record.uri)
                    ps.setString(3, record.fileName)
                    ps.setString(4, "ملف")
                    ps.setString(5, record.fileName)
                    ps.setString(6, normalizedFileName)
                    ps.executeUpdate()
                }
            }

            conn.commit()
        } catch (t: Throwable) {
            try { conn.rollback() } catch (_: Throwable) {}
            throw t
        } finally {
            conn.autoCommit = prevAuto
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
        } else if (location.contains("جدول:")) {
            val parts = location.split("،")
            sheet = parts.getOrNull(0)?.substringAfter("جدول:")?.trim()
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
        conn: Connection,
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
            conn.prepareStatement(sql).use { ps ->
                chunk.forEachIndexed { idx, uri -> ps.setString(idx + 1, uri) }
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val uri = rs.getString(1)
                        val sheet = rs.getString(2)
                        val text = rs.getString(3).orEmpty()
                        headersMap[Pair(uri, sheet)] = if (text.isNotEmpty()) text.split("\t") else emptyList()
                    }
                }
            }
        }
        return headersMap
    }

    fun search(
        rawQuery: String,
        filter: SearchFilter = SearchFilter(),
        limit: Int = 250
    ): List<SearchResult> = synchronized(lock) {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery).trim()
        if (normalizedQuery.isBlank()) return@synchronized emptyList()

        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@synchronized emptyList()

        val conn = getConnection()
        val additionalWhere = mutableListOf<String>()
        val additionalArgs = mutableListOf<String>()

        if (filter.selectedWorkspaceIds.isNotEmpty()) {
            val placeholders = filter.selectedWorkspaceIds.joinToString(",") { "?" }
            additionalWhere.add("$FTS_COL_WS_ID IN ($placeholders)")
            additionalArgs.addAll(filter.selectedWorkspaceIds)
        }

        if (filter.selectedFileUris.isNotEmpty()) {
            val placeholders = filter.selectedFileUris.joinToString(",") { "?" }
            additionalWhere.add("$FTS_COL_URI IN ($placeholders)")
            additionalArgs.addAll(filter.selectedFileUris)
        }

        if (filter.selectedExtensions.isNotEmpty()) {
            val extConditions = filter.selectedExtensions.joinToString(" OR ") { "LOWER($FTS_COL_NAME) LIKE ?" }
            additionalWhere.add("($extConditions)")
            filter.selectedExtensions.forEach { ext ->
                additionalArgs.add("%.${ext.lowercase()}")
            }
        }

        val rawHits = mutableListOf<SearchHit>()

        if (filter.method == SearchMethod.CONTAINS) {
            val instrConditions = tokens.map { "instr($FTS_COL_NORMALIZED, ?) > 0" }
            val allWhere = (instrConditions + additionalWhere).joinToString(" AND ")
            val sql = """
                SELECT rowid, $FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                FROM $TABLE_FTS
                WHERE $allWhere
                LIMIT ?
            """.trimIndent()
            try {
                conn.prepareStatement(sql).use { ps ->
                    var pIdx = 1
                    tokens.forEach { ps.setString(pIdx++, it) }
                    additionalArgs.forEach { ps.setString(pIdx++, it) }
                    ps.setInt(pIdx, limit)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            rawHits.add(
                                SearchHit(
                                    id = rs.getLong(1),
                                    workspaceId = rs.getString(2),
                                    fileUri = rs.getString(3),
                                    fileName = rs.getString(4),
                                    location = rs.getString(5),
                                    content = rs.getString(6)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
            }
        } else {
            val matchExpr = when (filter.method) {
                SearchMethod.EXACT_PHRASE -> "\"${tokens.joinToString(" ")}\""
                SearchMethod.WHOLE_WORD -> tokens.joinToString(" ") { "\"$it\"" }
                SearchMethod.ALL_WORDS, SearchMethod.CONTAINS -> tokens.joinToString(" ") { "\"$it\"*" }
            }
            val whereList = mutableListOf("$TABLE_FTS MATCH ?")
            whereList.addAll(additionalWhere)
            val sql = """
                SELECT rowid, $FTS_COL_WS_ID, $FTS_COL_URI, $FTS_COL_NAME, $FTS_COL_LOCATION, $FTS_COL_CONTENT
                FROM $TABLE_FTS
                WHERE ${whereList.joinToString(" AND ")}
                ORDER BY bm25($TABLE_FTS)
                LIMIT ?
            """.trimIndent()
            try {
                conn.prepareStatement(sql).use { ps ->
                    var pIdx = 1
                    ps.setString(pIdx++, matchExpr)
                    additionalArgs.forEach { ps.setString(pIdx++, it) }
                    ps.setInt(pIdx, limit)
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            rawHits.add(
                                SearchHit(
                                    id = rs.getLong(1),
                                    workspaceId = rs.getString(2),
                                    fileUri = rs.getString(3),
                                    fileName = rs.getString(4),
                                    location = rs.getString(5),
                                    content = rs.getString(6)
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
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
                    SearchMethod.EXACT_PHRASE -> hit.content.contains(rawTrimmed) || hit.fileName.contains(rawTrimmed)
                    SearchMethod.WHOLE_WORD -> rawTokens.all { token ->
                        val regex = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(token)}(?![\\p{L}\\p{Nd}])")
                        regex.containsMatchIn(hit.content) || regex.containsMatchIn(hit.fileName)
                    }
                    SearchMethod.CONTAINS, SearchMethod.ALL_WORDS -> rawTokens.all {
                        hit.content.contains(it) || hit.fileName.contains(it)
                    }
                }
            }
        }

        if (processedHits.isEmpty()) return@synchronized emptyList()

        val headersMap = getSheetHeadersForFiles(conn, processedHits.map { it.fileUri }.toSet())
        processedHits.map { hit ->
            val (sheet, row) = parseSheetAndRow(hit.location)
            val headers = if (sheet != null) headersMap[Pair(hit.fileUri, sheet)] ?: emptyList() else emptyList()
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

    fun getStats(workspaceId: String? = null): Pair<Int, Int> = synchronized(lock) {
        val conn = getConnection()
        var filesCount = 0
        var segmentsCount = 0
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

        conn.prepareStatement(filesSql).use { ps ->
            if (workspaceId != null) ps.setString(1, workspaceId)
            ps.executeQuery().use { rs ->
                if (rs.next()) filesCount = rs.getInt(1)
            }
        }
        conn.prepareStatement(segmentsSql).use { ps ->
            if (workspaceId != null) ps.setString(1, workspaceId)
            ps.executeQuery().use { rs ->
                if (rs.next()) segmentsCount = rs.getInt(1)
            }
        }
        Pair(filesCount, segmentsCount)
    }

    override fun close() = synchronized(lock) {
        try {
            connection?.close()
        } catch (_: Throwable) {
        }
        connection = null
    }
}
