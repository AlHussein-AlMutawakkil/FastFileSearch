package com.example.fastfilesearch.desktop

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.example.fastfilesearch.desktop.engine.DesktopFolderWatcher
import com.example.fastfilesearch.desktop.engine.DesktopSearchEngine
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockMode
import com.example.fastfilesearch.desktop.licensing.DesktopLicenseManager
import com.example.fastfilesearch.desktop.licensing.LicenseStatus
import com.example.fastfilesearch.desktop.model.DesktopSearchResponse
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.ui.DesktopAboutScreen
import com.example.fastfilesearch.desktop.ui.DesktopActivationScreen
import com.example.fastfilesearch.desktop.ui.DesktopDesignTokens
import com.example.fastfilesearch.desktop.ui.DesktopHelpScreen
import com.example.fastfilesearch.desktop.ui.DesktopLockScreen
import com.example.fastfilesearch.desktop.ui.DesktopSearchScreen
import com.example.fastfilesearch.desktop.ui.DesktopSettingsScreen
import com.example.fastfilesearch.desktop.ui.DesktopSystemActions
import com.example.fastfilesearch.desktop.ui.DesktopWorkspacesScreen
import com.example.fastfilesearch.desktop.ui.FluentTooltip
import com.example.fastfilesearch.desktop.ui.LocalDesktopColors
import com.example.fastfilesearch.desktop.ui.ProvideDesktopTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Dimension
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import javax.swing.UIManager

private enum class DesktopTab(val title: String, val icon: ImageVector) {
    SEARCH("البحث", Icons.Outlined.Search),
    WORKSPACES("المجلدات", Icons.Outlined.FolderOpen),
    SETTINGS("الإعدادات والترخيص", Icons.Outlined.Settings),
    HELP("المساعدة", Icons.AutoMirrored.Outlined.HelpOutline),
    ABOUT("حول التطبيق", Icons.Outlined.Info)
}

fun main() {
    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (_: Throwable) {
    }

    val windowIconPainter = try {
        Thread.currentThread().contextClassLoader.getResourceAsStream("icon.png")?.use { stream ->
            ImageIO.read(stream)?.toComposeImageBitmap()?.let { BitmapPainter(it) }
        }
    } catch (_: Throwable) {
        null
    }

    application {
        val windowState = rememberWindowState(width = 1220.dp, height = 800.dp)
        Window(
            onCloseRequest = ::exitApplication,
            title = "بحث آمن",
            icon = windowIconPainter,
            state = windowState
        ) {
            window.minimumSize = Dimension(640, 560)
            SafeSearchDesktopApp()
        }
    }
}

