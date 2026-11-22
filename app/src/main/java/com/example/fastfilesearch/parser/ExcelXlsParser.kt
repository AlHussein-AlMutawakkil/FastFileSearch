package com.example.fastfilesearch.parser

import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import jxl.Workbook
import java.io.InputStream

object ExcelXlsParser {

    /**
     * Extracts text from legacy Excel (.xls) files using JExcelApi (jxl).
     * Records sheet name and 1-based row number.
     * Preserves empty cells ("") to maintain column positions, trims trailing empty cells only,
     * and extracts real column headers from the first 5 non-empty rows.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val workbook = Workbook.getWorkbook(inputStream)
        try {
            for (sheet in workbook.sheets) {
                val candidateHeaderRows = mutableListOf<List<String>>()

                for (rowIndex in 0 until sheet.rows) {
                    val rowCells = sheet.getRow(rowIndex)
                    val cells = mutableListOf<String>()

                    for (cell in rowCells) {
                        val contents = cell?.contents?.replace("[\t\r\n]+".toRegex(), " ")?.trim() ?: ""
                        cells.add(contents)
                    }

                    // Remove trailing empty cells from the end of the row only
                    while (cells.isNotEmpty() && cells.last().isEmpty()) {
                        cells.removeAt(cells.size - 1)
                    }

                    if (cells.any { it.isNotEmpty() }) {
                        if (candidateHeaderRows.size < 5) {
                            candidateHeaderRows.add(cells.toList())
                        }

                        val rowContent = cells.joinToString("\t")
                        val normalized = ArabicNormalizer.normalize(rowContent)
                        if (normalized.isNotEmpty()) {
                            onSegmentFound(
                                FileSegment(
                                    fileUri = fileUri,
                                    fileName = fileName,
                                    location = "ورقة: ${sheet.name}، صف: ${rowIndex + 1}",
                                    content = rowContent,
                                    normalizedContent = normalized
                                )
                            )
                        }
                    }
                }

                // Determine true column headers from the first non-empty candidate rows
                val headers = selectSheetHeaders(candidateHeaderRows)
                if (headers.isNotEmpty()) {
                    onHeaderFound(sheet.name, headers)
                }
            }
        } finally {
            workbook.close()
        }
    }

    private fun selectSheetHeaders(candidateRows: List<List<String>>): List<String> {
        if (candidateRows.isEmpty()) return emptyList()
        val multiCellRow = candidateRows.firstOrNull { row ->
            row.count { it.isNotBlank() } > 1
        }
        if (multiCellRow != null) {
            return multiCellRow
        }
        val mostFilledRow = candidateRows.maxByOrNull { row ->
            row.count { it.isNotBlank() }
        }
        return mostFilledRow ?: emptyList()
    }
}
