package com.example.fastfilesearch

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fastfilesearch.model.SearchResult
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val folderUri: Uri? = null,
    val folderName: String = "",
    val searchQuery: String = "",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val indexProgress: IndexProgress = IndexProgress(),
    val filesCount: Int = 0,
    val segmentsCount: Int = 0,
    val userMessage: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchEngine = SearchEngine(application)

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        // Restore saved folder
        val savedUri = searchEngine.getSavedFolderUri()
        val savedName = searchEngine.getSavedFolderName()
        val stats = searchEngine.getIndexStats()

        _uiState.update {
            it.copy(
                folderUri = savedUri,
                folderName = savedName,
                filesCount = stats.first,
                segmentsCount = stats.second
            )
        }

        // Setup debounced search query listener
        viewModelScope.launch {
            searchQueryFlow
                .debounce(150L)
                .distinctUntilChanged()
                .collect { query ->
                    executeSearch(query)
                }
        }

        // Auto delta-sync on startup if a folder is already configured
        if (savedUri != null) {
            syncFolder(savedUri)
        }
    }

    fun onFolderSelected(uri: Uri, displayName: String = "") {
        searchEngine.saveFolderUri(uri, displayName)
        val name = searchEngine.getSavedFolderName()
        _uiState.update {
            it.copy(
                folderUri = uri,
                folderName = name,
                userMessage = "تم اختيار المجلد بنجاح"
            )
        }
        syncFolder(uri)
    }

    fun onRefreshClicked() {
        val uri = _uiState.value.folderUri ?: return
        syncFolder(uri)
    }

    fun onQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchQueryFlow.value = newQuery
    }

    fun onClearQuery() {
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchResults = emptyList(),
                isSearching = false
            )
        }
        searchQueryFlow.value = ""
    }

    private fun executeSearch(query: String) {
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

        viewModelScope.launch {
            try {
                val results = searchEngine.search(query)
                _uiState.update {
                    it.copy(
                        searchResults = results,
                        isSearching = false
                    )
                }
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

    private fun syncFolder(uri: Uri) {
        if (_uiState.value.indexProgress.isIndexing) return

        viewModelScope.launch {
            try {
                val summary = searchEngine.syncFolder(uri) { progress ->
                    _uiState.update { it.copy(indexProgress = progress) }
                }

                val stats = searchEngine.getIndexStats()
                val message = when {
                    summary.indexedCount > 0 ->
                        "اكتملت الفهرسة: تم تحديث ${summary.indexedCount} ملف، وتخطي ${summary.skippedCount} ملف دون تغيير."
                    summary.deletedCount > 0 ->
                        "تم حذف ${summary.deletedCount} ملف غير موجود."
                    else ->
                        "جميع الملفات محدثة (${summary.skippedCount} ملف)."
                }

                _uiState.update {
                    it.copy(
                        filesCount = stats.first,
                        segmentsCount = stats.second,
                        userMessage = message
                    )
                }

                // If user currently has an active query, refresh search results with updated index
                val currentQuery = _uiState.value.searchQuery
                if (currentQuery.isNotBlank()) {
                    executeSearch(currentQuery)
                }
            } catch (t: Throwable) {
                android.util.Log.e("SearchViewModel", "Folder sync error", t)
                _uiState.update {
                    it.copy(
                        indexProgress = IndexProgress(isIndexing = false),
                        userMessage = "فشل مزامنة المجلد: ${t.localizedMessage}"
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

    fun onDismissMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
