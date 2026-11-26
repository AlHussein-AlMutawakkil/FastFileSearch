package com.example.fastfilesearch.desktop.export

import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.ui.DesktopTableCellHelper
import com.example.fastfilesearch.desktop.ui.selection.RowKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectedRowData
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * Exports desktop search results, selected rows, or selected cells to styled RTL .xlsx workbooks via Apache POI.
 */
object DesktopExcelExporter {

    private val INVALID_SHEET_CHARS = Regex("""[\\/?*\[\]:]""")

    fun sanitizeSheetName(rawName: String, existingNames: MutableSet<String>): String {
        var clean = rawName.replace(INVALID_SHEET_CHARS, "_").trim()
        if (clean.isBlank()) clean = "ورقة"
        if (clean.length > 31) clean = clean.take(31)
        var candidate = clean
        var counter = 2
        while (existingNames.contains(candidate)) {
            val suffix = "_$counter"
            val maxPrefix = (31 - suffix.length).coerceAtLeast(1)
            candidate = clean.take(maxPrefix) + suffix
            counter++
        }
        existingNames.add(candidate)
        return candidate
    }

    private fun createStyles(workbook: XSSFWorkbook): Pair<XSSFCellStyle, XSSFCellStyle> {
        val headerFont = workbook.createFont().apply {
            bold = true
            fontHeightInPoints = 11.toShort()
        }
        val headerStyle = workbook.createCellStyle().apply {
            setFont(headerFont)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
            alignment = HorizontalAlignment.CENTER
        }
        val cellStyle = workbook.createCellStyle().apply {
            borderBottom = BorderStyle.THIN
            borderTop = BorderStyle.THIN
            borderLeft = BorderStyle.THIN
            borderRight = BorderStyle.THIN
        }
        return headerStyle to cellStyle
    }

    fun buildWorkbookForRows(selectedRows: List<SelectedRowData>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val existingSheets = mutableSetOf<String>()
        val (headerStyle, cellStyle) = createStyles(workbook)

        val docRows = selectedRows.filter { it.isDocument }
        val tableRows = selectedRows.filterNot { it.isDocument }

        val groupedTables = tableRows.groupBy { "${it.fileName}_${it.sheetName}" }
        groupedTables.forEach { (_, rows) ->
            val first = rows.first()
            val sheetTitle = if (first.sheetName.isNotBlank() && first.sheetName != "ورقة 1") {
                "${first.fileName}_${first.sheetName}"
            } else {
                first.fileName
            }
            val sheet = workbook.createSheet(sanitizeSheetName(sheetTitle, existingSheets))
            sheet.isRightToLeft = true

            val maxCols = rows.maxOfOrNull { maxOf(it.cells.size, it.headers.size) }?.coerceAtLeast(1) ?: 1
            val headerRow = sheet.createRow(0)
            val maxLengths = IntArray(maxCols)

            for (colIdx in 0 until maxCols) {
                val title = first.headers.getOrNull(colIdx)?.takeIf { it.isNotBlank() } ?: "عمود ${colIdx + 1}"
                val cell = headerRow.createCell(colIdx)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
                maxLengths[colIdx] = maxOf(maxLengths[colIdx], title.length)
            }

            rows.forEachIndexed { rIdx, rowData ->
                val excelRow = sheet.createRow(rIdx + 1)
                for (colIdx in 0 until maxCols) {
                    val value = rowData.cells.getOrNull(colIdx) ?: ""
                    val cell = excelRow.createCell(colIdx)
                    cell.setCellValue(value)
                    cell.cellStyle = cellStyle
                    maxLengths[colIdx] = maxOf(maxLengths[colIdx], value.length)
                }
            }

            for (colIdx in 0 until maxCols) {
                sheet.setColumnWidth(colIdx, ((maxLengths[colIdx] + 4) * 256).coerceIn(12 * 256, 60 * 256))
            }
        }

        if (docRows.isNotEmpty()) {
            val sheet = workbook.createSheet(sanitizeSheetName("مستندات", existingSheets))
            sheet.isRightToLeft = true

            val docHeaders = listOf("الملف", "الموقع/الصفحة", "المحتوى")
            val headerRow = sheet.createRow(0)
            val maxLens = intArrayOf(docHeaders[0].length, docHeaders[1].length, docHeaders[2].length)

            docHeaders.forEachIndexed { colIdx, title ->
                val cell = headerRow.createCell(colIdx)
                cell.setCellValue(title)
                cell.cellStyle = headerStyle
            }

            docRows.forEachIndexed { rIdx, docRow ->
                val excelRow = sheet.createRow(rIdx + 1)
                val vals = listOf(docRow.fileName, docRow.location, docRow.snippet)
                vals.forEachIndexed { cIdx, v ->
                    val cell = excelRow.createCell(cIdx)
                    cell.setCellValue(v)
                    cell.cellStyle = cellStyle
                    maxLens[cIdx] = maxOf(maxLens[cIdx], v.length)
                }
            }

            for (colIdx in 0..2) {
                sheet.setColumnWidth(colIdx, ((maxLens[colIdx] + 4) * 256).coerceIn(15 * 256, 75 * 256))
            }
        }

        return workbook
    }

