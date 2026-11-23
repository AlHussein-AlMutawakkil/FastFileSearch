package com.example.fastfilesearch.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.fastfilesearch.model.RecordDetailData
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.ui.components.EmptyFolderPrompt
import com.example.fastfilesearch.ui.components.FileResultGroup
import com.example.fastfilesearch.ui.components.FilterSheet
import com.example.fastfilesearch.ui.components.NoResultsPrompt
import com.example.fastfilesearch.ui.components.OfficialExcelTableCard
import com.example.fastfilesearch.ui.components.ReadyToSearchPrompt
import com.example.fastfilesearch.ui.components.WordPdfGroupedCard
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppSelectableRow
import com.example.fastfilesearch.ui.designsystem.AppTwoLineMenuIcon
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens
import com.example.fastfilesearch.ui.settings.AboutScreen
import com.example.fastfilesearch.ui.settings.PermissionsScreen
import com.example.fastfilesearch.ui.settings.SettingsScreen
import com.example.fastfilesearch.ui.settings.SupportedFormatsScreen
import com.example.fastfilesearch.ui.settings.ThemeSettingsScreen
import com.example.fastfilesearch.ui.settings.WorkspacesScreen
import kotlinx.coroutines.launch

enum class AppDestination {
    SEARCH,
    SETTINGS,
    WORKSPACES,
    THEME_SETTINGS,
    PERMISSIONS,
    SUPPORTED_FORMATS,
    ABOUT
}

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
    onSelectWorkspace: (String?) -> Unit = {},
    onAddWorkspace: (Uri, String) -> Unit = { _, _ -> },
    onRenameWorkspace: (String, String) -> Unit = { _, _ -> },
    onDeleteWorkspace: (String) -> Unit = {},
    onToggleBackgroundSync: (Boolean) -> Unit = {},
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
    var currentDestination by rememberSaveable { mutableStateOf(AppDestination.SEARCH) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showWorkspaceSwitcherSheet by remember { mutableStateOf(false) }
    var activeRecordDetail by remember { mutableStateOf<RecordDetailData?>(null) }

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

    ProvideAppDesignTokens(isDark = isDark) {
        val colors = LocalAppColors.current

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            if (activeRecordDetail != null) {
                RecordDetailScreen(
                    data = activeRecordDetail!!,
                    verticalCopyFormat = uiState.verticalCopyFormat,
                    isDark = isDark,
                    onBack = { activeRecordDetail = null }
                )
                return@CompositionLocalProvider
            }

            when (currentDestination) {
                AppDestination.SETTINGS -> {
                    SettingsScreen(
                        uiState = uiState,
                        onSelectFolderClick = onSelectFolderClick,
                        onRefreshClick = onRefreshClick,
                        onToggleStretchSingleColumn = onToggleStretchSingleColumn,
                        onToggleShowAllRows = onToggleShowAllRows,
                        onToggleVerticalCopyFormat = onToggleVerticalCopyFormat,
                        onToggleExpandFullCellText = onToggleExpandFullCellText,
                        onNavigateToWorkspaces = { currentDestination = AppDestination.WORKSPACES },
                        onToggleBackgroundSync = onToggleBackgroundSync,
                        onNavigateToTheme = { currentDestination = AppDestination.THEME_SETTINGS },
                        onNavigateToPermissions = { currentDestination = AppDestination.PERMISSIONS },
                        onNavigateToSupportedFormats = { currentDestination = AppDestination.SUPPORTED_FORMATS },
                        onNavigateToAbout = { currentDestination = AppDestination.ABOUT },
                        onBack = { currentDestination = AppDestination.SEARCH }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.WORKSPACES -> {
                    WorkspacesScreen(
                        uiState = uiState,
                        onSelectWorkspace = onSelectWorkspace,
                        onAddWorkspace = onAddWorkspace,
                        onRenameWorkspace = onRenameWorkspace,
                        onDeleteWorkspace = onDeleteWorkspace,
                        onBack = { currentDestination = AppDestination.SETTINGS }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.THEME_SETTINGS -> {
                    ThemeSettingsScreen(
                        currentTheme = uiState.appTheme,
                        onSetTheme = onSetAppTheme,
                        onBack = { currentDestination = AppDestination.SETTINGS }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.PERMISSIONS -> {
                    PermissionsScreen(
                        folderUri = uiState.folderUri ?: uiState.workspaces.firstOrNull()?.let { Uri.parse(it.uri) },
                        onSelectFolderClick = onSelectFolderClick,
                        onBack = { currentDestination = AppDestination.SETTINGS }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.SUPPORTED_FORMATS -> {
                    SupportedFormatsScreen(
                        onBack = { currentDestination = AppDestination.SETTINGS }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.ABOUT -> {
                    AboutScreen(
                        onBack = { currentDestination = AppDestination.SETTINGS }
                    )
                    return@CompositionLocalProvider
                }

                AppDestination.SEARCH -> {
                    // Fallthrough to main search layout
                }
            }

            // Monochromatic Filter Bottom Sheet
            if (showFilterSheet) {
                FilterSheet(
                    uiState = uiState,
                    onApply = { newFilter ->
                        onFilterChanged(newFilter)
                        showFilterSheet = false
                    },
                    onDismiss = {
                        showFilterSheet = false
                    }
                )
            }

            // Quick Workspace Switcher Bottom Sheet matching the reference image
            if (showWorkspaceSwitcherSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showWorkspaceSwitcherSheet = false },
                    containerColor = colors.cardBackground,
                    shape = RoundedCornerShape(
                        topStart = AppDesignTokens.BottomSheetCornerRadius,
                        topEnd = AppDesignTokens.BottomSheetCornerRadius
                    ),
                    dragHandle = {
                        Box(
                            modifier = Modifier
                                .padding(vertical = 10.dp)
                                .width(42.dp)
                                .height(4.5.dp)
                                .clip(CircleShape)
                                .background(colors.divider)
                        )
                    }
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.cardBackground)
                                .navigationBarsPadding()
                        ) {
                            // Header: Title on right, Close (×) on left
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "اختيار المجلد",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                IconButton(
                                    onClick = { showWorkspaceSwitcherSheet = false },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // "جميع المجلدات" option
                            WorkspaceSwitcherItem(
                                title = "جميع المجلدات",
                                isSelected = uiState.activeWorkspace == null,
                                onClick = {
                                    onSelectWorkspace(null)
                                    showWorkspaceSwitcherSheet = false
                                }
                            )

                            // List of configured workspaces separated by clean thin dividers
                            uiState.workspaces.forEach { ws ->
                                HorizontalDivider(
                                    color = colors.divider,
                                    thickness = AppDesignTokens.DividerThickness,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                                WorkspaceSwitcherItem(
                                    title = ws.name,
                                    isSelected = uiState.activeWorkspace?.id == ws.id,
                                    onClick = {
                                        onSelectWorkspace(ws.id)
                                        showWorkspaceSwitcherSheet = false
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                }
            }

            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = colors.screenBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.screenBackground)
                        .statusBarsPadding()
                ) {
                    CenterAlignedTopAppBar(
                        title = {
                            val topBarTitle = if (uiState.activeWorkspace != null) {
                                uiState.activeWorkspace.name
                            } else if (uiState.workspaces.isNotEmpty()) {
                                "جميع المجلدات (${uiState.workspaces.size})"
                            } else {
                                "بحث آمن"
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showWorkspaceSwitcherSheet = true }
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = topBarTitle,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "اختيار المجلد",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                if (uiState.folderUri != null || uiState.workspaces.isNotEmpty()) {
                                    Text(
                                        text = "${uiState.filesCount} ملف مفهرس • ${uiState.segmentsCount} مقطع",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = { currentDestination = AppDestination.SETTINGS }) {
                                AppTwoLineMenuIcon(
                                    tint = colors.textPrimary
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

                            val hasWorkspaces = uiState.folderUri != null || uiState.workspaces.isNotEmpty()
                            IconButton(
                                onClick = onRefreshClick,
                                enabled = hasWorkspaces && !isIndexing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation },
                                    tint = if (hasWorkspaces && !isIndexing)
                                        colors.textPrimary
                                    else
                                        colors.textSecondary.copy(alpha = 0.38f)
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = colors.screenBackground
                        )
                    )

                    // Indexing Progress Bar
                    AnimatedVisibility(visible = uiState.indexProgress.isIndexing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.cardBackground)
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            val progress = uiState.indexProgress
                            if (progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = colors.controlActiveBackground,
                                    trackColor = colors.divider
                                )
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp)),
                                    color = colors.controlActiveBackground,
                                    trackColor = colors.divider
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progress.statusMessage,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
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
                    .navigationBarsPadding()
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
                                    color = colors.textPrimary
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
                                tint = colors.textPrimary
                            )
                            if (isFilterActive) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .align(Alignment.TopEnd)
                                        .offset(x = 1.dp, y = (-1).dp)
                                        .background(
                                            color = colors.controlActiveBackground,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Content View depending on State
                val hasWorkspaces = uiState.folderUri != null || uiState.workspaces.isNotEmpty()
                when {
                    !hasWorkspaces -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            EmptyFolderPrompt(onSelectFolderClick = onSelectFolderClick)
                        }
                    }

                    uiState.searchQuery.isBlank() -> {
                        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
                            ReadyToSearchPrompt(
                                folderName = if (uiState.folderName.isNotBlank()) uiState.folderName else "جميع المجلدات",
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
                        val fileCount = groupedResults.size
                        val fileWord = when {
                            fileCount == 1 -> "ملف واحد"
                            fileCount == 2 -> "ملفين"
                            fileCount in 3..10 -> "$fileCount ملفات"
                            else -> "$fileCount ملفاً"
                        }
                        val resultCount = uiState.searchResults.size

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "تم العثور على $resultCount نتيجة في $fileWord",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
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
                                        onViewRecord = { activeRecordDetail = it },
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
}

@Composable
private fun WorkspaceSwitcherItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = colors.textPrimary
        )

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(colors.controlActiveBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.controlActiveContent,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
