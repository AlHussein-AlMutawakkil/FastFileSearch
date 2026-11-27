package com.example.fastfilesearch.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.ui.DesktopExcelTableSection
import com.example.fastfilesearch.desktop.ui.DesktopTableDimensions
import com.example.fastfilesearch.desktop.ui.DesktopTableLayoutCalculator
import com.example.fastfilesearch.desktop.ui.ProvideDesktopTheme
import com.example.fastfilesearch.desktop.ui.selection.CellKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

@OptIn(ExperimentalComposeUiApi::class)
class DesktopTableLayoutAndRtlTest {

    @Test
    fun testRtlColumnResizeDeltaAndDoubleClickReset() {
        // Dragging LEFT (negative dx) on an RTL trailing border widens the column
        val widened = DesktopTableLayoutCalculator.applyColumnResizeDelta(
            currentWidth = 140.dp,
            dragDeltaDp = (-55).dp,
            isRtl = true
        )
        assertEquals(195.dp, widened)

        // Dragging RIGHT (positive dx) on an RTL trailing border narrows the column
        val narrowed = DesktopTableLayoutCalculator.applyColumnResizeDelta(
            currentWidth = 140.dp,
            dragDeltaDp = 40.dp,
            isRtl = true
        )
        assertEquals(100.dp, narrowed)

        // Clamped at minimum resize width (48.dp)
        val clampedMin = DesktopTableLayoutCalculator.applyColumnResizeDelta(
            currentWidth = 70.dp,
            dragDeltaDp = 80.dp,
            isRtl = true
        )
        assertEquals(DesktopTableLayoutCalculator.MIN_RESIZE_COL_WIDTH, clampedMin)

        // Applying custom width overrides updates total width; removing resets to auto width
        val autoDims = DesktopTableDimensions(
            rowNumWidth = 46.dp,
            columnWidths = listOf(80.dp, 200.dp, 120.dp),
            totalTableWidth = 446.dp
        )
        val customMap = mapOf(1 to 320.dp)
        val resizedDims = DesktopTableLayoutCalculator.applyCustomColumnWidths(autoDims, customMap)
        assertEquals(listOf(80.dp, 320.dp, 120.dp), resizedDims.columnWidths)
        assertEquals(566.dp, resizedDims.totalTableWidth)

        // Double-click removes key from custom map and restores exact auto-fit dimensions
        val resetDims = DesktopTableLayoutCalculator.applyCustomColumnWidths(autoDims, customMap - 1)
        assertEquals(autoDims, resetDims)
    }

