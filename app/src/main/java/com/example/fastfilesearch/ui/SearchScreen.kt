package com.example.fastfilesearch.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.ui.components.EmptyFolderPrompt
import com.example.fastfilesearch.ui.components.FileResultGroup
import com.example.fastfilesearch.ui.components.FilterSheet
import com.example.fastfilesearch.ui.components.NoResultsPrompt
import com.example.fastfilesearch.ui.components.OfficialExcelTableCard
import com.example.fastfilesearch.ui.components.ReadyToSearchPrompt
import com.example.fastfilesearch.ui.components.SettingsDialog
import com.example.fastfilesearch.ui.components.WordPdfGroupedCard
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onQueryChanged: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenFile: (String) -> Unit,
    onDismissMessage: () -> Unit,
    onFilterChanged: (SearchFilter) -> Unit = {},
    onToggleStretchSingleColumn: (Boolean) -> Unit = {},
    onToggleShowAllRows: (Boolean) -> Unit = {},
    onToggleVerticalCopyFormat: (Boolean) -> Unit = {},
    onToggleExpandFullCellText: (Boolean) -> Unit = {},
    onSetAppTheme: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.userMessage) {
        val msg = uiState.userMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            onDismissMessage()
        }
    }

    // Group results by file URI: Word/PDF use Grouped Card, Excel/Text use Official Excel Grid
    val groupedResults = remember(uiState.searchResults) {
        uiState.searchResults
            .groupBy { it.fileUri }
            .map { (uri, results) ->
                val fileName = results.firstOrNull()?.fileName ?: ""
                FileResultGroup(
                    fileUri = uri,
                    fileName = fileName,
                    isDocument = TableCellHelper.isDocumentType(fileName),
                    results = results
                )
            }
    }

    val isDark = when (uiState.appTheme) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // Settings Modal Dialog wrapped in RTL Provider to ensure correct layout direction on all devices
        if (showSettingsDialog) {
            SettingsDialog(
                uiState = uiState,
                onSelectFolderClick = onSelectFolderClick,
                onRefreshClick = onRefreshClick,
                onToggleStretchSingleColumn = onToggleStretchSingleColumn,
                onToggleShowAllRows = onToggleShowAllRows,
                onToggleVerticalCopyFormat = onToggleVerticalCopyFormat,
                onToggleExpandFullCellText = onToggleExpandFullCellText,
                onSetAppTheme = onSetAppTheme,
                onDismiss = { showSettingsDialog = false }
            )
        }

        // Monochromatic Filter Bottom Sheet
        if (showFilterSheet) {
            FilterSheet(
                uiState = uiState,
                isDark = isDark,
                onApply = { newFilter ->
                    onFilterChanged(newFilter)
                    showFilterSheet = false
                },
                onDismiss = {
                    showFilterSheet = false
                }
            )
        }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (uiState.folderName.isNotBlank()) uiState.folderName else "البحث السريع في الملفات",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (uiState.folderUri != null) {
                                    Text(
                                        text = "${uiState.filesCount} ملف مفهرس • ${uiState.segmentsCount} مقطع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { showSettingsDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "الإعدادات",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        actions = {
                            val isIndexing = uiState.indexProgress.isIndexing
                            val rotation = if (isIndexing) {
                                val infiniteTransition = rememberInfiniteTransition(label = "spin")
                                val angle by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "spinAngle"
                                )
                                angle
                            } else 0f

                            IconButton(
                                onClick = onRefreshClick,
                                enabled = uiState.folderUri != null && !isIndexing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                                    tint = if (uiState.folderUri != null && !isIndexing)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    // Indexing Progress Bar
                    AnimatedVisibility(visible = uiState.indexProgress.isIndexing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            val progress = uiState.indexProgress
                            if (progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progress.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Integrated Search Bar with Filter Icon matching design
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Search Input Box
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color(0xFF262A30) else Color(0xFFEFEFEF))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "بحث",
                                modifier = Modifier.size(21.dp),
                                tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF757575)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = uiState.searchQuery,
                                onValueChange = onQueryChanged,
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    color = if (isDark) Color(0xFFEDEDED) else Color(0xFF1A1A1A)
                                ),
                                decorationBox = { innerTextField ->
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            text = "البحث",
                                            fontSize = 15.sp,
                                            color = if (isDark) Color(0xFF8E8E93) else Color(0xFF757575)
                                        )
                                    }
                                    innerTextField()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            if (uiState.isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearQuery,
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "مسح",
                                        tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF757575),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Side Filter Icon Button (clean and unbordered)
                    val isFilterActive = uiState.searchFilter.isFilterActive
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "التصفية",
                                modifier = Modifier.size(26.dp),
                                tint = if (isFilterActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = (-1).dp)
                                        .background(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content View depending on State
                when {
                    uiState.folderUri == null -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EmptyFolderPrompt(onSelectFolderClick = onSelectFolderClick)
                        }
                    }

                    uiState.searchQuery.isBlank() -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            ReadyToSearchPrompt(
                                folderName = uiState.folderName,
                                filesCount = uiState.filesCount,
                                segmentsCount = uiState.segmentsCount
                            )
                        }
                    }

                    uiState.searchResults.isEmpty() && !uiState.isSearching -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            NoResultsPrompt(query = uiState.searchQuery)
                        }
                    }

                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم العثور على ${uiState.searchResults.size} نتيجة في ${groupedResults.size} ملف",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            items(
                                items = groupedResults,
                                key = { it.fileUri }
                            ) { group ->
                                if (group.isDocument) {
                                    // Grouped Card for Word (.docx) & PDF (.pdf) with padding
                                    Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        WordPdfGroupedCard(
                                            group = group,
                                            onOpenFile = { onOpenFile(group.fileUri) },
                                            isDark = isDark,
                                            showAllRows = uiState.showAllRows,
                                            searchFilter = uiState.searchFilter,
                                            onCopySnippet = { text ->
                                                clipboardManager.setText(AnnotatedString(text))
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar("تم نسخ الفقرة إلى الحافظة")
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    // Flat data table replica (Edge-to-Edge)
                                    OfficialExcelTableCard(
                                        group = group,
                                        isDark = isDark,
                                        stretchSingleColumn = uiState.stretchSingleColumn,
                                        showAllRows = uiState.showAllRows,
                                        verticalCopyFormat = uiState.verticalCopyFormat,
                                        expandFullCellText = uiState.expandFullCellText,
                                        searchFilter = uiState.searchFilter,
                                        onOpenFile = { onOpenFile(group.fileUri) },
                                        onCopyRow = { text ->
                                            clipboardManager.setText(AnnotatedString(text))
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar("تم نسخ بيانات الصف إلى الحافظة")
                                            }
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