@Composable
private fun SafeSearchDesktopApp() {
    val licenseManager = remember { DesktopLicenseManager() }
    val appLockManager = remember { DesktopAppLockManager() }
    val searchEngine = remember { DesktopSearchEngine() }

    DisposableEffect(Unit) {
        onDispose { searchEngine.close() }
    }

    var licenseVersion by remember { mutableIntStateOf(0) }
    val licenseStatus = remember(licenseVersion) { licenseManager.getLicenseStatus() }
    val isLocked by appLockManager.isLocked.collectAsState()
    val lockMode by appLockManager.lockModeFlow.collectAsState()
    val autoLockTimeout by appLockManager.autoLockTimeoutFlow.collectAsState()

    var themeMode by remember { mutableStateOf(searchEngine.getThemeMode()) }
    var defaultSearchMethod by remember { mutableStateOf(searchEngine.getDefaultSearchMethod()) }
    var showAllRows by remember { mutableStateOf(searchEngine.isShowAllRows()) }
    var stretchSingleColumn by remember { mutableStateOf(searchEngine.isStretchSingleColumn()) }
    var verticalCopyFormat by remember { mutableStateOf(searchEngine.isVerticalCopyFormat()) }
    var expandFullCellText by remember { mutableStateOf(searchEngine.isExpandFullCellText()) }
    var backgroundSyncEnabled by remember { mutableStateOf(searchEngine.isBackgroundSyncEnabled()) }
    var searchHistoryEnabled by remember { mutableStateOf(searchEngine.isSearchHistoryEnabled()) }
    var searchHistory by remember {
        mutableStateOf(if (searchHistoryEnabled) searchEngine.getSearchHistory() else emptyList())
    }

    // Idle auto-lock check
    LaunchedEffect(licenseStatus, lockMode, autoLockTimeout, isLocked) {
        if (licenseStatus == LicenseStatus.ACTIVE && lockMode != DesktopAppLockMode.DISABLED && !isLocked) {
            while (isActive) {
                delay(5000L)
                appLockManager.checkIdleTimeout()
            }
        }
    }

    val windowsDarkHint = remember { detectWindowsDarkTheme() }
    val systemDark = windowsDarkHint ?: isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> systemDark
    }

    ProvideDesktopTheme(isDark = isDark) {
        when {
            licenseStatus != LicenseStatus.ACTIVE -> {
                DesktopActivationScreen(
                    licenseManager = licenseManager,
                    onActivated = { licenseVersion++ }
                )
            }
            isLocked -> {
                DesktopLockScreen(
                    lockManager = appLockManager,
                    onUnlocked = { appLockManager.recordUserActivity() }
                )
            }
            else -> {
                DesktopMainShell(
                    licenseManager = licenseManager,
                    appLockManager = appLockManager,
                    searchEngine = searchEngine,
                    themeMode = themeMode,
                    defaultSearchMethod = defaultSearchMethod,
                    showAllRows = showAllRows,
                    stretchSingleColumn = stretchSingleColumn,
                    verticalCopyFormat = verticalCopyFormat,
                    expandFullCellText = expandFullCellText,
                    backgroundSyncEnabled = backgroundSyncEnabled,
                    searchHistoryEnabled = searchHistoryEnabled,
                    searchHistory = searchHistory,
                    onThemeChanged = { newTheme ->
                        themeMode = newTheme
                        searchEngine.setThemeMode(newTheme)
                    },
                    onDefaultMethodChanged = { newMethod ->
                        defaultSearchMethod = newMethod
                        searchEngine.setDefaultSearchMethod(newMethod)
                    },
                    onShowAllRowsChanged = { enabled ->
                        showAllRows = enabled
                        searchEngine.setShowAllRows(enabled)
                    },
                    onStretchSingleColumnChanged = { enabled ->
                        stretchSingleColumn = enabled
                        searchEngine.setStretchSingleColumn(enabled)
                    },
                    onVerticalCopyFormatChanged = { enabled ->
                        verticalCopyFormat = enabled
                        searchEngine.setVerticalCopyFormat(enabled)
                    },
                    onExpandFullCellTextChanged = { enabled ->
                        expandFullCellText = enabled
                        searchEngine.setExpandFullCellText(enabled)
                    },
                    onBackgroundSyncChanged = { enabled ->
                        backgroundSyncEnabled = enabled
                        searchEngine.setBackgroundSyncEnabled(enabled)
                    },
                    onSearchHistoryEnabledChanged = { enabled ->
                        searchHistoryEnabled = enabled
                        searchEngine.setSearchHistoryEnabled(enabled)
                        searchHistory = if (enabled) searchEngine.getSearchHistory() else emptyList()
                    },
                    onRecordHistory = { q, minLen ->
                        if (searchHistoryEnabled) {
                            searchHistory = searchEngine.addSearchHistory(q, minLength = minLen)
                        }
                    },
                    onRemoveHistoryItem = { q ->
                        searchHistory = searchEngine.removeSearchHistoryItem(q)
                    },
                    onClearSearchHistory = {
                        searchEngine.clearSearchHistory()
                        searchHistory = emptyList()
                    },
                    onLicenseUpdated = { licenseVersion++ }
                )
            }
        }
    }
}

