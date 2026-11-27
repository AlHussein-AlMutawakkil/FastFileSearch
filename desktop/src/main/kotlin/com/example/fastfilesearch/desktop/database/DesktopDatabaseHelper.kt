package com.example.fastfilesearch.desktop.database

import com.example.fastfilesearch.desktop.model.DesktopSearchResponse
import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.MatchedFileSummary
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.model.WorkspaceDeleteStats
import com.example.fastfilesearch.desktop.ui.DesktopTableCellHelper
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import java.io.Closeable
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.Statement
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Local offline SQLite + FTS5 search database powered by org.xerial:sqlite-jdbc.
 * Optimized for large databases (2.5M+ rows) with concurrent cancellable FTS5 queries,
 * balanced per-file window retrieval (no global 250-row starvation), and relational indices.
 */
class DesktopDatabaseHelper(private val dbFile: File) : Closeable {

    companion object {
        const val TABLE_WORKSPACES = "workspaces"
        const val COL_WS_ID = "id"
        const val COL_WS_NAME = "name"
        const val COL_WS_PATH = "path"
        const val COL_WS_ADDED_AT = "added_at"

        const val TABLE_FILES = "indexed_files"
        const val COL_FILE_ID = "file_id"
        const val COL_FILE_URI = "file_uri"
        const val COL_FILE_WS_ID = "workspace_id"
        const val COL_FILE_NAME = "file_name"
        const val COL_FILE_TYPE = "file_type"
        const val COL_MIME_TYPE = "mime_type"
        const val COL_FILE_SIZE = "file_size"
        const val COL_LAST_MODIFIED = "last_modified"
        const val COL_INDEXED_AT = "indexed_at"

        const val TABLE_SHEET_HEADERS = "sheet_headers"
        const val COL_SH_FILE_URI = "file_uri"
        const val COL_SH_SHEET_NAME = "sheet_name"
        const val COL_SH_HEADERS_TEXT = "headers_text"

        const val TABLE_SEG_META = "segment_meta"
        const val COL_SM_FILE_ID = "file_id"
        const val COL_SM_WS_ID = "workspace_id"
        const val COL_SM_FILE_TYPE = "file_type"
        const val COL_SM_FILE_URI = "file_uri"

        const val TABLE_FTS = "file_segments_fts"
        const val FTS_COL_WS_ID = "workspace_id"
        const val FTS_COL_URI = "file_uri"
        const val FTS_COL_NAME = "file_name"
        const val FTS_COL_LOCATION = "location"
        const val FTS_COL_CONTENT = "content"
        const val FTS_COL_NORMALIZED = "normalized_content"

        const val DEFAULT_PER_FILE_INITIAL_ROWS = 35
    }

    private val writeLock = Any()
    private val readLock = Any()
    private var writeConnection: Connection? = null
    private var readConnection: Connection? = null
    private val activeSearchStmt = AtomicReference<PreparedStatement?>(null)
    private val searchGeneration = AtomicLong(0L)

    init {
        dbFile.parentFile?.mkdirs()
        synchronized(writeLock) {
            ensureInitialized()
        }
    }

    private fun openConfiguredConnection(readOnly: Boolean): Connection {
        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
        conn.createStatement().use { st ->
            st.execute("PRAGMA journal_mode=WAL;")
            st.execute("PRAGMA synchronous=NORMAL;")
            st.execute("PRAGMA temp_store=MEMORY;")
            st.execute("PRAGMA cache_size=-65536;")
            st.execute("PRAGMA mmap_size=268435456;")
            st.execute("PRAGMA busy_timeout=5000;")
            if (readOnly) {
                st.execute("PRAGMA query_only=ON;")
            }
        }
        return conn
    }

    private fun getWriteConnection(): Connection {
        val current = writeConnection
        if (current != null && !current.isClosed) return current
        val conn = openConfiguredConnection(readOnly = false)
        writeConnection = conn
        return conn
    }

    private fun getReadConnection(): Connection {
        val current = readConnection
        if (current != null && !current.isClosed) return current
        val conn = openConfiguredConnection(readOnly = true)
        readConnection = conn
        return conn
    }

    private fun extractExtensionLower(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase().trim()
    }

    private fun stableFileId(uri: String): Long {
        // Deterministic positive 63-bit ID derived from URI for fast relational joins without colliding sequentially
        var h = 1125899906842597L
        for (ch in uri) {
            h = 31L * h + ch.code
        }
        return h and 0x7FFF_FFFF_FFFF_FFFFL
    }

    private fun normalizeFileNameForFts(fileName: String): String {
        val spaced = fileName.replace('_', ' ').replace('-', ' ').replace('.', ' ')
        return ArabicNormalizer.normalize(spaced)
    }

