package com.example.fastfilesearch

import com.example.fastfilesearch.model.SearchHistoryCodec
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.ui.selection.CellKey
import com.example.fastfilesearch.ui.selection.RowKey
import com.example.fastfilesearch.ui.selection.SelectedCellData
import com.example.fastfilesearch.ui.selection.SelectionMode
import com.example.fastfilesearch.ui.selection.SelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryAndRowSelectionTest {

    @Test
    fun testSearchHistoryCodecDeduplicatesCapsAndRoundtrips() {
        var list = emptyList<String>()
        // Ignores blank and short queries when minLength = 2
        list = SearchHistoryCodec.record(list, "   ", minLength = 2)
        list = SearchHistoryCodec.record(list, "ا", minLength = 2)
        assertTrue(list.isEmpty())

        // Allows 1-char query on explicit submit (minLength = 1)
        list = SearchHistoryCodec.record(list, "5", minLength = 1)
        assertEquals(listOf("5"), list)

        // Adds up to MAX_HISTORY_ITEMS + 2 and deduplicates keeping newest first
        for (i in 1..12) {
            list = SearchHistoryCodec.record(list, "كلمة_$i", minLength = 2)
        }
        assertEquals(SearchHistoryCodec.MAX_HISTORY_ITEMS, list.size)
        assertEquals("كلمة_12", list.first())

        // Moving an existing item to top
        list = SearchHistoryCodec.record(list, "كلمة_10", minLength = 2)
        assertEquals("كلمة_10", list.first())
        assertEquals(1, list.count { it == "كلمة_10" })

        // Encode & decode roundtrip
        val encoded = SearchHistoryCodec.encode(list)
        val decoded = SearchHistoryCodec.decode(encoded)
        assertEquals(list, decoded)
    }

    @Test
    fun testBuildSelectableRowsSortsPerSheetAndGeneratesMatchingKeys() {
        val results = listOf(
            SearchResult(
                fileUri = "uri://excel1",
                fileName = "sheet.xlsx",
                location = "ورقة: المبيعات، صف: 12",
                snippet = "أحمد\t500",
                query = "500",
                sheetName = "المبيعات",
                rowNumber = 12,
                columnHeaders = listOf("الاسم", "القيمة")
            ),
            SearchResult(
                fileUri = "uri://excel1",
                fileName = "sheet.xlsx",
                location = "ورقة: المبيعات، صف: 3",
                snippet = "محمد\t500",
                query = "500",
                sheetName = "المبيعات",
                rowNumber = 3,
                columnHeaders = listOf("الاسم", "القيمة")
            )
        )

        val selectable = TableCellHelper.buildSelectableRowsForFile(
            fileUri = "uri://excel1",
            fileName = "sheet.xlsx",
            isDocument = false,
            results = results
        )

        assertEquals(2, selectable.size)
        assertEquals("3", selectable[0].rowNumber)
        assertEquals(RowKey("uri://excel1", "المبيعات", 0), selectable[0].key)
        assertEquals("12", selectable[1].rowNumber)
        assertEquals(RowKey("uri://excel1", "المبيعات", 1), selectable[1].key)

        // Merging via SelectionState.addRows preserves existing rows and switches from CELLS
        val cellState = SelectionState().toggleCell(
            SelectedCellData(CellKey("uri://other", "S1", 0, 0), "other.xlsx", "S1", "1", "Col", "Val")
        )
        assertEquals(SelectionMode.CELLS, cellState.mode)

        val rowsState = cellState.addRows(selectable)
        assertEquals(SelectionMode.ROWS, rowsState.mode)
        assertEquals(2, rowsState.count)
        assertFalse(rowsState.selectedCells.isNotEmpty())
    }

    @Test
    fun testFormatFileSizeLabels() {
        assertEquals("غير معروف", TableCellHelper.formatFileSize(-1L))
        assertEquals("512 بايت", TableCellHelper.formatFileSize(512L))
        assertTrue(TableCellHelper.formatFileSize(2048L).contains("كيلوبايت"))
        assertTrue(TableCellHelper.formatFileSize(3 * 1024L * 1024L).contains("ميجابايت"))
    }
}
