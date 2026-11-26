package com.example.fastfilesearch.desktop

import com.example.fastfilesearch.desktop.engine.DesktopSearchEngine
import com.example.fastfilesearch.desktop.export.DesktopExcelExporter
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.parser.DesktopTextParser
import com.example.fastfilesearch.desktop.ui.DesktopFileExpansionStateHelper
import com.healthmarketscience.jackcess.ColumnBuilder
import com.healthmarketscience.jackcess.DataType
import com.healthmarketscience.jackcess.Database
import com.healthmarketscience.jackcess.DatabaseBuilder
import com.healthmarketscience.jackcess.TableBuilder
import kotlinx.coroutines.runBlocking
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class DesktopSearchAndExportTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    private fun createSampleExcel(file: File) {
        XSSFWorkbook().use { wb ->
            val sheet = wb.createSheet("الموظفون")
            val header = sheet.createRow(0)
            header.createCell(0).setCellValue("الرقم الوظيفي")
            header.createCell(1).setCellValue("الاسم الكامل")
            header.createCell(2).setCellValue("الإدارة")

            val row1 = sheet.createRow(1)
            row1.createCell(0).setCellValue("101")
            row1.createCell(1).setCellValue("أَحْمَد عَبْدِ الرَّحْمَن")
            row1.createCell(2).setCellValue("الموارد البشرية")

            val row2 = sheet.createRow(2)
            row2.createCell(0).setCellValue("102")
            row2.createCell(1).setCellValue("سارة خالد المنصور")
            row2.createCell(2).setCellValue("المالية والمحاسبة")

            FileOutputStream(file).use { wb.write(it) }
        }
    }

    private fun createSampleAccess(file: File) {
        DatabaseBuilder.create(Database.FileFormat.V2016, file).use { db ->
            val table = TableBuilder("العملاء")
                .addColumn(ColumnBuilder("المعرف", DataType.TEXT))
                .addColumn(ColumnBuilder("اسم العميل", DataType.TEXT))
                .addColumn(ColumnBuilder("المدينة", DataType.TEXT))
                .toTable(db)
            table.addRow("C-01", "شركة الأفق الذهبي", "الرياض")
            table.addRow("C-02", "مؤسسة البناء الحديث", "جدة")
        }
    }

    private fun createSampleDocx(file: File) {
        XWPFDocument().use { doc ->
            doc.createParagraph().createRun().setText("عقد توريد تجهيزات مكتبية ومعدات شبكات محلية")
            doc.createParagraph().createRun().setText("Reference Code: ENT-2026-ALPHA")
            FileOutputStream(file).use { doc.write(it) }
        }
    }

    @Test
    fun testIndexingArabicNormalizationAccessExcelWordTextAndExport() = runBlocking {
        val dbDir = tempFolder.newFolder("app_data")
        val wsDir = tempFolder.newFolder("documents_ws")

        createSampleExcel(File(wsDir, "employees.xlsx"))
        createSampleAccess(File(wsDir, "clients.accdb"))
        createSampleDocx(File(wsDir, "contract.docx"))

        val utf8Txt = File(wsDir, "report_utf8.txt")
        utf8Txt.writeText("ملخص الأرباح التشغيلية للعام المالي الحالي\nبند المراجعة الأول", StandardCharsets.UTF_8)

        val win1256 = Charset.forName("windows-1256")
        val ansiTxt = File(wsDir, "legacy_win1256.txt")
        ansiTxt.writeBytes("بيان الفواتير المستحقة لشهر يناير\r\nرقم المستند 7788".toByteArray(win1256))
        assertEquals(win1256, DesktopTextParser.detectCharset(ansiTxt))
        assertEquals(StandardCharsets.UTF_8, DesktopTextParser.detectCharset(utf8Txt))

        val exportFile = File(tempFolder.root, "exported_results.xlsx")

        DesktopSearchEngine(dbDir).use { engine ->
            val ws = engine.addWorkspace(wsDir)
            val summary = engine.syncWorkspaces(listOf(ws))
            assertEquals(5, summary.totalFound)
            assertEquals(5, summary.indexedCount)
            assertFalse(summary.cancelled)

            val (filesCount, segmentsCount) = engine.getIndexStats()
            assertEquals(5, filesCount)
            assertTrue(segmentsCount >= 7)

            // 1. Arabic normalization: query without hamza/tashkeel finds accented/hamzated Excel cell
            val excelResults = engine.search("احمد", SearchFilter(method = SearchMethod.CONTAINS))
            assertTrue(excelResults.any { it.fileName == "employees.xlsx" && it.columnHeaders.contains("الاسم الكامل") })

            // 2. Access filter only returns clients.accdb
            val accessOnly = engine.search(
                "الافق",
                SearchFilter(
                    method = SearchMethod.CONTAINS,
                    selectedExtensions = setOf("mdb", "accdb")
                )
            )
            assertEquals(1, accessOnly.size)
            assertEquals("clients.accdb", accessOnly.first().fileName)
            assertTrue(accessOnly.first().columnHeaders.contains("اسم العميل"))

            // 3. Word English + Arabic exact phrase search
            val docxExact = engine.search(
                "تجهيزات مكتبية",
                SearchFilter(method = SearchMethod.EXACT_PHRASE)
            )
            assertEquals(1, docxExact.size)
            assertEquals("contract.docx", docxExact.first().fileName)

            val docxEnglish = engine.search("ALPHA", SearchFilter(method = SearchMethod.CONTAINS))
            assertTrue(docxEnglish.any { it.fileName == "contract.docx" })

            // 4. Windows-1256 Arabic ANSI file search
            val ansiResults = engine.search("الفواتير", SearchFilter(method = SearchMethod.WHOLE_WORD))
            assertEquals(1, ansiResults.size)
            assertEquals("legacy_win1256.txt", ansiResults.first().fileName)

            // 5. Differential sync skips unmodified files
            val secondSync = engine.syncWorkspaces(listOf(ws))
            assertEquals(0, secondSync.indexedCount)
            assertEquals(5, secondSync.skippedCount)

            // 6. Export combined tabular + document results to Excel and inspect output
            val combinedResults = excelResults + accessOnly + docxExact + ansiResults
            DesktopExcelExporter.exportToFile(combinedResults, exportFile)
            assertTrue(exportFile.exists() && exportFile.length() > 0L)

            XSSFWorkbook(FileInputStream(exportFile)).use { exportedWb ->
                assertTrue(exportedWb.numberOfSheets >= 3)
                var hasDocsSheet = false
                var foundArabicAccessRow = false
                for (i in 0 until exportedWb.numberOfSheets) {
                    val sh = exportedWb.getSheetAt(i)
                    assertTrue(sh.isRightToLeft)
                    if (sh.sheetName == "مستندات") {
                        hasDocsSheet = true
                    }
                    for (r in 0..sh.lastRowNum) {
                        val row = sh.getRow(r) ?: continue
                        for (c in 0 until row.lastCellNum) {
                            val text = row.getCell(c)?.stringCellValue.orEmpty()
                            if (text.contains("شركة الأفق الذهبي")) {
                                foundArabicAccessRow = true
                            }
                        }
                    }
                }
                assertTrue(hasDocsSheet)
                assertTrue(foundArabicAccessRow)
            }
        }
    }

    @Test
    fun testCollapseExpandAllToolbarSyncHelper() {
        val visible = listOf("a.xlsx", "b.accdb", "c.docx")
        var collapsed = emptySet<String>()

        assertTrue(DesktopFileExpansionStateHelper.hasAnyExpanded(visible, collapsed))
        collapsed = DesktopFileExpansionStateHelper.toggleAll(visible, collapsed)
        assertEquals(visible.toSet(), collapsed)
        assertFalse(DesktopFileExpansionStateHelper.hasAnyExpanded(visible, collapsed))

        // Re-expanding one card makes hasAnyExpanded true again
        collapsed = DesktopFileExpansionStateHelper.toggleFile("b.accdb", collapsed)
        assertTrue(DesktopFileExpansionStateHelper.isExpanded("b.accdb", collapsed))
        assertTrue(DesktopFileExpansionStateHelper.hasAnyExpanded(visible, collapsed))

        // Global toggle collapses all remaining expanded cards
        collapsed = DesktopFileExpansionStateHelper.toggleAll(visible, collapsed)
        assertEquals(visible.toSet(), collapsed)
    }
}
