package com.example.fastfilesearch.desktop.parser

import com.example.fastfilesearch.desktop.model.FileSegment
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.File
import java.util.Locale

/**
 * Extracts sheets, headers, and rows from Excel (.xlsx / .xls) via Apache POI in Read-Only mode.
 */
object DesktopExcelParser {

    fun parseFile(
        file: File,
        fileUri: String = file.absolutePath,
        fileName: String = file.name,
        onHeaderFound: (sheetName: String, headers: List<String>) -> Unit = { _, _ -> },
        onSegmentFound: (FileSegment) -> Unit
    ) {
        if (!file.exists() || !file.isFile || file.length() == 0L) return

        val formatter = DataFormatter(Locale.US)
        try {
            WorkbookFactory.create(file, null, true).use { workbook ->
                val evaluator = try {
                    workbook.creationHelper.createFormulaEvaluator()
                } catch (_: Throwable) {
                    null
                }

                for (sheetIdx in 0 until workbook.numberOfSheets) {
                    val sheet = workbook.getSheetAt(sheetIdx) ?: continue
                    val sheetName = sheet.sheetName?.takeIf { it.isNotBlank() } ?: "ورقة ${sheetIdx + 1}"
                    val candidateHeaderRows = mutableListOf<List<String>>()

                    for (row in sheet) {
                        val lastCellNum = row.lastCellNum.toInt()
                        if (lastCellNum <= 0) continue

                        val rowCells = ArrayList<String>(lastCellNum)
                        for (c in 0 until lastCellNum) {
                            val cell = row.getCell(c)
                            val raw = if (cell == null) {
                                ""
                            } else {
                                try {
                                    if (evaluator != null) {
                                        formatter.formatCellValue(cell, evaluator)
                                    } else {
                                        formatter.formatCellValue(cell)
                                    }
                                } catch (_: Throwable) {
                                    try {
                                        formatter.formatCellValue(cell)
                                    } catch (_: Throwable) {
                                        cell.toString()
                                    }
                                }
                            }
                            rowCells.add(raw.replace("[\t\r\n]+".toRegex(), " ").trim())
                        }

                        while (rowCells.isNotEmpty() && rowCells.last().isEmpty()) {
                            rowCells.removeAt(rowCells.size - 1)
                        }

                        if (rowCells.any { it.isNotEmpty() }) {
                            if (candidateHeaderRows.size < 5) {
                                candidateHeaderRows.add(rowCells.toList())
                            }
                            val rowText = rowCells.joinToString("\t")
                            val normalized = ArabicNormalizer.normalize(rowText)
                            if (normalized.isNotEmpty()) {
                                val displayRowNum = row.rowNum + 1
                                onSegmentFound(
                                    FileSegment(
                                        fileUri = fileUri,
                                        fileName = fileName,
                                        location = "ورقة: $sheetName، صف: $displayRowNum",
                                        content = rowText,
                                        normalizedContent = normalized
                                    )
                                )
                            }
                        }
                    }

                    val headers = selectSheetHeaders(candidateHeaderRows)
                    if (headers.isNotEmpty()) {
                        onHeaderFound(sheetName, headers)
                    }
                }
            }
        } catch (_: Throwable) {
            // Skip encrypted / corrupt Excel files safely
        }
    }

    private fun selectSheetHeaders(candidateRows: List<List<String>>): List<String> {
        if (candidateRows.isEmpty()) return emptyList()
        return candidateRows.firstOrNull { row -> row.count { it.isNotBlank() } > 1 }
            ?: candidateRows.maxByOrNull { row -> row.count { it.isNotBlank() } }
            ?: emptyList()
    }
}
