package com.example.fastfilesearch.parser

import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.util.ArabicNormalizer
import jxl.Workbook
import java.io.InputStream

object ExcelXlsParser {

    /**
     * Extracts text from legacy Excel (.xls) files using JExcelApi (jxl).
     * Records sheet name and 1-based row number.
     */
    fun parse(
        inputStream: InputStream,
        fileUri: String,
        fileName: String,
        onSegmentFound: (FileSegment) -> Unit
    ) {
        val workbook = Workbook.getWorkbook(inputStream)
        try {
            for (sheet in workbook.sheets) {
                for (rowIndex in 0 until sheet.rows) {
                    val rowCells = sheet.getRow(rowIndex)
                    val cells = mutableListOf<String>()
                    for (cell in rowCells) {
                        val contents = cell?.contents?.trim()
                        if (!contents.isNullOrEmpty()) {
                            cells.add(contents)
                        }
                    }

                    if (cells.isNotEmpty()) {
                        val rowContent = cells.joinToString(" | ")
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
            }
        } finally {
            workbook.close()
        }
    }
}
