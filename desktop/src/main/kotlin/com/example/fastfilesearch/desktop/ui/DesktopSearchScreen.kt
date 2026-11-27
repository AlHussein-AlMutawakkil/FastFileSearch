@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.fastfilesearch.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material.icons.outlined.VerticalSplit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.export.DesktopExcelExporter
import com.example.fastfilesearch.desktop.model.DesktopSearchResponse
import com.example.fastfilesearch.desktop.model.FileResultGroup
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.SupportedFileTypeGroups
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.ui.selection.DesktopGridNavigator
import com.example.fastfilesearch.desktop.ui.selection.GridNavDirection
import com.example.fastfilesearch.desktop.ui.selection.RowKey
import com.example.fastfilesearch.desktop.ui.selection.SelectedCellData
import com.example.fastfilesearch.desktop.ui.selection.SelectedRowData
import com.example.fastfilesearch.desktop.ui.selection.SelectionMode
import com.example.fastfilesearch.desktop.ui.selection.SelectionState
import com.example.fastfilesearch.desktop.ui.selection.SheetGridModel
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private data class CellDetailTarget(
    val title: String,
    val subtitle: String?,
    val text: String,
    val query: String
)

private data class FileDetailsTarget(
    val fileUri: String,
    val fileName: String,
    val matchesCount: Int
)

private data class SelectedPreviewInfo(
    val locationLabel: String,
    val fileUri: String,
    val fileName: String,
    val subtitle: String,
    val previewText: String,
    val headers: List<String>,
    val cells: List<String>,
    val activeColIndex: Int?,
    val isDocument: Boolean,
    val activeCellData: SelectedCellData?,
    val matchingResult: SearchResult?
)

