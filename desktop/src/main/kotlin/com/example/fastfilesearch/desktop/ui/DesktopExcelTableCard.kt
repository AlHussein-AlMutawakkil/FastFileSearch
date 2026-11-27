@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.ui.selection.CellKey
import com.example.fastfilesearch.desktop.ui.selection.RowKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectedRowData
import com.example.fastfilesearch.desktop.ui.selection.SelectionMode
import com.example.fastfilesearch.desktop.ui.selection.SelectionState
import com.example.fastfilesearch.desktop.ui.selection.SheetGridModel
import com.example.fastfilesearch.desktop.util.ArabicNormalizer
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import java.awt.Cursor
import kotlin.math.abs
import kotlin.math.roundToInt

private val EMPTY_INT_SET: Set<Int> = emptySet()

@Immutable
private data class DesktopExcelTableThemeColors(
    val tableBg: Color,
    val rowAltBg: Color,
    val colHeaderBg: Color,
    val rowNumBg: Color,
    val gridBorderColor: Color,
    val headerBorderColor: Color,
    val headerTextColor: Color,
    val cellTextColor: Color,
    val matchBg: Color,
    val sheetStripBg: Color,
    val selectedCellBg: Color,
    val selectedRowBg: Color,
    val activeCellBorder: Color,
    val selectedCellBorder: Color
)

@Composable
private fun rememberDesktopExcelTableColors(isDark: Boolean): DesktopExcelTableThemeColors {
    val desktopColors = LocalDesktopColors.current
    return remember(isDark, desktopColors.controlActiveBackground) {
        val accent = desktopColors.controlActiveBackground
        val selectedCellBg = accent.copy(alpha = if (isDark) 0.16f else 0.11f)
        val selectedRowBg = accent.copy(alpha = if (isDark) 0.10f else 0.06f)
        val selectedCellBorder = accent.copy(alpha = 0.45f)
        if (isDark) {
            DesktopExcelTableThemeColors(
                tableBg = Color(0xFF131518),
                rowAltBg = Color(0xFF16191D),
                colHeaderBg = Color(0xFF1F2328),
                rowNumBg = Color(0xFF1B1E22),
                gridBorderColor = Color(0xFF2D3137),
                headerBorderColor = Color(0xFF33383F),
                headerTextColor = Color(0xFFB5B9BF),
                cellTextColor = Color(0xFFE4E7EB),
                matchBg = Color(0xFF28251C),
                sheetStripBg = Color(0xFF181B1F),
                selectedCellBg = selectedCellBg,
                selectedRowBg = selectedRowBg,
                activeCellBorder = accent,
                selectedCellBorder = selectedCellBorder
            )
        } else {
            DesktopExcelTableThemeColors(
                tableBg = Color(0xFFFFFFFF),
                rowAltBg = Color(0xFFF9FAFB),
                colHeaderBg = Color(0xFFF1F3F5),
                rowNumBg = Color(0xFFEDEDF0),
                gridBorderColor = Color(0xFFE2E4E8),
                headerBorderColor = Color(0xFFD0D3D9),
                headerTextColor = Color(0xFF495057),
                cellTextColor = Color(0xFF212529),
                matchBg = Color(0xFFFFF8E7),
                sheetStripBg = Color(0xFFF1F3F5),
                selectedCellBg = selectedCellBg,
                selectedRowBg = selectedRowBg,
                activeCellBorder = accent,
                selectedCellBorder = selectedCellBorder
            )
        }
    }
}

private data class CellContextMenuTarget(
    val rowIndex: Int,
    val colIndex: Int,
    val cellData: SelectedCellData,
    val rowData: SelectedRowData,
    val result: SearchResult,
    val localClickPos: Offset
)

