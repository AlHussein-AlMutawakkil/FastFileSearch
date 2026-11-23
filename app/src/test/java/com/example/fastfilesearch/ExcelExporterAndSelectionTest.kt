package com.example.fastfilesearch

import com.example.fastfilesearch.export.ExcelExporter
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.selection.CellKey
import com.example.fastfilesearch.ui.selection.RowKey
import com.example.fastfilesearch.ui.selection.SelectedCellData
import com.example.fastfilesearch.ui.selection.SelectedRowData
import com.example.fastfilesearch.ui.selection.SelectionMode
import com.example.fastfilesearch.ui.selection.SelectionState
import com.example.fastfilesearch.util.ValueInteractionHelper
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class ExcelExporterAndSelectionTest {

    @Test
    fun testSelectionStateMachineTransitions() {
        var state = SelectionState()
        assertEquals(SelectionMode.NONE, state.mode)
        assertFalse(state.isActive)
        assertEquals(0, state.count)

        // Enter ROWS mode by selecting a row
        val rowKey1 = RowKey("file://test.xlsx", "Sheet1", 1)
        val row1 = SelectedRowData(
            key = rowKey1,
            fileName = "test.xlsx",
            sheetName = "Sheet1",
            rowNumber = "2",
            cells = listOf("محمد", "مهندس"),
            headers = listOf("الاسم", "الوظيفة")
        )
        state = state.toggleRow(row1)
        assertEquals(SelectionMode.ROWS, state.mode)
        assertTrue(state.isActive)
        assertEquals(1, state.count)
        assertTrue(state.isRowSelected(rowKey1))
        assertEquals("صف واحد", state.getFormattedCountArabic())

        // Add second row
        val rowKey2 = RowKey("file://test.xlsx", "Sheet1", 2)
        val row2 = SelectedRowData(
            key = rowKey2,
            fileName = "test.xlsx",
            sheetName = "Sheet1",
            rowNumber = "3",
            cells = listOf("أحمد", "محاسب"),
            headers = listOf("الاسم", "الوظيفة")
        )
        state = state.toggleRow(row2)
        assertEquals(2, state.count)
        assertEquals("صفان", state.getFormattedCountArabic())

        // Add third row
        val rowKey3 = RowKey("file://test.xlsx", "Sheet1", 3)
        val row3 = SelectedRowData(
            key = rowKey3,
            fileName = "test.xlsx",
            sheetName = "Sheet1",
            rowNumber = "4",
            cells = listOf("علي", "مدير"),
            headers = listOf("الاسم", "الوظيفة")
        )
        state = state.toggleRow(row3)
        assertEquals(3, state.count)
        assertEquals("3 صفوف", state.getFormattedCountArabic())

        // Remove row 1 & 2
        state = state.toggleRow(row1)
        state = state.toggleRow(row2)
        assertEquals(1, state.count)

        // Remove the last row -> auto exit to NONE
        state = state.toggleRow(row3)
        assertEquals(SelectionMode.NONE, state.mode)
        assertFalse(state.isActive)
        assertEquals(0, state.count)

        // Enter CELLS mode
        val cellKey1 = CellKey("file://test.xlsx", "Sheet1", 1, 0)
        val cell1 = SelectedCellData(
            key = cellKey1,
            fileName = "test.xlsx",
            sheetName = "Sheet1",
            rowNumber = "2",
            columnName = "الاسم",
            value = "محمد"
        )
        state = state.toggleCell(cell1)
        assertEquals(SelectionMode.CELLS, state.mode)
        assertTrue(state.isActive)
        assertEquals(1, state.count)
        assertTrue(state.isCellSelected(cellKey1))
        assertEquals("خلية واحدة", state.getFormattedCountArabic())

        val cellKey2 = CellKey("file://test.xlsx", "Sheet1", 1, 1)
        val cell2 = SelectedCellData(
            key = cellKey2,
            fileName = "test.xlsx",
            sheetName = "Sheet1",
            rowNumber = "2",
            columnName = "الوظيفة",
            value = "مهندس"
        )
        state = state.toggleCell(cell2)
        assertEquals(2, state.count)
        assertEquals("خليتان", state.getFormattedCountArabic())

        // Clear resets everything
        state = state.clear()
        assertEquals(SelectionMode.NONE, state.mode)
        assertFalse(state.isActive)
        assertEquals(0, state.count)
    }

    @Test
    fun testSelectAllAndFormatting() {
        val row1 = SelectedRowData(
            key = RowKey("uri1", "ورقة 1", 0),
            fileName = "data.xlsx",
            sheetName = "ورقة 1",
            rowNumber = "1",
            cells = listOf("القيمة أ", "القيمة ب"),
            headers = listOf("العمود 1", "العمود 2")
        )
        val row2 = SelectedRowData(
            key = RowKey("uri1", "ورقة 1", 1),
            fileName = "data.xlsx",
            sheetName = "ورقة 1",
            rowNumber = "2",
            cells = listOf("القيمة ج", "القيمة د"),
            headers = listOf("العمود 1", "العمود 2")
        )

        var state = SelectionState().selectAllRows(listOf(row1, row2))
        assertEquals(SelectionMode.ROWS, state.mode)
        assertEquals(2, state.count)

        // Horizontal format
        val textHorizontal = ValueInteractionHelper.formatMultipleRows(listOf(row1, row2), vertical = false)
        assertTrue(textHorizontal.contains("العمود 1: القيمة أ"))
        assertTrue(textHorizontal.contains("العمود 2: القيمة د"))

        // Vertical format
        val textVertical = ValueInteractionHelper.formatMultipleRows(listOf(row1, row2), vertical = true)
        assertTrue(textVertical.contains("----------------------------------------"))
        assertTrue(textVertical.contains("صف 1:"))
        assertTrue(textVertical.contains("صف 2:"))

        // Cells formatting
        val cell1 = SelectedCellData(
            key = CellKey("uri1", "ورقة 1", 0, 0),
            fileName = "data.xlsx",
            sheetName = "ورقة 1",
            rowNumber = "1",
            columnName = "العمود 1",
            value = "القيمة أ"
        )
        val cell2 = SelectedCellData(
            key = CellKey("uri1", "ورقة 1", 0, 1),
            fileName = "data.xlsx",
            sheetName = "ورقة 1",
            rowNumber = "1",
            columnName = "العمود 2",
            value = "القيمة ب"
        )

        val cellText = ValueInteractionHelper.formatMultipleCells(listOf(cell1, cell2), vertical = false)
        assertTrue(cellText.contains("العمود 1: القيمة أ"))
        assertTrue(cellText.contains("العمود 2: القيمة ب"))
    }

    @Test
    fun testSanitizeSheetName() {
        val set = mutableSetOf<String>()

        // Strips invalid characters \ / ? * [ ] :
        val cleaned = ExcelExporter.sanitizeSheetName("تقرير/المبيعات*2024[1]:أكتوبر?", set)
        assertFalse(cleaned.contains("/"))
        assertFalse(cleaned.contains("*"))
        assertFalse(cleaned.contains("["))
        assertFalse(cleaned.contains("]"))
        assertFalse(cleaned.contains(":"))
        assertFalse(cleaned.contains("?"))

        // Handles empty / blank name
        val blankCleaned = ExcelExporter.sanitizeSheetName("   ", set)
        assertEquals("ورقة", blankCleaned)

        // Handles long name > 31 characters
        val longName = "هذا_اسم_ورقة_طويل_جداً_جداً_جداً_ويتجاوز_الحد_المسموح_به"
        val truncated = ExcelExporter.sanitizeSheetName(longName, set)
        assertTrue(truncated.length <= 31)

        // Handles deduplication
        val dup1 = ExcelExporter.sanitizeSheetName("تقرير", set)
        val dup2 = ExcelExporter.sanitizeSheetName("تقرير", set)
        val dup3 = ExcelExporter.sanitizeSheetName("تقرير", set)
        assertEquals("تقرير", dup1)
        assertEquals("تقرير_2", dup2)
        assertEquals("تقرير_3", dup3)
    }

    @Test
    fun testExcelExporterRealWorkbookInMemory() {
        val row1 = SelectedRowData(
            key = RowKey("file://doc.xlsx", "بيانات", 0),
            fileName = "doc.xlsx",
            sheetName = "بيانات",
            rowNumber = "1",
            cells = listOf("أحمد", "30", "الرياض"),
            headers = listOf("الاسم", "العمر", "المدينة")
        )
        val row2 = SelectedRowData(
            key = RowKey("file://doc.xlsx", "بيانات", 1),
            fileName = "doc.xlsx",
            sheetName = "بيانات",
            rowNumber = "2",
            cells = listOf("سارة", "25", "جدة"),
            headers = listOf("الاسم", "العمر", "المدينة")
        )
        val docRow = SelectedRowData(
            key = RowKey("file://notes.docx", "مستند", 0),
            fileName = "notes.docx",
            sheetName = "مستند",
            rowNumber = "1",
            cells = emptyList(),
            headers = emptyList(),
            isDocument = true,
            snippet = "هذا نص تجريبي داخل مستند وورد",
            location = "فقرة 3"
        )

        val workbook = ExcelExporter.buildWorkbookForRows(listOf(row1, row2, docRow))
        assertNotNull(workbook)

        // Write to byte array
        val baos = ByteArrayOutputStream()
        workbook.write(baos)
        val bytes = baos.toByteArray()
        assertTrue(bytes.isNotEmpty())

        // Read back using XSSFWorkbook
        val readWb = XSSFWorkbook(ByteArrayInputStream(bytes))
        assertEquals(2, readWb.numberOfSheets)

        val tableSheet = readWb.getSheetAt(0)
        assertNotNull(tableSheet)
        assertTrue(tableSheet.isRightToLeft)
        // Check header row
        val headerRow = tableSheet.getRow(0)
        assertEquals("الاسم", headerRow.getCell(0).stringCellValue)
        assertEquals("العمر", headerRow.getCell(1).stringCellValue)
        assertEquals("المدينة", headerRow.getCell(2).stringCellValue)

        // Check data row
        val r1 = tableSheet.getRow(1)
        assertEquals("أحمد", r1.getCell(0).stringCellValue)
        assertEquals("30", r1.getCell(1).stringCellValue)
        assertEquals("الرياض", r1.getCell(2).stringCellValue)

        // Check document sheet
        val docSheet = readWb.getSheetAt(1)
        assertNotNull(docSheet)
        assertTrue(docSheet.isRightToLeft)
        val docHeader = docSheet.getRow(0)
        assertEquals("الملف", docHeader.getCell(0).stringCellValue)
        assertEquals("الموقع/الصفحة", docHeader.getCell(1).stringCellValue)
        assertEquals("المحتوى", docHeader.getCell(2).stringCellValue)

        val docData = docSheet.getRow(1)
        assertEquals("notes.docx", docData.getCell(0).stringCellValue)
        assertEquals("فقرة 3", docData.getCell(1).stringCellValue)
        assertEquals("هذا نص تجريبي داخل مستند وورد", docData.getCell(2).stringCellValue)

        readWb.close()
    }

    @Test
    fun testExcelExporterCellsWorkbook() {
        val cell1 = SelectedCellData(
            key = CellKey("file://data.xlsx", "Sheet1", 0, 0),
            fileName = "data.xlsx",
            sheetName = "Sheet1",
            rowNumber = "1",
            columnName = "الاسم",
            value = "خالد"
        )
        val cell2 = SelectedCellData(
            key = CellKey("file://data.xlsx", "Sheet1", 2, 2),
            fileName = "data.xlsx",
            sheetName = "Sheet1",
            rowNumber = "3",
            columnName = "الراتب",
            value = "5000"
        )

        val workbook = ExcelExporter.buildWorkbookForCells(listOf(cell1, cell2))
        val baos = ByteArrayOutputStream()
        workbook.write(baos)
        val readWb = XSSFWorkbook(ByteArrayInputStream(baos.toByteArray()))

        assertEquals(1, readWb.numberOfSheets)
        val sheet = readWb.getSheetAt(0)
        assertTrue(sheet.isRightToLeft)

        // Header: [رقم الصف, الاسم, الراتب]
        val headerRow = sheet.getRow(0)
        assertEquals("رقم الصف", headerRow.getCell(0).stringCellValue)
        assertEquals("الاسم", headerRow.getCell(1).stringCellValue)
        assertEquals("الراتب", headerRow.getCell(2).stringCellValue)

        // Row 1 (index 1): rowNumber 1, cell 1 has "خالد", cell 2 empty
        val row1 = sheet.getRow(1)
        assertEquals("1", row1.getCell(0).stringCellValue)
        assertEquals("خالد", row1.getCell(1).stringCellValue)
        assertEquals("", row1.getCell(2).stringCellValue)

        // Row 2 (index 2): rowNumber 3, cell 1 empty, cell 2 has "5000"
        val row2 = sheet.getRow(2)
        assertEquals("3", row2.getCell(0).stringCellValue)
        assertEquals("", row2.getCell(1).stringCellValue)
        assertEquals("5000", row2.getCell(2).stringCellValue)

        readWb.close()
    }

    @Test
    fun testExcelExporterBuildAllResults() {
        val result1 = SearchResult(
            fileUri = "file://test.xlsx",
            fileName = "test.xlsx",
            sheetName = "ورقة 1",
            rowNumber = 5,
            snippet = "محمد\tمهندس\t5000",
            query = "محمد",
            location = "ورقة 1 - صف 5",
            columnHeaders = listOf("الاسم", "الوظيفة", "الراتب")
        )
        val result2 = SearchResult(
            fileUri = "file://doc.pdf",
            fileName = "doc.pdf",
            sheetName = null,
            rowNumber = null,
            snippet = "نص من ملف PDF",
            query = "نص",
            location = "صفحة 2",
            columnHeaders = emptyList()
        )

        val workbook = ExcelExporter.buildWorkbookForAllResults(listOf(result1, result2))
        val baos = ByteArrayOutputStream()
        workbook.write(baos)
        val readWb = XSSFWorkbook(ByteArrayInputStream(baos.toByteArray()))

        assertEquals(2, readWb.numberOfSheets)
        readWb.close()
    }
}
