package com.example.fastfilesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.ui.SearchScreen
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens
import com.example.fastfilesearch.ui.theme.FastFileSearchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsState()
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

                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = colors.screenBackground
                    ) {
                        SearchScreen(
                            uiState = uiState,
                            onSelectFolderClick = { folderPickerLauncher.launch(null) },
                            onRefreshClick = { viewModel.onRefreshClicked() },
                            onQueryChanged = { viewModel.onQueryChanged(it) },
                            onClearQuery = { viewModel.onClearQuery() },
                            onOpenFile = { viewModel.onOpenFile(it) },
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
                            onClearSelection = { viewModel.clearSelection() }
                        )
                    }
                }
            }
        }
    }
}
