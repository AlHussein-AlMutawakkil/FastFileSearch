package com.example.fastfilesearch.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.use
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.fastfilesearch.desktop.engine.DesktopSearchEngine
import com.example.fastfilesearch.desktop.export.DesktopExcelExporter
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockMode
import com.example.fastfilesearch.desktop.licensing.DesktopAutoLockTimeout
import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.ui.DesktopExcelTableSection
import com.example.fastfilesearch.desktop.ui.ProvideDesktopTheme
import com.example.fastfilesearch.desktop.ui.selection.CellKey
import com.example.fastfilesearch.desktop.ui.selection.DesktopGridNavigator
import com.example.fastfilesearch.desktop.ui.selection.GridNavDirection
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectionMode
import com.example.fastfilesearch.desktop.ui.selection.SelectionState
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import kotlinx.coroutines.runBlocking
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalComposeUiApi::class)
class DesktopParityAndSelectionTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun testFilenameContainsSearchSelectedFileFilterWorkspaceRenameDeleteAndPrefs() = runBlocking {
        val dbDir = tempFolder.newFolder("db_parity")
        val ws1Dir = tempFolder.newFolder("ws_finance")
        val ws2Dir = tempFolder.newFolder("ws_hr")

        val invoiceFile = File(ws1Dir, "تقرير_المبيعات_السنوي.txt")
        invoiceFile.writeText("ملخص الايرادات للشهر الاول", StandardCharsets.UTF_8)

        val emptyFile = File(ws1Dir, "ملف_الميزانية_فارغ.txt")
        emptyFile.writeText("", StandardCharsets.UTF_8)

        val hrFile = File(ws2Dir, "كشف_الرواتب.txt")
        hrFile.writeText("موظف احمد", StandardCharsets.UTF_8)