    fun buildWorkbookForCells(selectedCells: List<SelectedCellData>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val existingSheets = mutableSetOf<String>()
        val (headerStyle, cellStyle) = createStyles(workbook)

        val grouped = selectedCells.groupBy { "${it.fileName}_${it.sheetName}" }
        grouped.forEach { (_, cellsInSheet) ->
            val first = cellsInSheet.first()
            val sheetTitle = if (first.sheetName.isNotBlank() && first.sheetName != "ورقة 1") {
                "${first.fileName}_${first.sheetName}"
            } else {
                first.fileName
            }
            val sheet = workbook.createSheet(sanitizeSheetName(sheetTitle, existingSheets))
            sheet.isRightToLeft = true

            val distinctColIndices = cellsInSheet.map { it.key.colIndex }.distinct().sorted()
            val headerRow = sheet.createRow(0)
            val rowNumHeaderCell = headerRow.createCell(0)
            rowNumHeaderCell.setCellValue("رقم الصف")
            rowNumHeaderCell.cellStyle = headerStyle

            val maxLens = IntArray(distinctColIndices.size + 1) { 10 }
            maxLens[0] = 8

            distinctColIndices.forEachIndexed { outIdx, origIdx ->
                val colName = cellsInSheet.firstOrNull { it.key.colIndex == origIdx }?.columnName ?: "عمود ${origIdx + 1}"
                val cell = headerRow.createCell(outIdx + 1)
                cell.setCellValue(colName)
                cell.cellStyle = headerStyle
                maxLens[outIdx + 1] = maxOf(maxLens[outIdx + 1], colName.length)
            }

            val rowsInSheet = cellsInSheet.groupBy { it.key.rowIndex }.toList().sortedBy { it.first }
            rowsInSheet.forEachIndexed { outRowIdx, (rowIndex, rowCells) ->
                val excelRow = sheet.createRow(outRowIdx + 1)

                val rNumCell = excelRow.createCell(0)
                val rowNumStr = rowCells.firstOrNull()?.rowNumber ?: rowIndex.toString()
                rNumCell.setCellValue(rowNumStr)
                rNumCell.cellStyle = cellStyle
                maxLens[0] = maxOf(maxLens[0], rowNumStr.length)

                distinctColIndices.forEachIndexed { outColIdx, origColIdx ->
                    val cellVal = rowCells.firstOrNull { it.key.colIndex == origColIdx }?.value ?: ""
                    val cell = excelRow.createCell(outColIdx + 1)
                    cell.setCellValue(cellVal)
                    cell.cellStyle = cellStyle
                    maxLens[outColIdx + 1] = maxOf(maxLens[outColIdx + 1], cellVal.length)
                }
            }

            for (colIdx in 0 until (distinctColIndices.size + 1)) {
                sheet.setColumnWidth(colIdx, ((maxLens[colIdx] + 4) * 256).coerceIn(12 * 256, 60 * 256))
            }
        }

        return workbook
    }

    fun buildWorkbookForResults(results: List<SearchResult>): XSSFWorkbook {
        val selectedRows = results.mapIndexed { idx, res ->
            val isTabular = DesktopTableCellHelper.isTabular(res.fileName, res.snippet)
            val sheet = res.sheetName ?: DesktopTableCellHelper.extractSheetAndRow(res.location).first
            val rowNum = res.rowNumber?.toString() ?: (idx + 1).toString()
            val cells = if (isTabular) DesktopTableCellHelper.splitIntoCells(res.snippet) else emptyList()
            SelectedRowData(
                key = RowKey(res.fileUri, sheet, idx),
                fileName = res.fileName,
                sheetName = sheet,
                rowNumber = rowNum,
                cells = cells,
                headers = res.columnHeaders,
                isDocument = !isTabular,
                snippet = res.snippet,
                location = res.location
            )
        }
        return buildWorkbookForRows(selectedRows)
    }

    fun exportToFile(results: List<SearchResult>, outputFile: File) {
        writeWorkbookToFile(buildWorkbookForResults(results), outputFile)
    }

    fun exportRowsToFile(selectedRows: List<SelectedRowData>, outputFile: File) {
        writeWorkbookToFile(buildWorkbookForRows(selectedRows), outputFile)
    }

    fun exportCellsToFile(selectedCells: List<SelectedCellData>, outputFile: File) {
        writeWorkbookToFile(buildWorkbookForCells(selectedCells), outputFile)
    }

    private fun writeWorkbookToFile(workbook: XSSFWorkbook, outputFile: File) {
        val target = if (outputFile.name.endsWith(".xlsx", ignoreCase = true)) {
            outputFile
        } else {
            File(outputFile.parentFile, "${outputFile.name}.xlsx")
        }
        target.parentFile?.mkdirs()
        workbook.use { wb ->
            FileOutputStream(target).use { fos ->
                wb.write(fos)
            }
        }
    }
}
