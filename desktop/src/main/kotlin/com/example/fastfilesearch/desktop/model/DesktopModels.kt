package com.example.fastfilesearch.desktop.model

enum class SearchMethod(val displayName: String, val description: String) {
    CONTAINS("يحتوي على", "مطابقة النص حتى في وسط الكلمة"),
    ALL_WORDS("جميع الكلمات", "يشترط وجود جميع الكلمات في نفس السطر"),
    EXACT_PHRASE("العبارة بالضبط", "مطابقة العبارة بترتيب كلماتها نصاً"),
    WHOLE_WORD("الكلمة بالكامل", "مطابقة الكلمة المستقلة بالكامل");

    companion object {
        fun fromStoredName(name: String?): SearchMethod {
            if (name.isNullOrBlank()) return CONTAINS
            return entries.firstOrNull { it.name == name } ?: CONTAINS
        }
    }
}

data class Workspace(
    val id: String,
    val name: String,
    val path: String,
    val addedAt: Long
)

data class WorkspaceDeleteStats(
    val deleted: Boolean,
    val deletedFilesCount: Int,
    val deletedSegmentsCount: Int
)

data class IndexedFileRecord(
    val uri: String,
    val workspaceId: String = "",
    val fileName: String,
    val mimeType: String? = null,
    val size: Long,
    val lastModified: Long,
    val indexedAt: Long = System.currentTimeMillis(),
    val fileId: Long = 0L,
    val fileType: String = fileName.substringAfterLast('.', "").lowercase()
)

data class FileSegment(
    val fileUri: String,
    val workspaceId: String = "",
    val fileName: String,
    val location: String,
    val content: String,
    val normalizedContent: String
)

data class SearchResult(
    val id: Long = 0,
    val fileUri: String,
    val workspaceId: String = "",
    val fileName: String,
    val location: String,
    val snippet: String,
    val query: String,
    val sheetName: String? = null,
    val rowNumber: Int? = null,
    val columnHeaders: List<String> = emptyList()
)

data class MatchedFileSummary(
    val fileUri: String,
    val workspaceId: String,
    val fileName: String,
    val fileType: String,
    val totalMatches: Int
)

data class FileResultGroup(
    val fileUri: String,
    val fileName: String,
    val isDocument: Boolean,
    val results: List<SearchResult>,
    val totalMatchesCount: Int = results.size,
    val hasMoreRows: Boolean = results.size < totalMatchesCount
)

data class DesktopSearchResponse(
    val totalResultsCount: Int = 0,
    val fileSummaries: List<MatchedFileSummary> = emptyList(),
    val typeCounts: Map<String, Int> = emptyMap(),
    val groups: List<FileResultGroup> = emptyList()
)

data class FilteredFileType(
    val id: String,
    val displayName: String,
    val extensions: Set<String>
)

object SupportedFileTypeGroups {
    val EXCEL = FilteredFileType("excel", "Excel", setOf("xlsx", "xls"))
    val ACCESS = FilteredFileType("access", "قواعد Access", setOf("mdb", "accdb"))
    val WORD = FilteredFileType("word", "Word", setOf("docx", "doc"))
    val PDF = FilteredFileType("pdf", "PDF", setOf("pdf"))
    val TEXT = FilteredFileType("text", "نصوص", setOf("txt", "log"))
    val CSV = FilteredFileType("csv", "CSV", setOf("csv"))
    val JSON = FilteredFileType("json", "JSON", setOf("json"))
    val MARKDOWN = FilteredFileType("markdown", "Markdown", setOf("md"))

    val ALL_GROUPS = listOf(
        EXCEL,
        ACCESS,
        WORD,
        PDF,
        TEXT,
        CSV,
        JSON,
        MARKDOWN
    )
}

data class SearchFilter(
    val method: SearchMethod = SearchMethod.CONTAINS,
    val ignoreDiacritics: Boolean = true,
    val selectedExtensions: Set<String> = emptySet(),
    val selectedFileUris: Set<String> = emptySet(),
    val selectedWorkspaceIds: Set<String> = emptySet()
) {
    val isFilterActive: Boolean
        get() = isFilterActive(SearchMethod.CONTAINS)

    fun isFilterActive(defaultMethod: SearchMethod): Boolean {
        return method != defaultMethod ||
            !ignoreDiacritics ||
            selectedExtensions.isNotEmpty() ||
            selectedFileUris.isNotEmpty() ||
            selectedWorkspaceIds.isNotEmpty()
    }
}

object SearchHistoryCodec {
    const val MAX_HISTORY_ITEMS = 10

    fun decode(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_HISTORY_ITEMS)
            .toList()
    }

    fun encode(items: List<String>): String {
        return items
            .map { it.replace('\n', ' ').replace('\r', ' ').trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_HISTORY_ITEMS)
            .joinToString("\n")
    }

    fun record(current: List<String>, newQuery: String, minLength: Int = 2): List<String> {
        val clean = newQuery.replace('\n', ' ').replace('\r', ' ').trim()
        if (clean.length < minLength) return current
        val updated = ArrayList<String>(MAX_HISTORY_ITEMS)
        updated.add(clean)
        for (item in current) {
            if (item != clean) {
                updated.add(item)
            }
            if (updated.size >= MAX_HISTORY_ITEMS) break
        }
        return updated
    }
}

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
    val deletedCount: Int = 0,
    val cancelled: Boolean = false
)
