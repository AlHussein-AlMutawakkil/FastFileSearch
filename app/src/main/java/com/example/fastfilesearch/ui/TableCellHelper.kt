package com.example.fastfilesearch.ui

import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.ui.selection.RowKey
import com.example.fastfilesearch.ui.selection.SelectedRowData
import com.example.fastfilesearch.util.ArabicNormalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TableCellHelper {

    private val WHITESPACE_REGEX = "\\s+".toRegex()

    fun extractSheetAndRow(location: String): Pair<String, String> {
        var sheet = "ورقة 1"
        var row = "1"

        if (location.contains("ورقة:")) {
            val parts = location.split("،")
            sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim() ?: "ورقة 1"
            row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
        } else if (location.contains("سطر")) {
            sheet = "بيانات جدولية"
            row = location.replace("سطر", "").replace(":", "").trim()
        } else {
            row = location.filter { it.isDigit() }.ifEmpty { "1" }
        }

        return Pair(sheet, row)
    }

    fun buildSelectableRowsForFile(
        fileUri: String,
        fileName: String,
        isDocument: Boolean,
        results: List<SearchResult>,
        combineDocumentRows: Boolean = false
    ): List<SelectedRowData> {
        if (results.isEmpty()) return emptyList()
        if (isDocument) {
            if (combineDocumentRows) {
                val combinedText = results
                    .map { it.snippet.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n\n")
                return listOf(
                    SelectedRowData(
                        key = RowKey(fileUri, "مستند", 0),
                        fileName = fileName,
                        sheetName = "مستند",
                        rowNumber = "1",
                        cells = emptyList(),
                        headers = emptyList(),
                        isDocument = true,
                        snippet = combinedText,
                        location = results.firstOrNull()?.location ?: ""
                    )
                )
            }
            return results.mapIndexed { idx, res ->
                SelectedRowData(
                    key = RowKey(fileUri, "مستند", idx),
                    fileName = fileName,
                    sheetName = "مستند",
                    rowNumber = "${idx + 1}",
                    cells = emptyList(),
                    headers = emptyList(),
                    isDocument = true,
                    snippet = res.snippet,
                    location = res.location
                )
            }
        }

        val sheetGroups = results.groupBy { res ->
            res.sheetName ?: extractSheetAndRow(res.location).first
        }
        val list = mutableListOf<SelectedRowData>()
        sheetGroups.forEach { (sheetName, sheetResults) ->
            val sortedResults = sheetResults.sortedBy { res ->
                res.rowNumber ?: extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
            }
            val sheetHeaders = sortedResults.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            sortedResults.forEachIndexed { index, res ->
                val rowNum = res.rowNumber?.toString() ?: extractSheetAndRow(res.location).second
                val cells = splitIntoCells(res.snippet)
                list.add(
                    SelectedRowData(
                        key = RowKey(fileUri = fileUri, sheetName = sheetName, rowIndex = index),
                        fileName = fileName,
                        sheetName = sheetName,
                        rowNumber = rowNum,
                        cells = cells,
                        headers = sheetHeaders,
                        isDocument = false
                    )
                )
            }
        }
        return list
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes < 0L) return "غير معروف"
        if (bytes < 1024L) return "$bytes بايت"
        val kb = bytes / 1024.0
        if (kb < 1024.0) return String.format(Locale.US, "%.1f كيلوبايت", kb)
        val mb = kb / 1024.0
        if (mb < 1024.0) return String.format(Locale.US, "%.2f ميجابايت", mb)
        val gb = mb / 1024.0
        return String.format(Locale.US, "%.2f جيجابايت", gb)
    }

    fun formatTimestamp(epochMillis: Long): String {
        if (epochMillis <= 0L) return "غير معروف"
        return SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date(epochMillis))
    }

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
