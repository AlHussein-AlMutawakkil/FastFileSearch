package com.example.fastfilesearch

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.FragmentActivity
import com.example.fastfilesearch.security.AppLockManager
import com.example.fastfilesearch.ui.SearchScreen
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens
import com.example.fastfilesearch.ui.security.LockScreen
import com.example.fastfilesearch.ui.theme.FastFileSearchTheme

class MainActivity : FragmentActivity() {

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var lockManager: AppLockManager
    private var pendingProcessText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockManager = AppLockManager.getInstance(this)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsState()
            val isLocked by lockManager.isLocked.collectAsState()

            val isDark = when (uiState.appTheme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            // Dynamically set system status and navigation bar icon colors based on active theme
            val insetsController = WindowCompat.getInsetsController(window, window.decorView)
            insetsController.isAppearanceLightStatusBars = !isDark
            insetsController.isAppearanceLightNavigationBars = !isDark

            FastFileSearchTheme(darkTheme = isDark) {
                ProvideAppDesignTokens(isDark = isDark) {
                    val colors = LocalAppColors.current

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.screenBackground
                    ) {
                        if (isLocked) {
                            LockScreen(
                                lockManager = lockManager,
                                isDark = isDark,
                                onUnlock = {
                                    val query = pendingProcessText
                                    if (query != null) {
                                        pendingProcessText = null
                                        viewModel.onProcessTextReceived(query)
                                    }
                                }
                            )
                        } else {
                            // Folder Picker Contract using SAF
                            val folderPickerLauncher = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.OpenDocumentTree()
                            ) { uri ->
                                if (uri != null) {
                                    val docFile = DocumentFile.fromTreeUri(this, uri)
                                    val folderName = docFile?.name ?: uri.lastPathSegment ?: "المجلد المحدد"
                                    viewModel.onFolderSelected(uri, folderName)
                                }
                            }

                            SearchScreen(
                                uiState = uiState,
                                onSelectFolderClick = {
                                    lockManager.suppressNextLockOnce()
                                    folderPickerLauncher.launch(null)
                                },
                                onRefreshClick = { viewModel.onRefreshClicked() },
                                onQueryChanged = { viewModel.onQueryChanged(it) },
                                onClearQuery = { viewModel.onClearQuery() },
                                onOpenFile = {
                                    lockManager.suppressNextLockOnce()
                                    viewModel.onOpenFile(it)
                                },
                                onDismissMessage = { viewModel.onDismissMessage() },
                                onSelectWorkspace = { viewModel.onSelectWorkspace(it) },
                                onAddWorkspace = { uri, name -> viewModel.onAddWorkspace(uri, name) },
                                onRenameWorkspace = { id, name -> viewModel.onRenameWorkspace(id, name) },
                                onDeleteWorkspace = { viewModel.onDeleteWorkspace(it) },
                                onToggleBackgroundSync = { viewModel.onToggleBackgroundSync(it) },
                                onFilterChanged = { viewModel.onFilterChanged(it) },
                                onToggleStretchSingleColumn = { viewModel.onToggleStretchSingleColumn(it) },
                                onToggleShowAllRows = { viewModel.onToggleShowAllRows(it) },
                                onToggleVerticalCopyFormat = { viewModel.onToggleVerticalCopyFormat(it) },
                                onToggleExpandFullCellText = { viewModel.onToggleExpandFullCellText(it) },
                                onSetAppTheme = { viewModel.onSetAppTheme(it) },
                                onToggleRowSelection = { viewModel.toggleRowSelection(it) },
                                onToggleCellSelection = { viewModel.toggleCellSelection(it) },
                                onSelectAllRows = { viewModel.selectAllRows(it) },
                                onSelectAllCells = { viewModel.selectAllCells(it) },
                                onClearSelection = { viewModel.clearSelection() },
                                lockManager = lockManager
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        lockManager.onAppForegrounded()
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            lockManager.onAppBackgrounded()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_PROCESS_TEXT) {
            val rawText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            val cleanText = rawText?.trim()
            if (!cleanText.isNullOrBlank()) {
                if (lockManager.isLocked.value) {
                    pendingProcessText = cleanText
                } else {
                    viewModel.onProcessTextReceived(cleanText)
                }
            }
        }
    }
}