@Composable
fun DesktopExcelTableSection(
    results: List<SearchResult>,
    query: String,
    searchFilter: SearchFilter,
    showAllRows: Boolean,
    accentColor: Color,
    onViewDetail: (SearchResult) -> Unit,
    stretchSingleColumn: Boolean = true,
    expandFullCellText: Boolean = true,
    verticalCopyFormat: Boolean = true,
    fileUri: String = results.firstOrNull()?.fileUri ?: "",
    fileName: String = results.firstOrNull()?.fileName ?: "",
    selectionState: SelectionState = SelectionState(),
    onSelectionStateChange: ((SelectionState) -> Unit)? = null,
    onCopyText: ((String) -> Unit)? = null,
    onViewCellDetail: ((SelectedCellData) -> Unit)? = null,
    onExportSelection: (() -> Unit)? = null,
    onRequestKeyboardFocus: (() -> Unit)? = null,
    expandedSheets: Set<String> = emptySet(),
    onExpandSheet: ((String) -> Unit)? = null,
    viewportRootBounds: Rect? = null,
    viewportBoundsProvider: (() -> Rect?)? = null,
    onRegisterBoundsResolver: ((String, ((Any) -> Pair<Rect, Float>?)?) -> Unit)? = null,
    onRegisterLiveSheet: ((String, SheetGridModel?) -> Unit)? = null,
    onRowNumBounds: ((LayoutCoordinates) -> Unit)? = null,
    onHeaderCellBounds: ((Int, LayoutCoordinates) -> Unit)? = null,
    onDataCellBounds: ((Int, Int, LayoutCoordinates) -> Unit)? = null,
    onCellContextMenuBounds: ((LayoutCoordinates) -> Unit)? = null
) {
    val desktopColors = LocalDesktopColors.current
    val tableColors = rememberDesktopExcelTableColors(desktopColors.isDark)
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl

    val sheetGroups = remember(results) {
        results.groupBy { res ->
            res.sheetName ?: DesktopTableCellHelper.extractSheetAndRow(res.location).first
        }
    }

    Column(modifier = Modifier.fillMaxWidth().background(tableColors.tableBg)) {
        sheetGroups.forEach { (sheetName, sheetResults) ->
            var sortFilterState by remember(fileUri, sheetName, query) {
                mutableStateOf(TableSortFilterState())
            }
            var openFilterColIdx by remember(fileUri, sheetName, query) {
                mutableStateOf<Int?>(null)
            }

            val rawSortedResults = remember(sheetResults) {
                sheetResults.sortedBy { res ->
                    res.rowNumber ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
                }
            }
            val headers = remember(rawSortedResults) {
                rawSortedResults.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            }
            val rawParsedAllRows = remember(rawSortedResults) {
                rawSortedResults.map { res -> DesktopTableCellHelper.splitIntoCells(res.snippet) }
            }
            val maxCols = remember(rawParsedAllRows, headers) {
                maxOf(
                    rawParsedAllRows.maxOfOrNull { it.size } ?: 1,
                    headers.size
                ).coerceAtLeast(1)
            }
            val sortedResults = remember(rawSortedResults, sortFilterState) {
                DesktopTableSortFilterHelper.applyFilterAndSort(rawSortedResults, sortFilterState) { res ->
                    DesktopTableCellHelper.splitIntoCells(res.snippet)
                }
            }

            val visibleRows = sortedResults

            val parsedRows = remember(visibleRows) {
                visibleRows.map { res ->
                    val rNum = res.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second
                    Triple(rNum, DesktopTableCellHelper.splitIntoCells(res.snippet), res)
                }
            }
            val rawSampleTriples = remember(rawSortedResults) {
                rawSortedResults.take(50).map { res ->
                    val rNum = res.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second
                    Triple(rNum, DesktopTableCellHelper.splitIntoCells(res.snippet), res)
                }
            }
            val layoutSampleRows = remember(parsedRows, rawSampleTriples, sortFilterState) {
                if (sortFilterState.hasActiveConstraints()) {
                    rawSampleTriples
                } else {
                    parsedRows.take(50)
                }
            }

            val sheetModel = remember(fileUri, fileName, sheetName, headers, parsedRows, visibleRows, maxCols) {
                SheetGridModel(
                    fileUri = fileUri,
                    fileName = fileName,
                    sheetName = sheetName,
                    headers = headers,
                    rowNumbers = parsedRows.map { it.first },
                    rows = parsedRows.map { it.second },
                    results = visibleRows,
                    maxCols = maxCols
                )
            }

            if (sheetGroups.size > 1 || sheetName != "ورقة 1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tableColors.sheetStripBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(accentColor))
                    Text(
                        text = sheetName,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = tableColors.headerTextColor
                    )
                }
                HorizontalDivider(color = tableColors.gridBorderColor, thickness = 0.5.dp)
            }

            val sheetResetKey = remember(sheetName, fileUri, maxCols) {
                Triple(fileUri, sheetName, maxCols)
            }
            var customColumnWidths by remember(sheetResetKey) { mutableStateOf<Map<Int, Dp>>(emptyMap()) }
            val hScroll = remember(sheetResetKey) { ScrollState(0) }

            LaunchedEffect(sheetResetKey) {
                if (hScroll.value != 0) {
                    hScroll.scrollTo(0)
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalContainerWidth = maxWidth
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()

                val cellTextStyle = remember { TextStyle(fontSize = 13.sp, lineHeight = 18.sp) }
                val headerTextStyle = remember { TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp) }
                val rowNumTextStyle = remember {
                    TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                val autoDims = remember(
                    layoutSampleRows,
                    headers,
                    maxCols,
                    totalContainerWidth,
                    stretchSingleColumn,
                    density
                ) {
                    DesktopTableLayoutCalculator.calculate(
                        rowsData = layoutSampleRows,
                        columnHeaders = headers,
                        maxCols = maxCols,
                        totalContainerWidth = totalContainerWidth,
                        stretchSingleColumn = stretchSingleColumn,
                        textMeasurer = textMeasurer,
                        density = density,
                        cellTextStyle = cellTextStyle,
                        headerTextStyle = headerTextStyle,
                        rowNumTextStyle = rowNumTextStyle
                    )
                }

                val dims = remember(autoDims, customColumnWidths) {
                    DesktopTableLayoutCalculator.applyCustomColumnWidths(autoDims, customColumnWidths)
                }

                // Auto-scroll horizontally when activeCell moves via keyboard arrows
                val activeCell = selectionState.activeCell
                LaunchedEffect(activeCell, dims) {
                    if (activeCell != null &&
                        activeCell.fileUri == fileUri &&
                        activeCell.sheetName == sheetName &&
                        activeCell.colIndex in 0 until maxCols
                    ) {
                        val colOffsetDp = dims.rowNumWidth +
                            dims.columnWidths.take(activeCell.colIndex).fold(0.dp) { acc, w -> acc + w }
                        val colWidthDp = dims.columnWidths.getOrElse(activeCell.colIndex) { 110.dp }
                        val colStartPx = with(density) { colOffsetDp.toPx() }.toInt()
                        val colEndPx = colStartPx + with(density) { colWidthDp.toPx() }.toInt()
                        val viewportPx = with(density) { totalContainerWidth.toPx() }.toInt()
                        val curScroll = hScroll.value
                        if (colStartPx < curScroll) {
                            hScroll.scrollTo(colStartPx.coerceAtLeast(0))
                        } else if (colEndPx > curScroll + viewportPx) {
                            hScroll.scrollTo((colEndPx - viewportPx).coerceAtLeast(0))
                        }
                    }
                }

                var sheetMenuExpanded by remember { mutableStateOf(false) }
                var rowContextMenuTarget by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
                var cellContextMenu by remember { mutableStateOf<CellContextMenuTarget?>(null) }

                val currentSelectionState by rememberUpdatedState(selectionState)
                val currentSheetModel by rememberUpdatedState(sheetModel)
                val currentOnSelectionChange by rememberUpdatedState(onSelectionStateChange)
                val currentOnViewDetail by rememberUpdatedState(onViewDetail)
                val currentOnViewCellDetail by rememberUpdatedState(onViewCellDetail)
                val currentOnRequestFocus by rememberUpdatedState(onRequestKeyboardFocus)
                val currentOnCopyText by rememberUpdatedState(onCopyText)
                val currentOnExportSelection by rememberUpdatedState(onExportSelection)

                val rowLocalSpanMap = remember(sheetResetKey, parsedRows.size) { HashMap<Int, Pair<Float, Float>>() }
                var gridRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                val stickyOffsetState = remember(sheetResetKey) { mutableIntStateOf(0) }
                val headerHeightRef = remember(sheetResetKey) { intArrayOf(0) }
                val sheetBodyCoordsRef = remember(sheetResetKey) { arrayOfNulls<LayoutCoordinates>(1) }
                val latestViewportBounds by rememberUpdatedState(viewportRootBounds)
                val latestViewportProvider by rememberUpdatedState(viewportBoundsProvider)
                val latestOnRegisterResolver by rememberUpdatedState(onRegisterBoundsResolver)
                val latestOnRegisterLiveSheet by rememberUpdatedState(onRegisterLiveSheet)
                val sheetResolverKey = remember(fileUri, sheetName) { "${fileUri}::${sheetName}" }

                DisposableEffect(sheetResolverKey, sheetModel) {
                    latestOnRegisterLiveSheet?.invoke(sheetResolverKey, sheetModel)
                    onDispose {
                        latestOnRegisterLiveSheet?.invoke(sheetResolverKey, null)
                    }
                }

                DisposableEffect(sheetResolverKey) {
                    val reg = latestOnRegisterResolver
                    if (reg != null) {
                        val resolver: (Any) -> Pair<Rect, Float>? = resolver@{ targetKey ->
                            val matchesSheet = when (targetKey) {
                                is CellKey -> targetKey.fileUri == fileUri && targetKey.sheetName == sheetName
                                is RowKey -> targetKey.fileUri == fileUri && targetKey.sheetName == sheetName
                                else -> false
                            }
                            if (!matchesSheet) return@resolver null
                            val bodyCoords = sheetBodyCoordsRef[0] ?: return@resolver null
                            if (!bodyCoords.isAttached) return@resolver null

                            val rIdx = when (targetKey) {
                                is CellKey -> targetKey.rowIndex
                                is RowKey -> targetKey.rowIndex
                                else -> return@resolver null
                            }
                            val span = rowLocalSpanMap[rIdx] ?: return@resolver null
                            val bodyPos = bodyCoords.positionInRoot()
                            val hdrH = headerHeightRef[0]
                            val rowTop = bodyPos.y + hdrH + span.first
                            val rowBottom = rowTop + span.second
                            val vp = latestViewportProvider?.invoke() ?: latestViewportBounds
                            val isPinned = stickyOffsetState.intValue > 0 || (vp != null && hdrH > 0 && bodyPos.y < vp.top)
                            val safeTopInset = if (isPinned && hdrH > 0) hdrH.toFloat() else 0f
                            Rect(bodyPos.x, rowTop, bodyPos.x + bodyCoords.size.width.toFloat(), rowBottom) to safeTopInset
                        }
                        reg(sheetResolverKey, resolver)
                    }
                    onDispose {
                        latestOnRegisterResolver?.invoke(sheetResolverKey, null)
                    }
                }

                fun findCellAtLocalPos(localPos: Offset, allowNearestFallback: Boolean = false): Pair<Int, Int>? {
                    val rowCount = parsedRows.size
                    if (rowCount <= 0 || maxCols <= 0) return null

                    var targetRow: Int? = null
                    for (r in 0 until rowCount) {
                        val span = rowLocalSpanMap[r] ?: continue
                        if (localPos.y >= span.first && localPos.y <= span.first + span.second) {
                            targetRow = r
                            break
                        }
                    }
                    if (targetRow == null) {
                        if (!allowNearestFallback || rowLocalSpanMap.isEmpty()) return null
                        var bestRow = 0
                        var bestDist = Float.MAX_VALUE
                        for ((r, span) in rowLocalSpanMap) {
                            val d = when {
                                localPos.y < span.first -> span.first - localPos.y
                                localPos.y > span.first + span.second -> localPos.y - (span.first + span.second)
                                else -> 0f
                            }
                            if (d < bestDist) {
                                bestDist = d
                                bestRow = r
                            }
                        }
                        targetRow = bestRow
                    }

                    val totalWPx = gridRootCoords?.takeIf { it.isAttached && it.size.width > 0 }?.size?.width?.toFloat()
                        ?: with(density) { dims.totalTableWidth.toPx() }
                    val rowNumWPx = with(density) { dims.rowNumWidth.toPx() }
                    val xFromStart = if (isRtl) totalWPx - localPos.x else localPos.x
                    if (xFromStart < rowNumWPx) {
                        return if (allowNearestFallback) targetRow to 0 else null
                    }
                    var cumDp = dims.rowNumWidth
                    for (c in 0 until maxCols) {
                        cumDp += dims.columnWidths.getOrElse(c) { 110.dp }
                        val rightEdgePx = with(density) { cumDp.toPx() }
                        if (xFromStart <= rightEdgePx) {
                            return targetRow to c
                        }
                    }
                    return if (allowNearestFallback) targetRow to (maxCols - 1) else null
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        // Single horizontalScroll wraps Header & Data Rows (unconstrained height)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(hScroll)
                        ) {
                            Layout(
                                modifier = Modifier
                                    .width(dims.totalTableWidth)
                                    .onGloballyPositioned { coords ->
                                        if (coords.isAttached) {
                                            sheetBodyCoordsRef[0] = coords
                                            val vpTop = (latestViewportProvider?.invoke() ?: latestViewportBounds)?.top
                                            val hdrH = headerHeightRef[0]
                                            val totalH = coords.size.height
                                            val newSticky = if (vpTop != null && hdrH > 0 && totalH > hdrH) {
                                                (vpTop - coords.positionInRoot().y)
                                                    .roundToInt()
                                                    .coerceIn(0, (totalH - hdrH).coerceAtLeast(0))
                                            } else {
                                                0
                                            }
                                            if (stickyOffsetState.intValue != newSticky) {
                                                stickyOffsetState.intValue = newSticky
                                            }
                                        }
                                    },
                                content = {
                                    // 1. Sticky Table Header Row (offset only moves header placement, zero row re-layout)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .zIndex(10f)
                                            .offset { IntOffset(0, stickyOffsetState.intValue) }
                                    ) {
                                    Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .defaultMinSize(minHeight = 38.dp)
                                        .height(IntrinsicSize.Min)
                                        .background(tableColors.colHeaderBg),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                // Corner cell (Icon only) + Sheet options menu
                                Box(
                                    modifier = Modifier
                                        .width(dims.rowNumWidth)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 38.dp)
                                        .then(
                                            if (onRowNumBounds != null) {
                                                Modifier.onGloballyPositioned(onRowNumBounds)
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .background(tableColors.colHeaderBg)
                                        .border(0.5.dp, tableColors.headerBorderColor)
                                        .clickable { sheetMenuExpanded = true }
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Menu,
                                        contentDescription = "خيارات الجدول",
                                        tint = tableColors.headerTextColor,
                                        modifier = Modifier.size(15.dp)
                                    )

                                    DropdownMenu(
                                        expanded = sheetMenuExpanded,
                                        onDismissRequest = { sheetMenuExpanded = false },
                                        modifier = Modifier
                                            .background(desktopColors.cardBackground)
                                            .border(0.5.dp, desktopColors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text("نسخ", fontSize = 13.sp, color = desktopColors.textPrimary)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContentCopy,
                                                    contentDescription = null,
                                                    tint = desktopColors.textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            },
                                            onClick = {
                                                sheetMenuExpanded = false
                                                val tsvText = parsedRows.joinToString("\n") { (_, cells, _) ->
                                                    cells.joinToString("\t")
                                                }
                                                onCopyText?.invoke(tsvText)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text("نسخ منسق", fontSize = 13.sp, color = desktopColors.textPrimary)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.CopyAll,
                                                    contentDescription = null,
                                                    tint = desktopColors.textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            },
                                            onClick = {
                                                sheetMenuExpanded = false
                                                val allText = if (verticalCopyFormat) {
                                                    parsedRows.joinToString("\n----------------------------------------\n") { (rNum, cells, _) ->
                                                        "صف $rNum:\n" + DesktopValueInteractionHelper.formatRow(cells, headers, vertical = true)
                                                    }
                                                } else {
                                                    parsedRows.joinToString("\n") { (rNum, cells, _) ->
                                                        "صف $rNum: " + DesktopValueInteractionHelper.formatRow(cells, headers, vertical = false)
                                                    }
                                                }
                                                onCopyText?.invoke(allText)
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = {
                                                Text("تحديد", fontSize = 13.sp, color = desktopColors.textPrimary)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.SelectAll,
                                                    contentDescription = null,
                                                    tint = desktopColors.textSecondary,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                            },
                                            onClick = {
                                                sheetMenuExpanded = false
                                                currentOnRequestFocus?.invoke()
                                                val allRows = (0 until sheetModel.rowCount).mapNotNull { sheetModel.buildRowData(it) }
                                                currentOnSelectionChange?.invoke(currentSelectionState.addRows(allRows))
                                            }
                                        )
                                    }
                                }

                                for (cIdx in 0 until maxCols) {
                                    val colW = dims.columnWidths.getOrElse(cIdx) { 110.dp }
                                    val colTitle = headers.getOrNull(cIdx)?.takeIf { it.isNotBlank() } ?: "عمود ${cIdx + 1}"
                                    val currentColW by rememberUpdatedState(colW)
                                    val handleInteraction = remember { MutableInteractionSource() }
                                    val isHoveringHandle by handleInteraction.collectIsHoveredAsState()
                                    var handleCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                                    Box(
                                        modifier = Modifier
                                            .width(colW)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 38.dp)
                                            .then(
                                                if (onHeaderCellBounds != null) {
                                                    Modifier.onGloballyPositioned { onHeaderCellBounds(cIdx, it) }
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .background(tableColors.colHeaderBg)
                                            .border(0.5.dp, tableColors.headerBorderColor)
                                    ) {
                                        val colSortOrder = sortFilterState.getColumnSortOrder(cIdx)
                                        val isColFiltered = sortFilterState.isColumnFiltered(cIdx)
                                        val isColActive = colSortOrder != ColumnSortOrder.NONE || isColFiltered

                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(start = 6.dp, end = 9.dp)
                                                .clickable { openFilterColIdx = cIdx }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = colTitle,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    lineHeight = 17.sp,
                                                    color = if (isColActive) accentColor else tableColors.headerTextColor,
                                                    maxLines = 2,
                                                    softWrap = true,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )

                                                if (isColActive) {
                                                    Icon(
                                                        imageVector = when {
                                                            isColFiltered -> Icons.Default.FilterList
                                                            colSortOrder == ColumnSortOrder.ASCENDING -> Icons.Outlined.ArrowUpward
                                                            else -> Icons.Outlined.ArrowDownward
                                                        },
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                            }

                                            if (openFilterColIdx == cIdx) {
                                                ColumnSortFilterDropdownMenu(
                                                    colIndex = cIdx,
                                                    colTitle = colTitle,
                                                    rawResults = rawSortedResults,
                                                    sortFilterState = sortFilterState,
                                                    accentColor = accentColor,
                                                    onApplyState = { newState ->
                                                        sortFilterState = newState
                                                        openFilterColIdx = null
                                                        onSelectionStateChange?.invoke(selectionState.clear())
                                                    },
                                                    onDismiss = { openFilterColIdx = null }
                                                )
                                            }
                                        }

                                        // Trailing column-border resize handle
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .fillMaxHeight()
                                                .width(8.dp)
                                                .onGloballyPositioned { handleCoords = it }
                                                .hoverable(handleInteraction)
                                                .pointerHoverIcon(
                                                    PointerIcon(Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
                                                )
                                                .pointerInput(cIdx) {
                                                    detectTapGestures(
                                                        onDoubleTap = {
                                                            customColumnWidths = customColumnWidths - cIdx
                                                        }
                                                    )
                                                }
                                                .pointerInput(cIdx, isRtl) {
                                                    var dragStartRootX = 0f
                                                    var dragStartWidth = currentColW
                                                    detectHorizontalDragGestures(
                                                        onDragStart = { downOffset ->
                                                            dragStartWidth = currentColW
                                                            val coords = handleCoords
                                                            dragStartRootX = if (coords != null && coords.isAttached) {
                                                                coords.localToRoot(downOffset).x
                                                            } else {
                                                                downOffset.x
                                                            }
                                                        },
                                                        onHorizontalDrag = { change, dragAmount ->
                                                            change.consume()
                                                            val coords = handleCoords
                                                            val newWidth = if (coords != null && coords.isAttached) {
                                                                val currentRootX = coords.localToRoot(change.position).x
                                                                val deltaPx = currentRootX - dragStartRootX
                                                                val deltaDp = with(density) { deltaPx.toDp() }
                                                                DesktopTableLayoutCalculator.applyColumnResizeDelta(
                                                                    currentWidth = dragStartWidth,
                                                                    dragDeltaDp = deltaDp,
                                                                    isRtl = isRtl
                                                                )
                                                            } else {
                                                                val deltaDp = with(density) { dragAmount.toDp() }
                                                                val baseW = customColumnWidths[cIdx] ?: currentColW
                                                                DesktopTableLayoutCalculator.applyColumnResizeDelta(
                                                                    currentWidth = baseW,
                                                                    dragDeltaDp = deltaDp,
                                                                    isRtl = isRtl
                                                                )
                                                            }
                                                            customColumnWidths = customColumnWidths + (cIdx to newWidth)
                                                        }
                                                    )
                                                },
                                            contentAlignment = Alignment.CenterEnd
                                        ) {
                                            if (isHoveringHandle || customColumnWidths.containsKey(cIdx)) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxHeight()
                                                        .width(2.dp)
                                                        .background(
                                                            if (isHoveringHandle) {
                                                                accentColor.copy(alpha = 0.75f)
                                                            } else {
                                                                tableColors.headerBorderColor
                                                            }
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
                                }

                                HorizontalDivider(
                                    color = tableColors.headerBorderColor,
                                    thickness = 0.5.dp
                                )
                                }

                                // 2. Unconstrained-Height Data Rows
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onGloballyPositioned { gridRootCoords = it }
                                            .pointerInput(sheetResetKey) {
                                        var lastPressTimeMs = 0L
                                        var lastPressedCell: CellKey? = null
                                        var dragAnchorCoord: Pair<Int, Int>? = null
                                        var pressStartPos = Offset.Zero
                                        var didDragAcrossCells = false
                                        var dragAdditive = false
                                        var dragBaseCells: Map<CellKey, SelectedCellData> = emptyMap()

                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val change = event.changes.firstOrNull() ?: continue
                                                val localPos = change.position

                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        if (change.isConsumed) continue
                                                        val targetCoord = findCellAtLocalPos(localPos, allowNearestFallback = false) ?: continue
                                                        val (rIdx, cIdx) = targetCoord
                                                        val model = currentSheetModel
                                                        val cellData = model.buildCellData(rIdx, cIdx) ?: continue
                                                        val rowData = model.buildRowData(rIdx) ?: continue
                                                        val res = model.results.getOrNull(rIdx) ?: continue

                                                        currentOnRequestFocus?.invoke()

                                                        if (event.button == PointerButton.Secondary) {
                                                            val sel = currentSelectionState
                                                            if (!sel.isCellSelected(cellData.key)) {
                                                                currentOnSelectionChange?.invoke(sel.selectSingleCell(cellData))
                                                            }
                                                            cellContextMenu = CellContextMenuTarget(
                                                                rowIndex = rIdx,
                                                                colIndex = cIdx,
                                                                cellData = cellData,
                                                                rowData = rowData,
                                                                result = res,
                                                                localClickPos = localPos
                                                            )
                                                            dragAnchorCoord = null
                                                            change.consume()
                                                        } else if (event.button == PointerButton.Primary) {
                                                            val now = change.uptimeMillis
                                                            val isCtrl = event.keyboardModifiers.isCtrlPressed ||
                                                                event.keyboardModifiers.isMetaPressed
                                                            val isDoubleClick = (now - lastPressTimeMs) in 1L..349L &&
                                                                lastPressedCell == cellData.key &&
                                                                !isCtrl

                                                            lastPressTimeMs = now
                                                            lastPressedCell = cellData.key

                                                            if (isDoubleClick) {
                                                                dragAnchorCoord = null
                                                                if (currentOnViewCellDetail != null) {
                                                                    currentOnViewCellDetail?.invoke(cellData)
                                                                } else {
                                                                    currentOnViewDetail(res)
                                                                }
                                                                change.consume()
                                                                continue
                                                            }

                                                            val sel = currentSelectionState
                                                            val preCells = if (sel.mode == SelectionMode.CELLS) sel.selectedCells else emptyMap()
                                                            if (isCtrl) {
                                                                currentOnSelectionChange?.invoke(sel.toggleCell(cellData))
                                                            } else {
                                                                currentOnSelectionChange?.invoke(sel.selectSingleCell(cellData))
                                                            }
                                                            dragAnchorCoord = targetCoord
                                                            pressStartPos = localPos
                                                            didDragAcrossCells = false
                                                            dragAdditive = isCtrl
                                                            dragBaseCells = preCells
                                                            change.consume()
                                                        }
                                                    }

                                                    PointerEventType.Move -> {
                                                        val anchorCoord = dragAnchorCoord ?: continue
                                                        if (!event.buttons.isPrimaryPressed) {
                                                            dragAnchorCoord = null
                                                            didDragAcrossCells = false
                                                            continue
                                                        }
                                                        val dist = abs(localPos.x - pressStartPos.x) + abs(localPos.y - pressStartPos.y)
                                                        if (!didDragAcrossCells && dist < 5f) continue

                                                        val hoverCoord = findCellAtLocalPos(localPos, allowNearestFallback = true) ?: continue
                                                        if (hoverCoord != anchorCoord || didDragAcrossCells) {
                                                            didDragAcrossCells = true
                                                            val model = currentSheetModel
                                                            val anchorKey = CellKey(model.fileUri, model.sheetName, anchorCoord.first, anchorCoord.second)
                                                            val targetKey = CellKey(model.fileUri, model.sheetName, hoverCoord.first, hoverCoord.second)
                                                            val range = model.buildCellRange(anchorKey, targetKey)
                                                            currentOnSelectionChange?.invoke(
                                                                currentSelectionState.selectDraggedCellRange(
                                                                    rangeCells = range,
                                                                    focusKey = targetKey,
                                                                    baseCells = dragBaseCells,
                                                                    additive = dragAdditive
                                                                )
                                                            )
                                                            change.consume()
                                                        }
                                                    }

                                                    PointerEventType.Release -> {
                                                        dragAnchorCoord = null
                                                        didDragAcrossCells = false
                                                    }
                                                }
                                            }
                                        }
                                    }
                            ) {
                                if (sortedResults.isEmpty() && sortFilterState.hasActiveConstraints()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(tableColors.rowAltBg)
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "لا توجد صفوف مطابقة لعامل التصفية الحالي.",
                                            fontSize = 12.5.sp,
                                            color = desktopColors.textSecondary
                                        )
                                        Text(
                                            text = "إلغاء عامل التصفية",
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = accentColor,
                                            modifier = Modifier.clickable {
                                                sortFilterState = TableSortFilterState()
                                                onSelectionStateChange?.invoke(selectionState.clear())
                                            }
                                        )
                                    }
                                }

                                val activeRowForSheet = if (selectionState.mode == SelectionMode.CELLS &&
                                    selectionState.activeCell?.fileUri == fileUri &&
                                    selectionState.activeCell?.sheetName == sheetName
                                ) {
                                    selectionState.activeCell
                                } else {
                                    null
                                }
                                val selectedColsByRow = remember(selectionState, fileUri, sheetName) {
                                    if (selectionState.mode != SelectionMode.CELLS || selectionState.selectedCells.isEmpty()) {
                                        emptyMap()
                                    } else {
                                        val map = HashMap<Int, Set<Int>>()
                                        for (cellKey in selectionState.selectedCells.keys) {
                                            if (cellKey.fileUri == fileUri && cellKey.sheetName == sheetName) {
                                                val prev = map[cellKey.rowIndex]
                                                map[cellKey.rowIndex] = if (prev == null) setOf(cellKey.colIndex) else prev + cellKey.colIndex
                                            }
                                        }
                                        map
                                    }
                                }
                                val selectedRowSet = remember(selectionState, fileUri, sheetName) {
                                    if (selectionState.mode != SelectionMode.ROWS || selectionState.selectedRows.isEmpty()) {
                                        EMPTY_INT_SET
                                    } else {
                                        val set = HashSet<Int>()
                                        for (rk in selectionState.selectedRows.keys) {
                                            if (rk.fileUri == fileUri && rk.sheetName == sheetName) {
                                                set.add(rk.rowIndex)
                                            }
                                        }
                                        set
                                    }
                                }
                                val rowSelectionReader = remember(sheetResetKey) { { currentSelectionState } }
                                val rowSheetModelReader = remember(sheetResetKey) { { currentSheetModel } }
                                val rowOnSelectionChange = remember(sheetResetKey) { { st: SelectionState -> currentOnSelectionChange?.invoke(st); Unit } }
                                val rowOnRequestFocus = remember(sheetResetKey) { { currentOnRequestFocus?.invoke(); Unit } }
                                val rowOnViewDetail = remember(sheetResetKey) { { r: SearchResult -> currentOnViewDetail(r) } }
                                val rowOnShowMenu = remember(sheetResetKey) { { idx: Int, pos: Offset -> rowContextMenuTarget = idx to pos } }
                                val rowOnDismissMenu = remember(sheetResetKey) { { rowContextMenuTarget = null } }
                                val rowOnPlaced = remember(rowLocalSpanMap) { { idx: Int, y: Float, h: Float -> rowLocalSpanMap[idx] = y to h } }
                                val rowOnCopyText = remember(sheetResetKey) { { txt: String -> currentOnCopyText?.invoke(txt); Unit } }
                                val rowOnExportSelection = remember(sheetResetKey) { { currentOnExportSelection?.invoke(); Unit } }

                                parsedRows.forEachIndexed { rIdx, (rowNum, cells, res) ->
                                    val selectedCols = selectedColsByRow[rIdx] ?: EMPTY_INT_SET
                                    val activeColInRow = if (activeRowForSheet?.rowIndex == rIdx) activeRowForSheet.colIndex else null
                                    val isRowSelected = rIdx in selectedRowSet
                                    val rowMenuOffset = rowContextMenuTarget?.takeIf { it.first == rIdx }?.second

                                    ExcelTableDataRow(
                                        rIdx = rIdx,
                                        sheetResetKey = sheetResetKey,
                                        fileUri = fileUri,
                                        sheetName = sheetName,
                                        rowNum = rowNum,
                                        cells = cells,
                                        res = res,
                                        headers = headers,
                                        query = query,
                                        searchFilter = searchFilter,
                                        maxCols = maxCols,
                                        expandFullCellText = expandFullCellText,
                                        verticalCopyFormat = verticalCopyFormat,
                                        isRowSelected = isRowSelected,
                                        activeColIndex = activeColInRow,
                                        selectedCols = selectedCols,
                                        rowMenuOffset = rowMenuOffset,
                                        dims = dims,
                                        tableColors = tableColors,
                                        desktopColors = desktopColors,
                                        onRowPlaced = rowOnPlaced,
                                        selectionStateReader = rowSelectionReader,
                                        sheetModelReader = rowSheetModelReader,
                                        onSelectionChange = rowOnSelectionChange,
                                        onRequestFocus = rowOnRequestFocus,
                                        onViewDetail = rowOnViewDetail,
                                        onShowRowMenu = rowOnShowMenu,
                                        onDismissRowMenu = rowOnDismissMenu,
                                        onCopyText = if (onCopyText != null) rowOnCopyText else null,
                                        onExportSelection = if (onExportSelection != null) rowOnExportSelection else null,
                                        onDataCellBounds = onDataCellBounds
                                    )
                                }
                            }

                                    val activeContext = cellContextMenu
                                    if (activeContext != null) {
                                        Box(
                                            modifier = Modifier
                                                .align(AbsoluteAlignment.TopLeft)
                                                .absoluteOffset {
                                                    IntOffset(
                                                        activeContext.localClickPos.x.roundToInt(),
                                                        activeContext.localClickPos.y.roundToInt()
                                                    )
                                                }
                                                .size(0.dp)
                                        ) {
                                            DropdownMenu(
                                                expanded = true,
                                                onDismissRequest = { cellContextMenu = null },
                                                modifier = Modifier
                                                    .then(
                                                        if (onCellContextMenuBounds != null) {
                                                            Modifier.onGloballyPositioned(onCellContextMenuBounds)
                                                        } else {
                                                            Modifier
                                                        }
                                                    )
                                                    .background(desktopColors.cardBackground)
                                                    .border(0.5.dp, desktopColors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                                            ) {
                                                val isCurrentCellSelected = selectionState.isActive &&
                                                    selectionState.mode == SelectionMode.CELLS &&
                                                    selectionState.isCellSelected(activeContext.cellData.key)
                                                DropdownMenuItem(
                                                    text = { Text("نسخ", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                    onClick = {
                                                        cellContextMenu = null
                                                        val txt = if (isCurrentCellSelected) {
                                                            DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                                                selectionState = selectionState,
                                                                verticalCopyFormat = verticalCopyFormat,
                                                                preferFormattedLabels = false
                                                            )
                                                        } else {
                                                            activeContext.cellData.value
                                                        }
                                                        onCopyText?.invoke(txt)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("نسخ منسق", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                    leadingIcon = { Icon(Icons.Outlined.CopyAll, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                    onClick = {
                                                        cellContextMenu = null
                                                        val txt = if (isCurrentCellSelected) {
                                                            DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                                                selectionState = selectionState,
                                                                verticalCopyFormat = verticalCopyFormat,
                                                                preferFormattedLabels = true
                                                            )
                                                        } else {
                                                            DesktopValueInteractionHelper.formatMultipleCells(
                                                                listOf(activeContext.cellData),
                                                                verticalCopyFormat
                                                            )
                                                        }
                                                        onCopyText?.invoke(txt)
                                                    }
                                                )
                                                if (selectionState.shouldShowPreviewMenuItem()) {
                                                    DropdownMenuItem(
                                                        text = { Text("معاينة", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                        leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                        onClick = {
                                                            cellContextMenu = null
                                                            if (onViewCellDetail != null) {
                                                                onViewCellDetail(activeContext.cellData)
                                                            } else {
                                                                onViewDetail(activeContext.result)
                                                            }
                                                        }
                                                    )
                                                }
                                                if (onExportSelection != null && selectionState.isActive) {
                                                    DropdownMenuItem(
                                                        text = { Text("تصدير", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                        onClick = {
                                                            cellContextMenu = null
                                                            onExportSelection()
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                }
                            ) { measurables, constraints ->
                                val headerPlaceable = measurables[0].measure(constraints.copy(minHeight = 0))
                                val rowsPlaceable = measurables[1].measure(constraints.copy(minHeight = 0))
                                headerHeightRef[0] = headerPlaceable.height
                                val totalW = maxOf(headerPlaceable.width, rowsPlaceable.width)
                                val totalH = headerPlaceable.height + rowsPlaceable.height
                                layout(totalW, totalH) {
                                    rowsPlaceable.placeRelative(0, headerPlaceable.height)
                                    headerPlaceable.placeRelative(0, 0, zIndex = 10f)
                                }
                            }
                        }
                    }

                    if (hScroll.maxValue in 1 until Int.MAX_VALUE) {
                        HorizontalScrollbar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 2.dp),
                            adapter = rememberScrollbarAdapter(hScroll)
                        )
                    }
                }
            }

            HorizontalDivider(color = tableColors.gridBorderColor, thickness = 1.dp)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColumnSortFilterDropdownMenu(
    colIndex: Int,
    colTitle: String,
    rawResults: List<SearchResult>,
    sortFilterState: TableSortFilterState,
    accentColor: Color,
    onApplyState: (TableSortFilterState) -> Unit,
    onDismiss: () -> Unit
) {
    val desktopColors = LocalDesktopColors.current
    val currentSortOrder = sortFilterState.getColumnSortOrder(colIndex)
    val isColFiltered = sortFilterState.isColumnFiltered(colIndex)
    val canClearColumn = currentSortOrder != ColumnSortOrder.NONE || isColFiltered
    val currentTextFilter = sortFilterState.textFilters[colIndex]

    val uniqueValues = remember(rawResults, colIndex, sortFilterState.activeFilters, sortFilterState.textFilters) {
        DesktopTableSortFilterHelper.extractUniqueColumnValues(
            items = rawResults,
            colIndex = colIndex,
            state = sortFilterState
        ) { res -> DesktopTableCellHelper.splitIntoCells(res.snippet) }
    }
    val allUniqueSet = remember(uniqueValues) { uniqueValues.toSet() }

    var searchText by remember(colIndex, uniqueValues) { mutableStateOf("") }
    var draftSelected by remember(colIndex, uniqueValues, sortFilterState.activeFilters) {
        mutableStateOf(
            sortFilterState.activeFilters[colIndex]?.intersect(allUniqueSet) ?: allUniqueSet
        )
    }
    var showTextConditionEditor by remember(colIndex) { mutableStateOf(currentTextFilter != null) }
    var selectedTextOp by remember(colIndex, currentTextFilter) {
        mutableStateOf(currentTextFilter?.operator ?: TextFilterOperator.CONTAINS)
    }
    var textConditionQuery by remember(colIndex, currentTextFilter) {
        mutableStateOf(currentTextFilter?.query.orEmpty())
    }

    val visibleValues = remember(uniqueValues, searchText) {
        val q = searchText.trim()
        if (q.isEmpty()) {
            uniqueValues
        } else {
            val normQ = ArabicNormalizer.normalize(q)
            uniqueValues.filter { valKey ->
                val label = DesktopTableSortFilterHelper.formatValueLabel(valKey)
                label.contains(q, ignoreCase = true) ||
                    ArabicNormalizer.normalize(label).contains(normQ)
            }
        }
    }
    val visibleSet = remember(visibleValues) { visibleValues.toSet() }
    val allVisibleSelected = visibleValues.isNotEmpty() && visibleValues.all { it in draftSelected }

    val lazyListState = rememberLazyListState()
    val canScrollChecklist by remember {
        derivedStateOf { lazyListState.canScrollForward || lazyListState.canScrollBackward }
    }
    // Explicit bounded height (avoids DropdownMenu Constraints.Infinity -> Int.MAX_VALUE on VerticalScrollbar)
    val checklistHeight = remember(visibleValues.size) {
        val visibleRows = (visibleValues.size + 1).coerceAtLeast(3)
        (visibleRows * 28 + 12).coerceIn(120, 196).dp
    }

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .width(276.dp)
            .background(desktopColors.cardBackground)
            .border(1.dp, desktopColors.divider, RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            // Sort Ascending ("فرز تصاعدي")
            ExcelPopupMenuRow(
                label = "فرز تصاعدي",
                selected = currentSortOrder == ColumnSortOrder.ASCENDING,
                accentColor = accentColor,
                leadingIcon = {
                    ExcelSortAzGlyph(descending = false, primaryColor = accentColor, secondaryColor = desktopColors.textSecondary)
                },
                onClick = {
                    onApplyState(sortFilterState.withSort(colIndex, ColumnSortOrder.ASCENDING))
                }
            )

            // Sort Descending ("فرز تنازلي")
            ExcelPopupMenuRow(
                label = "فرز تنازلي",
                selected = currentSortOrder == ColumnSortOrder.DESCENDING,
                accentColor = accentColor,
                leadingIcon = {
                    ExcelSortAzGlyph(descending = true, primaryColor = accentColor, secondaryColor = desktopColors.textSecondary)
                },
                onClick = {
                    onApplyState(sortFilterState.withSort(colIndex, ColumnSortOrder.DESCENDING))
                }
            )

            HorizontalDivider(color = desktopColors.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 3.dp))

            // Clear column filter/sort
            ExcelPopupMenuRow(
                label = "إلغاء عامل التصفية من \"$colTitle\"",
                selected = false,
                enabled = canClearColumn,
                accentColor = accentColor,
                leadingIcon = {
                    ExcelClearFilterGlyph(
                        tint = if (canClearColumn) desktopColors.textPrimary else desktopColors.textSecondary.copy(alpha = 0.45f)
                    )
                },
                onClick = {
                    onApplyState(sortFilterState.clearColumn(colIndex))
                }
            )

            // Working Text Condition Filters ("عوامل تصفية النصوص")
            ExcelPopupMenuRow(
                label = "عوامل تصفية النصوص",
                selected = currentTextFilter != null,
                accentColor = accentColor,
                trailingContent = {
                    Icon(
                        imageVector = if (showTextConditionEditor) {
                            Icons.Outlined.KeyboardArrowDown
                        } else {
                            Icons.AutoMirrored.Outlined.KeyboardArrowLeft
                        },
                        contentDescription = null,
                        tint = if (currentTextFilter != null) accentColor else desktopColors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                },
                onClick = { showTextConditionEditor = !showTextConditionEditor }
            )

            if (showTextConditionEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(desktopColors.subCardBackground)
                        .border(0.5.dp, desktopColors.divider, RoundedCornerShape(4.dp))
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TextFilterOperator.entries.forEach { op ->
                            val isSelectedOp = selectedTextOp == op
                            Text(
                                text = op.label,
                                fontSize = 11.sp,
                                fontWeight = if (isSelectedOp) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelectedOp) desktopColors.controlActiveContent else desktopColors.textPrimary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelectedOp) accentColor else desktopColors.cardBackground)
                                    .clickable { selectedTextOp = op }
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }
                    FluentTextBox(
                        value = textConditionQuery,
                        onValueChange = { textConditionQuery = it },
                        placeholder = "قيمة التصفية...",
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = TextStyle(color = desktopColors.textPrimary, fontSize = 12.sp)
                    )
                    if (currentTextFilter != null) {
                        Text(
                            text = "مسح شرط النص",
                            fontSize = 11.sp,
                            color = desktopColors.dangerText,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable {
                                    textConditionQuery = ""
                                    showTextConditionEditor = false
                                    onApplyState(sortFilterState.withTextFilter(colIndex, null))
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Instant Search box inside unique values
            FluentTextBox(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = "بحث",
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(color = desktopColors.textPrimary, fontSize = 12.5.sp),
                trailingContent = {
                    if (searchText.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "مسح",
                            tint = desktopColors.textSecondary,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable { searchText = "" }
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = desktopColors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            )

            // Checklist box with explicit bounded width & height + LazyColumn scrollbar
            // (explicit width/height avoids DropdownMenu IntrinsicSize.Max crashing SubcomposeLayout or VerticalScrollbar)
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .width(260.dp)
                    .height(checklistHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .border(0.8.dp, desktopColors.divider, RoundedCornerShape(3.dp))
                    .background(desktopColors.cardBackground)
            ) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .width(260.dp)
                        .height(checklistHeight)
                        .padding(start = 4.dp, end = 12.dp, top = 4.dp, bottom = 4.dp)
                ) {
                    item(key = "__select_all__") {
                        FilterValueCheckRow(
                            label = "(تحديد الكل)",
                            checked = allVisibleSelected,
                            accentColor = accentColor,
                            isBold = true,
                            onToggle = {
                                draftSelected = if (allVisibleSelected) {
                                    draftSelected - visibleSet
                                } else {
                                    draftSelected + visibleSet
                                }
                            }
                        )
                    }

                    if (visibleValues.isEmpty()) {
                        item(key = "__empty__") {
                            Text(
                                text = "لا توجد قيم مطابقة",
                                fontSize = 12.sp,
                                color = desktopColors.textSecondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                            )
                        }
                    } else {
                        items(visibleValues, key = { it }) { valKey ->
                            val isChecked = valKey in draftSelected
                            FilterValueCheckRow(
                                label = DesktopTableSortFilterHelper.formatValueLabel(valKey),
                                checked = isChecked,
                                accentColor = accentColor,
                                isBold = false,
                                onToggle = {
                                    draftSelected = if (isChecked) {
                                        draftSelected - valKey
                                    } else {
                                        draftSelected + valKey
                                    }
                                }
                            )
                        }
                    }
                }

                if (canScrollChecklist) {
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .height(checklistHeight - 4.dp)
                            .padding(2.dp),
                        adapter = rememberScrollbarAdapter(lazyListState)
                    )
                }
            }

            // Compact bottom trailing OK / Cancel buttons (matching screenshot)
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(desktopColors.controlActiveBackground)
                        .border(0.8.dp, desktopColors.controlActiveBackground, RoundedCornerShape(3.dp))
                        .clickable {
                            val effectiveSelected = if (searchText.isNotBlank()) {
                                draftSelected.intersect(visibleSet)
                            } else {
                                draftSelected
                            }
                            var nextState = sortFilterState.withColumnFilter(
                                colIndex = colIndex,
                                selectedValues = effectiveSelected,
                                allUniqueValues = allUniqueSet
                            )
                            val cleanTextQ = textConditionQuery.trim()
                            nextState = if (showTextConditionEditor && cleanTextQ.isNotEmpty()) {
                                nextState.withTextFilter(colIndex, ColumnTextFilter(selectedTextOp, cleanTextQ))
                            } else {
                                nextState.withTextFilter(colIndex, null)
                            }
                            onApplyState(nextState)
                        }
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("موافق", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = desktopColors.controlActiveContent)
                }

                Box(
                    modifier = Modifier
                        .width(82.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(desktopColors.subCardBackground)
                        .border(0.8.dp, desktopColors.divider, RoundedCornerShape(3.dp))
                        .clickable(onClick = onDismiss)
                        .padding(vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("إلغاء الأمر", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = desktopColors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun ExcelPopupMenuRow(
    label: String,
    selected: Boolean,
    accentColor: Color,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                when {
                    selected -> accentColor.copy(alpha = 0.12f)
                    isHovered && enabled -> colors.subCardBackground
                    else -> Color.Transparent
                }
            )
            .hoverable(interactionSource = interactionSource, enabled = enabled)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            leadingIcon?.invoke()
        }
        Text(
            text = label,
            fontSize = 12.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailingContent != null) {
            trailingContent()
        } else if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

/**
 * Borderless Excel-style A-Z / Z-A sort icon glyph matching standard Windows Excel AutoFilter menu.
 */
@Composable
private fun ExcelSortAzGlyph(
    descending: Boolean,
    primaryColor: Color,
    secondaryColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (descending) "Z" else "A",
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (descending) secondaryColor else primaryColor
            )
            Text(
                text = if (descending) "A" else "Z",
                fontSize = 8.sp,
                lineHeight = 8.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (descending) primaryColor else secondaryColor
            )
        }
        Canvas(modifier = Modifier.size(width = 7.dp, height = 14.dp)) {
            val midX = size.width * 0.5f
            drawLine(
                color = secondaryColor,
                start = Offset(midX, 1f),
                end = Offset(midX, size.height - 2f),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round
            )
            val arrowHead = Path().apply {
                moveTo(1f, size.height - 5f)
                lineTo(midX, size.height - 1f)
                lineTo(size.width - 1f, size.height - 5f)
            }
            drawPath(arrowHead, color = secondaryColor, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
        }
    }
}

/**
 * Borderless funnel + cross glyph for clearing column filters.
 */
@Composable
private fun ExcelClearFilterGlyph(tint: Color) {
    Canvas(modifier = Modifier.size(16.dp)) {
        val w = size.width
        val h = size.height
        val funnel = Path().apply {
            moveTo(2f, 2.5f)
            lineTo(w - 2f, 2.5f)
            lineTo(w * 0.60f, h * 0.52f)
            lineTo(w * 0.60f, h * 0.85f)
            lineTo(w * 0.40f, h * 0.78f)
            lineTo(w * 0.40f, h * 0.52f)
            close()
        }
        drawPath(funnel, color = tint, style = Stroke(width = 1.35f, cap = StrokeCap.Round))
        // Small X on corner
        drawLine(tint, Offset(w * 0.65f, h * 0.62f), Offset(w * 0.92f, h * 0.90f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.92f, h * 0.62f), Offset(w * 0.65f, h * 0.90f), strokeWidth = 1.5f, cap = StrokeCap.Round)
    }
}

@Composable
private fun FilterValueCheckRow(
    label: String,
    checked: Boolean,
    accentColor: Color,
    isBold: Boolean,
    onToggle: () -> Unit
) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = accentColor,
                checkmarkColor = colors.controlActiveContent,
                uncheckedColor = colors.textSecondary
            ),
            modifier = Modifier.size(19.dp)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ExcelTableDataRow(
    rIdx: Int,
    sheetResetKey: Any,
    fileUri: String,
    sheetName: String,
    rowNum: String,
    cells: List<String>,
    res: SearchResult,
    headers: List<String>,
    query: String,
    searchFilter: SearchFilter,
    maxCols: Int,
    expandFullCellText: Boolean,
    verticalCopyFormat: Boolean,
    isRowSelected: Boolean,
    activeColIndex: Int?,
    selectedCols: Set<Int>,
    rowMenuOffset: Offset?,
    dims: DesktopTableDimensions,
    tableColors: DesktopExcelTableThemeColors,
    desktopColors: DesktopColors,
    onRowPlaced: (Int, Float, Float) -> Unit,
    selectionStateReader: () -> SelectionState,
    sheetModelReader: () -> SheetGridModel,
    onSelectionChange: (SelectionState) -> Unit,
    onRequestFocus: () -> Unit,
    onViewDetail: (SearchResult) -> Unit,
    onShowRowMenu: (Int, Offset) -> Unit,
    onDismissRowMenu: () -> Unit,
    onCopyText: ((String) -> Unit)?,
    onExportSelection: (() -> Unit)?,
    onDataCellBounds: ((Int, Int, LayoutCoordinates) -> Unit)?
) {
    val isEven = rIdx % 2 == 0
    val rowBg = if (isRowSelected) {
        tableColors.selectedRowBg
    } else if (isEven) {
        tableColors.tableBg
    } else {
        tableColors.rowAltBg
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .onPlaced { coords ->
                onRowPlaced(rIdx, coords.positionInParent().y, coords.size.height.toFloat())
            }
            .background(rowBg)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val rowNumBg = if (isRowSelected) desktopColors.controlActiveBackground else tableColors.rowNumBg
        val rowNumFg = if (isRowSelected) desktopColors.controlActiveContent else tableColors.headerTextColor

        Box(
            modifier = Modifier
                .width(dims.rowNumWidth)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 42.dp)
                .background(rowNumBg)
                .border(0.5.dp, tableColors.headerBorderColor)
                .pointerInput(sheetResetKey, rIdx) {
                    var lastRowPressMs = 0L
                    awaitPointerEventScope {
                        while (true) {
                            val ev = awaitPointerEvent()
                            if (ev.type != PointerEventType.Press) continue
                            val ch = ev.changes.firstOrNull() ?: continue
                            val model = sheetModelReader()
                            val rowData = model.buildRowData(rIdx) ?: continue
                            onRequestFocus()

                            if (ev.button == PointerButton.Secondary) {
                                val sel = selectionStateReader()
                                if (!sel.isRowSelected(rowData.key)) {
                                    onSelectionChange(sel.selectSingleRow(rowData))
                                }
                                onShowRowMenu(rIdx, ch.position)
                                ch.consume()
                            } else if (ev.button == PointerButton.Primary) {
                                val now = ch.uptimeMillis
                                val isCtrl = ev.keyboardModifiers.isCtrlPressed || ev.keyboardModifiers.isMetaPressed
                                val isDouble = (now - lastRowPressMs) in 1L..349L && !isCtrl
                                lastRowPressMs = now
                                if (isDouble) {
                                    onViewDetail(res)
                                    ch.consume()
                                    continue
                                }
                                val sel = selectionStateReader()
                                if (isCtrl) {
                                    onSelectionChange(sel.toggleRow(rowData))
                                } else {
                                    onSelectionChange(sel.selectSingleRow(rowData))
                                }
                                ch.consume()
                            }
                        }
                    }
                }
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rowNum,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = rowNumFg,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )

            if (rowMenuOffset != null) {
                val liveSel = selectionStateReader()
                val rowKey = RowKey(fileUri = fileUri, sheetName = sheetName, rowIndex = rIdx)
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset {
                            IntOffset(rowMenuOffset.x.roundToInt(), rowMenuOffset.y.roundToInt())
                        }
                        .size(0.dp)
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = onDismissRowMenu,
                        modifier = Modifier
                            .background(desktopColors.cardBackground)
                            .border(0.5.dp, desktopColors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                    ) {
                        val formattedRow = DesktopValueInteractionHelper.formatRow(cells, headers, verticalCopyFormat)
                        val isCurrentRowSelected = liveSel.isActive &&
                            liveSel.mode == SelectionMode.ROWS &&
                            liveSel.isRowSelected(rowKey)
                        DropdownMenuItem(
                            text = { Text("نسخ", fontSize = 13.sp, color = desktopColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                onDismissRowMenu()
                                val copyTxt = if (isCurrentRowSelected) {
                                    DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                        selectionState = liveSel,
                                        verticalCopyFormat = verticalCopyFormat,
                                        preferFormattedLabels = false
                                    )
                                } else {
                                    cells.joinToString("\t")
                                }
                                onCopyText?.invoke(copyTxt)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نسخ منسق", fontSize = 13.sp, color = desktopColors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.CopyAll, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                onDismissRowMenu()
                                val copyTxt = if (isCurrentRowSelected) {
                                    DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                        selectionState = liveSel,
                                        verticalCopyFormat = verticalCopyFormat,
                                        preferFormattedLabels = true
                                    )
                                } else {
                                    formattedRow
                                }
                                onCopyText?.invoke(copyTxt)
                            }
                        )
                        if (liveSel.shouldShowPreviewMenuItem()) {
                            DropdownMenuItem(
                                text = { Text("معاينة", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    onDismissRowMenu()
                                    onViewDetail(res)
                                }
                            )
                        }
                        if (onExportSelection != null && liveSel.isActive) {
                            DropdownMenuItem(
                                text = { Text("تصدير", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    onDismissRowMenu()
                                    onExportSelection()
                                }
                            )
                        }
                    }
                }
            }
        }

        for (cIdx in 0 until maxCols) {
            val cellVal = cells.getOrNull(cIdx) ?: ""
            val colW = dims.columnWidths.getOrElse(cIdx) { 110.dp }
            val isCellSelected = cIdx in selectedCols
            val isCellActive = activeColIndex == cIdx
            val isMatch = remember(cellVal, query) {
                DesktopTableCellHelper.isCellMatching(cellVal, query)
            }
            val cellBg = when {
                isCellSelected -> tableColors.selectedCellBg
                isMatch -> tableColors.matchBg
                else -> Color.Transparent
            }
            val cellBorderModifier = when {
                isCellActive -> Modifier.border(2.dp, tableColors.activeCellBorder)
                isCellSelected -> Modifier.border(1.dp, tableColors.selectedCellBorder)
                else -> Modifier.border(0.5.dp, tableColors.gridBorderColor)
            }
            val isLong = cellVal.length > 22 || maxCols == 1
            val contentAlignment = if (isLong) Alignment.CenterStart else Alignment.Center
            val textAlign = if (isLong) TextAlign.Start else TextAlign.Center

            Box(
                modifier = Modifier
                    .width(colW)
                    .fillMaxHeight()
                    .defaultMinSize(minHeight = 42.dp)
                    .then(
                        if (onDataCellBounds != null) {
                            Modifier.onGloballyPositioned { coords ->
                                onDataCellBounds(rIdx, cIdx, coords)
                            }
                        } else {
                            Modifier
                        }
                    )
                    .background(cellBg)
                    .then(cellBorderModifier)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = contentAlignment
            ) {
                if (cellVal.isNotEmpty()) {
                    val highlighted = remember(cellVal, query, searchFilter, tableColors) {
                        DesktopTextHighlighter.highlight(
                            text = cellVal,
                            query = query,
                            method = searchFilter.method,
                            ignoreDiacritics = searchFilter.ignoreDiacritics,
                            highlightColor = desktopColors.highlightBg,
                            highlightTextColor = desktopColors.highlightFg
                        )
                    }
                    Text(
                        text = highlighted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = tableColors.cellTextColor,
                        maxLines = if (expandFullCellText) Int.MAX_VALUE else 4,
                        overflow = if (expandFullCellText) TextOverflow.Clip else TextOverflow.Ellipsis,
                        textAlign = textAlign
                    )
                }
            }
        }
    }
}
