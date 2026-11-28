package com.example.fastfilesearch.desktop

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.fastfilesearch.desktop.engine.DesktopSearchEngine
import com.example.fastfilesearch.desktop.export.DesktopExcelExporter
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockMode
import com.example.fastfilesearch.desktop.licensing.DesktopAutoLockTimeout
import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.SupportedFileTypeGroups
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.ui.ColumnSortOrder
import com.example.fastfilesearch.desktop.ui.ColumnTextFilter
import com.example.fastfilesearch.desktop.ui.DesktopColors
import com.example.fastfilesearch.desktop.ui.DesktopExcelTableSection
import com.example.fastfilesearch.desktop.ui.DesktopFormatFilterHelper
import com.example.fastfilesearch.desktop.ui.DesktopLockScreen
import com.example.fastfilesearch.desktop.ui.DesktopSearchScreen
import com.example.fastfilesearch.desktop.ui.DesktopSettingsScreen
import com.example.fastfilesearch.desktop.ui.DesktopTableCellHelper
import com.example.fastfilesearch.desktop.ui.DesktopTableSortFilterHelper
import com.example.fastfilesearch.desktop.ui.DesktopWorkspacesScreen
import com.example.fastfilesearch.desktop.ui.unclippedBoundsInRoot
import com.example.fastfilesearch.desktop.ui.FluentToggleSwitch
import com.example.fastfilesearch.desktop.ui.LocalDesktopColors
import com.example.fastfilesearch.desktop.ui.ProvideDesktopTheme
import com.example.fastfilesearch.desktop.ui.TableSortFilterState
import com.example.fastfilesearch.desktop.ui.TextFilterOperator
import com.example.fastfilesearch.desktop.ui.selection.CellKey
import com.example.fastfilesearch.desktop.ui.selection.DesktopGridNavigator
import com.example.fastfilesearch.desktop.ui.selection.GridNavDirection
import com.example.fastfilesearch.desktop.ui.selection.RowKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectionMode
import com.example.fastfilesearch.desktop.ui.selection.SelectionState
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import kotlin.math.abs

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
        assertTrue(sel.shouldShowPreviewMenuItem())
        assertEquals("101", DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(sel, verticalCopyFormat = true))

        // RTL: Left arrow moves visually left -> colIndex + 1 (0 -> 1)
        sel = DesktopGridNavigator.navigateArrow(sheets, sel, GridNavDirection.LEFT, isRtl = true)
        assertEquals(CellKey("C:/docs/staff.xlsx", "الموظفون", 0, 1), sel.activeCell)
        assertTrue(sel.shouldShowPreviewMenuItem())
        assertEquals("أحمد علي", DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(sel, verticalCopyFormat = true))

        // Ctrl+Click additive multi-cell selection builds 2x2 selection; preview menu item disappears when > 1 cell
        sel = sel.toggleCell(sheets[0].buildCellData(0, 2)!!)
        sel = sel.toggleCell(sheets[0].buildCellData(1, 1)!!)
        sel = sel.toggleCell(sheets[0].buildCellData(1, 2)!!)
        assertEquals(SelectionMode.CELLS, sel.mode)
        assertEquals(4, sel.count)
        assertFalse(sel.shouldShowPreviewMenuItem())
        assertEquals("4 خلايا", sel.getFormattedCountArabic())

        // Single row shows preview; > 1 rows hides preview
        var rowSel = SelectionState().selectSingleRow(sheets[0].buildRowData(0)!!)
        assertTrue(rowSel.shouldShowPreviewMenuItem())
        rowSel = rowSel.toggleRow(sheets[0].buildRowData(1)!!)
        assertFalse(rowSel.shouldShowPreviewMenuItem())

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
    fun testComposeDesktopCtrlMultiCellRowNumberClickAndDoubleClick() {
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
        var rowNumHeaderBounds by mutableStateOf<Rect?>(null)
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
                        onRowNumBounds = { coords ->
                            if (coords.isAttached) rowNumHeaderBounds = coords.boundsInRoot()
                        },
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
            val r1c0 = cellBounds[1 to 0]!!
            val rnBox = rowNumHeaderBounds!!

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
            assertTrue(selectionState.shouldShowPreviewMenuItem())
            assertEquals(CellKey("C:/docs/items.xlsx", "رئيسي", 0, 0), selectionState.activeCell)
            assertNull(openedCellDetail)

            // 2. Ctrl + Click on cell (1,1) adds to multi-cell selection; preview disappears (>1 cell)
            val endPos = Offset((r1c1.left + r1c1.right) / 2f, (r1c1.top + r1c1.bottom) / 2f)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = endPos,
                timeMillis = 500L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
                button = PointerButton.Primary
            )
            scene.render(510_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = endPos,
                timeMillis = 520L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
                button = PointerButton.Primary
            )
            scene.render(530_000_000L)

            assertEquals(SelectionMode.CELLS, selectionState.mode)
            assertEquals(2, selectionState.count)
            assertFalse(selectionState.shouldShowPreviewMenuItem())
            assertTrue(selectionState.isCellSelected(CellKey("C:/docs/items.xlsx", "رئيسي", 0, 0)))
            assertTrue(selectionState.isCellSelected(CellKey("C:/docs/items.xlsx", "رئيسي", 1, 1)))

            // 2b. Mouse drag from (0,0) to (1,1) selects 2x2 rectangular block (4 cells)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = clickPos,
                timeMillis = 800L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(810_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Move,
                position = endPos,
                timeMillis = 840L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true)
            )
            scene.render(850_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = endPos,
                timeMillis = 870L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(880_000_000L)

            assertEquals(SelectionMode.CELLS, selectionState.mode)
            assertEquals(4, selectionState.count)
            assertFalse(selectionState.shouldShowPreviewMenuItem())
            assertTrue(selectionState.isCellSelected(CellKey("C:/docs/items.xlsx", "رئيسي", 0, 1)))
            assertTrue(selectionState.isCellSelected(CellKey("C:/docs/items.xlsx", "رئيسي", 1, 0)))

            // 3. Double click on (1,1) opens cell detail modal
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

            // 4. Click on Row Number cell of Row 0 selects full Row 0
            val rnX = (rnBox.left + rnBox.right) / 2f
            val rn0Pos = Offset(rnX, (r0c0.top + r0c0.bottom) / 2f)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = rn0Pos,
                timeMillis = 1800L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(1810_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = rn0Pos,
                timeMillis = 1820L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(1830_000_000L)

            assertEquals(SelectionMode.ROWS, selectionState.mode)
            assertEquals(1, selectionState.count)
            assertTrue(selectionState.shouldShowPreviewMenuItem())
            assertTrue(selectionState.isRowSelected(RowKey("C:/docs/items.xlsx", "رئيسي", 0)))

            // 5. Ctrl + Click on Row Number cell of Row 1 selects both rows cleanly
            val rn1Pos = Offset(rnX, (r1c0.top + r1c0.bottom) / 2f)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = rn1Pos,
                timeMillis = 2200L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
                button = PointerButton.Primary
            )
            scene.render(2210_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = rn1Pos,
                timeMillis = 2220L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                keyboardModifiers = PointerKeyboardModifiers(isCtrlPressed = true),
                button = PointerButton.Primary
            )
            scene.render(2230_000_000L)

            assertEquals(SelectionMode.ROWS, selectionState.mode)
            assertEquals(2, selectionState.count)
            assertFalse(selectionState.shouldShowPreviewMenuItem())
            assertTrue(selectionState.isRowSelected(RowKey("C:/docs/items.xlsx", "رئيسي", 0)))
            assertTrue(selectionState.isRowSelected(RowKey("C:/docs/items.xlsx", "رئيسي", 1)))
        }
    }

    @Test
    fun testWinUiPalettePreservedSearchScreenAndToggleSwitch() {
        var lightPalette: DesktopColors? = null
        var darkPalette: DesktopColors? = null
        var toggleChecked by mutableStateOf(false)
        var toggleBounds by mutableStateOf<Rect?>(null)
        var resultsSummary by mutableStateOf(0 to 0)

        val ws = Workspace(id = "ws-winui", name = "مجلد العمل", path = "C:/docs", addedAt = 1L)
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/report.xlsx",
                fileName = "report.xlsx",
                location = "ورقة: عام، صف: 2",
                sheetName = "عام",
                rowNumber = 2,
                columnHeaders = listOf("البند", "القيمة"),
                snippet = "أجهزة\t500",
                query = "أجهزة"
            ),
            SearchResult(
                id = 2,
                fileUri = "C:/docs/report.xlsx",
                fileName = "report.xlsx",
                location = "ورقة: عام، صف: 3",
                sheetName = "عام",
                rowNumber = 3,
                columnHeaders = listOf("البند", "القيمة"),
                snippet = "أجهزة حاسوب\t1200",
                query = "أجهزة"
            )
        )

        ImageComposeScene(
            width = 1100,
            height = 740,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                lightPalette = LocalDesktopColors.current
                Box {
                    DesktopSearchScreen(
                        searchQuery = "أجهزة",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        defaultSearchMethod = SearchMethod.CONTAINS,
                        searchResults = results,
                        isSearching = false,
                        workspaces = listOf(ws),
                        activeWorkspace = ws,
                        indexedFiles = emptyList(),
                        indexProgress = IndexProgress(),
                        filesCount = 1,
                        segmentsCount = 2,
                        showAllRows = false,
                        stretchSingleColumn = true,
                        verticalCopyFormat = true,
                        expandFullCellText = false,
                        isSearchHistoryEnabled = true,
                        searchHistory = emptyList(),
                        onQueryChange = {},
                        onSubmitSearch = {},
                        onSelectHistoryQuery = {},
                        onRemoveHistoryItem = {},
                        onClearSearchHistory = {},
                        onSelectActiveWorkspace = {},
                        onRefreshIndex = {},
                        onFilterChange = {},
                        onOpenWorkspaces = {},
                        onResultsSummaryChange = { rCount, fCount -> resultsSummary = rCount to fCount }
                    )
                    FluentToggleSwitch(
                        checked = toggleChecked,
                        onCheckedChange = { toggleChecked = it },
                        modifier = Modifier.onGloballyPositioned { toggleBounds = it.boundsInRoot() }
                    )
                }
            }
            ProvideDesktopTheme(isDark = true) {
                darkPalette = LocalDesktopColors.current
            }
        }.use { scene ->
            scene.render(100_000_000L)
            assertEquals(Color(0xFF1C1C1E), lightPalette?.controlActiveBackground)
            assertEquals(Color(0xFFF5F6F8), lightPalette?.screenBackground)
            assertEquals(Color(0xFFF4F5F7), darkPalette?.controlActiveBackground)
            assertEquals(Color(0xFF0E0F12), darkPalette?.screenBackground)
            assertEquals(2 to 1, resultsSummary)

            val bounds = toggleBounds!!
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = bounds.center,
                timeMillis = 200L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(210_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = bounds.center,
                timeMillis = 230L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(240_000_000L)
            assertTrue(toggleChecked)
        }
    }

    @Test
    fun testSearchGroupedUnblocksLargeMultiFileMultiTypeAndLoadMore() = runBlocking {
        val dbDir = tempFolder.newFolder("db_grouped")
        val wsDir = tempFolder.newFolder("ws_grouped")

        val largeCsv = File(wsDir, "a_large_records.csv")
        largeCsv.writeText(
            buildString {
                appendLine("id,project,amount")
                for (i in 1..310) {
                    appendLine("$i,المشروع رقم $i,${i * 10}")
                }
            },
            StandardCharsets.UTF_8
        )

        val secondaryTxt = File(wsDir, "b_summary.txt")
        secondaryTxt.writeText(
            (1..8).joinToString("\n") { "تقرير المشروع الفرعي رقم $it" },
            StandardCharsets.UTF_8
        )

        val tertiaryMd = File(wsDir, "c_notes.md")
        tertiaryMd.writeText(
            (1..4).joinToString("\n") { "- ملاحظة المشروع رقم $it" },
            StandardCharsets.UTF_8
        )

        DesktopSearchEngine(dbDir).use { engine ->
            val ws = engine.addWorkspace(wsDir)
            val sync = engine.syncWorkspaces(listOf(ws))
            assertEquals(3, sync.totalFound)

            val indexedCounts = engine.getIndexedTypeCounts()
            assertEquals(1, indexedCounts["csv"])
            assertEquals(1, indexedCounts["txt"])
            assertEquals(1, indexedCounts["md"])

            val response = engine.searchGrouped(
                query = "المشروع",
                filter = SearchFilter(method = SearchMethod.CONTAINS),
                perFileInitialRows = 35
            )
            assertEquals(3, response.groups.size)
            assertEquals(322, response.totalResultsCount)
            assertEquals(310, response.typeCounts["csv"])
            assertEquals(8, response.typeCounts["txt"])
            assertEquals(4, response.typeCounts["md"])

            val csvGroup = response.groups.first { it.fileName == largeCsv.name }
            assertEquals(35, csvGroup.results.size)
            assertEquals(310, csvGroup.totalMatchesCount)
            assertTrue(csvGroup.hasMoreRows)

            val txtGroup = response.groups.first { it.fileName == secondaryTxt.name }
            assertEquals(8, txtGroup.results.size)
            assertFalse(txtGroup.hasMoreRows)

            val mdGroup = response.groups.first { it.fileName == tertiaryMd.name }
            assertEquals(4, mdGroup.results.size)
            assertFalse(mdGroup.hasMoreRows)

            val moreCsvRows = engine.loadMoreForFile(
                query = "المشروع",
                filter = SearchFilter(method = SearchMethod.CONTAINS),
                fileUri = largeCsv.absolutePath,
                offset = 35,
                limit = 150
            )
            assertEquals(150, moreCsvRows.size)

            engine.cancelActiveSearch()
            val afterCancel = engine.searchGrouped("المشروع", SearchFilter(selectedExtensions = setOf("md")))
            assertEquals(1, afterCancel.groups.size)
            assertEquals(4, afterCancel.totalResultsCount)
            assertEquals(310, afterCancel.typeCounts["csv"])
        }
    }

    @Test
    fun testExcelColumnNumericArabicSortingAndUniqueValueFiltering() {
        val rows = listOf(
            listOf("2", "إبراهيم"),
            listOf("", ""),
            listOf("10", "بكر"),
            listOf("1,250", "أحمد"),
            listOf("٠٥", "مُحَمَّد"),
            listOf("3", "إبراهيم")
        )

        // Unique values of col 1 ignore blanks at end and deduplicate
        val uniqueNames = DesktopTableSortFilterHelper.extractUniqueColumnValues(rows, colIndex = 1)
        assertEquals(listOf("إبراهيم", "أحمد", "بكر", "مُحَمَّد", ""), uniqueNames)
        assertEquals("(فارغ)", DesktopTableSortFilterHelper.formatValueLabel(""))

        // Numeric ascending keeps blanks at bottom
        val numAsc = DesktopTableSortFilterHelper.applyFilterAndSortRows(
            rows = rows,
            state = TableSortFilterState().withSort(0, ColumnSortOrder.ASCENDING)
        )
        assertEquals(listOf("2", "3", "٠٥", "10", "1,250", ""), numAsc.map { it[0] })

        // Numeric descending keeps blanks AT BOTTOM too
        val numDesc = DesktopTableSortFilterHelper.applyFilterAndSortRows(
            rows = rows,
            state = TableSortFilterState().withSort(0, ColumnSortOrder.DESCENDING)
        )
        assertEquals(listOf("1,250", "10", "٠٥", "3", "2", ""), numDesc.map { it[0] })

        // Arabic-normalized text ascending puts hamza-normalized letters in proper order and blanks at bottom
        val arAsc = DesktopTableSortFilterHelper.applyFilterAndSortRows(
            rows = rows,
            state = TableSortFilterState().withSort(1, ColumnSortOrder.ASCENDING)
        )
        assertEquals("", arAsc.last()[1])
        assertEquals(listOf("إبراهيم", "إبراهيم", "أحمد", "بكر", "مُحَمَّد", ""), arAsc.map { it[1] })

        // Filter unique values cleanly
        val filteredState = TableSortFilterState().withColumnFilter(
            colIndex = 1,
            selectedValues = setOf("إبراهيم"),
            allUniqueValues = uniqueNames.toSet()
        )
        val filtered = DesktopTableSortFilterHelper.applyFilterAndSortRows(rows, filteredState)
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it[1] == "إبراهيم" })

        // Text condition filter (e.g. Arabic-normalized CONTAINS / STARTS_WITH)
        val textFilteredState = TableSortFilterState().withTextFilter(
            colIndex = 1,
            filter = ColumnTextFilter(TextFilterOperator.STARTS_WITH, "احمد")
        )
        val textFiltered = DesktopTableSortFilterHelper.applyFilterAndSortRows(rows, textFilteredState)
        assertEquals(1, textFiltered.size)
        assertEquals("أحمد", textFiltered.first()[1])
    }

    @Test
    fun testFormatFilterHelperHidesZeroCountFormats() {
        val visible = DesktopFormatFilterHelper.resolveVisibleGroups(
            typeCounts = mapOf("xlsx" to 40, "pdf" to 3, "csv" to 0)
        )
        assertEquals(listOf(SupportedFileTypeGroups.EXCEL, SupportedFileTypeGroups.PDF), visible.map { it.group })
        assertEquals(40, visible[0].count)
        assertEquals(3, visible[1].count)

        val emptyVisible = DesktopFormatFilterHelper.resolveVisibleGroups(typeCounts = emptyMap())
        assertTrue(emptyVisible.isEmpty())
    }

    @Test
    fun testConcurrentCancelActiveSearchNeverThrowsSqliteInterrupt() = runBlocking {
        val dbDir = tempFolder.newFolder("db_interrupt")
        val wsDir = tempFolder.newFolder("ws_interrupt")
        val bigCsv = File(wsDir, "records_3000.csv")
        bigCsv.writeText(
            buildString {
                appendLine("id,name,notes")
                for (i in 1..3000) {
                    appendLine("$i,الاسم العربي رقم $i,الصنف التجاري رقم $i")
                }
            },
            StandardCharsets.UTF_8
        )

        DesktopSearchEngine(dbDir).use { engine ->
            val ws = engine.addWorkspace(wsDir)
            engine.syncWorkspaces(listOf(ws))

            val canceller = launch(Dispatchers.Default) {
                repeat(40) {
                    engine.cancelActiveSearch()
                    Thread.sleep(2)
                }
            }

            // Running searchGrouped repeatedly while cancelActiveSearch interrupts SQLite must never throw
            repeat(15) { idx ->
                val res = engine.searchGrouped(if (idx % 2 == 0) "ال" else "الصنف")
                assertNotNull(res)
            }
            canceller.join()

            // Final uninterrupted search returns full expected count cleanly
            val settled = engine.searchGrouped("الصنف")
            assertEquals(3000, settled.totalResultsCount)
        }
    }

    @Test
    fun testComposeRtlRightClickMenuOpensNearCursorNotSidebar() {
        val headers = listOf("الرقم", "الاسم الكامل للموظف", "الإدارة")
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/menu_pos.xlsx",
                fileName = "menu_pos.xlsx",
                location = "ورقة: البيانات، صف: 2",
                sheetName = "البيانات",
                rowNumber = 2,
                columnHeaders = headers,
                snippet = "101\tأحمد محمد عبدالله المتوكل\tالإدارة المالية",
                query = "المتوكل"
            )
        )

        val cellBounds = mutableMapOf<Pair<Int, Int>, Rect>()
        var menuWindowRect by mutableStateOf<Rect?>(null)

        ImageComposeScene(
            width = 900,
            height = 460,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                Box(modifier = Modifier.width(900.dp)) {
                    DesktopExcelTableSection(
                        results = results,
                        query = "المتوكل",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        showAllRows = true,
                        accentColor = Color(0xFF1E7E34),
                        onViewDetail = {},
                        onDataCellBounds = { r, c, coords ->
                            if (coords.isAttached) cellBounds[r to c] = coords.boundsInRoot()
                        },
                        onCellContextMenuBounds = { coords ->
                            if (coords.isAttached) {
                                val topLeft = coords.localToWindow(Offset.Zero)
                                val bottomRight = coords.localToWindow(
                                    Offset(coords.size.width.toFloat(), coords.size.height.toFloat())
                                )
                                menuWindowRect = Rect(topLeft, bottomRight)
                            }
                        }
                    )
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)

            val targetCell = cellBounds[0 to 2]!! // Far left column in RTL!
            val clickPos = Offset((targetCell.left + targetCell.right) / 2f, (targetCell.top + targetCell.bottom) / 2f)
            assertTrue("Expected cell (0,2) to lie in left half of 900px RTL window (${clickPos.x})", clickPos.x < 450f)

            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = clickPos,
                timeMillis = 100L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isSecondaryPressed = true),
                button = PointerButton.Secondary
            )
            scene.render(150_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = clickPos,
                timeMillis = 170L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Secondary
            )
            scene.render(220_000_000L)

            val popupRect = menuWindowRect
            assertNotNull("Expected context menu to open on secondary click", popupRect)
            // Prior bug (Alignment.TopStart in RTL) pushed popup past x=800; fixed menu stays anchored near clickPos.x
            assertTrue(
                "Context menu must open near cursor (x=${clickPos.x}), not pushed to RTL edge (popup=${popupRect})",
                popupRect!!.right < 600f && abs(popupRect.left - clickPos.x) < 280f
            )
        }
    }

    @Test
    fun testComposeColumnHeaderFilterMenuWithManyUniqueValuesDoesNotCrashScrollbar() {
        val headers = listOf("م", "الاسم الكامل", "الوحدة")
        val results = (1..35).map { idx ->
            SearchResult(
                id = idx.toLong(),
                fileUri = "C:/docs/taiz_db.xlsx",
                fileName = "taiz_db.xlsx",
                location = "ورقة: ورقة1، صف: ${idx + 1}",
                sheetName = "ورقة1",
                rowNumber = idx + 1,
                columnHeaders = headers,
                snippet = "$idx\tمحمد عبده الصبري رقم $idx\tمحور تعز",
                query = "الصبري"
            )
        }

        val headerBounds = mutableMapOf<Int, Rect>()

        ImageComposeScene(
            width = 1000,
            height = 700,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = true) {
                Box(modifier = Modifier.width(1000.dp)) {
                    DesktopExcelTableSection(
                        results = results,
                        query = "الصبري",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        showAllRows = true,
                        accentColor = Color(0xFF1E7E34),
                        onViewDetail = {},
                        onHeaderCellBounds = { colIdx, coords ->
                            if (coords.isAttached) headerBounds[colIdx] = coords.boundsInRoot()
                        }
                    )
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)

            val nameHeader = headerBounds[1]!!
            val clickPos = Offset((nameHeader.left + nameHeader.right) / 2f, (nameHeader.top + nameHeader.bottom) / 2f)

            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = clickPos,
                timeMillis = 100L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(150_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = clickPos,
                timeMillis = 170L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            // Render multiple frames so LazyColumn + VerticalScrollbar inside DropdownMenu measure & draw
            scene.render(220_000_000L)
            scene.render(300_000_000L)
        }
    }

    @Test
    fun testFormattedCopyVsPlainCopyForCellsRowsAndDocuments() {
        val headers = listOf("الرقم", "الاسم")
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/team.xlsx",
                fileName = "team.xlsx",
                location = "ورقة: الموظفون، صف: 2",
                sheetName = "الموظفون",
                rowNumber = 2,
                columnHeaders = headers,
                snippet = "10\tأحمد",
                query = "أحمد"
            )
        )
        val group = FileResultGroup("C:/docs/team.xlsx", "team.xlsx", isDocument = false, results = results)
        val sheet = DesktopGridNavigator.buildSheetModelsForGroup(group, showAllRows = true).single()

        // Cell: plain vs formatted
        val cellSel = SelectionState().selectSingleCell(sheet.buildCellData(0, 1)!!)
        assertEquals(
            "أحمد",
            DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(cellSel, verticalCopyFormat = true, preferFormattedLabels = false)
        )
        val formattedCell = DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
            cellSel,
            verticalCopyFormat = true,
            preferFormattedLabels = true
        )
        assertTrue(formattedCell.contains("الاسم: أحمد"))
        assertTrue(formattedCell.contains("team.xlsx"))

        // Tabular row: plain TSV vs formatted labeled row
        val rowData = sheet.buildRowData(0)!!
        val rowSel = SelectionState().selectSingleRow(rowData)
        assertEquals(
            "10\tأحمد",
            DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(rowSel, verticalCopyFormat = true, preferFormattedLabels = false)
        )
        val formattedRow = DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
            rowSel,
            verticalCopyFormat = true,
            preferFormattedLabels = true
        )
        assertTrue(formattedRow.contains("صف 2:"))
        assertTrue(formattedRow.contains("الرقم: 10"))
        assertTrue(formattedRow.contains("الاسم: أحمد"))

        // Document row: plain snippet vs formatted header+snippet
        val docRow = com.example.fastfilesearch.desktop.ui.selection.SelectedRowData(
            key = RowKey("C:/docs/notes.txt", "", 0),
            fileName = "notes.txt",
            sheetName = "",
            rowNumber = "1",
            location = "سطر 1",
            headers = emptyList(),
            cells = listOf("ملاحظة مهمة"),
            snippet = "ملاحظة مهمة",
            isDocument = true
        )
        val docSel = SelectionState().selectSingleRow(docRow)
        assertEquals(
            "ملاحظة مهمة",
            DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(docSel, verticalCopyFormat = true, preferFormattedLabels = false)
        )
        val formattedDoc = DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
            docSel,
            verticalCopyFormat = true,
            preferFormattedLabels = true
        )
        assertTrue(formattedDoc.contains("notes.txt (سطر 1):"))
        assertTrue(formattedDoc.contains("ملاحظة مهمة"))
    }

    @Test
    fun testWordAndPdfMergedIntoSingleSelectableParagraph() {
        val pdfResults = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/report.pdf",
                fileName = "report.pdf",
                location = "صفحة 1",
                snippet = "السطر الأول من الصفحة الأولى",
                query = "الصفحة"
            ),
            SearchResult(
                id = 2,
                fileUri = "C:/docs/report.pdf",
                fileName = "report.pdf",
                location = "صفحة 4",
                snippet = "وتكملة النص في الصفحة الرابعة",
                query = "الصفحة"
            )
        )
        val mergedPdfRows = DesktopTableCellHelper.buildSelectableRowsForFile(
            fileUri = "C:/docs/report.pdf",
            fileName = "report.pdf",
            isDocument = true,
            results = pdfResults
        )
        assertEquals(1, mergedPdfRows.size)
        assertEquals("السطر الأول من الصفحة الأولى وتكملة النص في الصفحة الرابعة", mergedPdfRows[0].snippet)
        assertEquals("", mergedPdfRows[0].location)

        val wordResults = listOf(
            SearchResult(
                id = 3,
                fileUri = "C:/docs/memo.docx",
                fileName = "memo.docx",
                location = "فقرة 2",
                snippet = "مذكرة عمل",
                query = "مذكرة"
            ),
            SearchResult(
                id = 4,
                fileUri = "C:/docs/memo.docx",
                fileName = "memo.docx",
                location = "فقرة 7",
                snippet = "بتاريخ اليوم",
                query = "مذكرة"
            )
        )
        val mergedWordRows = DesktopTableCellHelper.buildSelectableRowsForFile(
            fileUri = "C:/docs/memo.docx",
            fileName = "memo.docx",
            isDocument = true,
            results = wordResults
        )
        assertEquals(1, mergedWordRows.size)
        assertEquals("مذكرة عمل بتاريخ اليوم", mergedWordRows[0].snippet)

        // Plain TXT remains split per line cleanly
        val txtResults = listOf(
            SearchResult(id = 5, fileUri = "C:/docs/plain.txt", fileName = "plain.txt", location = "سطر 1", snippet = "سطر أول", query = "سطر"),
            SearchResult(id = 6, fileUri = "C:/docs/plain.txt", fileName = "plain.txt", location = "سطر 3", snippet = "سطر ثالث", query = "سطر")
        )
        val txtRows = DesktopTableCellHelper.buildSelectableRowsForFile(
            fileUri = "C:/docs/plain.txt",
            fileName = "plain.txt",
            isDocument = true,
            results = txtResults
        )
        assertEquals(2, txtRows.size)
        assertEquals("سطر 1", txtRows[0].location)
    }

    @Test
    fun testComposeStickyHeaderPinsDuringTallScrollAndUnpinsWhenTableEnds() {
        val headers = listOf("م", "الاسم الكامل", "القسم")
        val results = (1..30).map { idx ->
            SearchResult(
                id = idx.toLong(),
                fileUri = "C:/docs/tall_sheet.xlsx",
                fileName = "tall_sheet.xlsx",
                location = "ورقة: ورقة 1، صف: ${idx + 1}",
                sheetName = "ورقة 1",
                rowNumber = idx + 1,
                columnHeaders = headers,
                snippet = "$idx\tموظف رقم $idx\tالإدارة",
                query = "موظف"
            )
        }

        val headerPosY = mutableMapOf<Int, Float>()
        var viewportBounds by mutableStateOf<Rect?>(null)
        lateinit var listState: androidx.compose.foundation.lazy.LazyListState

        ImageComposeScene(
            width = 900,
            height = 420,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                listState = rememberLazyListState()
                Column(modifier = Modifier.fillMaxSize()) {
                    // Non-zero top bar (vpTop = 60f) so unclipped positionInRoot().y truly proves pinning
                    Spacer(modifier = Modifier.height(60.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                            .onGloballyPositioned { coords ->
                                coords.unclippedBoundsInRoot()?.let { viewportBounds = it }
                            }
                    ) {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            item(key = "table") {
                                DesktopExcelTableSection(
                                    results = results,
                                    query = "موظف",
                                    searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                                    showAllRows = true,
                                    accentColor = Color(0xFF1E7E34),
                                    viewportRootBounds = viewportBounds,
                                    onViewDetail = {},
                                    onHeaderCellBounds = { colIdx, coords ->
                                        if (coords.isAttached) headerPosY[colIdx] = coords.positionInRoot().y
                                    }
                                )
                            }
                            item(key = "after_spacer") {
                                Spacer(modifier = Modifier.height(600.dp))
                            }
                        }
                    }
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)
            scene.render(20_000_000L)
            val vpTop = viewportBounds!!.top
            assertTrue("Expected vpTop ~ 60f (actual=$vpTop)", abs(vpTop - 60f) < 2f)
            val initialHeaderY = headerPosY[0]!!
            assertTrue("Expected initial header at vpTop=$vpTop (actual=$initialHeaderY)", abs(initialHeaderY - vpTop) <= 4f)

            // Scroll 350px down inside the tall table -> unclipped header positionInRoot().y must STAY pinned at 60f!
            runBlocking { listState.scrollBy(350f) }
            scene.render(30_000_000L)
            scene.render(40_000_000L)
            val pinnedY = headerPosY[0]!!
            assertTrue(
                "Sticky header unclipped Y must stay pinned at vpTop ($vpTop) while scrolling table (actual=$pinnedY)",
                abs(pinnedY - vpTop) <= 2f
            )

            // Scroll near the bottom so only 15px of the table remains -> pinned header must ride upward (< vpTop)
            val tableItemSize = listState.layoutInfo.visibleItemsInfo.first { it.key == "table" }.size
            runBlocking { listState.scrollToItem(0, (tableItemSize - 15).coerceAtLeast(0)) }
            scene.render(50_000_000L)
            scene.render(60_000_000L)
            val ridingUpY = headerPosY[0]!!
            assertTrue("Sticky header should ride off above vpTop as table ends (actual=$ridingUpY, vpTop=$vpTop)", ridingUpY < vpTop)
        }
    }

    @Test
    fun testVerticalArrowNavigationPastRow30AndUnclippedViewportFollowsSelection() {
        val headers = listOf("م", "الاسم")
        val results = (1..45).map { idx ->
            SearchResult(
                id = idx.toLong(),
                fileUri = "C:/docs/big_45.xlsx",
                fileName = "big_45.xlsx",
                location = "ورقة: ورقة 1، صف: ${idx + 1}",
                sheetName = "ورقة 1",
                rowNumber = idx + 1,
                columnHeaders = headers,
                snippet = "$idx\tصف رقم $idx",
                query = "صف"
            )
        }
        val group = FileResultGroup("C:/docs/big_45.xlsx", "big_45.xlsx", isDocument = false, results = results)
        // Regression check: showAllRows = false must NOT truncate GridNavigator at row 30!
        val sheets = DesktopGridNavigator.buildSheetModelsForGroup(group, showAllRows = false)
        assertEquals(45, sheets.single().rowCount)

        var selectionState by mutableStateOf(SelectionState().selectSingleCell(sheets.single().buildCellData(0, 0)!!))
        var viewportBounds by mutableStateOf<Rect?>(null)
        val resolvers = HashMap<String, (Any) -> Pair<Rect, Float>?>()
        lateinit var listState: androidx.compose.foundation.lazy.LazyListState

        ImageComposeScene(
            width = 800,
            height = 420,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                listState = rememberLazyListState()
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coords ->
                            coords.unclippedBoundsInRoot()?.let { viewportBounds = it }
                        }
                ) {
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                        item(key = "big_45") {
                            DesktopExcelTableSection(
                                results = results,
                                query = "صف",
                                searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                                showAllRows = false,
                                accentColor = Color(0xFF1E7E34),
                                selectionState = selectionState,
                                viewportRootBounds = viewportBounds,
                                onRegisterBoundsResolver = { k, fn ->
                                    if (fn == null) resolvers.remove(k) else resolvers[k] = fn
                                },
                                onViewDetail = {}
                            )
                        }
                    }
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)
            scene.render(20_000_000L)

            // Press DOWN 35 times -> navigates cleanly past row 30 to row 35 and scrolls viewport to keep it visible
            repeat(35) {
                selectionState = DesktopGridNavigator.navigateArrow(sheets, selectionState, GridNavDirection.DOWN, isRtl = true)
                scene.render(30_000_000L + it * 1_000_000L)
                val target = selectionState.activeCell!!
                val hit = resolvers.values.firstNotNullOfOrNull { r -> r(target) }
                assertNotNull("Off-screen row ${target.rowIndex} must still resolve unclipped bounds!", hit)
                val rect = hit!!.first
                val inset = hit.second
                val vp = viewportBounds!!
                if (rect.bottom > vp.bottom - 4f) {
                    runBlocking { listState.scrollBy(rect.bottom - (vp.bottom - 4f) + 6f) }
                } else if (rect.top < vp.top + inset + 4f) {
                    runBlocking { listState.scrollBy(rect.top - (vp.top + inset + 4f) - 6f) }
                }
                scene.render(80_000_000L + it * 1_000_000L)
            }

            assertEquals(35, selectionState.activeCell!!.rowIndex)
            val row35Hit = resolvers.values.firstNotNullOfOrNull { r -> r(selectionState.activeCell!!) }!!
            val vp = viewportBounds!!
            assertTrue(
                "Row 35 must lie inside visible viewport [${vp.top + row35Hit.second}, ${vp.bottom}] (actual=${row35Hit.first})",
                row35Hit.first.top >= vp.top + row35Hit.second - 2f && row35Hit.first.bottom <= vp.bottom + 2f
            )

            // Press UP 35 times -> navigates all the way back to row 0 cleanly below unpinned header
            repeat(35) {
                selectionState = DesktopGridNavigator.navigateArrow(sheets, selectionState, GridNavDirection.UP, isRtl = true)
                scene.render(150_000_000L + it * 1_000_000L)
                val target = selectionState.activeCell!!
                val hit = resolvers.values.firstNotNullOfOrNull { r -> r(target) }!!
                val rect = hit.first
                val inset = hit.second
                if (rect.bottom > vp.bottom - 4f) {
                    runBlocking { listState.scrollBy(rect.bottom - (vp.bottom - 4f) + 6f) }
                } else if (rect.top < vp.top + inset + 4f) {
                    runBlocking { listState.scrollBy(rect.top - (vp.top + inset + 4f) - 6f) }
                }
                scene.render(200_000_000L + it * 1_000_000L)
            }
            assertEquals(0, selectionState.activeCell!!.rowIndex)
            assertEquals(0, listState.firstVisibleItemIndex)
            assertTrue("Expected listState back at top (scrollOffset=${listState.firstVisibleItemScrollOffset})", listState.firstVisibleItemScrollOffset <= 10)
        }
    }

    @Test
    fun testArrowNavigationSupportsRowSelectionModeAndMultiSheetCrossing() {
        val headers = listOf("م", "البيان")
        val s1 = (1..2).map { idx ->
            SearchResult(
                id = idx.toLong(),
                fileUri = "C:/docs/f1.xlsx",
                fileName = "f1.xlsx",
                location = "ورقة: و1، صف: $idx",
                snippet = "$idx\tسطر $idx",
                query = "سطر",
                sheetName = "و1",
                rowNumber = idx,
                columnHeaders = headers
            )
        }
        val s2 = (1..2).map { idx ->
            SearchResult(
                id = (idx + 10).toLong(),
                fileUri = "C:/docs/f2.xlsx",
                fileName = "f2.xlsx",
                location = "ورقة: و1، صف: $idx",
                snippet = "${idx + 10}\tملف2 سطر $idx",
                query = "سطر",
                sheetName = "و1",
                rowNumber = idx,
                columnHeaders = headers
            )
        }
        val sheets = DesktopGridNavigator.buildSheetModelsForGroup(FileResultGroup("C:/docs/f1.xlsx", "f1.xlsx", false, s1), true) +
            DesktopGridNavigator.buildSheetModelsForGroup(FileResultGroup("C:/docs/f2.xlsx", "f2.xlsx", false, s2), true)
        assertEquals(2, sheets.size)

        // 1. In ROWS mode starting at row 0 of f2, DOWN stays in f2 row 1 (does NOT jump back to f1 row 0)
        var rowSel = SelectionState().selectSingleRow(sheets[1].buildRowData(0)!!)
        rowSel = DesktopGridNavigator.navigateArrow(sheets, rowSel, GridNavDirection.DOWN, isRtl = true)
        assertEquals(SelectionMode.ROWS, rowSel.mode)
        assertEquals(RowKey("C:/docs/f2.xlsx", "و1", 1), rowSel.focusedRow)

        // UP from f2 row 0 crosses smoothly into f1 last row (row 1)
        rowSel = DesktopGridNavigator.navigateArrow(sheets, rowSel, GridNavDirection.UP, isRtl = true)
        rowSel = DesktopGridNavigator.navigateArrow(sheets, rowSel, GridNavDirection.UP, isRtl = true)
        assertEquals(RowKey("C:/docs/f1.xlsx", "و1", 1), rowSel.focusedRow)

        // 2. In CELLS mode, DOWN from f1 last row crosses cleanly to f2 row 0 keeping same column
        var cellSel = SelectionState().selectSingleCell(sheets[0].buildCellData(1, 1)!!)
        cellSel = DesktopGridNavigator.navigateArrow(sheets, cellSel, GridNavDirection.DOWN, isRtl = true)
        assertEquals(CellKey("C:/docs/f2.xlsx", "و1", 0, 1), cellSel.activeCell)
        cellSel = DesktopGridNavigator.navigateArrow(sheets, cellSel, GridNavDirection.UP, isRtl = true)
        assertEquals(CellKey("C:/docs/f1.xlsx", "و1", 1, 1), cellSel.activeCell)
    }

    @Test
    fun testDesktopSettingsAndWorkspacesRenderWithoutLicensing() {
        val lockDir = tempFolder.newFolder("lock_ui_dir")
        val lockManager = DesktopAppLockManager(baseDir = lockDir)

        ImageComposeScene(
            width = 960,
            height = 700,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                DesktopSettingsScreen(
                    appLockManager = lockManager,
                    themeMode = "light",
                    defaultSearchMethod = SearchMethod.CONTAINS,
                    showAllRows = false,
                    stretchSingleColumn = true,
                    verticalCopyFormat = true,
                    expandFullCellText = true,
                    backgroundSyncEnabled = true,
                    searchHistoryEnabled = true,
                    searchHistoryCount = 0,
                    onChangeTheme = {},
                    onChangeDefaultMethod = {},
                    onToggleShowAllRows = {},
                    onToggleStretchSingleColumn = {},
                    onToggleVerticalCopyFormat = {},
                    onToggleExpandFullCellText = {},
                    onToggleBackgroundSync = {},
                    onToggleSearchHistory = {},
                    onClearSearchHistory = {},
                    onOpenAbout = {}
                )
            }
        }.use { scene ->
            scene.render(10_000_000L)
        }

        ImageComposeScene(
            width = 960,
            height = 600,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = true) {
                DesktopWorkspacesScreen(
                    workspaces = listOf(Workspace(id = "ws1", name = "مجلد تجريبي", path = "C:/docs", addedAt = 1L)),
                    activeWorkspaceId = null,
                    indexProgress = IndexProgress(),
                    filesCount = 12,
                    segmentsCount = 340,
                    statusBanner = null,
                    onAddFolderClick = {},
                    onDeleteWorkspace = {},
                    onRefreshIndex = {},
                    onCancelIndexing = {}
                )
            }
        }.use { scene ->
            scene.render(10_000_000L)
        }
    }

    @Test
    fun testDesktopLockScreenRendersWithLogo() {
        val lockDir = tempFolder.newFolder("lock_screen_test_dir")
        val lockManager = DesktopAppLockManager(baseDir = lockDir)

        ImageComposeScene(
            width = 960,
            height = 700,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                DesktopLockScreen(
                    lockManager = lockManager,
                    onUnlocked = {}
                )
            }
        }.use { scene ->
            scene.render(10_000_000L)
        }

        ImageComposeScene(
            width = 960,
            height = 700,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = true) {
                DesktopLockScreen(
                    lockManager = lockManager,
                    onUnlocked = {}
                )
            }
        }.use { scene ->
            scene.render(10_000_000L)
        }
    }
}
