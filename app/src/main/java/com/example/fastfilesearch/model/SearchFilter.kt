package com.example.fastfilesearch.model

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

data class FilteredFileType(
    val id: String,
    val displayName: String,
    val extensions: Set<String>
)

object SupportedFileTypeGroups {
    val EXCEL = FilteredFileType("excel", "Excel", setOf("xlsx", "xls"))
    val WORD = FilteredFileType("word", "Word", setOf("docx", "doc"))
    val PDF = FilteredFileType("pdf", "PDF", setOf("pdf"))
    val TEXT = FilteredFileType("text", "نصوص", setOf("txt", "log"))
    val CSV = FilteredFileType("csv", "CSV", setOf("csv"))
    val JSON = FilteredFileType("json", "JSON", setOf("json"))
    val MARKDOWN = FilteredFileType("markdown", "Markdown", setOf("md"))

    val ALL_GROUPS = listOf(
        EXCEL,
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
