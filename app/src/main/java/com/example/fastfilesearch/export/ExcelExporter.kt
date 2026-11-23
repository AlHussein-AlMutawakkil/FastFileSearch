package com.example.fastfilesearch.export

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.ui.selection.SelectedCellData
import com.example.fastfilesearch.ui.selection.SelectedRowData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExcelExporter {

    private val INVALID_SHEET_CHARS = Regex("""[\\/?*\[\]:]""")

    fun sanitizeSheetName(rawName: String, existingNames: MutableSet<String>): String {
        var clean = rawName.replace(INVALID_SHEET_CHARS, "_").trim()
        if (clean.isBlank()) clean = "ورقة"
        if (clean.length > 31) {
            clean = clean.take(31)
        }
        var candidate = clean
        var counter = 2
        while (existingNames.contains(candidate)) {
            val suffix = "_$counter"
            val maxPrefixLen = (31 - suffix.length).coerceAtLeast(1)
            candidate = clean.take(maxPrefixLen) + suffix
            counter++
        }
        existingNames.add(candidate)
        return candidate
    }

    /**
     * Builds a workbook from selected rows.
     */
    fun buildWorkbookForRows(selectedRows: List<SelectedRowData>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val existingSheetNames = mutableSetOf<String>()

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

        // Separate document rows (Word/PDF) and table rows (Excel/CSV)
        val docRows = selectedRows.filter { it.isDocument }
        val tableRows = selectedRows.filterNot { it.isDocument }

        // Render Table Sheets grouped by file and sheet
        val groupedTableRows = tableRows.groupBy { "${it.fileName}_${it.sheetName}" }
        groupedTableRows.forEach { (_, rows) ->
            val first = rows.first()
            val sheetTitle = if (first.sheetName.isNotBlank() && first.sheetName != "ورقة 1") {
                "${first.fileName}_${first.sheetName}"
            } else {
                first.fileName
            }
            val sanitizedName = sanitizeSheetName(sheetTitle, existingSheetNames)
            val sheet = workbook.createSheet(sanitizedName)
            sheet.isRightToLeft = true

            // Headers
            val maxCols = rows.maxOfOrNull { maxOf(it.cells.size, it.headers.size) }?.coerceAtLeast(1) ?: 1
            val headerRow = sheet.createRow(0)
            val maxLengths = IntArray(maxCols) { 0 }

            for (colIdx in 0 until maxCols) {
                val headerText = first.headers.getOrNull(colIdx)?.takeIf { it.isNotBlank() } ?: "عمود ${colIdx + 1}"
                val cell = headerRow.createCell(colIdx)
                cell.setCellValue(headerText)
                cell.cellStyle = headerStyle
                maxLengths[colIdx] = maxOf(maxLengths[colIdx], headerText.length)
            }

            // Data rows
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

            // Safe column width calculation without autoSizeColumn (AWT crash safe)
            for (colIdx in 0 until maxCols) {
                val calculatedWidth = ((maxLengths[colIdx] + 4) * 256).coerceIn(12 * 256, 60 * 256)
                sheet.setColumnWidth(colIdx, calculatedWidth)
            }
        }

        // Render Document Rows (if any)
        if (docRows.isNotEmpty()) {
            val sanitizedName = sanitizeSheetName("مستندات", existingSheetNames)
            val sheet = workbook.createSheet(sanitizedName)
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

                val c0 = excelRow.createCell(0)
                c0.setCellValue(docRow.fileName)
                c0.cellStyle = cellStyle
                maxLens[0] = maxOf(maxLens[0], docRow.fileName.length)

                val c1 = excelRow.createCell(1)
                c1.setCellValue(docRow.location)
                c1.cellStyle = cellStyle
                maxLens[1] = maxOf(maxLens[1], docRow.location.length)

                val c2 = excelRow.createCell(2)
                c2.setCellValue(docRow.snippet)
                c2.cellStyle = cellStyle
                maxLens[2] = maxOf(maxLens[2], docRow.snippet.length)
            }

            for (colIdx in 0..2) {
                val calculatedWidth = ((maxLens[colIdx] + 4) * 256).coerceIn(15 * 256, 75 * 256)
                sheet.setColumnWidth(colIdx, calculatedWidth)
            }
        }

        return workbook
    }

    /**
     * Builds a workbook from selected cells:
     * Contains only columns that had cells selected, and rows containing selected cells.
     */
    fun buildWorkbookForCells(selectedCells: List<SelectedCellData>): XSSFWorkbook {
        val workbook = XSSFWorkbook()
        val existingSheetNames = mutableSetOf<String>()

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

        val grouped = selectedCells.groupBy { "${it.fileName}_${it.sheetName}" }
        grouped.forEach { (_, cellsInSheet) ->
            val first = cellsInSheet.first()
            val sheetTitle = if (first.sheetName.isNotBlank() && first.sheetName != "ورقة 1") {
                "${first.fileName}_${first.sheetName}"
            } else {
                first.fileName
            }
            val sanitizedName = sanitizeSheetName(sheetTitle, existingSheetNames)
            val sheet = workbook.createSheet(sanitizedName)
            sheet.isRightToLeft = true

            // Distinct column indices present in selected cells, sorted ascending
            val distinctColIndices = cellsInSheet.map { it.key.colIndex }.distinct().sorted()
            val colIndexToOutputCol = distinctColIndices.mapIndexed { outIdx, origIdx -> origIdx to outIdx }.toMap()

            // Header Row (Row 0: Row number title + selected column names)
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

            // Group cells by rowIndex
            val rowsInSheet = cellsInSheet.groupBy { it.key.rowIndex }.toList().sortedBy { it.first }
            rowsInSheet.forEachIndexed { outRowIdx, (rowIndex, rowCells) ->
                val excelRow = sheet.createRow(outRowIdx + 1)

                val rNumCell = excelRow.createCell(0)
                val rowNumStr = rowCells.firstOrNull()?.rowNumber ?: rowIndex.toString()
                rNumCell.setCellValue(rowNumStr)
                rNumCell.cellStyle = cellStyle
                maxLens[0] = maxOf(maxLens[0], rowNumStr.length)

                // Fill columns (empty if unselected in this row)
                distinctColIndices.forEachIndexed { outColIdx, origColIdx ->
                    val cellData = rowCells.firstOrNull { it.key.colIndex == origColIdx }
                    val cellVal = cellData?.value ?: ""
                    val cell = excelRow.createCell(outColIdx + 1)
                    cell.setCellValue(cellVal)
                    cell.cellStyle = cellStyle
                    maxLens[outColIdx + 1] = maxOf(maxLens[outColIdx + 1], cellVal.length)
                }
            }

            // Safe column width calculation
            for (colIdx in 0 until (distinctColIndices.size + 1)) {
                val calculatedWidth = ((maxLens[colIdx] + 4) * 256).coerceIn(12 * 256, 60 * 256)
                sheet.setColumnWidth(colIdx, calculatedWidth)
            }
        }

        return workbook
    }

    /**
     * Builds a workbook from all search results.
     */
    fun buildWorkbookForAllResults(results: List<SearchResult>): XSSFWorkbook {
        val selectedRows = results.mapIndexed { idx, res ->
            val isDoc = TableCellHelper.isDocumentType(res.fileName)
            val cells = if (isDoc) emptyList() else TableCellHelper.splitIntoCells(res.snippet)
            val sheet = res.sheetName ?: "ورقة 1"
            val rowNum = res.rowNumber?.toString() ?: (idx + 1).toString()

            SelectedRowData(
                key = com.example.fastfilesearch.ui.selection.RowKey(res.fileUri, sheet, idx),
                fileName = res.fileName,
                sheetName = sheet,
                rowNumber = rowNum,
                cells = cells,
                headers = res.columnHeaders,
                isDocument = isDoc,
                snippet = res.snippet,
                location = res.location
            )
        }
        return buildWorkbookForRows(selectedRows)
    }

    suspend fun exportRowsToCache(context: Context, selectedRows: List<SelectedRowData>): File = withContext(Dispatchers.IO) {
        cleanOldCachedExports(context)
        val workbook = buildWorkbookForRows(selectedRows)
        writeWorkbookToCache(context, workbook)
    }

    suspend fun exportCellsToCache(context: Context, selectedCells: List<SelectedCellData>): File = withContext(Dispatchers.IO) {
        cleanOldCachedExports(context)
        val workbook = buildWorkbookForCells(selectedCells)
        writeWorkbookToCache(context, workbook)
    }

    suspend fun exportAllResultsToCache(context: Context, results: List<SearchResult>): File = withContext(Dispatchers.IO) {
        cleanOldCachedExports(context)
        val workbook = buildWorkbookForAllResults(results)
        writeWorkbookToCache(context, workbook)
    }

    suspend fun writeWorkbookToStream(workbook: XSSFWorkbook, outputStream: OutputStream) = withContext(Dispatchers.IO) {
        outputStream.use { out ->
            workbook.write(out)
        }
        workbook.close()
    }

    private fun writeWorkbookToCache(context: Context, workbook: XSSFWorkbook): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val file = File(dir, "بحث_آمن_$timestamp.xlsx")
        FileOutputStream(file).use { out ->
            workbook.write(out)
        }
        workbook.close()
        return file
    }

    private fun cleanOldCachedExports(context: Context) {
        try {
            val dir = File(context.cacheDir, "exports")
            if (dir.exists()) {
                val oneHourAgo = System.currentTimeMillis() - 3600_000L
                dir.listFiles()?.forEach { file ->
                    if (file.lastModified() < oneHourAgo) {
                        file.delete()
                    }
                }
            }
        } catch (_: Exception) {}
    }

    fun shareExcelFile(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(sendIntent, "مشاركة كملف Excel")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
