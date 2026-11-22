package com.example.fastfilesearch.ui

import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.util.ArabicNormalizer

object TableCellHelper {

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    /**
     * Returns true if the file is a Word (.docx) or PDF (.pdf) document.
     */
    fun isDocumentType(fileName: String): Boolean {
        val ext = FileParserDispatcher.getExtension(fileName)
        return ext == "docx" || ext == "pdf"
    }

    /**
     * Determines whether the result should be displayed as a tabular grid of cells.
     */
    fun isTabular(fileName: String, content: String): Boolean {
        val ext = FileParserDispatcher.getExtension(fileName)
        if (ext in listOf("xlsx", "xls", "csv")) return true
        if (content.contains("\t") || content.contains(" | ")) return true
        if (content.count { it == ',' } >= 2) return true
        if (content.count { it == ';' } >= 2) return true
        if (content.count { it == '|' } >= 2) return true
        return false
    }

    /**
     * Splits a row into individual cell values based on common delimiters.
     * For tab-delimited tabular files, preserves all empty cells to maintain exact column alignment.
     */
    fun splitIntoCells(content: String): List<String> {
        if (content.contains("\t")) {
            return content.split("\t").map { it.trim() }
        }

        val rawCells = when {
            content.contains(" | ") -> content.split(" | ")
            content.count { it == ';' } >= 2 -> content.split(";")
            content.count { it == ',' } >= 2 -> content.split(",")
            content.contains("|") -> content.split("|")
            else -> listOf(content)
        }

        val cleaned = rawCells.map { it.trim() }
        return if (cleaned.size > 1) cleaned else listOf(content)
    }

    /**
     * Checks if a specific cell matches any token in the search query using Arabic normalization.
     */
    fun isCellMatching(cell: String, query: String): Boolean {
        if (query.isBlank() || cell.isBlank()) return false
        val normalizedCell = ArabicNormalizer.normalize(cell)
        val normalizedQuery = ArabicNormalizer.normalize(query)
        val tokens = normalizedQuery.split(WHITESPACE_REGEX).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        return tokens.any { normalizedCell.contains(it) }
    }
}
