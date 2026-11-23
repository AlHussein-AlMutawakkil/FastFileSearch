package com.example.fastfilesearch.util

object ValueInteractionHelper {
    // Matches international (+123...), local (05..., 01...), or formatted with spaces/dashes/parentheses
    private val PHONE_REGEX = Regex("""(?:\+?\d{1,4}[\s-]?)?(?:\(\d{1,4}\)[\s-]?)?\d{2,4}[\s-]\d{3,4}[\s-]?\d{3,6}|\+?\d{7,15}""")
    private val URL_REGEX = Regex("""https?://[^\s/$.?#].[^\s]*""", RegexOption.IGNORE_CASE)
    private val EMAIL_REGEX = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")

    fun extractPhoneNumbers(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return PHONE_REGEX.findAll(text)
            .map { it.value.trim() }
            .filter { candidate ->
                val digits = candidate.filter { it.isDigit() }
                digits.length in 7..15
            }
            .distinct()
            .toList()
    }

    fun extractUrls(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return URL_REGEX.findAll(text)
            .map { it.value.trim() }
            .distinct()
            .toList()
    }

    fun extractEmails(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        return EMAIL_REGEX.findAll(text)
            .map { it.value.trim() }
            .distinct()
            .toList()
    }

    fun formatRow(cells: List<String>, headers: List<String>, vertical: Boolean): String {
        val pairs = cells.mapIndexedNotNull { index, cell ->
            if (cell.isNotBlank()) {
                val colName = headers.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "عمود ${index + 1}"
                "$colName: $cell"
            } else null
        }
        val delimiter = if (vertical) "\n" else " - "
        return if (pairs.isNotEmpty()) {
            pairs.joinToString(delimiter)
        } else {
            cells.filter { it.isNotBlank() }.joinToString(delimiter)
        }
    }

    fun formatMultipleRows(rows: List<com.example.fastfilesearch.ui.selection.SelectedRowData>, vertical: Boolean): String {
        val delimiter = if (vertical) "\n----------------------------------------\n" else "\n"
        return rows.joinToString(delimiter) { row ->
            if (row.isDocument) {
                "${row.fileName} (${row.location}):\n${row.snippet}"
            } else {
                "صف ${row.rowNumber}:\n" + formatRow(row.cells, row.headers, vertical)
            }
        }
    }

    fun formatMultipleCells(cells: List<com.example.fastfilesearch.ui.selection.SelectedCellData>, vertical: Boolean): String {
        val groupedByRow = cells.groupBy { "${it.fileName}_${it.sheetName}_${it.key.rowIndex}" }
        val delimiter = if (vertical) "\n----------------------------------------\n" else "\n"
        return groupedByRow.values.joinToString(delimiter) { cellsInRow ->
            val first = cellsInRow.first()
            val rowHeader = "${first.fileName} • ${first.sheetName} • صف ${first.rowNumber}:"
            val cellParts = cellsInRow.map { "${it.columnName}: ${it.value}" }
            val cellDelimiter = if (vertical) "\n" else " - "
            "$rowHeader\n" + cellParts.joinToString(cellDelimiter)
        }
    }
}