@Composable
private fun DesktopMainShell(
    licenseManager: DesktopLicenseManager,
    appLockManager: DesktopAppLockManager,
    searchEngine: DesktopSearchEngine,
    themeMode: String,
    defaultSearchMethod: SearchMethod,
    showAllRows: Boolean,
    stretchSingleColumn: Boolean,
    verticalCopyFormat: Boolean,
    expandFullCellText: Boolean,
    backgroundSyncEnabled: Boolean,
    searchHistoryEnabled: Boolean,
    searchHistory: List<String>,
    onThemeChanged: (String) -> Unit,
    onDefaultMethodChanged: (SearchMethod) -> Unit,
    onShowAllRowsChanged: (Boolean) -> Unit,
    onStretchSingleColumnChanged: (Boolean) -> Unit,
    onVerticalCopyFormatChanged: (Boolean) -> Unit,
    onExpandFullCellTextChanged: (Boolean) -> Unit,
    onBackgroundSyncChanged: (Boolean) -> Unit,
    onSearchHistoryEnabledChanged: (Boolean) -> Unit,
    onRecordHistory: (String, Int) -> Unit,
    onRemoveHistoryItem: (String) -> Unit,
    onClearSearchHistory: () -> Unit,
    onLicenseUpdated: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val scope = rememberCoroutineScope()
    val lockMode by appLockManager.lockModeFlow.collectAsState()

    var currentTab by remember { mutableStateOf(DesktopTab.SEARCH) }
    var workspaces by remember { mutableStateOf(searchEngine.getAllWorkspaces()) }
    var activeWorkspaceId by remember { mutableStateOf(searchEngine.getActiveWorkspaceId()) }
    var stats by remember { mutableStateOf(searchEngine.getIndexStats()) }
    var indexedFiles by remember { mutableStateOf(searchEngine.getAllIndexedFiles()) }
    var indexProgress by remember { mutableStateOf(IndexProgress()) }
    var indexingJob by remember { mutableStateOf<Job?>(null) }
    var workspaceBanner by remember { mutableStateOf<String?>(null) }

    var indexedTypeCounts by remember { mutableStateOf(searchEngine.getIndexedTypeCounts(activeWorkspaceId)) }
    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter(method = defaultSearchMethod)) }
    var searchResponse by remember { mutableStateOf(DesktopSearchResponse()) }
    var unfilteredSearchTypeCounts by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    fun refreshMetadata() {
        val currentWs = searchEngine.getActiveWorkspaceId()
        workspaces = searchEngine.getAllWorkspaces()
        activeWorkspaceId = currentWs
        stats = searchEngine.getIndexStats()
        indexedFiles = searchEngine.getAllIndexedFiles()
        indexedTypeCounts = searchEngine.getIndexedTypeCounts(currentWs)
    }

    fun startIndexing(
        targetWorkspaces: List<Workspace> = searchEngine.getAllWorkspaces(),
        silentIfUnchanged: Boolean = false
    ) {
        if (indexProgress.isIndexing || targetWorkspaces.isEmpty()) return
        indexingJob = scope.launch {
            val summary = searchEngine.syncWorkspaces(targetWorkspaces) { prog ->
                indexProgress = prog
            }
            refreshMetadata()
            if (summary.cancelled) {
                workspaceBanner = "تم إيقاف الفهرسة بناءً على طلبك."
            } else if (!silentIfUnchanged || summary.indexedCount > 0 || summary.deletedCount > 0) {
                workspaceBanner = "اكتملت الفهرسة: ${summary.indexedCount} محدث • ${summary.skippedCount} دون تغيير • ${summary.deletedCount} محذوف."
            }
            if (searchQuery.isNotBlank()) {
                isSearching = true
                try {
                    val res = withContext(Dispatchers.IO) {
                        searchEngine.searchGrouped(
                            searchQuery,
                            searchFilter,
                            perFileInitialRows = if (showAllRows) 500 else 35
                        )
                    }
                    searchResponse = res
                    searchResults = res.groups.flatMap { it.results }
                    if (searchFilter.selectedExtensions.isEmpty() || unfilteredSearchTypeCounts.isEmpty()) {
                        unfilteredSearchTypeCounts = res.typeCounts
                    }
                    isSearching = false
                } catch (c: kotlinx.coroutines.CancellationException) {
                    searchEngine.cancelActiveSearch()
                    throw c
                } catch (_: Throwable) {
                    isSearching = false
                }
            }
        }
    }

    // Folder Watcher for Desktop Auto-Sync
    val folderWatcher = remember {
        DesktopFolderWatcher(debounceMs = 2500L) {
            withContext(Dispatchers.Main) {
                if (!indexProgress.isIndexing) {
                    startIndexing(searchEngine.getAllWorkspaces(), silentIfUnchanged = true)
                }
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose { folderWatcher.close() }
    }
    LaunchedEffect(workspaces, backgroundSyncEnabled) {
        val activeSync = backgroundSyncEnabled && licenseManager.canUseBackgroundSync()
        folderWatcher.updateWatchedWorkspaces(workspaces, activeSync)
    }

    // Startup differential sync if workspaces exist
    LaunchedEffect(Unit) {
        if (workspaces.isNotEmpty()) {
            startIndexing(workspaces, silentIfUnchanged = true)
        }
    }

    LaunchedEffect(
        searchQuery,
        searchFilter.method,
        searchFilter.ignoreDiacritics,
        searchFilter.selectedWorkspaceIds,
        searchFilter.selectedFileUris,
        activeWorkspaceId
    ) {
        unfilteredSearchTypeCounts = emptyMap()
    }

    // Smart-debounced live search with active query cancellation
    LaunchedEffect(searchQuery, searchFilter, activeWorkspaceId, showAllRows) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            searchEngine.cancelActiveSearch()
            searchResponse = DesktopSearchResponse()
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        searchEngine.cancelActiveSearch()
        val debounceMs = if (q.length <= 2) 380L else 220L
        delay(debounceMs)
        try {
            val response = withContext(Dispatchers.IO) {
                searchEngine.searchGrouped(
                    q,
                    searchFilter,
                    perFileInitialRows = if (showAllRows) 500 else 35
                )
            }
            searchResponse = response
            searchResults = response.groups.flatMap { it.results }
            if (searchFilter.selectedExtensions.isEmpty() || unfilteredSearchTypeCounts.isEmpty()) {
                unfilteredSearchTypeCounts = response.typeCounts
            }
            isSearching = false
        } catch (c: kotlinx.coroutines.CancellationException) {
            searchEngine.cancelActiveSearch()
            throw c
        } catch (_: Throwable) {
            isSearching = false
        }
    }

    val activeWorkspace = remember(workspaces, activeWorkspaceId) {
        workspaces.firstOrNull { it.id == activeWorkspaceId }
    }
    val activeTypeCounts = remember(searchQuery, indexedTypeCounts, searchResponse) {
        if (searchQuery.trim().isEmpty()) {
            indexedTypeCounts
        } else {
            searchResponse.typeCounts
        }
    }
    var selectionSummary by remember { mutableStateOf<String?>(null) }
    var resultsSummary by remember { mutableStateOf(0 to 0) }
    val watcherActive = backgroundSyncEnabled && licenseManager.canUseBackgroundSync() && workspaces.isNotEmpty()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isCompactWidth = maxWidth < 1100.dp
        var isNavExpanded by remember { mutableStateOf(!isCompactWidth) }
        var previousCompactWidth by remember { mutableStateOf(isCompactWidth) }
        LaunchedEffect(isCompactWidth) {
            if (isCompactWidth != previousCompactWidth) {
                previousCompactWidth = isCompactWidth
                isNavExpanded = !isCompactWidth
            }
        }
        val navWidth by animateDpAsState(targetValue = if (isNavExpanded) 224.dp else 62.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            appLockManager.recordUserActivity()
                        }
                    }
                }
                .onPreviewKeyEvent { keyEvent ->
                    appLockManager.recordUserActivity()
                    if (keyEvent.type == KeyEventType.KeyDown &&
                        (keyEvent.isCtrlPressed || keyEvent.isMetaPressed) &&
                        keyEvent.key == Key.L &&
                        lockMode != DesktopAppLockMode.DISABLED
                    ) {
                        appLockManager.lockNow()
                        true
                    } else if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.F1) {
                        currentTab = DesktopTab.HELP
                        true
                    } else if (keyEvent.type == KeyEventType.KeyDown &&
                        keyEvent.key == Key.F5 &&
                        workspaces.isNotEmpty() &&
                        !indexProgress.isIndexing
                    ) {
                        val targets = activeWorkspace?.let { listOf(it) } ?: workspaces
                        startIndexing(targets, silentIfUnchanged = false)
                        true
                    } else {
                        false
                    }
                }
        ) {
        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
            // Collapsible Windows 11 NavigationView Rail
            Column(
                modifier = Modifier
                    .width(navWidth)
                    .fillMaxHeight()
                    .background(colors.sidebarBackground)
                    .padding(horizontal = if (isNavExpanded) 12.dp else 8.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (isNavExpanded) Arrangement.spacedBy(8.dp) else Arrangement.Center
                    ) {
                        FluentTooltip(if (isNavExpanded) "طيّ القائمة الجانبية" else "توسيع القائمة الجانبية") {
                            IconButton(
                                onClick = { isNavExpanded = !isNavExpanded },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = if (isNavExpanded) "طيّ القائمة" else "توسيع القائمة",
                                    tint = colors.textPrimary,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }

                        if (isNavExpanded) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "بحث آمن",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1
                                )
                                Text(
                                    text = "الإصدار المكتبي — ويندوز",
                                    fontSize = 11.sp,
                                    color = colors.textSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    DesktopTab.entries.forEach { tab ->
                        DesktopNavRailItem(
                            title = tab.title,
                            icon = tab.icon,
                            selected = currentTab == tab,
                            expanded = isNavExpanded,
                            onClick = { currentTab = tab }
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider(color = colors.divider, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(2.dp))

                    if (lockMode != DesktopAppLockMode.DISABLED) {
                        DesktopNavRailItem(
                            title = "قفل التطبيق",
                            icon = Icons.Outlined.Lock,
                            selected = false,
                            expanded = isNavExpanded,
                            tooltipOverride = "قفل التطبيق (Ctrl+L)",
                            onClick = { appLockManager.lockNow() }
                        )
                    }

                    val themeLabel = if (colors.isDark) "الوضع الفاتح" else "الوضع الداكن"
                    DesktopNavRailItem(
                        title = themeLabel,
                        icon = Icons.Outlined.BrightnessMedium,
                        selected = false,
                        expanded = isNavExpanded,
                        tooltipOverride = themeLabel,
                        onClick = {
                            val next = when (themeMode) {
                                "light" -> "dark"
                                "dark" -> "light"
                                else -> if (colors.isDark) "light" else "dark"
                            }
                            onThemeChanged(next)
                        }
                    )
                }
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(colors.divider))

            // Main Screen Content
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                when (currentTab) {
                    DesktopTab.SEARCH -> {
                        DesktopSearchScreen(
                            searchQuery = searchQuery,
                            searchFilter = searchFilter,
                            defaultSearchMethod = defaultSearchMethod,
                            searchResults = searchResults,
                            isSearching = isSearching,
                            workspaces = workspaces,
                            activeWorkspace = activeWorkspace,
                            indexedFiles = indexedFiles,
                            indexProgress = indexProgress,
                            filesCount = stats.first,
                            segmentsCount = stats.second,
                            canExportExcel = licenseManager.canExportExcel(),
                            showAllRows = showAllRows,
                            stretchSingleColumn = stretchSingleColumn,
                            verticalCopyFormat = verticalCopyFormat,
                            expandFullCellText = expandFullCellText,
                            isSearchHistoryEnabled = searchHistoryEnabled,
                            searchHistory = searchHistory,
                            onQueryChange = { searchQuery = it },
                            onSubmitSearch = { q ->
                                val clean = q.trim()
                                if (clean.isNotEmpty()) {
                                    searchQuery = clean
                                    onRecordHistory(clean, 1)
                                }
                            },
                            onSelectHistoryQuery = { q ->
                                searchQuery = q
                                onRecordHistory(q, 1)
                            },
                            onRemoveHistoryItem = onRemoveHistoryItem,
                            onClearSearchHistory = onClearSearchHistory,
                            onSelectActiveWorkspace = { wsId ->
                                searchEngine.setActiveWorkspaceId(wsId)
                                searchFilter = searchFilter.copy(selectedWorkspaceIds = emptySet(), selectedFileUris = emptySet())
                                refreshMetadata()
                            },
                            onRefreshIndex = {
                                val targets = activeWorkspace?.let { listOf(it) } ?: workspaces
                                startIndexing(targets, silentIfUnchanged = false)
                            },
                            onFilterChange = { searchFilter = it },
                            onOpenWorkspaces = { currentTab = DesktopTab.WORKSPACES },
                            onSelectionSummaryChange = { selectionSummary = it },
                            onResultsSummaryChange = { resCount, fileCount ->
                                resultsSummary = resCount to fileCount
                            },
                            searchResponse = searchResponse,
                            typeCounts = activeTypeCounts,
                            onLoadMoreForFile = { targetUri ->
                                val q = searchQuery.trim()
                                if (q.isNotEmpty()) {
                                    scope.launch {
                                        try {
                                            val currentGrp = searchResponse.groups.firstOrNull { it.fileUri == targetUri }
                                            val offset = currentGrp?.results?.size ?: 0
                                            val more = withContext(Dispatchers.IO) {
                                                searchEngine.loadMoreForFile(q, searchFilter, targetUri, offset = offset, limit = 200)
                                            }
                                            val updatedGroups = searchResponse.groups.map { grp ->
                                                if (grp.fileUri == targetUri) {
                                                    val merged = (grp.results + more).distinctBy {
                                                        "${it.fileUri}|${it.sheetName}|${it.rowNumber}|${it.location}|${it.snippet}"
                                                    }
                                                    grp.copy(
                                                        results = merged,
                                                        hasMoreRows = more.isNotEmpty() && merged.size < grp.totalMatchesCount
                                                    )
                                                } else {
                                                    grp
                                                }
                                            }
                                            searchResponse = searchResponse.copy(groups = updatedGroups)
                                            searchResults = updatedGroups.flatMap { it.results }
                                        } catch (c: kotlinx.coroutines.CancellationException) {
                                            throw c
                                        } catch (_: Throwable) {
                                        }
                                    }
                                }
                            }
                        )
                    }

                    DesktopTab.WORKSPACES -> {
                        val activePayload = licenseManager.getActivePayload()
                        DesktopWorkspacesScreen(
                            workspaces = workspaces,
                            activeWorkspaceId = activeWorkspaceId,
                            maxFoldersAllowed = activePayload?.maxWorkspaces ?: 0,
                            indexProgress = indexProgress,
                            filesCount = stats.first,
                            segmentsCount = stats.second,
                            statusBanner = workspaceBanner,
                            onSelectWorkspace = { wsId ->
                                searchEngine.setActiveWorkspaceId(wsId)
                                searchFilter = searchFilter.copy(selectedWorkspaceIds = emptySet(), selectedFileUris = emptySet())
                                refreshMetadata()
                            },
                            onAddFolderClick = {
                                if (!licenseManager.canAddWorkspace(workspaces.size)) {
                                    workspaceBanner = "تم بلوغ الحد الأقصى للمجلدات المسموح بها في الترخيص الحالي."
                                    return@DesktopWorkspacesScreen
                                }
                                val picked = DesktopSystemActions.pickFolder()
                                if (picked != null && picked.isDirectory) {
                                    val added = searchEngine.addWorkspace(picked)
                                    refreshMetadata()
                                    workspaceBanner = "تمت إضافة المجلد «${added.name}» وبدأت الفهرسة..."
                                    startIndexing(listOf(added), silentIfUnchanged = false)
                                }
                            },
                            onRenameWorkspace = { ws, newName ->
                                if (searchEngine.renameWorkspace(ws.id, newName)) {
                                    refreshMetadata()
                                    workspaceBanner = "تمت إعادة تسمية المجلد إلى «$newName»."
                                }
                            },
                            onDeleteWorkspace = { ws ->
                                val delStats = searchEngine.deleteWorkspaceWithStats(ws.id)
                                refreshMetadata()
                                workspaceBanner = if (delStats.deleted) {
                                    "تم حذف المجلد «${ws.name}» وإزالة ${delStats.deletedFilesCount} ملف و ${delStats.deletedSegmentsCount} مقطع من الفهرس."
                                } else {
                                    "تعذر حذف المجلد «${ws.name}»."
                                }
                            },
                            onRefreshIndex = { startIndexing(workspaces, silentIfUnchanged = false) },
                            onCancelIndexing = { indexingJob?.cancel() }
                        )
                    }

                    DesktopTab.SETTINGS -> {
                        DesktopSettingsScreen(
                            licenseManager = licenseManager,
                            appLockManager = appLockManager,
                            themeMode = themeMode,
                            defaultSearchMethod = defaultSearchMethod,
                            showAllRows = showAllRows,
                            stretchSingleColumn = stretchSingleColumn,
                            verticalCopyFormat = verticalCopyFormat,
                            expandFullCellText = expandFullCellText,
                            backgroundSyncEnabled = backgroundSyncEnabled,
                            searchHistoryEnabled = searchHistoryEnabled,
                            searchHistoryCount = searchHistory.size,
                            onChangeTheme = onThemeChanged,
                            onChangeDefaultMethod = { method ->
                                onDefaultMethodChanged(method)
                                searchFilter = searchFilter.copy(method = method)
                            },
                            onToggleShowAllRows = onShowAllRowsChanged,
                            onToggleStretchSingleColumn = onStretchSingleColumnChanged,
                            onToggleVerticalCopyFormat = onVerticalCopyFormatChanged,
                            onToggleExpandFullCellText = onExpandFullCellTextChanged,
                            onToggleBackgroundSync = onBackgroundSyncChanged,
                            onToggleSearchHistory = onSearchHistoryEnabledChanged,
                            onClearSearchHistory = onClearSearchHistory,
                            onOpenAbout = { currentTab = DesktopTab.ABOUT },
                            onLicenseUpdated = onLicenseUpdated
                        )
                    }

                    DesktopTab.HELP -> {
                        DesktopHelpScreen()
                    }

                    DesktopTab.ABOUT -> {
                        DesktopAboutScreen(
                            onStatusMessage = { msg -> workspaceBanner = msg }
                        )
                    }
                }
            }
        }

        // Bottom Windows 11 Status Bar (28.dp)
        DesktopStatusBar(
            indexProgress = indexProgress,
            watcherActive = watcherActive,
            activeWorkspaceName = activeWorkspace?.name,
            workspacesCount = workspaces.size,
            filesCount = stats.first,
            segmentsCount = stats.second,
            resultsSummary = if (searchQuery.isNotBlank() && resultsSummary.first > 0) resultsSummary else null,
            selectionSummary = selectionSummary
        )
    }
    }
}

