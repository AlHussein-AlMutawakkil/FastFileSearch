@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollState
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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import java.awt.Cursor
import kotlin.math.abs

private class DesktopExcelTableThemeColors(
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
    return remember(isDark) {
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
                selectedCellBg = Color(0xFF253549),
                selectedRowBg = Color(0xFF1F2A38),
                activeCellBorder = Color(0xFFF4F5F7),
                selectedCellBorder = Color(0xFF64B5F6)
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
                selectedCellBg = Color(0xFFDDEBF7),
                selectedRowBg = Color(0xFFEBF3FB),
                activeCellBorder = Color(0xFF1C1C1E),
                selectedCellBorder = Color(0xFF2B7CD3)
            )
        }
    }
}

private data class CellContextMenuTarget(
    val rowIndex: Int,
    val colIndex: Int,
    val cellData: SelectedCellData,
    val rowData: SelectedRowData,
    val result: SearchResult
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
    onRowNumBounds: ((LayoutCoordinates) -> Unit)? = null,
    onHeaderCellBounds: ((Int, LayoutCoordinates) -> Unit)? = null,
    onDataCellBounds: ((Int, Int, LayoutCoordinates) -> Unit)? = null
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
            val sortedResults = remember(sheetResults) {
                sheetResults.sortedBy { res ->
                    res.rowNumber ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
                }
            }
            val headers = remember(sortedResults) {
                sortedResults.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            }

            val sheetExpandKey = "$fileUri::$sheetName"
            var localExpand by remember { mutableStateOf(false) }
            val isSheetExpanded = showAllRows || localExpand || (sheetExpandKey in expandedSheets) || sortedResults.size <= 30
            val visibleRows = if (isSheetExpanded) sortedResults else sortedResults.take(30)

            val parsedRows = remember(visibleRows) {
                visibleRows.map { res ->
                    val rNum = res.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(res.location).second
                    Triple(rNum, DesktopTableCellHelper.splitIntoCells(res.snippet), res)
                }
            }
            val maxCols = remember(parsedRows, headers) {
                maxOf(
                    parsedRows.maxOfOrNull { it.second.size } ?: 1,
                    headers.size
                ).coerceAtLeast(1)
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
                    parsedRows,
                    headers,
                    maxCols,
                    totalContainerWidth,
                    stretchSingleColumn,
                    density
                ) {
                    DesktopTableLayoutCalculator.calculate(
                        rowsData = parsedRows,
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
                var rowContextMenuIndex by remember { mutableStateOf<Int?>(null) }
                var cellContextMenu by remember { mutableStateOf<CellContextMenuTarget?>(null) }

                val currentSelectionState by rememberUpdatedState(selectionState)
                val currentSheetModel by rememberUpdatedState(sheetModel)
                val currentOnSelectionChange by rememberUpdatedState(onSelectionStateChange)
                val currentOnViewDetail by rememberUpdatedState(onViewDetail)
                val currentOnViewCellDetail by rememberUpdatedState(onViewCellDetail)
                val currentOnRequestFocus by rememberUpdatedState(onRequestKeyboardFocus)

                val cellRootBounds = remember(sheetResetKey) { mutableStateMapOf<Pair<Int, Int>, Rect>() }
                var gridRootCoords by remember { mutableStateOf<LayoutCoordinates?>(null) }

                fun findCellAtRootPos(rootPos: Offset): Pair<Int, Int>? {
                    cellRootBounds.entries.firstOrNull { (_, rect) -> rect.contains(rootPos) }?.let {
                        return it.key
                    }
                    if (cellRootBounds.isEmpty()) return null
                    var bestEntry: Map.Entry<Pair<Int, Int>, Rect>? = null
                    var bestDist = Float.MAX_VALUE
                    for (entry in cellRootBounds.entries) {
                        val r = entry.value
                        val dx = when {
                            rootPos.x < r.left -> r.left - rootPos.x
                            rootPos.x > r.right -> rootPos.x - r.right
                            else -> 0f
                        }
                        val dy = when {
                            rootPos.y < r.top -> r.top - rootPos.y
                            rootPos.y > r.bottom -> rootPos.y - r.bottom
                            else -> 0f
                        }
                        val dist = dx * dx + dy * dy
                        if (dist < bestDist) {
                            bestDist = dist
                            bestEntry = entry
                        }
                    }
                    return bestEntry?.key
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(hScroll)
                    ) {
                        Column(modifier = Modifier.width(dims.totalTableWidth)) {
                            // Header Row: wraps up to 2 lines cleanly with uniform row height
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 38.dp)
                                    .height(IntrinsicSize.Min)
                                    .background(tableColors.colHeaderBg),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Corner cell ("صف") + Sheet options menu
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
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "صف",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = tableColors.headerTextColor,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                        Icon(
                                            imageVector = Icons.Default.Menu,
                                            contentDescription = "خيارات الورقة",
                                            tint = tableColors.headerTextColor,
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = sheetMenuExpanded,
                                        onDismissRequest = { sheetMenuExpanded = false },
                                        modifier = Modifier
                                            .background(desktopColors.cardBackground)
                                            .border(0.5.dp, desktopColors.divider, RoundedCornerShape(8.dp))
                                    ) {
                                        DropdownMenuItem(
                                            text = {
                                                Text("نسخ جميع صفوف الورقة", fontSize = 13.sp, color = desktopColors.textPrimary)
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
                                                Text("تحديد جميع الصفوف", fontSize = 13.sp, color = desktopColors.textPrimary)
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
                                        DropdownMenuItem(
                                            text = {
                                                Text("تحديد جميع الخلايا", fontSize = 13.sp, color = desktopColors.textPrimary)
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
                                                currentOnSelectionChange?.invoke(
                                                    currentSelectionState.selectAllCells(sheetModel.buildAllCells())
                                                )
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
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(horizontal = 8.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = colTitle,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                lineHeight = 17.sp,
                                                color = tableColors.headerTextColor,
                                                maxLines = 2,
                                                softWrap = true,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center
                                            )
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

                            // Data Rows
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

                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val coords = gridRootCoords
                                                if (coords == null || !coords.isAttached) continue
                                                val change = event.changes.firstOrNull() ?: continue
                                                val rootPos = coords.localToRoot(change.position)

                                                when (event.type) {
                                                    PointerEventType.Press -> {
                                                        val targetCoord = findCellAtRootPos(rootPos) ?: continue
                                                        val (rIdx, cIdx) = targetCoord
                                                        val model = currentSheetModel
                                                        val cellData = model.buildCellData(rIdx, cIdx) ?: continue
                                                        val rowData = model.buildRowData(rIdx) ?: continue
                                                        val res = model.results.getOrNull(rIdx) ?: continue

                                                        currentOnRequestFocus?.invoke()

                                                        if (event.button == PointerButton.Secondary) {
                                                            // Right-click on cell -> select if not selected, then show context menu
                                                            val sel = currentSelectionState
                                                            if (!sel.isCellSelected(cellData.key)) {
                                                                currentOnSelectionChange?.invoke(sel.selectSingleCell(cellData))
                                                            }
                                                            cellContextMenu = CellContextMenuTarget(
                                                                rowIndex = rIdx,
                                                                colIndex = cIdx,
                                                                cellData = cellData,
                                                                rowData = rowData,
                                                                result = res
                                                            )
                                                            dragAnchorCoord = null
                                                            change.consume()
                                                        } else if (event.button == PointerButton.Primary) {
                                                            val now = change.uptimeMillis
                                                            val isDoubleClick = (now - lastPressTimeMs) in 1L..349L &&
                                                                lastPressedCell == cellData.key &&
                                                                !event.keyboardModifiers.isShiftPressed &&
                                                                !event.keyboardModifiers.isCtrlPressed &&
                                                                !event.keyboardModifiers.isMetaPressed

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
                                                            val isShift = event.keyboardModifiers.isShiftPressed
                                                            val isCtrl = event.keyboardModifiers.isCtrlPressed ||
                                                                event.keyboardModifiers.isMetaPressed

                                                            if (isShift) {
                                                                val anchor = sel.anchorCell
                                                                    ?.takeIf { it.fileUri == model.fileUri && it.sheetName == model.sheetName }
                                                                    ?: cellData.key
                                                                val range = model.buildCellRange(anchor, cellData.key)
                                                                currentOnSelectionChange?.invoke(
                                                                    sel.selectCellRange(range, cellData.key, anchor, additive = isCtrl)
                                                                )
                                                                dragAnchorCoord = anchor.rowIndex to anchor.colIndex
                                                            } else if (isCtrl) {
                                                                currentOnSelectionChange?.invoke(sel.toggleCell(cellData))
                                                                dragAnchorCoord = targetCoord
                                                            } else {
                                                                currentOnSelectionChange?.invoke(sel.selectSingleCell(cellData))
                                                                dragAnchorCoord = targetCoord
                                                            }
                                                            pressStartPos = rootPos
                                                            didDragAcrossCells = false
                                                            dragAdditive = isCtrl
                                                            change.consume()
                                                        }
                                                    }

                                                    PointerEventType.Move -> {
                                                        val anchorCoord = dragAnchorCoord ?: continue
                                                        if (!event.buttons.isPrimaryPressed) {
                                                            dragAnchorCoord = null
                                                            continue
                                                        }
                                                        val dist = abs(rootPos.x - pressStartPos.x) + abs(rootPos.y - pressStartPos.y)
                                                        if (!didDragAcrossCells && dist < 5f) continue

                                                        val hoverCoord = findCellAtRootPos(rootPos) ?: continue
                                                        if (hoverCoord != anchorCoord || didDragAcrossCells) {
                                                            didDragAcrossCells = true
                                                            val model = currentSheetModel
                                                            val anchorKey = CellKey(model.fileUri, model.sheetName, anchorCoord.first, anchorCoord.second)
                                                            val targetKey = CellKey(model.fileUri, model.sheetName, hoverCoord.first, hoverCoord.second)
                                                            val range = model.buildCellRange(anchorKey, targetKey)
                                                            currentOnSelectionChange?.invoke(
                                                                currentSelectionState.selectCellRange(
                                                                    rangeCells = range,
                                                                    focusKey = targetKey,
                                                                    anchorKey = anchorKey,
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
                                parsedRows.forEachIndexed { rIdx, (rowNum, cells, res) ->
                                    val isEven = rIdx % 2 == 0
                                    val rowKey = RowKey(fileUri = fileUri, sheetName = sheetName, rowIndex = rIdx)
                                    val isRowSelected = selectionState.isRowSelected(rowKey)
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
                                            .background(rowBg)
                                            .height(IntrinsicSize.Min),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Row Number Cell: Click selects row, Shift+Click range, Ctrl+Click toggle, Double-click opens row detail, Right-click opens menu
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
                                                            val model = currentSheetModel
                                                            val rowData = model.buildRowData(rIdx) ?: continue
                                                            currentOnRequestFocus?.invoke()

                                                            if (ev.button == PointerButton.Secondary) {
                                                                val sel = currentSelectionState
                                                                if (!sel.isRowSelected(rowData.key)) {
                                                                    currentOnSelectionChange?.invoke(sel.selectSingleRow(rowData))
                                                                }
                                                                rowContextMenuIndex = rIdx
                                                                ch.consume()
                                                            } else if (ev.button == PointerButton.Primary) {
                                                                val now = ch.uptimeMillis
                                                                val isDouble = (now - lastRowPressMs) in 1L..349L &&
                                                                    !ev.keyboardModifiers.isShiftPressed &&
                                                                    !ev.keyboardModifiers.isCtrlPressed &&
                                                                    !ev.keyboardModifiers.isMetaPressed
                                                                lastRowPressMs = now
                                                                if (isDouble) {
                                                                    currentOnViewDetail(res)
                                                                    ch.consume()
                                                                    continue
                                                                }
                                                                val sel = currentSelectionState
                                                                val isShift = ev.keyboardModifiers.isShiftPressed
                                                                val isCtrl = ev.keyboardModifiers.isCtrlPressed || ev.keyboardModifiers.isMetaPressed
                                                                if (isShift) {
                                                                    val anchor = sel.anchorRow
                                                                        ?.takeIf { it.fileUri == model.fileUri && it.sheetName == model.sheetName }
                                                                        ?: rowData.key
                                                                    val rangeRows = model.buildRowRange(anchor, rowData.key)
                                                                    currentOnSelectionChange?.invoke(
                                                                        sel.selectRowRange(rangeRows, rowData.key, anchor, additive = isCtrl)
                                                                    )
                                                                } else if (isCtrl) {
                                                                    currentOnSelectionChange?.invoke(sel.toggleRow(rowData))
                                                                } else {
                                                                    currentOnSelectionChange?.invoke(sel.selectSingleRow(rowData))
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

                                            DropdownMenu(
                                                expanded = rowContextMenuIndex == rIdx,
                                                onDismissRequest = { rowContextMenuIndex = null },
                                                modifier = Modifier
                                                    .background(desktopColors.cardBackground)
                                                    .border(0.5.dp, desktopColors.divider, RoundedCornerShape(8.dp))
                                            ) {
                                                val formattedRow = DesktopValueInteractionHelper.formatRow(cells, headers, verticalCopyFormat)
                                                DropdownMenuItem(
                                                    text = { Text("عرض تفاصيل السجل", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                    leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                    onClick = {
                                                        rowContextMenuIndex = null
                                                        onViewDetail(res)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = { Text("نسخ الصف منسّقاً", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                    leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                    onClick = {
                                                        rowContextMenuIndex = null
                                                        onCopyText?.invoke(formattedRow)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            if (isRowSelected) "إلغاء تحديد الصف" else "تحديد الصف",
                                                            fontSize = 13.sp,
                                                            color = desktopColors.textPrimary
                                                        )
                                                    },
                                                    leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                    onClick = {
                                                        rowContextMenuIndex = null
                                                        sheetModel.buildRowData(rIdx)?.let { rowData ->
                                                            onSelectionStateChange?.invoke(selectionState.toggleRow(rowData))
                                                        }
                                                    }
                                                )
                                                if (onExportSelection != null && selectionState.isActive) {
                                                    DropdownMenuItem(
                                                        text = { Text("تصدير المحدد إلى Excel", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                        leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                        onClick = {
                                                            rowContextMenuIndex = null
                                                            onExportSelection()
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        for (cIdx in 0 until maxCols) {
                                            val cellVal = cells.getOrNull(cIdx) ?: ""
                                            val colW = dims.columnWidths.getOrElse(cIdx) { 110.dp }
                                            val cellKey = CellKey(fileUri = fileUri, sheetName = sheetName, rowIndex = rIdx, colIndex = cIdx)
                                            val isCellSelected = selectionState.isCellSelected(cellKey)
                                            val isCellActive = selectionState.isActiveCell(cellKey)
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
                                                    .onGloballyPositioned { coords ->
                                                        if (coords.isAttached) {
                                                            cellRootBounds[rIdx to cIdx] = coords.boundsInRoot()
                                                        }
                                                        onDataCellBounds?.invoke(rIdx, cIdx, coords)
                                                    }
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

                                                val activeContext = cellContextMenu
                                                if (activeContext != null &&
                                                    activeContext.rowIndex == rIdx &&
                                                    activeContext.colIndex == cIdx
                                                ) {
                                                    DropdownMenu(
                                                        expanded = true,
                                                        onDismissRequest = { cellContextMenu = null },
                                                        modifier = Modifier
                                                            .background(desktopColors.cardBackground)
                                                            .border(0.5.dp, desktopColors.divider, RoundedCornerShape(8.dp))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = { Text("نسخ (Ctrl+C)", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                            onClick = {
                                                                cellContextMenu = null
                                                                val txt = if (selectionState.mode == SelectionMode.CELLS && selectionState.count > 1) {
                                                                    DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(selectionState, verticalCopyFormat)
                                                                } else {
                                                                    activeContext.cellData.value
                                                                }
                                                                onCopyText?.invoke(txt)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("نسخ الصف منسّقاً", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                            onClick = {
                                                                cellContextMenu = null
                                                                val formatted = DesktopValueInteractionHelper.formatRow(
                                                                    activeContext.rowData.cells,
                                                                    activeContext.rowData.headers,
                                                                    verticalCopyFormat
                                                                )
                                                                onCopyText?.invoke(formatted)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("عرض تفاصيل الخلية", fontSize = 13.sp, color = desktopColors.textPrimary) },
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
                                                        DropdownMenuItem(
                                                            text = { Text("عرض تفاصيل السجل كاملاً", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                            leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                            onClick = {
                                                                cellContextMenu = null
                                                                onViewDetail(activeContext.result)
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = { Text("تحديد الصف كاملاً", fontSize = 13.sp, color = desktopColors.textPrimary) },
                                                            leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, tint = desktopColors.textSecondary, modifier = Modifier.size(17.dp)) },
                                                            onClick = {
                                                                cellContextMenu = null
                                                                onSelectionStateChange?.invoke(selectionState.selectSingleRow(activeContext.rowData))
                                                            }
                                                        )
                                                        if (onExportSelection != null && selectionState.isActive) {
                                                            DropdownMenuItem(
                                                                text = { Text("تصدير المحدد إلى Excel", fontSize = 13.sp, color = desktopColors.textPrimary) },
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

            if (!isSheetExpanded && sortedResults.size > visibleRows.size) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tableColors.rowAltBg)
                        .clickable {
                            localExpand = true
                            onExpandSheet?.invoke(sheetExpandKey)
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "عرض المزيد (+${sortedResults.size - visibleRows.size} صف)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = desktopColors.textSecondary
                    )
                }
            }

            HorizontalDivider(color = tableColors.gridBorderColor, thickness = 1.dp)
        }
    }
}
