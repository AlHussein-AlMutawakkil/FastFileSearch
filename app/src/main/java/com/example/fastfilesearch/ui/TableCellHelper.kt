package com.example.fastfilesearch.ui

import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.util.ArabicNormalizer

object TableCellHelper {

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
        if (content.contains(" | ") || content.contains("\t")) return true
        if (content.count { it == ',' } >= 2) return true
        if (content.count { it == ';' } >= 2) return true
        if (content.count { it == '|' } >= 2) return true
        return false
    }

    /**
     * Splits a row into individual cell values based on common delimiters.
     */
    fun splitIntoCells(content: String): List<String> {
        val rawCells = when {
            content.contains(" | ") -> content.split(" | ")
            content.contains("\t") -> content.split("\t")
            content.count { it == ';' } >= 2 -> content.split(";")
            content.count { it == ',' } >= 2 -> content.split(",")
            content.contains("|") -> content.split("|")
            else -> listOf(content)
        }

        val cleaned = rawCells.map { it.trim() }.filter { it.isNotEmpty() }
        return if (cleaned.isNotEmpty()) cleaned else listOf(content)
    }

    /**
     * Checks if a specific cell matches any token in the search query using Arabic normalization.
     */
    fun isCellMatching(cell: String, query: String): Boolean {
        if (query.isBlank() || cell.isBlank()) return false
        val normalizedCell = ArabicNormalizer.normalize(cell)
        val normalizedQuery = ArabicNormalizer.normalize(query)
        val tokens = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return false
        return tokens.any { normalizedCell.contains(it) }
    }
}
