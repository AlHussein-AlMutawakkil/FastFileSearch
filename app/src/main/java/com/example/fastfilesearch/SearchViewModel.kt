package com.example.fastfilesearch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastfilesearch.licensing.LicenseManager
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.model.Workspace
import com.example.fastfilesearch.sync.SyncScheduler
import com.example.fastfilesearch.ui.selection.SelectedCellData
import com.example.fastfilesearch.ui.selection.SelectedRowData
import com.example.fastfilesearch.ui.selection.SelectionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SearchUiState(
    val workspaces: List<Workspace> = emptyList(),
    val activeWorkspace: Workspace? = null,
    val folderUri: Uri? = null,
    val folderName: String = "",
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val indexProgress: IndexProgress = IndexProgress(),
    val filesCount: Int = 0,
    val segmentsCount: Int = 0,
    val userMessage: String? = null,
    val stretchSingleColumn: Boolean = true,
    val showAllRows: Boolean = true,
    val verticalCopyFormat: Boolean = true,
    val expandFullCellText: Boolean = true,
    val appTheme: String = "system",
    val defaultSearchMethod: SearchMethod = SearchMethod.CONTAINS,
    val isSearchHistoryEnabled: Boolean = true,
    val searchHistory: List<String> = emptyList(),
    val isDeletingWorkspace: Boolean = false,
    val deletingWorkspaceMessage: String? = null,
    val workspaceStatusMessage: String? = null,
    val isExportingExcel: Boolean = false,
    val exportingMessage: String? = null,
    val isBackgroundSyncEnabled: Boolean = true,
    val searchFilter: SearchFilter = SearchFilter(),
    val indexedFiles: List<IndexedFileRecord> = emptyList(),
    val selectionState: SelectionState = SelectionState()
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchEngine = SearchEngine(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")
    private var searchJob: Job? = null

    init {
        // Restore preferences and workspaces
        val workspaces = searchEngine.getAllWorkspaces()
        val activeWs = searchEngine.getActiveWorkspace()
        val stretchPref = searchEngine.isStretchSingleColumn()
        val showAllRowsPref = searchEngine.isShowAllRows()
        val verticalCopyPref = searchEngine.isVerticalCopyFormat()
        val expandCellPref = searchEngine.isExpandFullCellText()
        val themePref = searchEngine.getAppTheme()
        val defaultMethodPref = searchEngine.getDefaultSearchMethod()
        val searchHistoryEnabledPref = searchEngine.isSearchHistoryEnabled()
        val savedHistory = if (searchHistoryEnabledPref) searchEngine.getSearchHistory() else emptyList()
        val bgSyncPref = searchEngine.isBackgroundSyncEnabled()

        val licenseManager = LicenseManager.getInstance(application)
        val canBgSync = licenseManager.canUseBackgroundSync()
        if (bgSyncPref && canBgSync) {
            SyncScheduler.schedulePeriodicSync(application)
        } else if (!canBgSync) {
            SyncScheduler.cancelPeriodicSync(application)
        }

        val displayName = activeWs?.name ?: if (workspaces.isNotEmpty()) "جميع المجلدات (${workspaces.size})" else ""
        val activeUri = activeWs?.let { Uri.parse(it.uri) }

        _uiState.update {
            it.copy(
                workspaces = workspaces,
                activeWorkspace = activeWs,
                folderUri = activeUri,
                folderName = displayName,
                stretchSingleColumn = stretchPref,
                showAllRows = showAllRowsPref,
                verticalCopyFormat = verticalCopyPref,
                expandFullCellText = expandCellPref,
                appTheme = themePref,
                defaultSearchMethod = defaultMethodPref,
                searchFilter = it.searchFilter.copy(method = defaultMethodPref),
                isSearchHistoryEnabled = searchHistoryEnabledPref,
                searchHistory = savedHistory,
                isBackgroundSyncEnabled = bgSyncPref
            )
        }

        refreshStatsAndFiles()

        // Debounced search query listener
        viewModelScope.launch {
            searchQueryFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    executeSearch(query)
                }
        }

        // Record settled queries in search history once user stops typing
        viewModelScope.launch {
            searchQueryFlow
                .debounce(1500L)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (_uiState.value.isSearchHistoryEnabled && query.trim().length >= 2) {
                        val updated = withContext(Dispatchers.IO) {
                            searchEngine.addSearchHistory(query, minLength = 2)
                        }
                        _uiState.update { it.copy(searchHistory = updated) }
                    }
                }
        }

        // Silent incremental sync on startup if workspaces exist
        if (workspaces.isNotEmpty()) {
            syncIncremental(isStartup = true)
        }
    }

    private fun refreshStatsAndFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val stats = searchEngine.getIndexStats()
            val allFiles = searchEngine.getAllIndexedFiles()
            val currentWorkspaces = searchEngine.getAllWorkspaces()
            val activeWs = searchEngine.getActiveWorkspace()
            val displayName = activeWs?.name ?: if (currentWorkspaces.isNotEmpty()) "جميع المجلدات (${currentWorkspaces.size})" else ""

            _uiState.update {
                it.copy(
                    workspaces = currentWorkspaces,
                    activeWorkspace = activeWs,
                    folderUri = activeWs?.let { ws -> Uri.parse(ws.uri) },
                    folderName = displayName,
                    filesCount = stats.first,
                    segmentsCount = stats.second,
                    indexedFiles = allFiles
                )
            }
        }
    }

    fun onSelectWorkspace(workspaceId: String?) {
        searchEngine.setActiveWorkspaceId(workspaceId)
        val activeWs = searchEngine.getActiveWorkspace()
        val displayName = activeWs?.name ?: if (_uiState.value.workspaces.isNotEmpty()) "جميع المجلدات (${_uiState.value.workspaces.size})" else ""

        _uiState.update {
            it.copy(
                activeWorkspace = activeWs,
                folderUri = activeWs?.let { ws -> Uri.parse(ws.uri) },
                folderName = displayName,
                searchFilter = it.searchFilter.copy(selectedWorkspaceIds = emptySet())
            )
        }
        refreshStatsAndFiles()

        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            executeSearch(currentQuery)
        }
    }

    fun onFolderSelected(uri: Uri, folderName: String) {
        onAddWorkspace(uri, folderName)
    }

    fun onAddWorkspace(uri: Uri, displayName: String = "") {
        val licenseManager = LicenseManager.getInstance(getApplication())
        if (!licenseManager.canAddWorkspace(_uiState.value.workspaces.size)) {
            val max = licenseManager.getActivePayload()?.maxWorkspaces ?: 1
            _uiState.update {
                it.copy(userMessage = "تم بلوغ الحد الأقصى للمجلدات ($max) المسموح به في الترخيص الحالي")
            }
            return
        }

        val newWs = searchEngine.addWorkspace(uri, displayName)
        refreshStatsAndFiles()

        viewModelScope.launch {
            try {
                val summary = searchEngine.syncWorkspaceIncremental(newWs) { progress ->
                    _uiState.update { it.copy(indexProgress = progress) }
                }
                refreshStatsAndFiles()
                _uiState.update {
                    it.copy(
                        userMessage = "تمت إضافة المجلد وفهرسة ${summary.indexedCount} ملف بنجاح."
                    )
                }
                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    executeSearch(currentQuery)
                }
            } catch (t: Throwable) {
                _uiState.update {
                    it.copy(userMessage = "فشلت فهرسة المجلد الجديد: ${t.localizedMessage}")
                }
            }
        }
    }

    fun onRenameWorkspace(workspaceId: String, newName: String) {
        searchEngine.renameWorkspace(workspaceId, newName)
        refreshStatsAndFiles()
    }

    fun onDeleteWorkspace(workspaceId: String) {
        if (_uiState.value.isDeletingWorkspace) return
        val wsName = _uiState.value.workspaces.firstOrNull { it.id == workspaceId }?.name ?: "المجلد"

        _uiState.update {
            it.copy(
                isDeletingWorkspace = true,
                deletingWorkspaceMessage = "جارٍ حذف المجلد «$wsName» وتنظيف الفهرس...",
                workspaceStatusMessage = null
            )
        }

        viewModelScope.launch {
            try {
                val stats = withContext(Dispatchers.IO) {
                    searchEngine.deleteWorkspaceWithStats(workspaceId)
                }
                val statsPair = withContext(Dispatchers.IO) { searchEngine.getIndexStats() }
                val allFiles = withContext(Dispatchers.IO) { searchEngine.getAllIndexedFiles() }
                val currentWorkspaces = withContext(Dispatchers.IO) { searchEngine.getAllWorkspaces() }
                val activeWs = withContext(Dispatchers.IO) { searchEngine.getActiveWorkspace() }
                val displayName = activeWs?.name ?: if (currentWorkspaces.isNotEmpty()) "جميع المجلدات (${currentWorkspaces.size})" else ""

                val resultMsg = if (stats.deleted) {
                    "تم حذف المجلد «$wsName» وإزالة ${stats.deletedFilesCount} ملف و ${stats.deletedSegmentsCount} مقطع من الفهرس."
                } else {
                    "تعذر حذف المجلد «$wsName»."
                }

                _uiState.update {
                    it.copy(
                        workspaces = currentWorkspaces,
                        activeWorkspace = activeWs,
                        folderUri = activeWs?.let { ws -> Uri.parse(ws.uri) },
                        folderName = displayName,
                        filesCount = statsPair.first,
                        segmentsCount = statsPair.second,
                        indexedFiles = allFiles,
                        isDeletingWorkspace = false,
                        deletingWorkspaceMessage = null,
                        workspaceStatusMessage = resultMsg,
                        userMessage = resultMsg,
                        selectionState = it.selectionState.clear()
                    )
                }

                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    executeSearch(currentQuery)
                }
            } catch (t: Throwable) {
                val errMsg = "فشل حذف المجلد «$wsName»: ${t.localizedMessage}"
                _uiState.update {
                    it.copy(
                        isDeletingWorkspace = false,
                        deletingWorkspaceMessage = null,
                        workspaceStatusMessage = errMsg,
                        userMessage = errMsg
                    )
                }
            }
        }
    }

    fun onDismissWorkspaceStatusMessage() {
        _uiState.update { it.copy(workspaceStatusMessage = null) }
    }

    fun onToggleBackgroundSync(enabled: Boolean) {
        val licenseManager = LicenseManager.getInstance(getApplication())
        if (enabled && !licenseManager.canUseBackgroundSync()) {
            _uiState.update {
                it.copy(
                    isBackgroundSyncEnabled = false,
                    userMessage = "ميزة المزامنة التلقائية غير مشمولة في الترخيص الحالي"
                )
            }
            SyncScheduler.cancelPeriodicSync(getApplication())
            return
        }
        searchEngine.setBackgroundSyncEnabled(enabled)
        _uiState.update { it.copy(isBackgroundSyncEnabled = enabled) }
        if (enabled) {
            SyncScheduler.schedulePeriodicSync(getApplication())
        } else {
            SyncScheduler.cancelPeriodicSync(getApplication())
        }
    }

    fun onRefreshClicked() {
        syncIncremental(isStartup = false)
    }

    private fun syncIncremental(isStartup: Boolean) {
        if (_uiState.value.indexProgress.isIndexing) return

        viewModelScope.launch {
            try {
                val activeWs = searchEngine.getActiveWorkspace()
                val summary = if (activeWs != null) {
                    searchEngine.syncWorkspaceIncremental(activeWs) { progress ->
                        _uiState.update { it.copy(indexProgress = progress) }
                    }
                } else {
                    searchEngine.syncAllWorkspacesIncremental { progress ->
                        _uiState.update { it.copy(indexProgress = progress) }
                    }
                }

                refreshStatsAndFiles()

                val message: String? = when {
                    summary.indexedCount > 0 ->
                        "اكتملت الفهرسة: تم تحديث ${summary.indexedCount} ملف، وتخطي ${summary.skippedCount} ملف دون تغيير."
                    summary.deletedCount > 0 ->
                        "تم حذف ${summary.deletedCount} ملف غير موجود."
                    isStartup ->
                        null // Silent on startup when all files are already up to date
                    else ->
                        "جميع الملفات محدثة (${summary.skippedCount} ملف)."
                }

                if (message != null) {
                    _uiState.update { it.copy(userMessage = message) }
                }

                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    executeSearch(currentQuery)
                }
            } catch (t: Throwable) {
                android.util.Log.e("SearchViewModel", "Incremental sync error", t)
                _uiState.update {
                    it.copy(
                        indexProgress = IndexProgress(isIndexing = false),
                        userMessage = if (!isStartup) "فشل تحديث الفهرس: ${t.localizedMessage}" else null
                    )
                }
            }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery, selectionState = it.selectionState.clear()) }
        searchQueryFlow.value = newQuery
    }

    fun onProcessTextReceived(text: String) {
        val clean = text.trim()
        if (clean.isBlank()) return
        searchJob?.cancel()
        val updatedHistory = if (_uiState.value.isSearchHistoryEnabled) {
            searchEngine.addSearchHistory(clean, minLength = 1)
        } else {
            _uiState.value.searchHistory
        }
        _uiState.update {
            it.copy(
                searchQuery = clean,
                searchHistory = updatedHistory,
                selectionState = it.selectionState.clear()
            )
        }
        searchQueryFlow.value = clean
        executeSearch(clean)
    }

    fun onSubmitSearch(query: String) {
        val clean = query.trim()
        if (clean.isBlank()) return
        val updatedHistory = if (_uiState.value.isSearchHistoryEnabled) {
            searchEngine.addSearchHistory(clean, minLength = 1)
        } else {
            _uiState.value.searchHistory
        }
        _uiState.update {
            it.copy(
                searchQuery = clean,
                searchHistory = updatedHistory,
                selectionState = it.selectionState.clear()
            )
        }
        searchQueryFlow.value = clean
        executeSearch(clean)
    }

    fun onSelectHistoryQuery(query: String) {
        onSubmitSearch(query)
    }

    fun onRemoveHistoryItem(query: String) {
        val updated = searchEngine.removeSearchHistoryItem(query)
        _uiState.update { it.copy(searchHistory = updated) }
    }

    fun onClearSearchHistory() {
        searchEngine.clearSearchHistory()
        _uiState.update { it.copy(searchHistory = emptyList(), userMessage = "تم مسح سجل البحث السريع") }
    }

    fun onToggleSearchHistoryEnabled(enabled: Boolean) {
        searchEngine.setSearchHistoryEnabled(enabled)
        val history = if (enabled) searchEngine.getSearchHistory() else emptyList()
        _uiState.update {
            it.copy(
                isSearchHistoryEnabled = enabled,
                searchHistory = history
            )
        }
    }

    fun onClearQuery() {
        searchJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false,
                selectionState = it.selectionState.clear()
            )
        }
        searchQueryFlow.value = ""
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        LicenseManager.getInstance(getApplication()).recordActivityTime()

        if (query.isBlank()) {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    isSearching = false
                )
            }
            return
        }

        _uiState.update { it.copy(isSearching = true) }

        searchJob = viewModelScope.launch {
            try {
                val results = searchEngine.search(query, _uiState.value.searchFilter)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        searchResults = emptyList(),
                        isSearching = false,
                        userMessage = "حدث خطأ أثناء البحث: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun onOpenFile(fileUri: String) {
        val result = searchEngine.openFile(fileUri)
        if (result.isFailure) {
            _uiState.update {
                it.copy(userMessage = "تعذر فتح الملف، تأكد من توفر تطبيق مناسب على الهاتف")
            }
        }
    }

    fun onToggleStretchSingleColumn(stretch: Boolean) {
        searchEngine.setStretchSingleColumn(stretch)
        _uiState.update { it.copy(stretchSingleColumn = stretch) }
    }

    fun onToggleShowAllRows(showAll: Boolean) {
        searchEngine.setShowAllRows(showAll)
        _uiState.update { it.copy(showAllRows = showAll) }
    }

    fun onToggleVerticalCopyFormat(vertical: Boolean) {
        searchEngine.setVerticalCopyFormat(vertical)
        _uiState.update { it.copy(verticalCopyFormat = vertical) }
    }

    fun onToggleExpandFullCellText(expand: Boolean) {
        searchEngine.setExpandFullCellText(expand)
        _uiState.update { it.copy(expandFullCellText = expand) }
    }

    fun onSetAppTheme(theme: String) {
        searchEngine.setAppTheme(theme)
        _uiState.update { it.copy(appTheme = theme) }
    }

    fun onSetDefaultSearchMethod(method: SearchMethod) {
        searchEngine.setDefaultSearchMethod(method)
        _uiState.update {
            it.copy(
                defaultSearchMethod = method,
                searchFilter = it.searchFilter.copy(method = method),
                selectionState = it.selectionState.clear()
            )
        }
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            executeSearch(currentQuery)
        }
    }

    fun onFilterChanged(newFilter: SearchFilter) {
        _uiState.update { it.copy(searchFilter = newFilter, selectionState = it.selectionState.clear()) }
        val currentQuery = _uiState.value.searchQuery
        if (currentQuery.isNotBlank()) {
            executeSearch(currentQuery)
        }
    }

    fun toggleRowSelection(rowData: SelectedRowData) {
        _uiState.update { it.copy(selectionState = it.selectionState.toggleRow(rowData)) }
    }

    fun toggleCellSelection(cellData: SelectedCellData) {
        _uiState.update { it.copy(selectionState = it.selectionState.toggleCell(cellData)) }
    }

    fun selectAllRows(allRows: List<SelectedRowData>) {
        _uiState.update { it.copy(selectionState = it.selectionState.selectAllRows(allRows)) }
    }

    fun addRowsToSelection(rows: List<SelectedRowData>) {
        _uiState.update { it.copy(selectionState = it.selectionState.addRows(rows)) }
    }

    fun setExportingState(isExporting: Boolean, message: String? = null) {
        _uiState.update { it.copy(isExportingExcel = isExporting, exportingMessage = message) }
    }

    fun selectAllCells(allCells: List<SelectedCellData>) {
        _uiState.update { it.copy(selectionState = it.selectionState.selectAllCells(allCells)) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectionState = it.selectionState.clear()) }
    }

    fun onResetFilter() {
        onFilterChanged(SearchFilter(method = _uiState.value.defaultSearchMethod))
    }

    fun onDismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
