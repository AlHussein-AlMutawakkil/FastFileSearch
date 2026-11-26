package com.example.fastfilesearch.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.ui.DesktopAboutScreen
import com.example.fastfilesearch.desktop.ui.DesktopActivationScreen
import com.example.fastfilesearch.desktop.ui.DesktopLockScreen
import com.example.fastfilesearch.desktop.ui.DesktopSearchScreen
import com.example.fastfilesearch.desktop.ui.DesktopSettingsScreen
import com.example.fastfilesearch.desktop.ui.DesktopSystemActions
import com.example.fastfilesearch.desktop.ui.DesktopWorkspacesScreen
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
            window.minimumSize = Dimension(900, 640)
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

    var searchQuery by remember { mutableStateOf("") }
    var searchFilter by remember { mutableStateOf(SearchFilter(method = defaultSearchMethod)) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    fun refreshMetadata() {
        workspaces = searchEngine.getAllWorkspaces()
        activeWorkspaceId = searchEngine.getActiveWorkspaceId()
        stats = searchEngine.getIndexStats()
        indexedFiles = searchEngine.getAllIndexedFiles()
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
                searchResults = withContext(Dispatchers.IO) {
                    searchEngine.search(searchQuery, searchFilter)
                }
                isSearching = false
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

    // Debounced live search
    LaunchedEffect(searchQuery, searchFilter, activeWorkspaceId) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(150L)
        val res = withContext(Dispatchers.IO) {
            searchEngine.search(q, searchFilter)
        }
        searchResults = res
        isSearching = false
    }

    // Record settled queries in search history after 1500ms
    LaunchedEffect(searchQuery, searchHistoryEnabled) {
        val q = searchQuery.trim()
        if (searchHistoryEnabled && q.length >= 2) {
            delay(1500L)
            onRecordHistory(q, 2)
        }
    }

    val activeWorkspace = remember(workspaces, activeWorkspaceId) {
        workspaces.firstOrNull { it.id == activeWorkspaceId }
    }

    Row(
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
                } else {
                    false
                }
            }
    ) {
        // Right-aligned RTL Sidebar Navigation Rail
        Column(
            modifier = Modifier
                .width(235.dp)
                .fillMaxHeight()
                .background(colors.sidebarBackground)
                .border(0.5.dp, colors.divider)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)) {
                    Text(
                        text = "بحث آمن",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "محرك البحث المحلي — ويندوز",
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                Spacer(modifier = Modifier.height(6.dp))

                DesktopTab.entries.forEach { tab ->
                    val selected = currentTab == tab
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) colors.controlActiveBackground else colors.sidebarBackground)
                            .clickable { currentTab = tab }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = if (selected) colors.controlActiveContent else colors.textSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                        Text(
                            text = tab.title,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = if (selected) colors.controlActiveContent else colors.textPrimary
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                Text(
                    text = "${stats.first} ملف • ${stats.second} سجل",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                if (lockMode != DesktopAppLockMode.DISABLED) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { appLockManager.lockNow() }
                            .padding(horizontal = 8.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "قفل التطبيق",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "قفل التطبيق (Ctrl+L)",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val next = when (themeMode) {
                                "light" -> "dark"
                                "dark" -> "light"
                                else -> if (colors.isDark) "light" else "dark"
                            }
                            onThemeChanged(next)
                        }
                        .padding(horizontal = 8.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.BrightnessMedium,
                        contentDescription = "تبديل المظهر",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (colors.isDark) "الوضع الفاتح" else "الوضع الداكن",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

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
                        onOpenWorkspaces = { currentTab = DesktopTab.WORKSPACES }
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

                DesktopTab.ABOUT -> {
                    DesktopAboutScreen(
                        onStatusMessage = { msg -> workspaceBanner = msg }
                    )
                }
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