        DesktopSearchEngine(dbDir).use { engine ->
            val ws1 = engine.addWorkspace(ws1Dir)
            val ws2 = engine.addWorkspace(ws2Dir)
            val summary = engine.syncWorkspaces(listOf(ws1, ws2))
            assertEquals(3, summary.totalFound)
            assertEquals(3, summary.indexedCount)

            // 1. CONTAINS matches normalized filename even when word is absent from file content
            val byFileName = engine.search("المبيعات", SearchFilter(method = SearchMethod.CONTAINS))
            assertTrue(byFileName.any { it.fileName == invoiceFile.name })

            // 2. Empty (0-segment) file is still discoverable by filename
            val emptyFileMatch = engine.search("الميزانية", SearchFilter(method = SearchMethod.CONTAINS))
            assertEquals(1, emptyFileMatch.size)
            assertEquals(emptyFile.name, emptyFileMatch.first().fileName)

            // 3. selectedFileUris restricts search strictly to specified file(s)
            val scopedToHr = engine.search(
                "ملف",
                SearchFilter(
                    method = SearchMethod.CONTAINS,
                    selectedFileUris = setOf(hrFile.absolutePath)
                )
            )
            assertTrue(scopedToHr.all { it.fileUri == hrFile.absolutePath })

            // 4. Rename workspace updates display name without altering path
            assertTrue(engine.renameWorkspace(ws1.id, "المالية المعتمدة"))
            assertEquals("المالية المعتمدة", engine.getWorkspace(ws1.id)?.name)
            assertEquals(ws1.path, engine.getWorkspace(ws1.id)?.path)

            // 5. Active workspace scoping
            engine.setActiveWorkspaceId(ws2.id)
            assertEquals(ws2.id, engine.getActiveWorkspaceId())
            val scopedStats = engine.getIndexStats()
            assertEquals(1, scopedStats.first)
            val scopedResults = engine.search("المبيعات")
            assertTrue(scopedResults.isEmpty())

            // 6. Delete workspace with stats clears active workspace & removes files/segments
            val delStats = engine.deleteWorkspaceWithStats(ws2.id)
            assertTrue(delStats.deleted)
            assertEquals(1, delStats.deletedFilesCount)
            assertTrue(delStats.deletedSegmentsCount >= 1)
            assertNull(engine.getActiveWorkspaceId())

            // 7. Settings & Search History persistence
            engine.setStretchSingleColumn(false)
            engine.setVerticalCopyFormat(false)
            engine.setExpandFullCellText(false)
            engine.setBackgroundSyncEnabled(false)
            engine.setSearchHistoryEnabled(true)
            assertFalse(engine.isStretchSingleColumn())
            assertFalse(engine.isVerticalCopyFormat())
            assertFalse(engine.isExpandFullCellText())
            assertFalse(engine.isBackgroundSyncEnabled())

            val h1 = engine.addSearchHistory("الرواتب")
            val h2 = engine.addSearchHistory("المبيعات")
            assertEquals(listOf("المبيعات", "الرواتب"), h2)
            assertEquals(listOf("المبيعات", "الرواتب"), engine.getSearchHistory())
            engine.removeSearchHistoryItem("الرواتب")
            assertEquals(listOf("المبيعات"), engine.getSearchHistory())
            engine.clearSearchHistory()
            assertTrue(engine.getSearchHistory().isEmpty())
            assertEquals(listOf("الرواتب"), h1)
        }
    }

    @Test
    fun testDesktopAppLockPasscodeLockoutAndIdleTimeout() {
        val baseDir = tempFolder.newFolder("lock_dir")
        var nowMs = 1_000_000L
        val manager = DesktopAppLockManager(baseDir = baseDir, nowMillisProvider = { nowMs })

        assertFalse(manager.hasPasscode())
        assertEquals(DesktopAppLockMode.DISABLED, manager.lockMode)

        manager.setPasscode("4321")
        manager.setLockMode(DesktopAppLockMode.PASSCODE_ONLY)
        manager.setAutoLockTimeout(DesktopAutoLockTimeout.ONE_MINUTE)
        assertTrue(manager.hasPasscode())
        assertFalse(manager.isLocked.value)

        // 5 failed attempts trigger 30s lockout
        repeat(4) {
            assertFalse(manager.verifyPasscode("0000", nowMs))
            assertEquals(0, manager.getRemainingLockoutSeconds(nowMs))
        }
        assertFalse(manager.verifyPasscode("0000", nowMs))
        assertEquals(30, manager.getRemainingLockoutSeconds(nowMs))
        assertFalse(manager.verifyPasscode("4321", nowMs + 5_000L))

        // After 30s lockout expires, correct passcode unlocks
        nowMs += 31_000L
        assertEquals(0, manager.getRemainingLockoutSeconds(nowMs))
        assertTrue(manager.verifyPasscode("4321", nowMs))
        assertFalse(manager.isLocked.value)

        // Idle timeout locks after 60s inactivity
        manager.recordUserActivity(nowMs)
        assertFalse(manager.checkIdleTimeout(nowMs + 30_000L))
        assertTrue(manager.checkIdleTimeout(nowMs + 61_000L))
        assertTrue(manager.isLocked.value)
    }

    @Test
    fun testRtlArrowNavigationRangeSelectionClipboardAndCellExport() {
        val headers = listOf("الرقم", "الاسم", "القسم")
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/staff.xlsx",
                fileName = "staff.xlsx",
                location = "ورقة: الموظفون، صف: 2",
                sheetName = "الموظفون",
                rowNumber = 2,
                columnHeaders = headers,
                snippet = "101\tأحمد علي\tالمالية",
                query = "أحمد"
            ),
            SearchResult(
                id = 2,
                fileUri = "C:/docs/staff.xlsx",
                fileName = "staff.xlsx",
                location = "ورقة: الموظفون، صف: 3",
                sheetName = "الموظفون",
                rowNumber = 3,
                columnHeaders = headers,
                snippet = "102\tسارة خالد\tالموارد البشرية",
                query = "أحمد"
            )
        )
        val group = FileResultGroup(
            fileUri = "C:/docs/staff.xlsx",
            fileName = "staff.xlsx",
            isDocument = false,
            results = results
        )
        val sheets = DesktopGridNavigator.buildSheetModelsForGroup(group, showAllRows = true)
        assertEquals(1, sheets.size)

        // Start at (0, 0)
        var sel = SelectionState().selectSingleCell(sheets[0].buildCellData(0, 0)!!)
        assertEquals(CellKey("C:/docs/staff.xlsx", "الموظفون", 0, 0), sel.activeCell)
        assertEquals("101", DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(sel, verticalCopyFormat = true))

        // RTL: Left arrow moves visually left -> colIndex + 1 (0 -> 1)
        sel = DesktopGridNavigator.navigateArrow(sheets, sel, GridNavDirection.LEFT, isShiftPressed = false, isRtl = true)
        assertEquals(CellKey("C:/docs/staff.xlsx", "الموظفون", 0, 1), sel.activeCell)
        assertEquals("أحمد علي", DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(sel, verticalCopyFormat = true))

        // Shift + Down then Shift + Left expands rectangular selection (0..1, 1..2) -> 4 cells
        sel = DesktopGridNavigator.navigateArrow(sheets, sel, GridNavDirection.DOWN, isShiftPressed = true, isRtl = true)
        sel = DesktopGridNavigator.navigateArrow(sheets, sel, GridNavDirection.LEFT, isShiftPressed = true, isRtl = true)
        assertEquals(SelectionMode.CELLS, sel.mode)
        assertEquals(4, sel.count)
        assertEquals("4 خلايا", sel.getFormattedCountArabic())

        // Ctrl+C clipboard text is valid 2x2 TSV grid ready for Excel paste
        val tsv = DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(sel, verticalCopyFormat = true)
        assertEquals("أحمد علي\tالمالية\nسارة خالد\tالموارد البشرية", tsv)

        // Export selected cells to .xlsx and verify only selected columns are included
        val outFile = File(tempFolder.root, "selected_cells.xlsx")
        DesktopExcelExporter.exportCellsToFile(sel.selectedCells.values.toList(), outFile)
        assertTrue(outFile.exists() && outFile.length() > 0L)

        XSSFWorkbook(FileInputStream(outFile)).use { wb ->
            val sh = wb.getSheetAt(0)
            assertEquals("رقم الصف", sh.getRow(0).getCell(0).stringCellValue)
            assertEquals("الاسم", sh.getRow(0).getCell(1).stringCellValue)
            assertEquals("القسم", sh.getRow(0).getCell(2).stringCellValue)
            assertEquals("أحمد علي", sh.getRow(1).getCell(1).stringCellValue)
            assertEquals("الموارد البشرية", sh.getRow(2).getCell(2).stringCellValue)
        }
    }

    @Test
    fun testComposeDesktopCellClickDragRangeAndDoubleClickDetail() {
        val headers = listOf("الرمز", "البيان", "الحالة")
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/items.xlsx",
                fileName = "items.xlsx",
                location = "ورقة: رئيسي، صف: 2",
                sheetName = "رئيسي",
                rowNumber = 2,
                columnHeaders = headers,
                snippet = "A1\tجهاز حاسب\tمتوفر",
                query = "حاسب"
            ),
            SearchResult(
                id = 2,
                fileUri = "C:/docs/items.xlsx",
                fileName = "items.xlsx",
                location = "ورقة: رئيسي، صف: 3",
                sheetName = "رئيسي",
                rowNumber = 3,
                columnHeaders = headers,
                snippet = "A2\tطابعة ليزر\tنفذ",
                query = "حاسب"
            )
        )

        var selectionState by mutableStateOf(SelectionState())
        var openedCellDetail by mutableStateOf<SelectedCellData?>(null)
        val cellBounds = mutableMapOf<Pair<Int, Int>, Rect>()

        ImageComposeScene(
            width = 760,
            height = 400,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                Box(modifier = Modifier.width(760.dp)) {
                    DesktopExcelTableSection(
                        results = results,
                        query = "حاسب",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        showAllRows = true,
                        accentColor = Color(0xFF1E7E34),
                        fileUri = "C:/docs/items.xlsx",
                        fileName = "items.xlsx",
                        selectionState = selectionState,
                        onSelectionStateChange = { selectionState = it },
                        onViewDetail = {},
                        onViewCellDetail = { openedCellDetail = it },
                        onDataCellBounds = { r, c, coords ->
                            if (coords.isAttached) cellBounds[r to c] = coords.boundsInRoot()
                        }
                    )
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)

            val r0c0 = cellBounds[0 to 0]!!
            val r1c1 = cellBounds[1 to 1]!!

            // 1. Single click selects cell (0,0) without opening detail modal
            val clickPos = Offset((r0c0.left + r0c0.right) / 2f, (r0c0.top + r0c0.bottom) / 2f)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = clickPos,
                timeMillis = 100L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(110_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = clickPos,
                timeMillis = 120L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(130_000_000L)

            assertEquals(SelectionMode.CELLS, selectionState.mode)
            assertEquals(1, selectionState.count)
            assertEquals(CellKey("C:/docs/items.xlsx", "رئيسي", 0, 0), selectionState.activeCell)
            assertNull(openedCellDetail)

            // 2. Mouse drag from (0,0) to (1,1) selects 2x2 range (4 cells)
            val endPos = Offset((r1c1.left + r1c1.right) / 2f, (r1c1.top + r1c1.bottom) / 2f)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = clickPos,
                timeMillis = 600L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(610_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Move,
                position = endPos,
                timeMillis = 640L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true)
            )
            scene.render(650_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = endPos,
                timeMillis = 670L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(680_000_000L)

            assertEquals(4, selectionState.count)
            assertTrue(selectionState.isCellSelected(CellKey("C:/docs/items.xlsx", "رئيسي", 1, 1)))

            // 3. Double click on (1,1) opens cell detail dialog
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = endPos,
                timeMillis = 1200L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(1210_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = endPos,
                timeMillis = 1230L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(1240_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = endPos,
                timeMillis = 1320L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(1330_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = endPos,
                timeMillis = 1350L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(1360_000_000L)

            assertNotNull(openedCellDetail)
            assertEquals("طابعة ليزر", openedCellDetail?.value)
        }
    }
}