    @Test
    fun testAutoFitWidthsUseTextMeasurerAndDistributeSurplusCleanly() {
        var calculatedDims: DesktopTableDimensions? = null

        val rowsData = listOf(
            Triple(
                "10",
                listOf("5", "شركة النور للتجارة والتوريدات العامة", "الرياض"),
                Unit
            ),
            Triple(
                "102",
                listOf("8", "مؤسسة الأفق للخدمات الهندسية والمقاولات المتكاملة", "جدة"),
                Unit
            )
        )
        val headers = listOf("م", "اسم الشركة بالكامل والنشاط المسجل", "المدينة")

        ImageComposeScene(
            width = 900,
            height = 300,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            val density = LocalDensity.current
            val textMeasurer = rememberTextMeasurer()
            calculatedDims = DesktopTableLayoutCalculator.calculate(
                rowsData = rowsData,
                columnHeaders = headers,
                maxCols = 3,
                totalContainerWidth = 860.dp,
                stretchSingleColumn = true,
                textMeasurer = textMeasurer,
                density = density,
                cellTextStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp),
                headerTextStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp),
                rowNumTextStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            )
        }.use { scene ->
            scene.render()
        }

        val dims = calculatedDims!!
        assertTrue("rowNumWidth (${dims.rowNumWidth}) should be in 42..68dp", dims.rowNumWidth in 42.dp..68.dp)
        assertEquals(3, dims.columnWidths.size)
        // Column 0 ("م" / "5") should remain significantly narrower than text Column 1
        assertTrue(
            "Numeric column (${dims.columnWidths[0]}) should stay narrower than long text column (${dims.columnWidths[1]})",
            dims.columnWidths[0] < dims.columnWidths[1]
        )
        // Exact fill to container width with zero 1px ghost scroll when base < remainingWidth
        assertTrue(
            "Expected totalTableWidth >= 860.dp but was ${dims.totalTableWidth}",
            dims.totalTableWidth >= 859.dp
        )
    }

    @Test
    fun testComposeRtlTableStartsAtFarRightWithIntrinsicRowHeightsAndDragResize() {
        val headers = listOf(
            "الرقم",
            "البيان التفصيلي الطويل للعملية المحاسبية المسجلة بالنظام",
            "القسم",
            "المبلغ",
            "المسؤول",
            "الحالة",
            "ملاحظات إضافية شاملة"
        )
        val results = listOf(
            SearchResult(
                id = 1,
                fileUri = "C:/docs/ledger.xlsx",
                fileName = "ledger.xlsx",
                location = "ورقة: القيود، صف: 2",
                sheetName = "القيود",
                rowNumber = 2,
                columnHeaders = headers,
                snippet = listOf(
                    "101",
                    "توريد أجهزة خوادم وملحقات شبكات عالية السرعة لمركز البيانات الرئيسي وتحديث وحدات التخزين الاحتياطية لعام 2026",
                    "تقنية المعلومات",
                    "45000",
                    "خالد المنصور",
                    "معتمد",
                    "مراجعة الفاتورة المرفقة واعتماد التسليم النهائي بالمستودع"
                ).joinToString("\t"),
                query = "توريد"
            ),
            SearchResult(
                id = 2,
                fileUri = "C:/docs/ledger.xlsx",
                fileName = "ledger.xlsx",
                location = "ورقة: القيود، صف: 3",
                sheetName = "القيود",
                rowNumber = 3,
                columnHeaders = headers,
                snippet = listOf(
                    "102",
                    "صيانة سنوية",
                    "الدعم",
                    "1200",
                    "سارة أحمد",
                    "قيد المراجعة",
                    "دفعة أولى"
                ).joinToString("\t"),
                query = "توريد"
            )
        )

        var rowNumBounds by mutableStateOf<Rect?>(null)
        val headerBoundsMap = mutableMapOf<Int, Rect>()
        val row0CellBoundsMap = mutableMapOf<Int, Rect>()

        ImageComposeScene(
            width = 760,
            height = 600,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                Box(modifier = Modifier.width(760.dp)) {
                    DesktopExcelTableSection(
                        results = results,
                        query = "توريد",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        showAllRows = true,
                        accentColor = Color(0xFF1E7E34),
                        onViewDetail = {},
                        onRowNumBounds = { rowNumBounds = it.boundsInRoot() },
                        onHeaderCellBounds = { col, coords -> headerBoundsMap[col] = coords.boundsInRoot() },
                        onDataCellBounds = { row, col, coords ->
                            if (row == 0) row0CellBoundsMap[col] = coords.boundsInRoot()
                        }
                    )
                }
            }
        }.use { scene ->
            scene.render(10_000_000L)

            val rNum = rowNumBounds!!
            val h0 = headerBoundsMap[0]!!
            val h1 = headerBoundsMap[1]!!

            // 1. Table must start from the FAR RIGHT: "صف" at the right edge, then Column 0 fully unclipped
            assertTrue("Row number column right (${rNum.right}) must align with right edge (760)", rNum.right >= 758f)
            assertTrue("Column 0 right (${h0.right}) must touch Row number left (${rNum.left})", h0.right <= rNum.left + 1f && h0.right >= rNum.left - 1f)
            assertTrue("Column 0 left (${h0.left}) must fit inside viewport (>= 0)", h0.left >= 0f)
            assertTrue("Column 1 right (${h1.right}) must align to the left of Column 0 (${h0.left})", h1.right <= h0.left + 1f)

            // 2. Column 0 ("الرقم") is narrower than long Column 1 ("البيان...") via TextMeasurer auto-fit
            assertTrue("Short Column 0 (${h0.width}) should be narrower than wrapped Column 1 (${h1.width})", h0.width < h1.width)

            // 3. Long header can wrap up to 2 lines -> header row grows beyond minimum 38dp cleanly
            assertEquals(h0.height, h1.height, 1f)
            assertTrue("Wrapped long header should exceed 38px min height, actual=${h1.height}", h1.height >= 40f)

            // 4. Data Row 0 cells have uniform height via IntrinsicSize.Min + fillMaxHeight (no short-cell white gap)
            val c0 = row0CellBoundsMap[0]!!
            val c1 = row0CellBoundsMap[1]!!
            assertTrue("Long text in Col 1 should wrap taller than 42dp (${c1.height})", c1.height > 60f)
            assertEquals("Short Col 0 height (${c0.height}) must match tall Col 1 height (${c1.height})", c1.height, c0.height, 1f)

            // 5. Interactive RTL mouse drag on Column 1's left border (CenterEnd in RTL) widens Column 1
            val initialH1Width = h1.width
            val dragX = h1.left + 3f
            val dragY = (h1.top + h1.bottom) / 2f
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = Offset(dragX, dragY),
                timeMillis = 100L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(110_000_000L)
            for (step in 1..6) {
                scene.sendPointerEvent(
                    eventType = PointerEventType.Move,
                    position = Offset(dragX - (step * 15f), dragY),
                    timeMillis = 100L + (step * 16L),
                    type = PointerType.Mouse,
                    buttons = PointerButtons(isPrimaryPressed = true)
                )
                scene.render((110L + step * 16L) * 1_000_000L)
            }
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = Offset(dragX - 90f, dragY),
                timeMillis = 220L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(240_000_000L)

            val resizedH1Width = headerBoundsMap[1]!!.width
            assertTrue(
                "Dragging RTL trailing handle LEFT should widen Column 1 from $initialH1Width to > ${initialH1Width + 50f}, got $resizedH1Width",
                resizedH1Width > initialH1Width + 50f
            )

            // 6. Double-click on the resized border resets Column 1 back to initial auto-fit width
            val resetX = headerBoundsMap[1]!!.left + 3f
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = Offset(resetX, dragY),
                timeMillis = 600L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(610_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = Offset(resetX, dragY),
                timeMillis = 620L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(630_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Press,
                position = Offset(resetX, dragY),
                timeMillis = 700L,
                type = PointerType.Mouse,
                buttons = PointerButtons(isPrimaryPressed = true),
                button = PointerButton.Primary
            )
            scene.render(710_000_000L)
            scene.sendPointerEvent(
                eventType = PointerEventType.Release,
                position = Offset(resetX, dragY),
                timeMillis = 720L,
                type = PointerType.Mouse,
                buttons = PointerButtons(),
                button = PointerButton.Primary
            )
            scene.render(740_000_000L)

            val resetH1Width = headerBoundsMap[1]!!.width
            assertTrue(
                "Double-clicking handle should reset Column 1 width back near $initialH1Width, got $resetH1Width",
                abs(resetH1Width - initialH1Width) <= 2f
            )
        }
    }

    @Test
    fun testUnconstrainedTableHeightRendersAllRowsAndSyncsRtlHorizontalScroll() {
        val headers = listOf("الرقم", "البيان التفصيلي الطويل للعملية", "الإدارة المسؤولة", "القيمة المالية", "الحالة التنفيذية", "ملاحظات المراجع")
        val results = (1..25).map { idx ->
            SearchResult(
                id = idx.toLong(),
                fileUri = "C:/docs/tall_sheet.xlsx",
                fileName = "tall_sheet.xlsx",
                location = "ورقة: السجل، صف: ${idx + 1}",
                sheetName = "السجل",
                rowNumber = idx + 1,
                columnHeaders = headers,
                snippet = listOf(
                    "${100 + idx}",
                    "عملية توريد وتجهيز معدات رقم $idx لفرع المركز الرئيسي",
                    "المشتريات والعقود",
                    "${idx * 250}",
                    "مكتمل",
                    "مطابقة أصل الفاتورة #$idx"
                ).joinToString("\t"),
                query = "توريد"
            )
        }

        val headerBoundsMap = mutableMapOf<Int, Rect>()
        val headerPosMap = mutableMapOf<Int, Offset>()
        val cellBoundsMap = mutableMapOf<Pair<Int, Int>, Rect>()
        val cellPosMap = mutableMapOf<Pair<Int, Int>, Offset>()
        var selectionState by mutableStateOf(SelectionState())

        ImageComposeScene(
            width = 460,
            height = 600,
            density = Density(1f),
            layoutDirection = LayoutDirection.Rtl
        ) {
            ProvideDesktopTheme(isDark = false) {
                Column(modifier = Modifier.width(460.dp).verticalScroll(rememberScrollState())) {
                    DesktopExcelTableSection(
                        results = results,
                        query = "توريد",
                        searchFilter = SearchFilter(method = SearchMethod.CONTAINS),
                        showAllRows = true,
                        accentColor = Color(0xFF1E7E34),
                        selectionState = selectionState,
                        onViewDetail = {},
                        onHeaderCellBounds = { colIdx, coords ->
                            if (coords.isAttached) {
                                headerBoundsMap[colIdx] = coords.boundsInRoot()
                                headerPosMap[colIdx] = coords.positionInRoot()
                            }
                        },
                        onDataCellBounds = { rowIdx, colIdx, coords ->
                            if (coords.isAttached) {
                                cellBoundsMap[rowIdx to colIdx] = coords.boundsInRoot()
                                cellPosMap[rowIdx to colIdx] = coords.positionInRoot()
                            }
                        }
                    )
                }
            }
        }.use { scene ->
            scene.render(100_000_000L)

            val h1Before = headerBoundsMap[1]!!
            val c01Before = cellBoundsMap[0 to 1]!!
            val c0PosBefore = cellPosMap[0 to 1]!!
            val c24PosBefore = cellPosMap[24 to 1]!!

            // Synchronized horizontally at rest & table has no fixed height cap (all 25 rows laid out continuously)
            assertEquals(h1Before.left, c01Before.left, 1f)
            assertEquals(h1Before.right, c01Before.right, 1f)
            assertTrue("Row 0 should start below header", c0PosBefore.y >= h1Before.bottom - 1f)
            assertTrue("Row 24 should extend naturally beyond fixed height caps (${c24PosBefore.y})", c24PosBefore.y > 700f)

            // Activate far column (Col 5) -> triggers real hScroll while header & row 2 are in viewport
            selectionState = selectionState.selectSingleCell(
                SelectedCellData(
                    key = CellKey("C:/docs/tall_sheet.xlsx", "السجل", 2, 5),
                    fileName = "tall_sheet.xlsx",
                    sheetName = "السجل",
                    rowNumber = "4",
                    columnName = headers[5],
                    value = "مطابقة"
                )
            )
            scene.render(200_000_000L)
            scene.render(240_000_000L)

            val h5AfterH = headerBoundsMap[5]!!
            val c5AfterH = cellBoundsMap[2 to 5]!!
            val h1PosAfterH = headerPosMap[1]!!
            assertTrue(
                "Expected horizontal scroll to shift Column 1 rightwards in RTL (${h1PosAfterH.x} > ${h1Before.left + 20f})",
                h1PosAfterH.x > h1Before.left + 20f
            )
            assertTrue("Expected Col 5 header to be visible after horizontal scroll", h5AfterH.width > 10f)
            assertEquals("Header Col 5 & data row Col 5 left edge must stay synced horizontally", h5AfterH.left, c5AfterH.left, 1f)
            assertEquals("Header Col 5 & data row Col 5 right edge must stay synced horizontally", h5AfterH.right, c5AfterH.right, 1f)

            // Scroll outer container vertically (table scrolls naturally with page)
            val scrollTargetPos = Offset(220f, 250f)
            repeat(4) { tick ->
                scene.sendPointerEvent(
                    eventType = PointerEventType.Scroll,
                    position = scrollTargetPos,
                    scrollDelta = Offset(0f, 15f),
                    timeMillis = 300L + (tick * 20L),
                    type = PointerType.Mouse
                )
                scene.render((310L + tick * 20L) * 1_000_000L)
            }

            val c24PosAfterV = cellPosMap[24 to 1]!!
            assertTrue("Table rows should scroll smoothly with outer page (${c24PosAfterV.y} < ${c24PosBefore.y})", c24PosAfterV.y < c24PosBefore.y)
        }
    }
}
