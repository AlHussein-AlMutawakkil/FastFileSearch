package com.example.fastfilesearch.model

enum class SearchMethod(val displayName: String, val description: String) {
    CONTAINS("يحتوي على", "مطابقة النص حتى في وسط الكلمة"),
    ALL_WORDS("جميع الكلمات", "يشترط وجود جميع الكلمات في نفس السطر"),
    EXACT_PHRASE("العبارة بالضبط", "مطابقة العبارة بترتيب كلماتها نصاً"),
    WHOLE_WORD("الكلمة بالكامل", "مطابقة الكلمة المستقلة بالكامل")
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
    val selectedFileUris: Set<String> = emptySet()
) {
    val isFilterActive: Boolean
        get() = method != SearchMethod.CONTAINS ||
                !ignoreDiacritics ||
                selectedExtensions.isNotEmpty() ||
                selectedFileUris.isNotEmpty()
}
