package com.example.fastfilesearch

import com.example.fastfilesearch.export.ExcelExporter
import com.example.fastfilesearch.model.FileSegment
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.model.SupportedFileTypeGroups
import com.example.fastfilesearch.parser.AccessDatabaseParser
import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.util.ArabicNormalizer
import com.healthmarketscience.jackcess.ColumnBuilder
import com.healthmarketscience.jackcess.DataType
import com.healthmarketscience.jackcess.Database
import com.healthmarketscience.jackcess.DatabaseBuilder
import com.healthmarketscience.jackcess.TableBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AccessDatabaseParserTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun testAccdbCreationStreamingExtractionArabicNormalizationAndExcelExport() {
        val dbFile = tempFolder.newFile("customers.accdb")
        DatabaseBuilder(dbFile)
            .setFileFormat(Database.FileFormat.V2010)
            .create()
            .use { db ->
                val customersTable = TableBuilder("العملاء")
                    .addColumn(ColumnBuilder("المعرف", DataType.LONG))
                    .addColumn(ColumnBuilder("الاسم", DataType.TEXT))
                    .addColumn(ColumnBuilder("المدينة", DataType.TEXT))
                    .addColumn(ColumnBuilder("الملاحظات", DataType.MEMO))
                    .toTable(db)

                customersTable.addRow(1, "أحمد محمد الصالح", "الرياض", "عميل ذهبي - Cairo Branch")
                customersTable.addRow(2, "فاطمة المنصور", "دبي", "Senior Developer")
                customersTable.addRow(3, "خالد علي", "جدة", "تقرير سنوي لعام 2026")

                val invoicesTable = TableBuilder("Invoices")
                    .addColumn(ColumnBuilder("InvoiceNo", DataType.TEXT))
                    .addColumn(ColumnBuilder("Description", DataType.TEXT))
                    .addColumn(ColumnBuilder("Amount", DataType.DOUBLE))
                    .toTable(db)

                invoicesTable.addRow("INV-101", "Laptop Purchase", 3500.0)
            }

        val capturedHeaders = mutableMapOf<String, List<String>>()
        val capturedSegments = mutableListOf<FileSegment>()

        AccessDatabaseParser.parseFile(
            dbFile = dbFile,
            fileUri = "content://docs/customers.accdb",
            fileName = "customers.accdb",
            onHeaderFound = { tableName, headers ->
                capturedHeaders[tableName] = headers
            },
            onSegmentFound = { segment ->
                capturedSegments.add(segment)
            }
        )

        // Verify all non-system tables & column headers
        assertEquals(listOf("المعرف", "الاسم", "المدينة", "الملاحظات"), capturedHeaders["العملاء"])
        assertEquals(listOf("InvoiceNo", "Description", "Amount"), capturedHeaders["Invoices"])
        assertEquals(4, capturedSegments.size)

        // Verify Arabic normalized matching ("احمد" matches "أحمد") & English search
        val ahmadNormalized = ArabicNormalizer.normalize("احمد")
        val ahmadMatches = capturedSegments.filter { it.normalizedContent.contains(ahmadNormalized) }
        assertEquals(1, ahmadMatches.size)
        val ahmadSegment = ahmadMatches.first()
        assertEquals("جدول: العملاء، صف: 1", ahmadSegment.location)
        assertTrue(TableCellHelper.isTabular("customers.accdb", ahmadSegment.content))

        val (parsedSheet, parsedRow) = TableCellHelper.extractSheetAndRow(ahmadSegment.location)
        assertEquals("العملاء", parsedSheet)
        assertEquals("1", parsedRow)
        val cells = TableCellHelper.splitIntoCells(ahmadSegment.content)
        assertEquals(listOf("1", "أحمد محمد الصالح", "الرياض", "عميل ذهبي - Cairo Branch"), cells)

        // Verify English match across another table
        val laptopMatches = capturedSegments.filter {
            it.normalizedContent.contains(ArabicNormalizer.normalize("Laptop"))
        }
        assertEquals(1, laptopMatches.size)
        assertEquals("جدول: Invoices، صف: 1", laptopMatches.first().location)
        assertTrue(laptopMatches.first().content.contains("3500"))

        // Verify Access SearchResult integrates cleanly with ExcelExporter
        val searchResults = listOf(
            SearchResult(
                id = 1L,
                fileUri = ahmadSegment.fileUri,
                fileName = ahmadSegment.fileName,
                location = ahmadSegment.location,
                snippet = ahmadSegment.content,
                query = "احمد",
                sheetName = "العملاء",
                rowNumber = 1,
                columnHeaders = capturedHeaders["العملاء"] ?: emptyList()
            )
        )
        ExcelExporter.buildWorkbookForAllResults(searchResults).use { wb ->
            assertEquals(1, wb.numberOfSheets)
            val sheet = wb.getSheetAt(0)
            assertEquals("المعرف", sheet.getRow(0).getCell(0).stringCellValue)
            assertEquals("الاسم", sheet.getRow(0).getCell(1).stringCellValue)
            assertEquals("أحمد محمد الصالح", sheet.getRow(1).getCell(1).stringCellValue)
        }
    }

    @Test
    fun testLegacyMdbFormatExtraction() {
        val mdbFile = tempFolder.newFile("legacy.mdb")
        DatabaseBuilder(mdbFile)
            .setFileFormat(Database.FileFormat.V2000)
            .create()
            .use { db ->
                val table = TableBuilder("Employees")
                    .addColumn(ColumnBuilder("ID", DataType.LONG))
                    .addColumn(ColumnBuilder("FullName", DataType.TEXT))
                    .toTable(db)

                table.addRow(10, "إبراهيم الحسن")
                table.addRow(11, "Sara Ali")
            }

        val headers = mutableMapOf<String, List<String>>()
        val segments = mutableListOf<FileSegment>()
        AccessDatabaseParser.parseFile(
            dbFile = mdbFile,
            fileUri = "content://docs/legacy.mdb",
            fileName = "legacy.mdb",
            onHeaderFound = { t, h -> headers[t] = h },
            onSegmentFound = { segments.add(it) }
        )

        assertEquals(listOf("ID", "FullName"), headers["Employees"])
        assertEquals(2, segments.size)
        assertTrue(segments[0].normalizedContent.contains(ArabicNormalizer.normalize("ابراهيم")))
        assertEquals("جدول: Employees، صف: 2", segments[1].location)
    }

    @Test
    fun testCorruptAndEmptyAccessFilesDoNotCrash() {
        val corruptFile = tempFolder.newFile("corrupt.accdb")
        corruptFile.writeBytes("THIS_IS_NOT_A_VALID_ACCESS_DATABASE_HEADER_BYTES".toByteArray())

        val emptyFile = tempFolder.newFile("empty.mdb")

        val missingFile = tempFolder.newFile("missing.accdb").apply { delete() }

        val segments = mutableListOf<FileSegment>()
        val headers = mutableListOf<String>()

        for (f in listOf(corruptFile, emptyFile, missingFile)) {
            AccessDatabaseParser.parseFile(
                dbFile = f,
                fileUri = "content://docs/${f.name}",
                fileName = f.name,
                onHeaderFound = { t, _ -> headers.add(t) },
                onSegmentFound = { segments.add(it) }
            )
        }

        assertTrue(segments.isEmpty())
        assertTrue(headers.isEmpty())
    }

    @Test
    fun testAccessSupportedExtensionsAndFilterGroups() {
        assertTrue(FileParserDispatcher.isSupportedFile("records.mdb"))
        assertTrue(FileParserDispatcher.isSupportedFile("records.ACCDB"))
        assertFalse(FileParserDispatcher.isSupportedFile("~\$records.accdb"))
        assertEquals(setOf("mdb", "accdb"), SupportedFileTypeGroups.ACCESS.extensions)
        assertTrue(SupportedFileTypeGroups.ALL_GROUPS.contains(SupportedFileTypeGroups.ACCESS))
        assertTrue(TableCellHelper.isTabular("data.mdb", "single_cell"))
        assertTrue(TableCellHelper.isTabular("data.accdb", "single_cell"))
    }
}
