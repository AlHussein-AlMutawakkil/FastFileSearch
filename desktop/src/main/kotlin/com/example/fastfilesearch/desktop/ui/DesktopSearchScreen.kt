@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package com.example.fastfilesearch.desktop.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.export.DesktopExcelExporter
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
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

private data class DesktopActiveFilterChip(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val tint: Color? = null,
    val onRemove: () -> Unit
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopSearchScreen(
    searchQuery: String,
    searchFilter: SearchFilter,
    defaultSearchMethod: SearchMethod,
    searchResults: List<SearchResult>,
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
    onOpenWorkspaces: () -> Unit
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

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showMethodMenu by remember { mutableStateOf(false) }
    var showWorkspaceMenu by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    var rowDetailTarget by remember { mutableStateOf<SearchResult?>(null) }
    var cellDetailTarget by remember { mutableStateOf<CellDetailTarget?>(null) }
    var fileDetailsTarget by remember { mutableStateOf<FileDetailsTarget?>(null) }

    LaunchedEffect(searchQuery, searchResults) {
        collapsedFileUris = emptySet()
        expandedSheets = emptySet()
        selectionState = selectionState.clear()
    }

    val groupedResults = remember(searchResults) {
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

    val indexedFilesByUri = remember(indexedFiles) { indexedFiles.associateBy { it.uri } }
    val workspacesById = remember(workspaces) { workspaces.associateBy { it.id } }

    val allSheetModels = remember(groupedResults, showAllRows, expandedSheets) {
        groupedResults.flatMap { group ->
            DesktopGridNavigator.buildSheetModelsForGroup(group, showAllRows, expandedSheets)
        }
    }

    val allSelectableRows = remember(groupedResults) {
        groupedResults.flatMap { group ->
            DesktopTableCellHelper.buildSelectableRowsForFile(
                fileUri = group.fileUri,
                fileName = group.fileName,
                isDocument = group.isDocument,
                results = group.results
            )
        }
    }

    val allSelectableCells = remember(allSheetModels) {
        allSheetModels.flatMap { it.buildAllCells() }
    }

    // Ensure card holding activeCell scrolls into view when navigating via keyboard arrows
    val activeCellUri = selectionState.activeCell?.fileUri
    LaunchedEffect(selectionState.activeCell) {
        val targetUri = activeCellUri ?: return@LaunchedEffect
        val itemIdx = groupedResults.indexOfFirst { it.fileUri == targetUri }
        if (itemIdx >= 0) {
            val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == itemIdx }
            if (!isVisible) {
                listState.animateScrollToItem(itemIdx)
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
            statusMessage = "تم نسخ ${selectionState.getFormattedCountArabic()} إلى الحافظة."
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .onPreviewKeyEvent { keyEvent ->
                if (keyEvent.type != KeyEventType.KeyDown || hasAnyModalOpen) return@onPreviewKeyEvent false
                val isCtrl = keyEvent.isCtrlPressed || keyEvent.isMetaPressed

                if (isCtrl && keyEvent.key == Key.F) {
                    searchBoxFocusRequester.requestFocus()
                    return@onPreviewKeyEvent true
                }

                if (isSearchBoxFocused) return@onPreviewKeyEvent false

                when {
                    isCtrl && keyEvent.key == Key.C && selectionState.isActive -> {
                        copySelectionToClipboard(preferFormattedLabels = false)
                        true
                    }
                    isCtrl && keyEvent.key == Key.A && groupedResults.isNotEmpty() -> {
                        val active = selectionState.activeCell
                        val activeSheet = if (active != null) {
                            allSheetModels.firstOrNull { it.fileUri == active.fileUri && it.sheetName == active.sheetName }
                        } else {
                            null
                        }
                        selectionState = if (activeSheet != null) {
                            selectionState.selectAllCells(activeSheet.buildAllCells())
                        } else if (allSelectableRows.isNotEmpty()) {
                            selectionState.selectAllRows(allSelectableRows)
                        } else {
                            selectionState
                        }
                        true
                    }
                    keyEvent.key == Key.Escape && selectionState.isActive -> {
                        selectionState = selectionState.clear()
                        true
                    }
                    keyEvent.key == Key.DirectionUp && allSheetModels.isNotEmpty() -> {
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = allSheetModels,
                            currentState = selectionState,
                            direction = GridNavDirection.UP,
                            isShiftPressed = keyEvent.isShiftPressed,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionDown && allSheetModels.isNotEmpty() -> {
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = allSheetModels,
                            currentState = selectionState,
                            direction = GridNavDirection.DOWN,
                            isShiftPressed = keyEvent.isShiftPressed,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionLeft && allSheetModels.isNotEmpty() -> {
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = allSheetModels,
                            currentState = selectionState,
                            direction = GridNavDirection.LEFT,
                            isShiftPressed = keyEvent.isShiftPressed,
                            isRtl = isRtl
                        )
                        true
                    }
                    keyEvent.key == Key.DirectionRight && allSheetModels.isNotEmpty() -> {
                        selectionState = DesktopGridNavigator.navigateArrow(
                            sheets = allSheetModels,
                            currentState = selectionState,
                            direction = GridNavDirection.RIGHT,
                            isShiftPressed = keyEvent.isShiftPressed,
                            isRtl = isRtl
                        )
                        true
                    }
                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && selectionState.isActive -> {
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
                                return@onPreviewKeyEvent true
                            }
                        }
                        if (selectionState.mode == SelectionMode.ROWS && selectionState.selectedRows.size == 1) {
                            val rowData = selectionState.selectedRows.values.first()
                            if (rowData.isDocument) {
                                cellDetailTarget = CellDetailTarget(
                                    title = rowData.fileName,
                                    subtitle = rowData.location,
                                    text = rowData.snippet,
                                    query = searchQuery
                                )
                            } else {
                                val res = searchResults.firstOrNull {
                                    it.fileUri == rowData.key.fileUri &&
                                        (it.sheetName ?: DesktopTableCellHelper.extractSheetAndRow(it.location).first) == rowData.sheetName &&
                                        (it.rowNumber?.toString() ?: DesktopTableCellHelper.extractSheetAndRow(it.location).second) == rowData.rowNumber
                                }
                                if (res != null) rowDetailTarget = res
                            }
                            return@onPreviewKeyEvent true
                        }
                        false
                    }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 0. Top Context Header: Workspace Switcher + Index Stats + Refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box {
                    val wsLabel = activeWorkspace?.name ?: if (workspaces.isNotEmpty()) {
                        "جميع المجلدات (${workspaces.size})"
                    } else {
                        "بحث آمن"
                    }
                    DesktopSecondaryButton(
                        text = wsLabel,
                        icon = Icons.Outlined.FolderOpen,
                        enabled = workspaces.isNotEmpty(),
                        onClick = { showWorkspaceMenu = true }
                    )
                    DropdownMenu(
                        expanded = showWorkspaceMenu,
                        onDismissRequest = { showWorkspaceMenu = false },
                        modifier = Modifier
                            .background(colors.cardBackground)
                            .border(0.5.dp, colors.divider, RoundedCornerShape(8.dp))
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

                if (workspaces.isNotEmpty()) {
                    Text(
                        text = "$filesCount ملف مفهرس • $segmentsCount مقطع",
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
            }

            DesktopSecondaryButton(
                text = if (indexProgress.isIndexing) "جارٍ التحديث..." else "تحديث الفهرس",
                icon = Icons.Outlined.Refresh,
                enabled = workspaces.isNotEmpty() && !indexProgress.isIndexing,
                onClick = onRefreshIndex
            )
        }

        // Indexing Progress Strip inside Search tab
        AnimatedVisibility(visible = indexProgress.isIndexing) {
            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp))
                    }
                }
            }
        }

        // 1. Search Box + Advanced Filter Button + Method Selector + Diacritics Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.cardBackground)
                    .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "بحث",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )

                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = TextStyle(color = colors.textPrimary, fontSize = 15.sp),
                    cursorBrush = SolidColor(colors.textPrimary),
                    modifier = Modifier
                        .weight(1f)
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
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "ابحث في المستندات، Excel، Access، PDF، والنصوص... (Ctrl+F)",
                                color = colors.textSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        }
                        inner()
                    }
                )

                if (isSearching) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }

                if (searchQuery.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            // Advanced Filter Button (for files, workspaces, method, diacritics)
            val isFilterActive = searchFilter.isFilterActive(defaultSearchMethod)
            Box {
                DesktopSecondaryButton(
                    text = "التصفية",
                    icon = Icons.Default.FilterList,
                    onClick = { showFilterDialog = true }
                )
                if (isFilterActive) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = (-4).dp, y = 4.dp)
                            .clip(CircleShape)
                            .background(colors.controlActiveBackground)
                    )
                }
            }

            // Search Method Selector
            Box {
                DesktopSecondaryButton(
                    text = searchFilter.method.displayName,
                    icon = Icons.Outlined.KeyboardArrowDown,
                    onClick = { showMethodMenu = true }
                )
                DropdownMenu(
                    expanded = showMethodMenu,
                    onDismissRequest = { showMethodMenu = false },
                    modifier = Modifier.background(colors.cardBackground)
                ) {
                    SearchMethod.entries.forEach { method ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(method.displayName, fontWeight = FontWeight.SemiBold, color = colors.textPrimary, fontSize = 13.sp)
                                    Text(method.description, color = colors.textSecondary, fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                showMethodMenu = false
                                onFilterChange(searchFilter.copy(method = method))
                            }
                        )
                    }
                }
            }

            // Ignore Diacritics & Hamza Toggle
            DesktopSecondaryButton(
                text = if (searchFilter.ignoreDiacritics) "تجاهل التشكيل والهمزات" else "مطابقة التشكيل",
                icon = Icons.Outlined.Spellcheck,
                onClick = {
                    onFilterChange(searchFilter.copy(ignoreDiacritics = !searchFilter.ignoreDiacritics))
                }
            )
        }

        // 2. Quick File Type Filter Strip (All 8 groups)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allSelected = searchFilter.selectedExtensions.isEmpty()
            FilterChipButton(
                label = "الكل",
                selected = allSelected,
                icon = null,
                tint = null,
                onClick = { onFilterChange(searchFilter.copy(selectedExtensions = emptySet())) }
            )

            SupportedFileTypeGroups.ALL_GROUPS.forEach { group ->
                val visual = DesktopFileTypeHelper.getVisualForGroup(group)
                val isSelected = group.extensions.all { it in searchFilter.selectedExtensions } &&
                    searchFilter.selectedExtensions.isNotEmpty()
                FilterChipButton(
                    label = group.displayName,
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

        // 2b. Active Filters Strip (when advanced filters, workspaces, or files are active)
        if (searchFilter.isFilterActive(defaultSearchMethod)) {
            val activeChips = remember(searchFilter, defaultSearchMethod, workspacesById, indexedFilesByUri) {
                buildList {
                    if (searchFilter.method != defaultSearchMethod) {
                        add(DesktopActiveFilterChip("method", searchFilter.method.displayName, Icons.Default.Search) {
                            onFilterChange(searchFilter.copy(method = defaultSearchMethod))
                        })
                    }
                    if (!searchFilter.ignoreDiacritics) {
                        add(DesktopActiveFilterChip("diac", "مطابقة التشكيل", Icons.Outlined.Spellcheck) {
                            onFilterChange(searchFilter.copy(ignoreDiacritics = true))
                        })
                    }
                    if (activeWorkspace == null) {
                        searchFilter.selectedWorkspaceIds.forEach { wsId ->
                            val wsName = workspacesById[wsId]?.name ?: "مجلد"
                            add(DesktopActiveFilterChip("ws_$wsId", wsName, Icons.Outlined.FolderOpen) {
                                onFilterChange(searchFilter.copy(selectedWorkspaceIds = searchFilter.selectedWorkspaceIds - wsId))
                            })
                        }
                    }
                    if (searchFilter.selectedFileUris.isNotEmpty()) {
                        val label = if (searchFilter.selectedFileUris.size == 1) {
                            indexedFilesByUri[searchFilter.selectedFileUris.first()]?.fileName ?: "ملف محدد"
                        } else {
                            "${searchFilter.selectedFileUris.size} ملفات محددة"
                        }
                        add(DesktopActiveFilterChip("files", label, Icons.Outlined.Description) {
                            onFilterChange(searchFilter.copy(selectedFileUris = emptySet()))
                        })
                    }
                }
            }
            if (activeChips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    activeChips.forEach { chip ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.subCardBackground)
                                .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                                .clickable { chip.onRemove() }
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(chip.icon, null, tint = chip.tint ?: colors.textSecondary, modifier = Modifier.size(14.dp))
                            Text(chip.label, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Outlined.Close, contentDescription = "إزالة", tint = colors.textSecondary, modifier = Modifier.size(13.dp))
                        }
                    }
                    Text(
                        text = "إعادة ضبط الفلاتر",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onFilterChange(SearchFilter(method = defaultSearchMethod)) }
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }

        // Contextual Selection Action Bar (PC-friendly toolbar when cells or rows are selected)
        AnimatedVisibility(visible = selectionState.isActive) {
            DesktopSectionCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        IconButton(
                            onClick = { selectionState = selectionState.clear() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "إلغاء التحديد", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        }
                        Text(
                            text = "تم تحديد ${selectionState.getFormattedCountArabic()}",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "• أسهم: تنقل • Shift+أسهم/سحب: نطاق • Ctrl+C: نسخ • Double-Click/Enter: تفاصيل",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DesktopSecondaryButton(
                            text = "تحديد الكل",
                            icon = Icons.Outlined.SelectAll,
                            onClick = {
                                selectionState = if (selectionState.mode == SelectionMode.CELLS) {
                                    selectionState.selectAllCells(allSelectableCells)
                                } else {
                                    selectionState.selectAllRows(allSelectableRows)
                                }
                            }
                        )
                        DesktopSecondaryButton(
                            text = "نسخ (Ctrl+C)",
                            icon = Icons.Outlined.ContentCopy,
                            onClick = { copySelectionToClipboard(preferFormattedLabels = false) }
                        )
                        DesktopSecondaryButton(
                            text = "نسخ منسّق",
                            icon = Icons.Outlined.ContentCopy,
                            onClick = { copySelectionToClipboard(preferFormattedLabels = true) }
                        )
                        DesktopPrimaryButton(
                            text = "تصدير المحدد إلى Excel",
                            icon = Icons.Outlined.TableChart,
                            onClick = { exportSelectionToExcel() }
                        )
                    }
                }
            }
        }

        statusMessage?.let { msg ->
            DesktopSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, fontSize = 13.sp, color = colors.textPrimary)
                    Text(
                        text = "إغلاق",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { statusMessage = null }
                    )
                }
            }
        }

        // 3. Body Content
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
                                        .clip(RoundedCornerShape(6.dp))
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSelectHistoryQuery(item) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                    text = "$filesCount ملف مفهرس • $segmentsCount سجل ومقطع في قاعدة البيانات المحلية",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            searchResults.isEmpty() && !isSearching -> {
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
                val resultCount = searchResults.size
                val fileCount = groupedResults.size

                // Results Toolbar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "تم العثور على $resultCount نتيجة في $fileCount ملف",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textSecondary
                    )

                    if (resultCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DesktopSecondaryButton(
                                text = toggleAllDescription,
                                icon = if (anyExpanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                                onClick = {
                                    collapsedFileUris = DesktopFileExpansionStateHelper.toggleAll(
                                        visibleFileUris = visibleFileUris,
                                        collapsedFileUris = collapsedFileUris
                                    )
                                }
                            )

                            DesktopSecondaryButton(
                                text = "تصدير الكل إلى Excel",
                                icon = Icons.Outlined.FileDownload,
                                onClick = { exportResultsToExcel(searchResults, "بحث_آمن") }
                            )
                        }
                    }
                }

                // Scrollable Results List + VerticalScrollbar (Focusable for keyboard navigation)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .focusRequester(resultsFocusRequester)
                        .focusable()
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
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
                                        matchesCount = group.results.size
                                    )
                                },
                                onExportSelection = { exportSelectionToExcel() },
                                onCopyText = { text ->
                                    DesktopSystemActions.copyToClipboard(text)
                                    statusMessage = "تم النسخ إلى الحافظة."
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
                statusMessage = "تم النسخ إلى الحافظة."
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
                statusMessage = "تم النسخ إلى الحافظة."
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
                statusMessage = "تم نسخ مسار الملف."
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
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) colors.controlActiveBackground else colors.cardBackground)
            .border(
                1.dp,
                if (selected) colors.controlActiveBackground else colors.divider,
                RoundedCornerShape(9.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) colors.controlActiveContent else (tint ?: colors.textSecondary),
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = label,
            fontSize = 12.sp,
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
    onExpandSheet: (String) -> Unit,
    onSelectionStateChange: (SelectionState) -> Unit,
    onRequestKeyboardFocus: () -> Unit,
    onToggleExpanded: () -> Unit,
    onOpenFile: () -> Unit,
    onRevealInExplorer: () -> Unit,
    onExportFileToExcel: () -> Unit,
    onShowFileDetails: () -> Unit,
    onExportSelection: () -> Unit,
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
                                    showHeaderMenu = true
                                    ev.changes.firstOrNull()?.consume()
                                }
                            }
                        }
                    }
                    .clickable(onClick = onToggleExpanded)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = visual.typeName,
                    tint = visual.color,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = visual.typeName,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(visual.color.copy(alpha = if (colors.isDark) 0.22f else 0.14f))
                        .padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${group.results.size}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (colors.isDark) colors.textPrimary else visual.color
                    )
                }

                IconButton(onClick = onOpenFile, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح الملف",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(onClick = onRevealInExplorer, modifier = Modifier.size(30.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = "فتح موقع الملف",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box {
                    IconButton(onClick = { showHeaderMenu = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "خيارات الملف",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showHeaderMenu,
                        onDismissRequest = { showHeaderMenu = false },
                        modifier = Modifier
                            .background(colors.cardBackground)
                            .border(0.5.dp, colors.divider, RoundedCornerShape(8.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("فتح الملف الأصلي", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onOpenFile()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("فتح موقع الملف", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.FolderOpen, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                showHeaderMenu = false
                                onRevealInExplorer()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("تصدير هذا الملف إلى Excel (.xlsx)", fontSize = 13.sp, color = colors.textPrimary) },
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

                Icon(
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "طيّ البطاقة" else "فتح البطاقة",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp).rotate(arrowRotation)
                )
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
                        selectionState = selectionState,
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
                        onExpandSheet = onExpandSheet
                    )
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
    selectionState: SelectionState,
    onSelectionStateChange: (SelectionState) -> Unit,
    onExportSelection: () -> Unit,
    onCopyText: (String) -> Unit,
    onViewDetail: (SearchResult) -> Unit
) {
    val colors = LocalDesktopColors.current
    val docRows = remember(group) {
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

    var contextSnippetIdx by remember { mutableStateOf<Int?>(null) }

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
                        .background(if (isSelected) colors.controlActiveBackground.copy(alpha = 0.08f) else Color.Transparent)
                        .pointerInput(group.fileUri, idx) {
                            var lastPressMs = 0L
                            awaitPointerEventScope {
                                while (true) {
                                    val ev = awaitPointerEvent()
                                    if (ev.type != PointerEventType.Press) continue
                                    val ch = ev.changes.firstOrNull() ?: continue
                                    if (ev.button == PointerButton.Secondary) {
                                        if (!selectionState.isRowSelected(rowData.key)) {
                                            onSelectionStateChange(selectionState.selectSingleRow(rowData))
                                        }
                                        contextSnippetIdx = idx
                                        ch.consume()
                                    } else if (ev.button == PointerButton.Primary) {
                                        val now = ch.uptimeMillis
                                        val isDouble = (now - lastPressMs) in 1L..349L &&
                                            !ev.keyboardModifiers.isShiftPressed &&
                                            !ev.keyboardModifiers.isCtrlPressed &&
                                            !ev.keyboardModifiers.isMetaPressed
                                        lastPressMs = now
                                        if (isDouble) {
                                            onViewDetail(res)
                                            ch.consume()
                                            continue
                                        }
                                        val isShift = ev.keyboardModifiers.isShiftPressed
                                        val isCtrl = ev.keyboardModifiers.isCtrlPressed || ev.keyboardModifiers.isMetaPressed
                                        when {
                                            isShift -> {
                                                val anchor = selectionState.anchorRow
                                                    ?.takeIf { it.fileUri == group.fileUri && it.sheetName == "مستند" }
                                                    ?: rowData.key
                                                val minIdx = minOf(anchor.rowIndex, idx)
                                                val maxIdx = maxOf(anchor.rowIndex, idx)
                                                val range = docRows.subList(minIdx, maxIdx + 1)
                                                onSelectionStateChange(selectionState.selectRowRange(range, rowData.key, anchor, additive = isCtrl))
                                            }
                                            isCtrl -> onSelectionStateChange(selectionState.toggleRow(rowData))
                                            else -> onSelectionStateChange(selectionState.selectSingleRow(rowData))
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

                    IconButton(onClick = { onCopyText(res.snippet) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "نسخ المقتطف",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = contextSnippetIdx == idx,
                    onDismissRequest = { contextSnippetIdx = null },
                    modifier = Modifier
                        .background(colors.cardBackground)
                        .border(0.5.dp, colors.divider, RoundedCornerShape(8.dp))
                ) {
                    DropdownMenuItem(
                        text = { Text("عرض التفاصيل", fontSize = 13.sp, color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.Visibility, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                        onClick = {
                            contextSnippetIdx = null
                            onViewDetail(res)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("نسخ الفقرة", fontSize = 13.sp, color = colors.textPrimary) },
                        leadingIcon = { Icon(Icons.Outlined.ContentCopy, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                        onClick = {
                            contextSnippetIdx = null
                            onCopyText(res.snippet)
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(if (isSelected) "إلغاء التحديد" else "تحديد الفقرة", fontSize = 13.sp, color = colors.textPrimary)
                        },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                        onClick = {
                            contextSnippetIdx = null
                            onSelectionStateChange(selectionState.toggleRow(rowData))
                        }
                    )
                    if (selectionState.isActive) {
                        DropdownMenuItem(
                            text = { Text("تصدير المحدد إلى Excel", fontSize = 13.sp, color = colors.textPrimary) },
                            leadingIcon = { Icon(Icons.Outlined.FileDownload, null, tint = colors.textSecondary, modifier = Modifier.size(17.dp)) },
                            onClick = {
                                contextSnippetIdx = null
                                onExportSelection()
                            }
                        )
                    }
                }
            }
            if (idx < group.results.lastIndex) {
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
            }
        }
    }
}