    private fun ensureInitialized() {
        val conn = getWriteConnection()
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
                    $COL_FILE_ID INTEGER NOT NULL DEFAULT 0,
                    $COL_FILE_WS_ID TEXT NOT NULL,
                    $COL_FILE_NAME TEXT NOT NULL,
                    $COL_FILE_TYPE TEXT NOT NULL DEFAULT '',
                    $COL_MIME_TYPE TEXT,
                    $COL_FILE_SIZE INTEGER NOT NULL,
                    $COL_LAST_MODIFIED INTEGER NOT NULL,
                    $COL_INDEXED_AT INTEGER NOT NULL
                );
                """.trimIndent()
            )
            migrateFilesColumnsIfNeeded(conn, st)

            st.execute("CREATE INDEX IF NOT EXISTS idx_files_ws ON $TABLE_FILES ($COL_FILE_WS_ID);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_uri ON $TABLE_FILES ($COL_FILE_URI);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_id ON $TABLE_FILES ($COL_FILE_ID);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_type ON $TABLE_FILES ($COL_FILE_TYPE);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_files_ws_type ON $TABLE_FILES ($COL_FILE_WS_ID, $COL_FILE_TYPE, $COL_FILE_URI);")

            st.execute(
                """
                CREATE TABLE IF NOT EXISTS $TABLE_SEG_META (
                    $COL_SM_FILE_URI TEXT PRIMARY KEY,
                    $COL_SM_FILE_ID INTEGER NOT NULL,
                    $COL_SM_WS_ID TEXT NOT NULL,
                    $COL_SM_FILE_TYPE TEXT NOT NULL
                );
                """.trimIndent()
            )
            st.execute("CREATE INDEX IF NOT EXISTS idx_seg_meta_file_id ON $TABLE_SEG_META ($COL_SM_FILE_ID);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_seg_meta_ws ON $TABLE_SEG_META ($COL_SM_WS_ID);")
            st.execute("CREATE INDEX IF NOT EXISTS idx_seg_meta_type ON $TABLE_SEG_META ($COL_SM_FILE_TYPE);")

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

    private fun migrateFilesColumnsIfNeeded(conn: Connection, st: Statement) {
        val existingCols = mutableSetOf<String>()
        st.executeQuery("PRAGMA table_info($TABLE_FILES)").use { rs ->
            while (rs.next()) {
                existingCols.add(rs.getString("name").lowercase())
            }
        }
        if (COL_FILE_ID !in existingCols) {
            st.execute("ALTER TABLE $TABLE_FILES ADD COLUMN $COL_FILE_ID INTEGER NOT NULL DEFAULT 0;")
        }
        if (COL_FILE_TYPE !in existingCols) {
            st.execute("ALTER TABLE $TABLE_FILES ADD COLUMN $COL_FILE_TYPE TEXT NOT NULL DEFAULT '';")
        }

        // Backfill missing file_id / file_type once for upgraded databases
        val missingRows = mutableListOf<Triple<String, Long, String>>()
        st.executeQuery(
            "SELECT $COL_FILE_URI, $COL_FILE_NAME FROM $TABLE_FILES WHERE $COL_FILE_ID = 0 OR $COL_FILE_TYPE = ''"
        ).use { rs ->
            while (rs.next()) {
                val uri = rs.getString(1)
                val name = rs.getString(2)
                missingRows.add(Triple(uri, stableFileId(uri), extractExtensionLower(name)))
            }
        }
        if (missingRows.isNotEmpty()) {
            val prevAuto = conn.autoCommit
            conn.autoCommit = false
            try {
                conn.prepareStatement(
                    "UPDATE $TABLE_FILES SET $COL_FILE_ID = ?, $COL_FILE_TYPE = ? WHERE $COL_FILE_URI = ?"
                ).use { ps ->
                    for ((uri, fid, ext) in missingRows) {
                        ps.setLong(1, fid)
                        ps.setString(2, ext)
                        ps.setString(3, uri)
                        ps.addBatch()
                    }
                    ps.executeBatch()
                }
                conn.commit()
            } catch (t: Throwable) {
                try { conn.rollback() } catch (_: Throwable) {}
                throw t
            } finally {
                conn.autoCommit = prevAuto
            }
        }
    }

    /** Cancels any in-flight SQLite query immediately (via sqlite3_interrupt). */
    fun cancelActiveSearch() {
        searchGeneration.incrementAndGet()
        val stmt = activeSearchStmt.getAndSet(null) ?: return
        try {
            if (!stmt.isClosed) stmt.cancel()
        } catch (_: Throwable) {
        }
    }

    private fun isInterruptException(t: Throwable?): Boolean {
        var cur: Throwable? = t
        var depth = 0
        while (cur != null && depth < 8) {
            val msg = cur.message.orEmpty()
            if (msg.contains("SQLITE_INTERRUPT", ignoreCase = true) ||
                msg.contains("sqlite3_interrupt", ignoreCase = true) ||
                (cur is SQLException && msg.contains("interrupted", ignoreCase = true))
            ) {
                return true
            }
            cur = cur.cause
            depth++
        }
        return false
    }

    // --- Workspaces ---

    fun insertWorkspace(workspace: Workspace) = synchronized(writeLock) {
        val conn = getWriteConnection()
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

    fun getAllWorkspaces(): List<Workspace> = synchronized(readLock) {
        val list = mutableListOf<Workspace>()
        val conn = getReadConnection()
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

    fun getWorkspace(id: String): Workspace? = synchronized(readLock) {
        val conn = getReadConnection()
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

    fun renameWorkspace(id: String, newName: String): Boolean = synchronized(writeLock) {
        val clean = newName.trim()
        if (clean.isEmpty()) return@synchronized false
        val conn = getWriteConnection()
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

    fun deleteWorkspaceWithStats(workspaceId: String): WorkspaceDeleteStats = synchronized(writeLock) {
        val conn = getWriteConnection()
        val prevAuto = conn.autoCommit
        conn.autoCommit = false
        try {
            val segmentsRemoved = conn.prepareStatement("DELETE FROM $TABLE_FTS WHERE $FTS_COL_WS_ID = ?").use { ps ->
                ps.setString(1, workspaceId)
                ps.executeUpdate()
            }
            conn.prepareStatement("DELETE FROM $TABLE_SEG_META WHERE $COL_SM_WS_ID = ?").use { ps ->
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

    fun getIndexedFilesForWorkspace(workspaceId: String): Map<String, IndexedFileRecord> = synchronized(readLock) {
        val map = mutableMapOf<String, IndexedFileRecord>()
        val conn = getReadConnection()
        conn.prepareStatement(
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT, $COL_FILE_ID, $COL_FILE_TYPE
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
                        indexedAt = rs.getLong(7),
                        fileId = rs.getLong(8),
                        fileType = rs.getString(9).ifBlank { extractExtensionLower(rs.getString(3)) }
                    )
                    map[rec.uri] = rec
                }
            }
        }
        map
    }

    fun getAllIndexedFiles(workspaceId: String? = null): List<IndexedFileRecord> = synchronized(readLock) {
        val list = mutableListOf<IndexedFileRecord>()
        val conn = getReadConnection()
        val sql = if (workspaceId != null) {
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT, $COL_FILE_ID, $COL_FILE_TYPE
            FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ?
            """.trimIndent()
        } else {
            """
            SELECT $COL_FILE_URI, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT, $COL_FILE_ID, $COL_FILE_TYPE
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
                            indexedAt = rs.getLong(7),
                            fileId = rs.getLong(8),
                            fileType = rs.getString(9).ifBlank { extractExtensionLower(rs.getString(3)) }
                        )
                    )
                }
            }
        }
        list
    }

    fun getIndexedTypeCounts(workspaceId: String? = null): Map<String, Int> = synchronized(readLock) {
        val counts = mutableMapOf<String, Int>()
        val conn = getReadConnection()
        val sql = if (workspaceId != null) {
            "SELECT $COL_FILE_TYPE, COUNT(*) FROM $TABLE_FILES WHERE $COL_FILE_WS_ID = ? GROUP BY $COL_FILE_TYPE"
        } else {
            "SELECT $COL_FILE_TYPE, COUNT(*) FROM $TABLE_FILES GROUP BY $COL_FILE_TYPE"
        }
        conn.prepareStatement(sql).use { ps ->
            if (workspaceId != null) ps.setString(1, workspaceId)
            ps.executeQuery().use { rs ->
                while (rs.next()) {
                    val ext = rs.getString(1).orEmpty().lowercase()
                    val c = rs.getInt(2)
                    if (ext.isNotEmpty() && c > 0) {
                        counts[ext] = (counts[ext] ?: 0) + c
                    }
                }
            }
        }
        counts
    }

    fun deleteFileByUri(fileUri: String) = synchronized(writeLock) {
        val conn = getWriteConnection()
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
            conn.prepareStatement("DELETE FROM $TABLE_SEG_META WHERE $COL_SM_FILE_URI = ?").use { ps ->
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
    ) = synchronized(writeLock) {
        val conn = getWriteConnection()
        val prevAuto = conn.autoCommit
        conn.autoCommit = false
        val fileId = if (record.fileId > 0L) record.fileId else stableFileId(record.uri)
        val fileType = record.fileType.ifBlank { extractExtensionLower(record.fileName) }
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
                ($COL_FILE_URI, $COL_FILE_ID, $COL_FILE_WS_ID, $COL_FILE_NAME, $COL_FILE_TYPE, $COL_MIME_TYPE, $COL_FILE_SIZE, $COL_LAST_MODIFIED, $COL_INDEXED_AT)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, record.uri)
                ps.setLong(2, fileId)
                ps.setString(3, record.workspaceId)
                ps.setString(4, record.fileName)
                ps.setString(5, fileType)
                ps.setString(6, record.mimeType)
                ps.setLong(7, record.size)
                ps.setLong(8, record.lastModified)
                ps.setLong(9, record.indexedAt)
                ps.executeUpdate()
            }

            conn.prepareStatement(
                """
                INSERT OR REPLACE INTO $TABLE_SEG_META
                ($COL_SM_FILE_URI, $COL_SM_FILE_ID, $COL_SM_WS_ID, $COL_SM_FILE_TYPE)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { ps ->
                ps.setString(1, record.uri)
                ps.setLong(2, fileId)
                ps.setString(3, record.workspaceId)
                ps.setString(4, fileType)
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

            val normalizedFileName = normalizeFileNameForFts(record.fileName)
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
        val fileType: String,
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

    private fun sanitizeFtsToken(rawToken: String): String {
        return rawToken.replace("\"", "").replace("*", "").trim()
    }

    private fun buildContainsMatchExpr(tokens: List<String>): String? {
        val clean = tokens.map(::sanitizeFtsToken).filter { it.isNotEmpty() }
        if (clean.isEmpty()) return null
        return clean.joinToString(" ") { tok ->
            val variants = LinkedHashSet<String>(12)
            variants.add(tok)
            if (tok.startsWith("ال") && tok.length >= 2) {
                variants.add("و$tok")
                variants.add("ب$tok")
                variants.add("ف$tok")
                variants.add("ل$tok")
                variants.add("ك$tok")
            } else if (tok.length >= 2) {
                variants.add("ال$tok")
                variants.add("وال$tok")
                variants.add("بال$tok")
                variants.add("كال$tok")
                variants.add("فال$tok")
                variants.add("لل$tok")
                variants.add("و$tok")
                variants.add("ب$tok")
                variants.add("ف$tok")
                variants.add("ل$tok")
                variants.add("ك$tok")
            }
            "(" + variants.joinToString(" OR ") { "\"$it\"*" } + ")"
        }
    }

    private fun buildMatchExpr(tokens: List<String>, method: SearchMethod): String? {
        val clean = tokens.map(::sanitizeFtsToken).filter { it.isNotEmpty() }
        if (clean.isEmpty()) return null
        return when (method) {
            SearchMethod.EXACT_PHRASE -> "\"${clean.joinToString(" ")}\""
            SearchMethod.WHOLE_WORD -> clean.joinToString(" ") { "\"$it\"" }
            SearchMethod.ALL_WORDS -> clean.joinToString(" ") { "\"$it\"*" }
            SearchMethod.CONTAINS -> buildContainsMatchExpr(clean)
        }
    }

    private fun hitMatchesPostFilters(
        hit: SearchHit,
        rawQuery: String,
        normalizedQuery: String,
        tokens: List<String>,
        filter: SearchFilter
    ): Boolean {
        if (filter.method == SearchMethod.EXACT_PHRASE) {
            val normContent = ArabicNormalizer.normalize(hit.content)
            val normName = normalizeFileNameForFts(hit.fileName)
            if (!normContent.contains(normalizedQuery) && !normName.contains(normalizedQuery)) {
                return false
            }
        } else if (filter.method == SearchMethod.WHOLE_WORD) {
            val normContent = ArabicNormalizer.normalize(hit.content)
            val normName = normalizeFileNameForFts(hit.fileName)
            val allMatch = tokens.all { token ->
                val regex = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(token)}(?![\\p{L}\\p{Nd}])")
                regex.containsMatchIn(normContent) || regex.containsMatchIn(normName)
            }
            if (!allMatch) return false
        }

        if (!filter.ignoreDiacritics) {
            val rawTrimmed = rawQuery.trim()
            val rawTokens = rawTrimmed.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
            val matchesRaw = when (filter.method) {
                SearchMethod.EXACT_PHRASE -> hit.content.contains(rawTrimmed) || hit.fileName.contains(rawTrimmed)
                SearchMethod.WHOLE_WORD -> rawTokens.all { token ->
                    val regex = Regex("(?<![\\p{L}\\p{Nd}])${Regex.escape(token)}(?![\\p{L}\\p{Nd}])")
                    regex.containsMatchIn(hit.content) || regex.containsMatchIn(hit.fileName)
                }
                SearchMethod.CONTAINS, SearchMethod.ALL_WORDS -> rawTokens.all {
                    hit.content.contains(it) || hit.fileName.contains(it)
                }
            }
            if (!matchesRaw) return false
        }

        return true
    }

    private fun buildFileFilterSql(filter: SearchFilter, includeExtensions: Boolean): Pair<List<String>, List<String>> {
        val where = mutableListOf<String>()
        val args = mutableListOf<String>()
        if (filter.selectedWorkspaceIds.isNotEmpty()) {
            val placeholders = filter.selectedWorkspaceIds.joinToString(",") { "?" }
            where.add("f.$COL_FILE_WS_ID IN ($placeholders)")
            args.addAll(filter.selectedWorkspaceIds)
        }
        if (filter.selectedFileUris.isNotEmpty()) {
            val placeholders = filter.selectedFileUris.joinToString(",") { "?" }
            where.add("f.$COL_FILE_URI IN ($placeholders)")
            args.addAll(filter.selectedFileUris)
        }
        if (includeExtensions && filter.selectedExtensions.isNotEmpty()) {
            val exts = filter.selectedExtensions.map { it.lowercase().trim() }.distinct()
            val placeholders = exts.joinToString(",") { "?" }
            where.add("f.$COL_FILE_TYPE IN ($placeholders)")
            args.addAll(exts)
        }
        return where to args
    }

    /**
     * Multi-file, multi-type balanced search without global LIMIT 250 starvation:
     * 1. Computes ALL matching files & exact match counts across formats via fast SQLite FTS5 MATCH + indexed files join.
     * 2. Fetches up to [perFileInitialRows] per matching file (plus cancellable infix rescue for rare CONTAINS substrings).
     */
    fun searchGrouped(
        rawQuery: String,
        filter: SearchFilter = SearchFilter(),
        perFileInitialRows: Int = DEFAULT_PER_FILE_INITIAL_ROWS
    ): DesktopSearchResponse = synchronized(readLock) {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery).trim()
        if (normalizedQuery.isBlank()) return@synchronized DesktopSearchResponse()

        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@synchronized DesktopSearchResponse()

        val matchExpr = buildMatchExpr(tokens, filter.method) ?: return@synchronized DesktopSearchResponse()
        val startGen = searchGeneration.get()
        try {
        val conn = getReadConnection()
        val (baseWhere, baseArgs) = buildFileFilterSql(filter, includeExtensions = false)
        val hasPostFilter = filter.method == SearchMethod.EXACT_PHRASE ||
            filter.method == SearchMethod.WHOLE_WORD ||
            !filter.ignoreDiacritics

        val summaryList = mutableListOf<MatchedFileSummary>()
        val initialHitsByFile = linkedMapOf<String, MutableList<SearchHit>>()
        val typeCounts = mutableMapOf<String, Int>()
        val allowedExts = filter.selectedExtensions.map { it.lowercase().trim() }.toSet()

        if (!hasPostFilter) {
            if (searchGeneration.get() != startGen) return@synchronized DesktopSearchResponse()
            // Fast Tier A: Direct FTS5 GROUP BY summary (reads 0 snippet strings across millions of rows)
            val summaryWhere = (listOf("$TABLE_FTS MATCH ?") + baseWhere).joinToString(" AND ")
            val summarySql = """
                SELECT f.$COL_FILE_URI, f.$COL_FILE_WS_ID, f.$COL_FILE_NAME, f.$COL_FILE_TYPE, COUNT(*) AS cnt
                FROM $TABLE_FTS fts
                JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
                WHERE $summaryWhere
                GROUP BY f.$COL_FILE_URI
                ORDER BY MIN(fts.rowid) ASC
            """.trimIndent()

            conn.prepareStatement(summarySql).use { ps ->
                activeSearchStmt.set(ps)
                try {
                    var idx = 1
                    ps.setString(idx++, matchExpr)
                    baseArgs.forEach { ps.setString(idx++, it) }
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val uri = rs.getString(1)
                            val wsId = rs.getString(2)
                            val name = rs.getString(3)
                            val fType = rs.getString(4).ifBlank { extractExtensionLower(name) }
                            val cnt = rs.getInt(5)
                            summaryList.add(MatchedFileSummary(uri, wsId, name, fType, cnt))
                            if (fType.isNotEmpty()) {
                                typeCounts[fType] = (typeCounts[fType] ?: 0) + cnt
                            }
                        }
                    }
                } finally {
                    activeSearchStmt.compareAndSet(ps, null)
                }
            }

            if (searchGeneration.get() != startGen) return@synchronized DesktopSearchResponse()

            // Optional Stage B: True substring infix rescue if CONTAINS with FTS prefix found 0 hits
            // (e.g. arbitrary mid-number "345" inside "123456"), cancelled immediately if search query changes.
            if (summaryList.isEmpty() && filter.method == SearchMethod.CONTAINS) {
                runCancellableInfixRescue(
                    conn = conn,
                    rawQuery = rawQuery,
                    normalizedQuery = normalizedQuery,
                    tokens = tokens,
                    baseWhere = baseWhere,
                    baseArgs = baseArgs,
                    filter = filter,
                    allowedExts = allowedExts,
                    perFileInitialRows = perFileInitialRows,
                    summaryOut = summaryList,
                    typeCountsOut = typeCounts,
                    initialHitsOut = initialHitsByFile
                )
            } else {
                val targetSummaries = if (allowedExts.isEmpty()) {
                    summaryList
                } else {
                    summaryList.filter { it.fileType in allowedExts }
                }

                if (targetSummaries.isNotEmpty()) {
                    val (extWhere, extArgs) = buildFileFilterSql(filter, includeExtensions = true)
                    val fetchWhere = (listOf("$TABLE_FTS MATCH ?") + extWhere).joinToString(" AND ")
                    // Two-phase late materialization: sort lightweight IDs first, read content only for rn <= N
                    val fetchSql = """
                        WITH ranked AS (
                            SELECT fts.rowid AS seg_id, f.$COL_FILE_WS_ID AS ws_id, f.$COL_FILE_URI AS f_uri,
                                   f.$COL_FILE_NAME AS f_name, f.$COL_FILE_TYPE AS f_type,
                                   ROW_NUMBER() OVER (PARTITION BY f.$COL_FILE_URI ORDER BY fts.rowid ASC) AS rn
                            FROM $TABLE_FTS fts
                            JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
                            WHERE $fetchWhere
                        )
                        SELECT r.seg_id, r.ws_id, r.f_uri, r.f_name, r.f_type, s.$FTS_COL_LOCATION, s.$FTS_COL_CONTENT
                        FROM ranked r
                        JOIN $TABLE_FTS s ON s.rowid = r.seg_id
                        WHERE r.rn <= ?
                        ORDER BY r.seg_id ASC
                    """.trimIndent()

                    conn.prepareStatement(fetchSql).use { ps ->
                        activeSearchStmt.set(ps)
                        try {
                            var idx = 1
                            ps.setString(idx++, matchExpr)
                            extArgs.forEach { ps.setString(idx++, it) }
                            ps.setInt(idx, perFileInitialRows)
                            ps.executeQuery().use { rs ->
                                while (rs.next()) {
                                    val hit = SearchHit(
                                        id = rs.getLong(1),
                                        workspaceId = rs.getString(2),
                                        fileUri = rs.getString(3),
                                        fileName = rs.getString(4),
                                        fileType = rs.getString(5),
                                        location = rs.getString(6),
                                        content = rs.getString(7)
                                    )
                                    initialHitsByFile.getOrPut(hit.fileUri) { mutableListOf() }.add(hit)
                                }
                            }
                        } finally {
                            activeSearchStmt.compareAndSet(ps, null)
                        }
                    }
                }
            }
        } else {
            if (searchGeneration.get() != startGen) return@synchronized DesktopSearchResponse()
            // Exact Phrase / Whole Word / Strict Diacritics: stream FTS candidates & cap initial rows per file in RAM
            val streamWhere = (listOf("$TABLE_FTS MATCH ?") + baseWhere).joinToString(" AND ")
            val streamSql = """
                SELECT fts.rowid, f.$COL_FILE_WS_ID, f.$COL_FILE_URI, f.$COL_FILE_NAME, f.$COL_FILE_TYPE,
                       fts.$FTS_COL_LOCATION, fts.$FTS_COL_CONTENT
                FROM $TABLE_FTS fts
                JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
                WHERE $streamWhere
                ORDER BY fts.rowid ASC
            """.trimIndent()

            val fileCounts = linkedMapOf<String, MatchedFileSummary>()
            conn.prepareStatement(streamSql).use { ps ->
                activeSearchStmt.set(ps)
                try {
                    var idx = 1
                    ps.setString(idx++, matchExpr)
                    baseArgs.forEach { ps.setString(idx++, it) }
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val hit = SearchHit(
                                id = rs.getLong(1),
                                workspaceId = rs.getString(2),
                                fileUri = rs.getString(3),
                                fileName = rs.getString(4),
                                fileType = rs.getString(5).ifBlank { extractExtensionLower(rs.getString(4)) },
                                location = rs.getString(6),
                                content = rs.getString(7)
                            )
                            if (!hitMatchesPostFilters(hit, rawQuery, normalizedQuery, tokens, filter)) continue

                            if (hit.fileType.isNotEmpty()) {
                                typeCounts[hit.fileType] = (typeCounts[hit.fileType] ?: 0) + 1
                            }
                            val prev = fileCounts[hit.fileUri]
                            fileCounts[hit.fileUri] = MatchedFileSummary(
                                fileUri = hit.fileUri,
                                workspaceId = hit.workspaceId,
                                fileName = hit.fileName,
                                fileType = hit.fileType,
                                totalMatches = (prev?.totalMatches ?: 0) + 1
                            )
                            if (allowedExts.isEmpty() || hit.fileType in allowedExts) {
                                val list = initialHitsByFile.getOrPut(hit.fileUri) { mutableListOf() }
                                if (list.size < perFileInitialRows) {
                                    list.add(hit)
                                }
                            }
                        }
                    }
                } finally {
                    activeSearchStmt.compareAndSet(ps, null)
                }
            }
            summaryList.addAll(fileCounts.values)
        }

        if (searchGeneration.get() != startGen) return@synchronized DesktopSearchResponse()

        val visibleSummaries = if (allowedExts.isEmpty()) {
            summaryList
        } else {
            summaryList.filter { it.fileType in allowedExts }
        }

        if (visibleSummaries.isEmpty() || initialHitsByFile.isEmpty()) {
            return@synchronized DesktopSearchResponse(
                totalResultsCount = 0,
                fileSummaries = visibleSummaries,
                typeCounts = typeCounts,
                groups = emptyList()
            )
        }

        val headersMap = getSheetHeadersForFiles(conn, initialHitsByFile.keys)
        val trimmedQuery = rawQuery.trim()
        val groups = visibleSummaries.mapNotNull { summary ->
            val hits = initialHitsByFile[summary.fileUri] ?: return@mapNotNull null
            val mapped = hits.map { hit ->
                val (sheet, row) = parseSheetAndRow(hit.location)
                val headers = if (sheet != null) headersMap[Pair(hit.fileUri, sheet)] ?: emptyList() else emptyList()
                SearchResult(
                    id = hit.id,
                    fileUri = hit.fileUri,
                    workspaceId = hit.workspaceId,
                    fileName = hit.fileName,
                    location = hit.location,
                    snippet = hit.content,
                    query = trimmedQuery,
                    sheetName = sheet,
                    rowNumber = row,
                    columnHeaders = headers
                )
            }
            val isDoc = !DesktopTableCellHelper.isTabular(summary.fileName, mapped.firstOrNull()?.snippet.orEmpty())
            FileResultGroup(
                fileUri = summary.fileUri,
                fileName = summary.fileName,
                isDocument = isDoc,
                results = mapped,
                totalMatchesCount = summary.totalMatches,
                hasMoreRows = mapped.size < summary.totalMatches
            )
        }

        val totalVisibleMatches = visibleSummaries.sumOf { it.totalMatches }
        DesktopSearchResponse(
            totalResultsCount = totalVisibleMatches,
            fileSummaries = visibleSummaries,
            typeCounts = typeCounts,
            groups = groups
        )
        } catch (t: Throwable) {
            if (isInterruptException(t)) {
                return@synchronized DesktopSearchResponse()
            }
            throw t
        }
    }

    private fun runCancellableInfixRescue(
        conn: Connection,
        rawQuery: String,
        normalizedQuery: String,
        tokens: List<String>,
        baseWhere: List<String>,
        baseArgs: List<String>,
        filter: SearchFilter,
        allowedExts: Set<String>,
        perFileInitialRows: Int,
        summaryOut: MutableList<MatchedFileSummary>,
        typeCountsOut: MutableMap<String, Int>,
        initialHitsOut: MutableMap<String, MutableList<SearchHit>>
    ) {
        val instrConditions = tokens.map { "instr(fts.$FTS_COL_NORMALIZED, ?) > 0" }
        val allWhere = (instrConditions + baseWhere).joinToString(" AND ")
        val sql = """
            SELECT fts.rowid, f.$COL_FILE_WS_ID, f.$COL_FILE_URI, f.$COL_FILE_NAME, f.$COL_FILE_TYPE,
                   fts.$FTS_COL_LOCATION, fts.$FTS_COL_CONTENT
            FROM $TABLE_FTS fts
            JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
            WHERE $allWhere
            ORDER BY fts.rowid ASC
        """.trimIndent()

        val fileCounts = linkedMapOf<String, MatchedFileSummary>()
        try {
            conn.prepareStatement(sql).use { ps ->
                activeSearchStmt.set(ps)
                try {
                    var pIdx = 1
                    tokens.forEach { ps.setString(pIdx++, it) }
                    baseArgs.forEach { ps.setString(pIdx++, it) }
                    ps.executeQuery().use { rs ->
                        while (rs.next()) {
                            val hit = SearchHit(
                                id = rs.getLong(1),
                                workspaceId = rs.getString(2),
                                fileUri = rs.getString(3),
                                fileName = rs.getString(4),
                                fileType = rs.getString(5).ifBlank { extractExtensionLower(rs.getString(4)) },
                                location = rs.getString(6),
                                content = rs.getString(7)
                            )
                            if (!hitMatchesPostFilters(hit, rawQuery, normalizedQuery, tokens, filter)) continue
                            if (hit.fileType.isNotEmpty()) {
                                typeCountsOut[hit.fileType] = (typeCountsOut[hit.fileType] ?: 0) + 1
                            }
                            val prev = fileCounts[hit.fileUri]
                            fileCounts[hit.fileUri] = MatchedFileSummary(
                                fileUri = hit.fileUri,
                                workspaceId = hit.workspaceId,
                                fileName = hit.fileName,
                                fileType = hit.fileType,
                                totalMatches = (prev?.totalMatches ?: 0) + 1
                            )
                            if (allowedExts.isEmpty() || hit.fileType in allowedExts) {
                                val list = initialHitsOut.getOrPut(hit.fileUri) { mutableListOf() }
                                if (list.size < perFileInitialRows) {
                                    list.add(hit)
                                }
                            }
                        }
                    }
                } finally {
                    activeSearchStmt.compareAndSet(ps, null)
                }
            }
        } catch (_: Exception) {
        }
        summaryOut.addAll(fileCounts.values)
    }

    /** Loads additional matching rows for a single file card starting at [offset]. */
    fun loadMoreForFile(
        rawQuery: String,
        filter: SearchFilter,
        fileUri: String,
        offset: Int,
        limit: Int = 200
    ): List<SearchResult> = synchronized(readLock) {
        val normalizedQuery = ArabicNormalizer.normalize(rawQuery).trim()
        if (normalizedQuery.isBlank()) return@synchronized emptyList()

        val tokens = normalizedQuery.split("[^\\p{L}\\p{Nd}]+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return@synchronized emptyList()

        try {
        val matchExpr = buildMatchExpr(tokens, filter.method)
        val conn = getReadConnection()
        val hits = mutableListOf<SearchHit>()

        if (matchExpr != null) {
            val sql = """
                SELECT fts.rowid, f.$COL_FILE_WS_ID, f.$COL_FILE_URI, f.$COL_FILE_NAME, f.$COL_FILE_TYPE,
                       fts.$FTS_COL_LOCATION, fts.$FTS_COL_CONTENT
                FROM $TABLE_FTS fts
                JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
                WHERE $TABLE_FTS MATCH ? AND fts.$FTS_COL_URI = ?
                ORDER BY fts.rowid ASC
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                ps.setString(1, matchExpr)
                ps.setString(2, fileUri)
                ps.executeQuery().use { rs ->
                    var matchedIdx = 0
                    while (rs.next()) {
                        val hit = SearchHit(
                            id = rs.getLong(1),
                            workspaceId = rs.getString(2),
                            fileUri = rs.getString(3),
                            fileName = rs.getString(4),
                            fileType = rs.getString(5),
                            location = rs.getString(6),
                            content = rs.getString(7)
                        )
                        if (!hitMatchesPostFilters(hit, rawQuery, normalizedQuery, tokens, filter)) continue
                        if (matchedIdx >= offset) {
                            hits.add(hit)
                            if (hits.size >= limit) break
                        }
                        matchedIdx++
                    }
                }
            }
        }

        if (hits.isEmpty() && offset == 0 && filter.method == SearchMethod.CONTAINS) {
            val instrConditions = tokens.map { "instr(fts.$FTS_COL_NORMALIZED, ?) > 0" }
            val sql = """
                SELECT fts.rowid, f.$COL_FILE_WS_ID, f.$COL_FILE_URI, f.$COL_FILE_NAME, f.$COL_FILE_TYPE,
                       fts.$FTS_COL_LOCATION, fts.$FTS_COL_CONTENT
                FROM $TABLE_FTS fts
                JOIN $TABLE_FILES f ON f.$COL_FILE_URI = fts.$FTS_COL_URI
                WHERE fts.$FTS_COL_URI = ? AND ${instrConditions.joinToString(" AND ")}
                ORDER BY fts.rowid ASC
                LIMIT ? OFFSET ?
            """.trimIndent()
            conn.prepareStatement(sql).use { ps ->
                var idx = 1
                ps.setString(idx++, fileUri)
                tokens.forEach { ps.setString(idx++, it) }
                ps.setInt(idx++, limit)
                ps.setInt(idx, offset)
                ps.executeQuery().use { rs ->
                    while (rs.next()) {
                        val hit = SearchHit(
                            id = rs.getLong(1),
                            workspaceId = rs.getString(2),
                            fileUri = rs.getString(3),
                            fileName = rs.getString(4),
                            fileType = rs.getString(5),
                            location = rs.getString(6),
                            content = rs.getString(7)
                        )
                        if (hitMatchesPostFilters(hit, rawQuery, normalizedQuery, tokens, filter)) {
                            hits.add(hit)
                        }
                    }
                }
            }
        }

        if (hits.isEmpty()) return@synchronized emptyList()
        val headersMap = getSheetHeadersForFiles(conn, setOf(fileUri))
        val trimmedQuery = rawQuery.trim()
        hits.map { hit ->
            val (sheet, row) = parseSheetAndRow(hit.location)
            val headers = if (sheet != null) headersMap[Pair(hit.fileUri, sheet)] ?: emptyList() else emptyList()
            SearchResult(
                id = hit.id,
                fileUri = hit.fileUri,
                workspaceId = hit.workspaceId,
                fileName = hit.fileName,
                location = hit.location,
                snippet = hit.content,
                query = trimmedQuery,
                sheetName = sheet,
                rowNumber = row,
                columnHeaders = headers
            )
        }
        } catch (t: Throwable) {
            if (isInterruptException(t)) emptyList() else throw t
        }
    }

    /** Backwards-compatible flat list search returning balanced hits across ALL matching files. */
    fun search(
        rawQuery: String,
        filter: SearchFilter = SearchFilter(),
        perFileLimit: Int = 500
    ): List<SearchResult> {
        return searchGrouped(rawQuery, filter, perFileInitialRows = perFileLimit)
            .groups
            .flatMap { it.results }
    }

    fun getStats(workspaceId: String? = null): Pair<Int, Int> = synchronized(readLock) {
        val conn = getReadConnection()
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

    override fun close() {
        cancelActiveSearch()
        synchronized(readLock) {
            try {
                readConnection?.close()
            } catch (_: Throwable) {
            }
            readConnection = null
        }
        synchronized(writeLock) {
            try {
                writeConnection?.close()
            } catch (_: Throwable) {
            }
            writeConnection = null
        }
    }
}