@Composable
private fun DesktopNavRailItem(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    expanded: Boolean,
    tooltipOverride: String? = null,
    onClick: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val bg = when {
        selected -> colors.subCardBackground
        isHovered -> colors.subCardBackground.copy(alpha = 0.65f)
        else -> Color.Transparent
    }

    val rowContent: @Composable () -> Unit = {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                .background(bg)
                .then(
                    if (selected) {
                        Modifier.border(0.5.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                    } else {
                        Modifier
                    }
                )
                .hoverable(interactionSource = interactionSource)
                .clickable(onClick = onClick)
                .padding(horizontal = if (expanded) 8.dp else 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (expanded) Arrangement.spacedBy(10.dp) else Arrangement.Center
        ) {
            // Fluent Accent Pill Indicator
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(18.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (selected) colors.controlActiveBackground else Color.Transparent)
            )

            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (selected) colors.controlActiveBackground else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )

            if (expanded) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (!expanded || tooltipOverride != null) {
        FluentTooltip(tooltipOverride ?: title) {
            rowContent()
        }
    } else {
        rowContent()
    }
}

@Composable
private fun DesktopStatusBar(
    indexProgress: IndexProgress,
    watcherActive: Boolean,
    activeWorkspaceName: String?,
    workspacesCount: Int,
    filesCount: Int,
    segmentsCount: Int,
    resultsSummary: Pair<Int, Int>?,
    selectionSummary: String?
) {
    val colors = LocalDesktopColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = colors.divider, thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .background(colors.sidebarBackground)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (indexProgress.isIndexing) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(11.dp),
                            strokeWidth = 1.5.dp,
                            color = colors.controlActiveBackground
                        )
                        val progressLabel = if (indexProgress.total > 0) {
                            "جارٍ الفهرسة (${indexProgress.current}/${indexProgress.total})"
                        } else {
                            "جارٍ تحديث الفهرس..."
                        }
                        Text(progressLabel, fontSize = 11.5.sp, color = colors.textPrimary)
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (watcherActive) Color(0xFF1E7E34) else colors.textSecondary.copy(alpha = 0.55f))
                        )
                        Text(
                            text = if (watcherActive) "المراقبة الفورية نشطة" else "جاهز",
                            fontSize = 11.5.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Box(modifier = Modifier.width(1.dp).height(13.dp).background(colors.divider))

                Text(
                    text = activeWorkspaceName ?: if (workspacesCount > 0) "جميع المجلدات ($workspacesCount)" else "لا توجد مجلدات",
                    fontSize = 11.5.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                selectionSummary?.let { sel ->
                    Box(modifier = Modifier.width(1.dp).height(13.dp).background(colors.divider))
                    Text(
                        text = sel,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                resultsSummary?.let { (resCount, fileCount) ->
                    Text(
                        text = "النتائج: $resCount في $fileCount ملف",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary
                    )
                    Box(modifier = Modifier.width(1.dp).height(13.dp).background(colors.divider))
                }

                Text(
                    text = "$filesCount ملف • $segmentsCount سجل",
                    fontSize = 11.5.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

private fun detectWindowsDarkTheme(): Boolean? {
    return try {
        val process = ProcessBuilder(
            "reg.exe", "query",
            "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
            "/v", "AppsUseLightTheme"
        ).redirectErrorStream(true).start()
        val ok = process.waitFor(2, TimeUnit.SECONDS)
        if (!ok) {
            process.destroyForcibly()
            return null
        }
        val output = process.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        when {
            output.contains("0x0") -> true
            output.contains("0x1") -> false
            else -> null
        }
    } catch (_: Throwable) {
        null
    }
}