@Composable
fun DesktopSearchScreen(
    searchQuery: String,
    searchFilter: SearchFilter,
    defaultSearchMethod: SearchMethod,
    searchResults: List<SearchResult> = emptyList(),
    isSearching: Boolean,
    workspaces: List<Workspace>,
    activeWorkspace: Workspace?,
    indexedFiles: List<IndexedFileRecord>,
    indexProgress: IndexProgress,
    filesCount: Int,
    segmentsCount: Int,
    canExportExcel: Boolean,
    showAllRows: Boolean,
    stretchSingleColumn: Boolean,
    verticalCopyFormat: Boolean,
    expandFullCellText: Boolean,
    isSearchHistoryEnabled: Boolean,
    searchHistory: List<String>,
    onQueryChange: (String) -> Unit,
    onSubmitSearch: (String) -> Unit,
    onSelectHistoryQuery: (String) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onSelectActiveWorkspace: (String?) -> Unit,
    onRefreshIndex: () -> Unit,
    onFilterChange: (SearchFilter) -> Unit,
    onOpenWorkspaces: () -> Unit,
    onSelectionSummaryChange: (String?) -> Unit = {},
    onResultsSummaryChange: (Int, Int) -> Unit = { _, _ -> },
    searchResponse: DesktopSearchResponse? = null,
    typeCounts: Map<String, Int>? = null,
    onLoadMoreForFile: (String) -> Unit = {}
) {
    val colors = LocalDesktopColors.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    val searchBoxFocusRequester = remember { FocusRequester() }
    val resultsFocusRequester = remember { FocusRequester() }
    var isSearchBoxFocused by remember { mutableStateOf(false) }

    var collapsedFileUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    var expandedSheets by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectionState by remember { mutableStateOf(SelectionState()) }
    var showDetailsPane by remember { mutableStateOf(false) }

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showFilterOptionsMenu by remember { mutableStateOf(false) }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var rowDetailTarget by remember { mutableStateOf<SearchResult?>(null) }
    var cellDetailTarget by remember { mutableStateOf<CellDetailTarget?>(null) }
    var fileDetailsTarget by remember { mutableStateOf<FileDetailsTarget?>(null) }

    LaunchedEffect(searchQuery, searchFilter, activeWorkspace?.id) {
        collapsedFileUris = emptySet()
        expandedSheets = emptySet()
        selectionState = selectionState.clear()
    }

    val groupedResults = remember(searchResponse, searchResults) {
        if (searchResponse != null) {
            searchResponse.groups
        } else {
            searchResults
                .groupBy { it.fileUri }
                .map { (uri, rows) ->
                    val name = rows.firstOrNull()?.fileName ?: ""
                    FileResultGroup(
                        fileUri = uri,
                        fileName = name,
                        isDocument = !DesktopTableCellHelper.isTabular(name, rows.firstOrNull()?.snippet.orEmpty()),
                        results = rows
                    )
                }
        }
    }

    val effectiveResults = remember(searchResponse, searchResults) {
        searchResponse?.groups?.flatMap { it.results } ?: searchResults
    }

    val totalMatchesCount = remember(searchResponse, effectiveResults) {
        searchResponse?.totalResultsCount ?: effectiveResults.size
    }

    val effectiveTypeCounts = remember(searchQuery, typeCounts, groupedResults, indexedFiles) {
        typeCounts ?: if (searchQuery.isBlank()) {
            DesktopFormatFilterHelper.deriveTypeCountsFromFiles(indexedFiles)
        } else {
            DesktopFormatFilterHelper.deriveTypeCountsFromResults(groupedResults)
        }
    }

    val indexedFilesByUri = remember(indexedFiles) { indexedFiles.associateBy { it.uri } }
    val workspacesById = remember(workspaces) { workspaces.associateBy { it.id } }

    val allSheetModels = remember(groupedResults, showAllRows, expandedSheets) {
        groupedResults.flatMap { group ->
            DesktopGridNavigator.buildSheetModelsForGroup(group, showAllRows, expandedSheets)
        }
    }

    LaunchedEffect(selectionState) {
        onSelectionSummaryChange(
            if (selectionState.isActive) "محدد: ${selectionState.getFormattedCountArabic()}" else null
        )
    }

    LaunchedEffect(totalMatchesCount, groupedResults) {
        onResultsSummaryChange(totalMatchesCount, groupedResults.size)
    }

    val effectiveResultByKey = remember(effectiveResults) {
        HashMap<Triple<String, String, String>, SearchResult>(effectiveResults.size * 2).apply {
            for (res in effectiveResults) {
                val (sheet, row) = DesktopTableCellHelper.extractSheetAndRow(res.location)
                val key = Triple(
                    res.fileUri,
                    res.sheetName ?: sheet,
                    res.rowNumber?.toString() ?: row
                )
                putIfAbsent(key, res)
            }
        }
    }

    val selectedPreview = remember(showDetailsPane, selectionState, allSheetModels, effectiveResultByKey) {
        if (!showDetailsPane) return@remember null
        val activeCellKey = selectionState.activeCell
        val activeCellData = activeCellKey?.let { selectionState.selectedCells[it] }
        if (activeCellData != null) {
            val sheetModel = allSheetModels.firstOrNull {
                it.fileUri == activeCellData.key.fileUri && it.sheetName == activeCellData.sheetName
            }
            val rowData = sheetModel?.buildRowData(activeCellData.key.rowIndex)
            val matchedResult = effectiveResultByKey[
                Triple(activeCellData.key.fileUri, activeCellData.sheetName, activeCellData.rowNumber)
            ]
            SelectedPreviewInfo(
                locationLabel = "${activeCellData.columnName} • صف ${activeCellData.rowNumber}",
                fileUri = activeCellData.key.fileUri,
                fileName = activeCellData.fileName,
                subtitle = "${activeCellData.sheetName} • صف ${activeCellData.rowNumber}",
                previewText = activeCellData.value,
                headers = rowData?.headers ?: listOf(activeCellData.columnName),
                cells = rowData?.cells ?: listOf(activeCellData.value),
                activeColIndex = activeCellData.key.colIndex,
                isDocument = false,
                activeCellData = activeCellData,
                matchingResult = matchedResult
            )
        } else if (selectionState.mode == SelectionMode.ROWS && selectionState.selectedRows.isNotEmpty()) {
            val rowData = selectionState.focusedRow?.let { selectionState.selectedRows[it] }
                ?: selectionState.selectedRows.values.first()
            val matchedResult = if (rowData.isDocument) {
                effectiveResults.firstOrNull { res ->
                    res.fileUri == rowData.key.fileUri && (rowData.location.isEmpty() || res.location == rowData.location)
                }
            } else {
                effectiveResultByKey[Triple(rowData.key.fileUri, rowData.sheetName, rowData.rowNumber)]
            }
            SelectedPreviewInfo(
                locationLabel = if (rowData.isDocument) rowData.location else "${rowData.sheetName} • صف ${rowData.rowNumber}",
                fileUri = rowData.key.fileUri,
                fileName = rowData.fileName,
                subtitle = if (rowData.isDocument) rowData.location else "${rowData.sheetName} • صف ${rowData.rowNumber}",
                previewText = if (rowData.isDocument) {
                    rowData.snippet
                } else {
                    DesktopValueInteractionHelper.formatRow(rowData.cells, rowData.headers, vertical = false)
                },
                headers = rowData.headers,
                cells = rowData.cells,
                activeColIndex = null,
                isDocument = rowData.isDocument,
                activeCellData = null,
                matchingResult = matchedResult
            )
        } else {
            null
        }
    }

    val viewportCoordsRef = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val boundsResolvers = remember { HashMap<String, (Any) -> Pair<Rect, Float>?>() }
    val liveSheetModels = remember { HashMap<String, SheetGridModel>() }

    // Ensure active cell or focused row stays vertically visible (below any pinned sticky header) on keyboard navigation
    val activeSelectionTarget: Any? = selectionState.activeCell ?: selectionState.focusedRow
    val activeSelectionUri: String? = selectionState.activeCell?.fileUri ?: selectionState.focusedRow?.fileUri
    LaunchedEffect(activeSelectionTarget) {
        val targetKey = activeSelectionTarget ?: return@LaunchedEffect
        val targetUri = activeSelectionUri ?: return@LaunchedEffect
        val itemIdx = groupedResults.indexOfFirst { it.fileUri == targetUri }
        if (itemIdx < 0) return@LaunchedEffect

        val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == itemIdx }
        if (!isVisible) {
            listState.scrollToItem(itemIdx)
            withFrameNanos { }
        }

        var resolved = boundsResolvers.values.firstNotNullOfOrNull { it(targetKey) }
        if (resolved == null) {
            withFrameNanos { }
            resolved = boundsResolvers.values.firstNotNullOfOrNull { it(targetKey) }
        }
        val hit = resolved ?: return@LaunchedEffect

        val vp = viewportCoordsRef[0]?.unclippedBoundsInRoot() ?: return@LaunchedEffect
        val rect = hit.first
        val stickyInset = hit.second
        val topLimit = vp.top + stickyInset + 4f
        val bottomLimit = vp.bottom - 4f
        when {
            rect.bottom > bottomLimit -> {
                val delta = (rect.bottom - bottomLimit + 6f).coerceAtLeast(0f)
                listState.scrollBy(delta)
            }
            rect.top < topLimit -> {
                val delta = (rect.top - topLimit - 6f).coerceAtMost(0f)
                listState.scrollBy(delta)
            }
        }
    }

    fun copySelectionToClipboard(preferFormattedLabels: Boolean = false) {
        if (!selectionState.isActive) return
        val text = DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
            selectionState = selectionState,
            verticalCopyFormat = verticalCopyFormat,
            preferFormattedLabels = preferFormattedLabels
        )
        if (text.isNotEmpty()) {
            DesktopSystemActions.copyToClipboard(text)
        }
    }

    fun exportSelectionToExcel() {
        if (!canExportExcel) {
            statusMessage = "ميزة تصدير Excel غير مفعّلة في هذا الترخيص."
            return
        }
        if (!selectionState.isActive) return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val targetFile = DesktopSystemActions.pickSaveExcelFile("تحديد_بحث_آمن_$timestamp.xlsx") ?: return
        val snapshotState = selectionState
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (snapshotState.mode == SelectionMode.CELLS) {
                        DesktopExcelExporter.exportCellsToFile(snapshotState.selectedCells.values.toList(), targetFile)
                    } else {
                        DesktopExcelExporter.exportRowsToFile(snapshotState.selectedRows.values.toList(), targetFile)
                    }
                }
                statusMessage = "تم حفظ ملف Excel بنجاح (${snapshotState.getFormattedCountArabic()}) في ${targetFile.name}"
            } catch (t: Throwable) {
                statusMessage = "تعذر تصدير ملف Excel: ${t.message}"
            }
        }
    }

    fun exportResultsToExcel(resultsToExport: List<SearchResult>, prefix: String = "بحث_آمن") {
        if (!canExportExcel) {
            statusMessage = "ميزة تصدير Excel غير مفعّلة في هذا الترخيص."
            return
        }
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
        val targetFile = DesktopSystemActions.pickSaveExcelFile("${prefix}_$timestamp.xlsx") ?: return
        val snapshot = resultsToExport.toList()
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DesktopExcelExporter.exportToFile(snapshot, targetFile)
                }
                statusMessage = "تم حفظ ملف Excel بنجاح (${snapshot.size} نتيجة) في ${targetFile.name}"
            } catch (t: Throwable) {
                statusMessage = "تعذر تصدير ملف Excel: ${t.message}"
            }
        }
    }

    val visibleFileUris = remember(groupedResults) { groupedResults.map { it.fileUri } }
    val anyExpanded = DesktopFileExpansionStateHelper.hasAnyExpanded(visibleFileUris, collapsedFileUris)
    val toggleAllDescription = if (anyExpanded) "طي جميع الملفات" else "توسيع جميع الملفات"

    val hasAnyModalOpen = rowDetailTarget != null || cellDetailTarget != null || fileDetailsTarget != null || showFilterDialog

    fun selectAllCurrentScope() {
        val effectiveSheets = allSheetModels.map { m -> liveSheetModels["${m.fileUri}::${m.sheetName}"] ?: m }
        val active = selectionState.activeCell
        val activeSheet = if (active != null) {
            effectiveSheets.firstOrNull { it.fileUri == active.fileUri && it.sheetName == active.sheetName }
        } else {
            null
        }
        selectionState = when {
            activeSheet != null -> selectionState.selectAllCells(activeSheet.buildAllCells())
            selectionState.mode == SelectionMode.CELLS -> {
                val allCells = effectiveSheets.flatMap { it.buildAllCells() }
                if (allCells.isNotEmpty()) selectionState.selectAllCells(allCells) else selectionState
            }
            else -> {
                val allRows = groupedResults.flatMap { group ->
                    DesktopTableCellHelper.buildSelectableRowsForFile(
                        fileUri = group.fileUri,
                        fileName = group.fileName,
                        isDocument = group.isDocument,
                        results = group.results
                    )
                }
                if (allRows.isNotEmpty()) selectionState.selectAllRows(allRows) else selectionState
            }
        }
    }

    fun openFocusedSelectionDetail(): Boolean {
        if (!selectionState.shouldShowPreviewMenuItem()) return false
        val active = selectionState.activeCell
        if (active != null) {
            val cellData = selectionState.selectedCells[active]
            if (cellData != null) {
                cellDetailTarget = CellDetailTarget(
                    title = cellData.columnName,
                    subtitle = "${cellData.fileName} • ${cellData.sheetName} • صف ${cellData.rowNumber}",
                    text = cellData.value,
                    query = searchQuery
                )
                return true
            }
        }
        if (selectionState.mode == SelectionMode.ROWS && selectionState.selectedRows.isNotEmpty()) {
            val rowData = selectionState.focusedRow?.let { selectionState.selectedRows[it] }
                ?: selectionState.selectedRows.values.first()
            if (rowData.isDocument) {
                cellDetailTarget = CellDetailTarget(
                    title = rowData.fileName,
                    subtitle = rowData.location,
                    text = rowData.snippet,
                    query = searchQuery
                )
            } else {
                val res = effectiveResults.firstOrNull {
                    it.fileUri == rowData.key.fileUri &&
                        (it.sheetName ?: DesktopTableCellHelper.extractSheetAndRow(it.location).first) == rowData.sheetName &&
                        (it.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(it.location).second) == rowData.rowNumber
                }
                if (res != null) rowDetailTarget = res
            }
            return true
        }
        return false
    }

    fun exportAutoContextToExcel() {
        if (selectionState.isActive) {
            exportSelectionToExcel()
        } else if (effectiveResults.isNotEmpty()) {
            exportResultsToExcel(effectiveResults, "بحث_آمن")
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || hasAnyModalOpen) return@onPreviewKeyEvent false
                val isCtrl = keyEvent.isCtrlPressed || keyEvent.isMetaPressed

                if (isCtrl && keyEvent.key == Key.F) {
                    searchBoxFocusRequester.requestFocus()
                    return@onPreviewKeyEvent true
                }

                if (keyEvent.key == Key.F5 && workspaces.isNotEmpty() && !indexProgress.isIndexing) {
                    onRefreshIndex()
                    return@onPreviewKeyEvent true
                }

                if (keyEvent.isAltPressed && (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                    showDetailsPane = !showDetailsPane
                    return@onPreviewKeyEvent true
                }

                if (isSearchBoxFocused) return@onPreviewKeyEvent false

                when {
                    isCtrl && keyEvent.isAltPressed && keyEvent.key == Key.C && selectionState.isActive -> {
                        copySelectionToClipboard(preferFormattedLabels = true)
                        true
                    }
                    isCtrl && !keyEvent.isAltPressed && keyEvent.key == Key.C && selectionState.isActive -> {
                        copySelectionToClipboard(preferFormattedLabels = false)
                        true
                    }
                    isCtrl && keyEvent.key == Key.A && groupedResults.isNotEmpty() -> {
                        selectAllCurrentScope()
                        true
                    }
                    keyEvent.key == Key.Escape && selectionState.isActive -> {
                        selectionState = selectionState.clear()
                        true
                    }
                    keyEvent.key == Key.DirectionUp && allSheetModels.isNotEmpty() -> {
                        val effectiveSheets = allSheetModels.map { m -> liveSheetModels["${m.fileUri}::${m.sheetName}"] ?: m }
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = effectiveSheets,
                            currentState = selectionState,
                            direction = GridNavDirection.UP,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionDown && allSheetModels.isNotEmpty() -> {
                        val effectiveSheets = allSheetModels.map { m -> liveSheetModels["${m.fileUri}::${m.sheetName}"] ?: m }
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = effectiveSheets,
                            currentState = selectionState,
                            direction = GridNavDirection.DOWN,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionLeft && allSheetModels.isNotEmpty() -> {
                        val effectiveSheets = allSheetModels.map { m -> liveSheetModels["${m.fileUri}::${m.sheetName}"] ?: m }
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = effectiveSheets,
                            currentState = selectionState,
                            direction = GridNavDirection.LEFT,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionRight && allSheetModels.isNotEmpty() -> {
                        val effectiveSheets = allSheetModels.map { m -> liveSheetModels["${m.fileUri}::${m.sheetName}"] ?: m }
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = effectiveSheets,
                            currentState = selectionState,
                            direction = GridNavDirection.RIGHT,
                            isRtl = isRtl
                        )
                        true
                    }
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && selectionState.isActive -> {
                        openFocusedSelectionDetail()
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Bar 1: Clean 3-item Search Toolbar (Workspace Switcher + Search Box + "التصفية" Popover)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Workspace Switcher (bounded width on narrow screens)
            Box(modifier = Modifier.widthIn(max = 175.dp)) {
                val wsLabel = activeWorkspace?.name ?: if (workspaces.isNotEmpty()) {
                    "جميع المجلدات (${workspaces.size})"
                } else {
                    "جميع المجلدات"
                }
                FluentTooltip("نطاق البحث والمجلد النشط") {
                    DesktopSecondaryButton(
                        text = wsLabel,
                        icon = Icons.Outlined.FolderOpen,
                        enabled = workspaces.isNotEmpty(),
                        onClick = { showWorkspaceMenu = true }
                    )
                }
                DropdownMenu(
                    expanded = showWorkspaceMenu,
                    onDismissRequest = { showWorkspaceMenu = false },
                    modifier = Modifier
                        .background(colors.cardBackground)
                        .border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("جميع المجلدات (${workspaces.size})", fontSize = 13.sp, color = colors.textPrimary)
                                if (activeWorkspace == null) {
                                    Icon(Icons.Default.Check, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            showWorkspaceMenu = false
                            onSelectActiveWorkspace(null)
                        }
                    )
                    workspaces.forEach { ws ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(ws.name, fontSize = 13.sp, color = colors.textPrimary)
                                    if (activeWorkspace?.id == ws.id) {
                                        Icon(Icons.Default.Check, null, tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                showWorkspaceMenu = false
                                onSelectActiveWorkspace(ws.id)
                            }
                        )
                    }
                }
            }

            // 2. Wide Priority Search Box
            FluentTextBox(
                value = searchQuery,
                onValueChange = onQueryChange,
                placeholder = "ابحث في المستندات والجداول والنصوص...",
                modifier = Modifier.weight(1f).widthIn(min = 180.dp),
                textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.5.sp),
                fieldModifier = Modifier
                    .focusRequester(searchBoxFocusRequester)
                    .onFocusChanged { isSearchBoxFocused = it.isFocused }
                    .onPreviewKeyEvent { ev ->
                        if (ev.type == KeyEventType.KeyDown &&
                            (ev.key == Key.Enter || ev.key == Key.NumPadEnter)
                        ) {
                            onSubmitSearch(searchQuery)
                            true
                        } else {
                            false
                        }
                    },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (isSearching) {
                            CircularProgressIndicator(modifier = Modifier.size(15.dp), strokeWidth = 2.dp)
                        }
                        if (searchQuery.isNotEmpty()) {
                            FluentTooltip("مسح البحث") {
                                IconButton(
                                    onClick = { onQueryChange("") },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            )

            // 3. Single Unified "التصفية" Popover Button + small count badge
            val activeOptionsCount = (if (searchFilter.method != defaultSearchMethod) 1 else 0) +
                (if (!searchFilter.ignoreDiacritics) 1 else 0) +
                searchFilter.selectedWorkspaceIds.size +
                (if (searchFilter.selectedFileUris.isNotEmpty()) 1 else 0)

            Box {
                FluentTooltip("طريقة البحث، التشكيل، والفلاتر المتقدمة") {
                    DesktopSecondaryButton(
                        text = "التصفية",
                        icon = Icons.Default.FilterList,
                        onClick = { showFilterOptionsMenu = true }
                    )
                }
                if (activeOptionsCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-3).dp, y = (-3).dp)
                            .defaultMinSize(minWidth = 17.dp)
                            .height(17.dp)
                            .clip(CircleShape)
                            .background(colors.controlActiveBackground)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$activeOptionsCount",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.controlActiveContent
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFilterOptionsMenu,
                    onDismissRequest = { showFilterOptionsMenu = false },
                    modifier = Modifier
                        .widthIn(min = 270.dp)
                        .background(colors.cardBackground)
                        .border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                ) {
                    Text(
                        text = "طريقة البحث",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                    SearchMethod.entries.forEach { method ->
                        val isMethodSelected = searchFilter.method == method
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(
                                            text = method.displayName,
                                            fontWeight = if (isMethodSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = colors.textPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = method.description,
                                            color = colors.textSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isMethodSelected) {
                                        Icon(Icons.Default.Check, null, tint = colors.controlActiveBackground, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            onClick = {
                                onFilterChange(searchFilter.copy(method = method))
                            }
                        )
                    }

                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onFilterChange(searchFilter.copy(ignoreDiacritics = !searchFilter.ignoreDiacritics))
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تجاهل التشكيل والهمزات",
                            fontSize = 13.sp,
                            color = colors.textPrimary
                        )
                        FluentToggleSwitch(
                            checked = searchFilter.ignoreDiacritics,
                            onCheckedChange = { checked ->
                                onFilterChange(searchFilter.copy(ignoreDiacritics = checked))
                            }
                        )
                    }

                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))

                    val advancedScopeCount = searchFilter.selectedWorkspaceIds.size + searchFilter.selectedFileUris.size
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الفلاتر المتقدمة", fontSize = 13.sp, color = colors.textPrimary)
                                if (advancedScopeCount > 0) {
                                    Text(
                                        text = "$advancedScopeCount",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(Icons.Default.FilterList, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp))
                        },
                        onClick = {
                            showFilterOptionsMenu = false
                            showFilterDialog = true
                        }
                    )

                    if (activeOptionsCount > 0) {
                        DropdownMenuItem(
                            text = {
                                Text("إعادة ضبط خيارات التصفية", fontSize = 12.5.sp, color = colors.dangerText)
                            },
                            leadingIcon = {
                                Icon(Icons.Outlined.Close, null, tint = colors.dangerText, modifier = Modifier.size(16.dp))
                            },
                            onClick = {
                                showFilterOptionsMenu = false
                                onFilterChange(
                                    searchFilter.copy(
                                        method = defaultSearchMethod,
                                        ignoreDiacritics = true,
                                        selectedWorkspaceIds = emptySet(),
                                        selectedFileUris = emptySet()
                                    )
                                )
                            }
                        )
                    }
                }
            }
        }

        // Bar 2: Single Contextual Command Bar (Normal Existing-Format Chips + Commands OR Selection Bar)
        val visibleFormatChips = remember(effectiveTypeCounts, searchFilter.selectedExtensions) {
            DesktopFormatFilterHelper.resolveVisibleGroups(
                typeCounts = effectiveTypeCounts,
                selectedExtensions = searchFilter.selectedExtensions
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                .background(colors.cardBackground)
                .border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!selectionState.isActive) {
                // Normal Mode: Single non-wrapping scrollable row of "الكل" + actually existing formats only
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val allSelected = searchFilter.selectedExtensions.isEmpty()
                    FilterChipButton(
                        label = "الكل",
                        selected = allSelected,
                        icon = null,
                        tint = null,
                        onClick = { onFilterChange(searchFilter.copy(selectedExtensions = emptySet())) }
                    )

                    visibleFormatChips.forEach { chip ->
                        val group = chip.group
                        val visual = DesktopFileTypeHelper.getVisualForGroup(group)
                        val isSelected = group.extensions.all { it in searchFilter.selectedExtensions } &&
                            searchFilter.selectedExtensions.isNotEmpty()
                        val chipLabel = group.displayName
                        FilterChipButton(
                            label = chipLabel,
                            selected = isSelected,
                            icon = visual.icon,
                            tint = visual.color,
                            onClick = {
                                val updated = if (isSelected) {
                                    searchFilter.selectedExtensions - group.extensions
                                } else {
                                    searchFilter.selectedExtensions + group.extensions
                                }
                                onFilterChange(searchFilter.copy(selectedExtensions = updated))
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(colors.divider))
                    Spacer(modifier = Modifier.width(2.dp))

                    FluentCommandButton(
                        text = null,
                        icon = Icons.Outlined.Refresh,
                        tooltip = if (indexProgress.isIndexing) "جارٍ تحديث الفهرس..." else "تحديث الفهرس (F5)",
                        enabled = workspaces.isNotEmpty() && !indexProgress.isIndexing,
                        onClick = onRefreshIndex
                    )

                    if (groupedResults.isNotEmpty()) {
                        FluentCommandButton(
                            text = if (anyExpanded) "طي الكل" else "توسيع الكل",
                            icon = if (anyExpanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                            tooltip = toggleAllDescription,
                            onClick = {
                                collapsedFileUris = DesktopFileExpansionStateHelper.toggleAll(
                                    visibleFileUris = visibleFileUris,
                                    collapsedFileUris = collapsedFileUris
                                )
                            }
                        )
                        FluentCommandButton(
                            text = "تصدير",
                            icon = Icons.Outlined.FileDownload,
                            tooltip = "تصدير جميع النتائج المعروضة إلى Excel (.xlsx)",
                            onClick = { exportAutoContextToExcel() }
                        )
                    }

                    FluentCommandButton(
                        text = null,
                        icon = Icons.Outlined.VerticalSplit,
                        tooltip = "إظهار / إخفاء لوحة التفاصيل الجانبية (Alt+Enter)",
                        active = showDetailsPane,
                        onClick = { showDetailsPane = !showDetailsPane }
                    )
                }
            } else {
                // Selection Mode: Replaces Bar 2 cleanly (Clear × + "محدد: X" + single "نسخ" + single "تصدير")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FluentTooltip("إلغاء التحديد (Esc)") {
                        IconButton(
                            onClick = { selectionState = selectionState.clear() },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "إلغاء التحديد", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                        }
                    }
                    Text(
                        text = "محدد: ${selectionState.getFormattedCountArabic()}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FluentCommandButton(
                        text = "نسخ",
                        icon = Icons.Outlined.ContentCopy,
                        tooltip = "نسخ المحدد تلقائياً (Ctrl+C)",
                        onClick = { copySelectionToClipboard(preferFormattedLabels = false) }
                    )

                    FluentCommandButton(
                        text = "نسخ منسق",
                        icon = Icons.Outlined.CopyAll,
                        tooltip = "نسخ المحدد بالتنسيق (Ctrl+Alt+C)",
                        onClick = { copySelectionToClipboard(preferFormattedLabels = true) }
                    )

                    FluentCommandButton(
                        text = "تصدير",
                        icon = Icons.Outlined.FileDownload,
                        tooltip = "تصدير المحدد تلقائياً إلى Excel (.xlsx)",
                        onClick = { exportAutoContextToExcel() }
                    )
                }
            }
        }

        // Indexing Progress Strip (only during active indexing)
        AnimatedVisibility(visible = indexProgress.isIndexing) {
            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = indexProgress.statusMessage.ifBlank { "جارٍ تحديث الفهرس..." },
                            fontSize = 12.5.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (indexProgress.total > 0) {
                            Text("${indexProgress.current} / ${indexProgress.total}", fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                    if (indexProgress.total > 0) {
                        LinearProgressIndicator(
                            progress = { (indexProgress.current.toFloat() / indexProgress.total).coerceIn(0f, 1f) },
                            color = colors.controlActiveBackground,
                            trackColor = colors.divider,
                            modifier = Modifier.fillMaxWidth().height(3.dp)
                        )
                    } else {
                        LinearProgressIndicator(
                            color = colors.controlActiveBackground,
                            trackColor = colors.divider,
                            modifier = Modifier.fillMaxWidth().height(3.dp)
                        )
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            DesktopSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, fontSize = 12.5.sp, color = colors.textPrimary)
                    Text(
                        text = "إغلاق",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { statusMessage = null }
                    )
                }
            }
        }

        // 3. Body Content + Collapsible Details Pane
        when {
            workspaces.isEmpty() -> {
                DesktopSectionCard(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "أضف مجلداً للبدء بالفهرسة والبحث",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "يدعم البحث المحلي ملفات Excel و Word و PDF وقواعد Access والنصوص دون اتصال.",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                            DesktopPrimaryButton(
                                text = "فتح إدارة المجلدات",
                                icon = Icons.Outlined.FolderOpen,
                                onClick = onOpenWorkspaces
                            )
                        }
                    }
                }
            }

            searchQuery.isBlank() -> {
                if (isSearchHistoryEnabled && searchHistory.isNotEmpty()) {
                    DesktopSectionCard(modifier = Modifier.weight(1f)) {
                        Column(modifier = Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("عمليات البحث الأخيرة", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                Text(
                                    text = "مسح الكل",
                                    fontSize = 12.5.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                                        .clickable { onClearSearchHistory() }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            HorizontalDivider(color = colors.divider, thickness = 1.dp)
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                items(searchHistory, key = { it }) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                                            .clickable { onSelectHistoryQuery(item) }
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Outlined.History, null, tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                                            Text(item, fontSize = 14.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                        IconButton(onClick = { onRemoveHistoryItem(item) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Outlined.Close, contentDescription = "حذف", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    DesktopSectionCard(modifier = Modifier.weight(1f)) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "جاهز للبحث الفوري",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$filesCount ملف • $segmentsCount سجل",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            groupedResults.isEmpty() && !isSearching -> {
                DesktopSectionCard(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "لا توجد نتائج مطابقة لعبارة «$searchQuery»",
                            fontSize = 15.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            else -> {
                val displayedCount = effectiveResults.size
                val fileCount = groupedResults.size
                val summaryLine = if (totalMatchesCount > displayedCount) {
                    "تم العثور على $totalMatchesCount نتيجة في $fileCount ملف (معروض $displayedCount)"
                } else {
                    "تم العثور على $totalMatchesCount نتيجة في $fileCount ملف"
                }

                Text(
                    text = summaryLine,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )

                // Split Pane: Results List + Collapsible Details Pane
                Row(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { coords ->
                                if (coords.isAttached) viewportCoordsRef[0] = coords
                            }
                            .focusRequester(resultsFocusRequester)
                            .focusable()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 14.dp)
                        ) {
                            items(groupedResults, key = { it.fileUri }) { group ->
                                val isExpanded = DesktopFileExpansionStateHelper.isExpanded(group.fileUri, collapsedFileUris)
                                DesktopFileResultCard(
                                    group = group,
                                    query = searchQuery,
                                    searchFilter = searchFilter,
                                    isExpanded = isExpanded,
                                    showAllRows = showAllRows,
                                    stretchSingleColumn = stretchSingleColumn,
                                    expandFullCellText = expandFullCellText,
                                    verticalCopyFormat = verticalCopyFormat,
                                    selectionState = selectionState,
                                    expandedSheets = expandedSheets,
                                    viewportBoundsProvider = { viewportCoordsRef[0]?.unclippedBoundsInRoot() },
                                    onRegisterBoundsResolver = { key, fn ->
                                        if (fn == null) boundsResolvers.remove(key) else boundsResolvers[key] = fn
                                    },
                                    onRegisterLiveSheet = { key, model ->
                                        if (model == null) liveSheetModels.remove(key) else liveSheetModels[key] = model
                                    },
                                    onExpandSheet = { sheetKey -> expandedSheets = expandedSheets + sheetKey },
                                    onSelectionStateChange = { newState ->
                                        selectionState = newState
                                        resultsFocusRequester.requestFocus()
                                    },
                                    onRequestKeyboardFocus = { resultsFocusRequester.requestFocus() },
                                    onToggleExpanded = {
                                        collapsedFileUris = DesktopFileExpansionStateHelper.toggleFile(
                                            group.fileUri,
                                            collapsedFileUris
                                        )
                                    },
                                    onOpenFile = {
                                        val res = DesktopSystemActions.openFile(group.fileUri)
                                        if (res.isFailure) {
                                            statusMessage = "تعذر فتح الملف: ${res.exceptionOrNull()?.message}"
                                        }
                                    },
                                    onRevealInExplorer = {
                                        val res = DesktopSystemActions.revealInExplorer(group.fileUri)
                                        if (res.isFailure) {
                                            statusMessage = "تعذر فتح موقع الملف: ${res.exceptionOrNull()?.message}"
                                        }
                                    },
                                    onExportFileToExcel = {
                                        val base = group.fileName.substringBeforeLast('.').ifBlank { "ملف" }
                                        exportResultsToExcel(group.results, base)
                                    },
                                    onShowFileDetails = {
                                        fileDetailsTarget = FileDetailsTarget(
                                            fileUri = group.fileUri,
                                            fileName = group.fileName,
                                            matchesCount = group.totalMatchesCount
                                        )
                                    },
                                    onExportSelection = { exportAutoContextToExcel() },
                                    onLoadMore = { onLoadMoreForFile(group.fileUri) },
                                    onCopyText = { text ->
                                        DesktopSystemActions.copyToClipboard(text)
                                    },
                                    onViewRowDetail = { res -> rowDetailTarget = res },
                                    onViewCellDetail = { cellData ->
                                        cellDetailTarget = CellDetailTarget(
                                            title = cellData.columnName,
                                            subtitle = "${cellData.fileName} • ${cellData.sheetName} • صف ${cellData.rowNumber}",
                                            text = cellData.value,
                                            query = searchQuery
                                        )
                                    },
                                    onViewSnippetDetail = { res ->
                                        cellDetailTarget = CellDetailTarget(
                                            title = res.fileName,
                                            subtitle = res.location,
                                            text = res.snippet,
                                            query = res.query
                                        )
                                    }
                                )
                            }
                        }

                        VerticalScrollbar(
                            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                            adapter = rememberScrollbarAdapter(listState)
                        )
                    }

                    AnimatedVisibility(
                        visible = showDetailsPane,
                        enter = fadeIn() + expandHorizontally(),
                        exit = fadeOut() + shrinkHorizontally()
                    ) {
                        DesktopDetailsPane(
                            preview = selectedPreview,
                            query = searchQuery,
                            searchFilter = searchFilter,
                            verticalCopyFormat = verticalCopyFormat,
                            onCopyText = { text ->
                                DesktopSystemActions.copyToClipboard(text)
                            },
                            onOpenFullRowModal = { res -> rowDetailTarget = res },
                            onClose = { showDetailsPane = false }
                        )
                    }
                }
            }
        }
    }

    rowDetailTarget?.let { res ->
        DesktopRowDetailModal(
            result = res,
            searchFilter = searchFilter,
            verticalCopyFormat = verticalCopyFormat,
            onCopy = { txt ->
                DesktopSystemActions.copyToClipboard(txt)
            },
            onDismiss = { rowDetailTarget = null }
        )
    }

    cellDetailTarget?.let { target ->
        DesktopCellDetailModal(
            title = target.title,
            subtitle = target.subtitle,
            text = target.text,
            query = target.query,
            searchFilter = searchFilter,
            onCopy = { txt ->
                DesktopSystemActions.copyToClipboard(txt)
            },
            onDismiss = { cellDetailTarget = null }
        )
    }

    fileDetailsTarget?.let { target ->
        val record = indexedFilesByUri[target.fileUri]
        val wsName = record?.let { workspacesById[it.workspaceId]?.name }
        DesktopFileDetailsDialog(
            fileName = target.fileName,
            fileUri = target.fileUri,
            matchesCount = target.matchesCount,
            fileRecord = record,
            workspaceName = wsName,
            onOpenFile = { DesktopSystemActions.openFile(target.fileUri) },
            onRevealInExplorer = { DesktopSystemActions.revealInExplorer(target.fileUri) },
            onCopyPath = { path ->
                DesktopSystemActions.copyToClipboard(path)
            },
            onDismiss = { fileDetailsTarget = null }
        )
    }

    if (showFilterDialog) {
        DesktopFilterDialog(
            currentFilter = searchFilter,
            defaultMethod = defaultSearchMethod,
            workspaces = workspaces,
            activeWorkspace = activeWorkspace,
            indexedFiles = indexedFiles,
            onApply = { newFilter ->
                onFilterChange(newFilter)
                showFilterDialog = false
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}

@Composable
private fun FilterChipButton(
    label: String,
    selected: Boolean,
    icon: ImageVector?,
    tint: Color?,
    onClick: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        selected -> colors.controlActiveBackground
        isHovered -> colors.subCardBackground
        else -> colors.cardBackground
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            .background(bg)
            .border(
                1.dp,
                if (selected) colors.controlActiveBackground else colors.divider,
                RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius)
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.controlActiveContent else (tint ?: colors.textSecondary),
                modifier = Modifier.size(14.dp)
            )
        }
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.controlActiveContent else colors.textPrimary
        )
    }
}

@Composable
private fun DesktopFileResultCard(
    group: FileResultGroup,
    query: String,
    searchFilter: SearchFilter,
    isExpanded: Boolean,
    showAllRows: Boolean,
    stretchSingleColumn: Boolean,
    expandFullCellText: Boolean,
    verticalCopyFormat: Boolean,
    selectionState: SelectionState,
    expandedSheets: Set<String>,
    viewportBoundsProvider: (() -> Rect?)? = null,
    onRegisterBoundsResolver: ((String, ((Any) -> Pair<Rect, Float>?)?) -> Unit)? = null,
    onRegisterLiveSheet: ((String, SheetGridModel?) -> Unit)? = null,
    onExpandSheet: (String) -> Unit,
    onSelectionStateChange: (SelectionState) -> Unit,
    onRequestKeyboardFocus: () -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenFile: () -> Unit,
    onRevealInExplorer: () -> Unit,
    onExportFileToExcel: () -> Unit,
    onShowFileDetails: () -> Unit,
    onExportSelection: () -> Unit,
    onLoadMore: () -> Unit = {},
    onCopyText: (String) -> Unit,
    onViewRowDetail: (SearchResult) -> Unit,
    onViewCellDetail: (SelectedCellData) -> Unit,
    onViewSnippetDetail: (SearchResult) -> Unit
) {
    val colors = LocalDesktopColors.current
    val visual = remember(group.fileName) { DesktopFileTypeHelper.getVisual(group.fileName) }
    val topBarBg = visual.color.copy(alpha = if (colors.isDark) 0.16f else 0.09f)
    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 0f else 180f)
    var showHeaderMenu by remember { mutableStateOf(false) }
    var headerMenuPos by remember { mutableStateOf(Offset.Zero) }

    DesktopSectionCard {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(topBarBg)
                    .pointerInput(group.fileUri) {
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.type == PointerEventType.Press && ev.button == PointerButton.Secondary) {
                                    val ch = ev.changes.firstOrNull()
                                    headerMenuPos = ch?.position ?: Offset.Zero
                                    showHeaderMenu = true
                                    ch?.consume()
                                }
                            }
                        }
                    }
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = visual.typeName,
                    tint = visual.color,
                    modifier = Modifier.size(19.dp)
                )

                Text(
                    text = group.fileName,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    val countBadge = if (group.totalMatchesCount > group.results.size) {
                        "${group.results.size} / ${group.totalMatchesCount}"
                    } else {
                        "${group.results.size}"
                    }
                    Text(
                        text = countBadge,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isDark) colors.textPrimary else visual.color
                    )
                }

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طيّ البطاقة" else "فتح البطاقة",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp).rotate(arrowRotation)
                )
            }

            if (showHeaderMenu) {
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset { IntOffset(headerMenuPos.x.roundToInt(), headerMenuPos.y.roundToInt()) }
                        .size(0.dp)
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { showHeaderMenu = false },
                        modifier = Modifier
                            .background(colors.cardBackground)
                            .border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                    ) {
                        DropdownMenuItem(
                            text = { Text("فتح الملف", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onOpenFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("موقع الملف", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onRevealInExplorer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تصدير", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.TableChart, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onExportFileToExcel()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تفاصيل الملف", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.Info, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onShowFileDetails()
                            }
                        )
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                if (group.isDocument) {
                    RenderDocumentSnippets(
                        group = group,
                        query = query,
                        searchFilter = searchFilter,
                        verticalCopyFormat = verticalCopyFormat,
                        selectionState = selectionState,
                        onRegisterBoundsResolver = onRegisterBoundsResolver,
                        onRequestKeyboardFocus = onRequestKeyboardFocus,
                        onSelectionStateChange = onSelectionStateChange,
                        onExportSelection = onExportSelection,
                        onCopyText = onCopyText,
                        onViewDetail = onViewSnippetDetail
                    )
                } else {
                    DesktopExcelTableSection(
                        results = group.results,
                        query = query,
                        searchFilter = searchFilter,
                        showAllRows = showAllRows,
                        accentColor = visual.color,
                        onViewDetail = onViewRowDetail,
                        stretchSingleColumn = stretchSingleColumn,
                        expandFullCellText = expandFullCellText,
                        verticalCopyFormat = verticalCopyFormat,
                        fileUri = group.fileUri,
                        fileName = group.fileName,
                        selectionState = selectionState,
                        onSelectionStateChange = onSelectionStateChange,
                        onCopyText = onCopyText,
                        onViewCellDetail = onViewCellDetail,
                        onExportSelection = onExportSelection,
                        onRequestKeyboardFocus = onRequestKeyboardFocus,
                        expandedSheets = expandedSheets,
                        onExpandSheet = onExpandSheet,
                        viewportBoundsProvider = viewportBoundsProvider,
                        onRegisterBoundsResolver = onRegisterBoundsResolver,
                        onRegisterLiveSheet = onRegisterLiveSheet
                    )
                }

                if (group.hasMoreRows) {
                    val remaining = (group.totalMatchesCount - group.results.size).coerceAtLeast(1)
                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.subCardBackground)
                            .clickable(onClick = onLoadMore)
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "عرض المزيد (+$remaining متبقي)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.controlActiveBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderDocumentSnippets(
    group: FileResultGroup,
    query: String,
    searchFilter: SearchFilter,
    verticalCopyFormat: Boolean,
    selectionState: SelectionState,
    onRegisterBoundsResolver: ((String, ((Any) -> Pair<Rect, Float>?)?) -> Unit)? = null,
    onRequestKeyboardFocus: () -> Unit = {},
    onSelectionStateChange: (SelectionState) -> Unit,
    onExportSelection: () -> Unit,
    onCopyText: (String) -> Unit,
    onViewDetail: (SearchResult) -> Unit
) {
    val colors = LocalDesktopColors.current
    val isUnifiedDoc = remember(group.fileName) { DesktopTableCellHelper.isDocumentType(group.fileName) }
    val mergedDocText = remember(group.results, isUnifiedDoc) {
        if (!isUnifiedDoc) {
            ""
        } else {
            group.results
                .mapNotNull { it.snippet.trim().takeIf { s -> s.isNotEmpty() } }
                .joinToString(" ")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
    val docRows = remember(group, isUnifiedDoc, mergedDocText) {
        if (isUnifiedDoc) {
            if (group.results.isEmpty()) emptyList()
            else listOf(
                SelectedRowData(
                    key = RowKey(group.fileUri, "مستند", 0),
                    fileName = group.fileName,
                    sheetName = "مستند",
                    rowNumber = "1",
                    cells = emptyList(),
                    headers = emptyList(),
                    isDocument = true,
                    snippet = mergedDocText,
                    location = ""
                )
            )
        } else {
            group.results.mapIndexed { idx, res ->
                SelectedRowData(
                    key = RowKey(group.fileUri, "مستند", idx),
                    fileName = group.fileName,
                    sheetName = "مستند",
                    rowNumber = "${idx + 1}",
                    cells = emptyList(),
                    headers = emptyList(),
                    isDocument = true,
                    snippet = res.snippet,
                    location = res.location
                )
            }
        }
    }

    var contextSnippetTarget by remember { mutableStateOf<Pair<Int, Offset>?>(null) }
    val rowCoordsMap = remember(group.fileUri, docRows.size) { HashMap<Int, LayoutCoordinates>() }
    val latestOnRegisterResolver by rememberUpdatedState(onRegisterBoundsResolver)
    val docResolverKey = remember(group.fileUri) { "doc::${group.fileUri}" }
    DisposableEffect(docResolverKey) {
        val reg = latestOnRegisterResolver
        if (reg != null) {
            val resolver: (Any) -> Pair<Rect, Float>? = { targetKey ->
                if (targetKey is RowKey && targetKey.fileUri == group.fileUri && targetKey.sheetName == "مستند") {
                    rowCoordsMap[targetKey.rowIndex]?.unclippedBoundsInRoot()?.let { it to 0f }
                } else {
                    null
                }
            }
            reg(docResolverKey, resolver)
        }
        onDispose {
            latestOnRegisterResolver?.invoke(docResolverKey, null)
        }
    }

    if (isUnifiedDoc) {
        val rowData = docRows.firstOrNull() ?: return
        val firstRes = group.results.firstOrNull() ?: return
        val unifiedRes = remember(firstRes, mergedDocText) {
            firstRes.copy(snippet = mergedDocText, location = group.fileName)
        }
        val isSelected = selectionState.isRowSelected(rowData.key)
        val highlighted = remember(mergedDocText, query, searchFilter, colors.isDark) {
            DesktopTextHighlighter.highlight(
                text = mergedDocText,
                query = query,
                method = searchFilter.method,
                ignoreDiacritics = searchFilter.ignoreDiacritics,
                highlightColor = colors.highlightBg,
                highlightTextColor = colors.highlightFg
            )
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onPlaced { coords -> rowCoordsMap[0] = coords }
                    .background(if (isSelected) colors.controlActiveBackground.copy(alpha = 0.08f) else Color.Transparent)
                    .pointerInput(group.fileUri, mergedDocText) {
                        var lastPressMs = 0L
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                if (ev.type != PointerEventType.Press) continue
                                val ch = ev.changes.firstOrNull() ?: continue
                                onRequestKeyboardFocus()
                                if (ev.button == PointerButton.Secondary) {
                                    if (!selectionState.isRowSelected(rowData.key)) {
                                        onSelectionStateChange(selectionState.selectSingleRow(rowData))
                                    }
                                    contextSnippetTarget = 0 to ch.position
                                    ch.consume()
                                } else if (ev.button == PointerButton.Primary) {
                                    val now = ch.uptimeMillis
                                    val isCtrl = ev.keyboardModifiers.isCtrlPressed || ev.keyboardModifiers.isMetaPressed
                                    val isDouble = (now - lastPressMs) in 1L..349L && !isCtrl
                                    lastPressMs = now
                                    if (isDouble) {
                                        onViewDetail(unifiedRes)
                                        ch.consume()
                                        continue
                                    }
                                    if (isCtrl) {
                                        onSelectionStateChange(selectionState.toggleRow(rowData))
                                    } else {
                                        onSelectionStateChange(selectionState.selectSingleRow(rowData))
                                    }
                                    ch.consume()
                                }
                            }
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = highlighted,
                    fontSize = 13.5.sp,
                    lineHeight = 22.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                FluentTooltip("نسخ النص") {
                    IconButton(onClick = { onCopyText(mergedDocText) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "نسخ النص",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            val activeDocMenu = contextSnippetTarget
            if (activeDocMenu != null && activeDocMenu.first == 0) {
                Box(
                    modifier = Modifier
                        .align(AbsoluteAlignment.TopLeft)
                        .absoluteOffset {
                            IntOffset(activeDocMenu.second.x.roundToInt(), activeDocMenu.second.y.roundToInt())
                        }
                        .size(0.dp)
                ) {
                    DropdownMenu(
                        expanded = true,
                        onDismissRequest = { contextSnippetTarget = null },
                        modifier = Modifier
                            .background(colors.cardBackground)
                            .border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                    ) {
                        val isDocRowSelected = selectionState.isActive &&
                            selectionState.mode == SelectionMode.ROWS &&
                            selectionState.isRowSelected(rowData.key)
                        DropdownMenuItem(
                            text = { Text("نسخ", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                contextSnippetTarget = null
                                val txt = if (isDocRowSelected) {
                                    DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                        selectionState = selectionState,
                                        verticalCopyFormat = verticalCopyFormat,
                                        preferFormattedLabels = false
                                    )
                                } else {
                                    mergedDocText
                                }
                                onCopyText(txt)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("نسخ منسق", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.CopyAll, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                contextSnippetTarget = null
                                val txt = if (isDocRowSelected) {
                                    DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                        selectionState = selectionState,
                                        verticalCopyFormat = verticalCopyFormat,
                                        preferFormattedLabels = true
                                    )
                                } else {
                                    DesktopValueInteractionHelper.formatMultipleRows(
                                        listOf(rowData),
                                        verticalCopyFormat
                                    )
                                }
                                onCopyText(txt)
                            }
                        )
                        if (selectionState.shouldShowPreviewMenuItem()) {
                            DropdownMenuItem(
                                text = { Text("معاينة", fontSize = 13.sp, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    contextSnippetTarget = null
                                    onViewDetail(unifiedRes)
                                }
                            )
                        }
                        if (selectionState.isActive) {
                            DropdownMenuItem(
                                text = { Text("تصدير", fontSize = 13.sp, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    contextSnippetTarget = null
                                    onExportSelection()
                                }
                            )
                        }
                    }
                }
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        group.results.forEachIndexed { idx, res ->
            val rowData = docRows[idx]
            val isSelected = selectionState.isRowSelected(rowData.key)
            val highlighted = remember(res.snippet, query, searchFilter, colors.isDark) {
                DesktopTextHighlighter.highlight(
                    text = res.snippet,
                    query = query,
                    method = searchFilter.method,
                    ignoreDiacritics = searchFilter.ignoreDiacritics,
                    highlightColor = colors.highlightBg,
                    highlightTextColor = colors.highlightFg
                )
            }

            Box(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onPlaced { coords -> rowCoordsMap[idx] = coords }
                        .background(if (isSelected) colors.controlActiveBackground.copy(alpha = 0.08f) else Color.Transparent)
                        .pointerInput(group.fileUri, idx) {
                            var lastPressMs = 0L
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    if (ev.type != PointerEventType.Press) continue
                                    val ch = ev.changes.firstOrNull() ?: continue
                                    onRequestKeyboardFocus()
                                    if (ev.button == PointerButton.Secondary) {
                                        if (!selectionState.isRowSelected(rowData.key)) {
                                            onSelectionStateChange(selectionState.selectSingleRow(rowData))
                                        }
                                        contextSnippetTarget = idx to ch.position
                                        ch.consume()
                                    } else if (ev.button == PointerButton.Primary) {
                                        val now = ch.uptimeMillis
                                        val isCtrl = ev.keyboardModifiers.isCtrlPressed || ev.keyboardModifiers.isMetaPressed
                                        val isDouble = (now - lastPressMs) in 1L..349L && !isCtrl
                                        lastPressMs = now
                                        if (isDouble) {
                                            onViewDetail(res)
                                            ch.consume()
                                            continue
                                        }
                                        if (isCtrl) {
                                            onSelectionStateChange(selectionState.toggleRow(rowData))
                                        } else {
                                            onSelectionStateChange(selectionState.selectSingleRow(rowData))
                                        }
                                        ch.consume()
                                    }
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) colors.controlActiveBackground else colors.subCardBackground)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = res.location,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) colors.controlActiveContent else colors.textSecondary
                        )
                    }

                    Text(
                        text = highlighted,
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f)
                    )

                    FluentTooltip("نسخ المقتطف") {
                        IconButton(onClick = { onCopyText(res.snippet) }, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Outlined.ContentCopy,
                                contentDescription = "نسخ المقتطف",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                val activeDocMenu = contextSnippetTarget
                if (activeDocMenu != null && activeDocMenu.first == idx) {
                    Box(
                        modifier = Modifier
                            .align(AbsoluteAlignment.TopLeft)
                            .absoluteOffset {
                                IntOffset(activeDocMenu.second.x.roundToInt(), activeDocMenu.second.y.roundToInt())
                            }
                            .size(0.dp)
                    ) {
                        DropdownMenu(
                            expanded = true,
                            onDismissRequest = { contextSnippetTarget = null },
                            modifier = Modifier
                                .background(colors.cardBackground)
                                .border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                        ) {
                            val isDocRowSelected = selectionState.isActive &&
                                selectionState.mode == SelectionMode.ROWS &&
                                selectionState.isRowSelected(rowData.key)
                            DropdownMenuItem(
                                text = { Text("نسخ", fontSize = 13.sp, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    contextSnippetTarget = null
                                    val txt = if (isDocRowSelected) {
                                        DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                            selectionState = selectionState,
                                            verticalCopyFormat = verticalCopyFormat,
                                            preferFormattedLabels = false
                                        )
                                    } else {
                                        res.snippet
                                    }
                                    onCopyText(txt)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("نسخ منسق", fontSize = 13.sp, color = colors.textPrimary) },
                                leadingIcon = { Icon(Icons.Outlined.CopyAll, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                onClick = {
                                    contextSnippetTarget = null
                                    val txt = if (isDocRowSelected) {
                                        DesktopValueInteractionHelper.formatSelectionForKeyboardCopy(
                                            selectionState = selectionState,
                                            verticalCopyFormat = verticalCopyFormat,
                                            preferFormattedLabels = true
                                        )
                                    } else {
                                        DesktopValueInteractionHelper.formatMultipleRows(
                                            listOf(rowData),
                                            verticalCopyFormat
                                        )
                                    }
                                    onCopyText(txt)
                                }
                            )
                            if (selectionState.shouldShowPreviewMenuItem()) {
                                DropdownMenuItem(
                                    text = { Text("معاينة", fontSize = 13.sp, color = colors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                    onClick = {
                                        contextSnippetTarget = null
                                        onViewDetail(res)
                                    }
                                )
                            }
                            if (selectionState.isActive) {
                                DropdownMenuItem(
                                    text = { Text("تصدير", fontSize = 13.sp, color = colors.textPrimary) },
                                    leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                                    onClick = {
                                        contextSnippetTarget = null
                                        onExportSelection()
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (idx < group.results.lastIndex) {
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopDetailsPane(
    preview: SelectedPreviewInfo?,
    query: String,
    searchFilter: SearchFilter,
    verticalCopyFormat: Boolean,
    onCopyText: (String) -> Unit,
    onOpenFullRowModal: (SearchResult) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalDesktopColors.current

    Column(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(DesktopDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.CardCornerRadius))
    ) {
        // Header bar of Details Pane
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.subCardBackground)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerticalSplit,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "لوحة التفاصيل",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            FluentTooltip("إغلاق لوحة التفاصيل (Alt+Enter)") {
                IconButton(onClick = onClose, modifier = Modifier.size(26.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "إغلاق لوحة التفاصيل",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        HorizontalDivider(color = colors.divider, thickness = 1.dp)

        if (preview == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "لم يتم تحديد خلية أو صف",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }
        } else {
            val visual = remember(preview.fileName) { DesktopFileTypeHelper.getVisual(preview.fileName) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // File Header (Open/Explorer actions live in menus to avoid button duplication)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                        .background(colors.subCardBackground)
                        .border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = preview.fileName,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = preview.subtitle,
                            fontSize = 11.5.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Active / Focused Value Box
                val highlightedFocused = remember(preview.previewText, query, searchFilter, colors.isDark) {
                    DesktopTextHighlighter.highlight(
                        text = preview.previewText,
                        query = query,
                        method = searchFilter.method,
                        ignoreDiacritics = searchFilter.ignoreDiacritics,
                        highlightColor = colors.highlightBg,
                        highlightTextColor = colors.highlightFg
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                        .background(colors.controlActiveBackground.copy(alpha = if (colors.isDark) 0.12f else 0.05f))
                        .border(1.5.dp, colors.controlActiveBackground, RoundedCornerShape(DesktopDesignTokens.SubCardCornerRadius))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (preview.activeCellData != null) preview.activeCellData.columnName else "المحتوى المحدد",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        FluentTooltip("نسخ") {
                            IconButton(onClick = { onCopyText(preview.previewText) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Outlined.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    SelectionContainer {
                        if (preview.previewText.isEmpty()) {
                            Text(
                                text = "—",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        } else {
                            Text(
                                text = highlightedFocused,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                // Extracted Smart Contacts & Links
                val contactSource = if (preview.isDocument || preview.cells.isEmpty()) {
                    preview.previewText
                } else {
                    preview.cells.joinToString(" ")
                }
                val phones = remember(contactSource) { DesktopValueInteractionHelper.extractPhoneNumbers(contactSource) }
                val urls = remember(contactSource) { DesktopValueInteractionHelper.extractUrls(contactSource) }
                val emails = remember(contactSource) { DesktopValueInteractionHelper.extractEmails(contactSource) }

                if (phones.isNotEmpty() || urls.isNotEmpty() || emails.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        phones.forEach { phone ->
                            FluentCommandButton(
                                text = phone,
                                icon = Icons.Outlined.Phone,
                                tooltip = "نسخ رقم الهاتف",
                                onClick = { onCopyText(phone) }
                            )
                        }
                        urls.forEach { url ->
                            FluentCommandButton(
                                text = url,
                                icon = Icons.Outlined.Language,
                                tooltip = "فتح الرابط في المتصفح",
                                onClick = { DesktopSystemActions.openUrl(url) }
                            )
                        }
                        emails.forEach { email ->
                            FluentCommandButton(
                                text = email,
                                icon = Icons.Outlined.Email,
                                tooltip = "مراسلة / نسخ البريد",
                                onClick = {
                                    val res = DesktopSystemActions.openMailto(email)
                                    if (res.isFailure) onCopyText(email)
                                }
                            )
                        }
                    }
                }

                // All Row Fields vertically (for Excel / CSV / Access tabular rows)
                if (!preview.isDocument && preview.cells.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "حقول السجل (${preview.cells.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FluentCommandButton(
                                text = "نسخ السجل",
                                icon = Icons.Outlined.ContentCopy,
                                tooltip = "نسخ جميع حقول السجل",
                                onClick = {
                                    onCopyText(
                                        DesktopValueInteractionHelper.formatRow(
                                            cells = preview.cells,
                                            headers = preview.headers,
                                            vertical = verticalCopyFormat
                                        )
                                    )
                                }
                            )
                            preview.matchingResult?.let { matched ->
                                FluentCommandButton(
                                    text = "نافذة",
                                    icon = Icons.Outlined.Visibility,
                                    tooltip = "فتح السجل في نافذة مستقلة",
                                    onClick = { onOpenFullRowModal(matched) }
                                )
                            }
                        }
                    }

                    preview.cells.forEachIndexed { colIdx, value ->
                        val headerName = preview.headers.getOrNull(colIdx)?.takeIf { it.isNotBlank() } ?: "العمود ${colIdx + 1}"
                        val isActiveCol = preview.activeColIndex == colIdx
                        val fieldHighlighted = remember(value, query, searchFilter, colors.isDark) {
                            DesktopTextHighlighter.highlight(
                                text = value,
                                query = query,
                                method = searchFilter.method,
                                ignoreDiacritics = searchFilter.ignoreDiacritics,
                                highlightColor = colors.highlightBg,
                                highlightTextColor = colors.highlightFg
                            )
                        }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                                .background(
                                    if (isActiveCol) colors.controlActiveBackground.copy(alpha = if (colors.isDark) 0.14f else 0.07f)
                                    else colors.subCardBackground
                                )
                                .border(
                                    width = if (isActiveCol) 1.5.dp else 1.dp,
                                    color = if (isActiveCol) colors.controlActiveBackground else colors.divider,
                                    shape = RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius)
                                )
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = headerName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActiveCol) colors.textPrimary else colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onCopyText(value) },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(13.dp))
                                }
                            }
                            SelectionContainer {
                                if (value.isEmpty()) {
                                    Text(
                                        text = "—",
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                } else {
                                    Text(
                                        text = fieldHighlighted,
                                        fontSize = 12.5.sp,
                                        lineHeight = 18.sp,
                                        color = colors.textPrimary
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
