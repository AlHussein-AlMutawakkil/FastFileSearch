package com.example.fastfilesearch.desktop.util

import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectedRowData
import com.example.fastfilesearch.desktop.ui.selection.SelectionMode
import com.example.fastfilesearch.desktop.ui.selection.SelectionState

object DesktopValueInteractionHelper {

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

    fun formatMultipleRows(rows: List<SelectedRowData>, vertical: Boolean): String {
        val delimiter = if (vertical) "\n----------------------------------------\n" else "\n"
        return rows.joinToString(delimiter) { row ->
            if (row.isDocument) {
                if (row.location.isNotBlank()) {
                    "${row.fileName} (${row.location}):\n${row.snippet}"
                } else {
                    "${row.fileName}:\n${row.snippet}"
                }
            } else {
                val header = "صف ${row.rowNumber}:"
                val body = formatRow(row.cells, row.headers, vertical)
                if (vertical) "$header\n$body" else "$header $body"
            }
        }
    }

    fun formatMultipleCells(cells: List<SelectedCellData>, vertical: Boolean): String {
        val groupedByRow = cells
            .sortedWith(compareBy({ it.fileName }, { it.sheetName }, { it.key.rowIndex }, { it.key.colIndex }))
            .groupBy { "${it.fileName}_${it.sheetName}_${it.key.rowIndex}" }
        val delimiter = if (vertical) "\n----------------------------------------\n" else "\n"
        return groupedByRow.values.joinToString(delimiter) { cellsInRow ->
            val first = cellsInRow.first()
            val rowHeader = "${first.fileName} • ${first.sheetName} • صف ${first.rowNumber}:"
            val cellParts = cellsInRow.map { "${it.columnName}: ${it.value}" }
            val cellDelimiter = if (vertical) "\n" else " - "
            "$rowHeader\n" + cellParts.joinToString(cellDelimiter)
        }
    }

    /**
     * PC-native clipboard format for Ctrl+C:
     * - Single cell -> raw cell value (just like Excel)
     * - Rectangular / single-sheet multi-cell selection -> TSV (Tab between columns, Newline between rows)
     * - Multi-sheet cells -> labeled grouped format
     * - Selected rows -> TSV when tabular & formattedCopy == false, or labeled vertical/horizontal rows
     */
    fun formatSelectionForKeyboardCopy(
        selectionState: SelectionState,
        verticalCopyFormat: Boolean,
        preferFormattedLabels: Boolean = false
    ): String {
        return when (selectionState.mode) {
            SelectionMode.NONE -> ""
            SelectionMode.CELLS -> {
                val cells = selectionState.selectedCells.values.toList()
                if (cells.isEmpty()) return ""
                if (cells.size == 1 && !preferFormattedLabels) {
                    return cells.first().value
                }
                val distinctSheets = cells.map { it.key.fileUri to it.key.sheetName }.distinct()
                if (distinctSheets.size == 1 && !preferFormattedLabels) {
                    val rowIndices = cells.map { it.key.rowIndex }.distinct().sorted()
                    val colIndices = cells.map { it.key.colIndex }.distinct().sorted()
                    val byKey = cells.associateBy { it.key.rowIndex to it.key.colIndex }
                    rowIndices.joinToString("\n") { r ->
                        colIndices.joinToString("\t") { c ->
                            byKey[r to c]?.value.orEmpty()
                        }
                    }
                } else {
                    formatMultipleCells(cells, verticalCopyFormat)
                }
            }
            SelectionMode.ROWS -> {
                val rows = selectionState.selectedRows.values
                    .sortedWith(compareBy({ it.fileName }, { it.sheetName }, { it.key.rowIndex }))
                if (rows.isEmpty()) return ""
                if (!preferFormattedLabels) {
                    if (rows.all { it.isDocument }) {
                        return rows.joinToString("\n") { it.snippet }
                    }
                    if (rows.none { it.isDocument }) {
                        val distinctSheets = rows.map { it.key.fileUri to it.key.sheetName }.distinct()
                        if (distinctSheets.size == 1) {
                            return rows.joinToString("\n") { row -> row.cells.joinToString("\t") }
                        }
                    }
                }
                formatMultipleRows(rows, verticalCopyFormat)
            }
        }
    }
}
