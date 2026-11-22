package com.example.fastfilesearch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.ui.SearchScreen
import com.example.fastfilesearch.ui.theme.FastFileSearchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FastFileSearchTheme {
                val uiState by viewModel.uiState.collectAsState()

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
                    color = MaterialTheme.colorScheme.background
                ) {
                    SearchScreen(
                        uiState = uiState,
                        onSelectFolderClick = { folderPickerLauncher.launch(null) },
                        onRefreshClick = { viewModel.onRefreshClicked() },
                        onQueryChanged = { viewModel.onQueryChanged(it) },
                        onClearQuery = { viewModel.onClearQuery() },
                        onOpenFile = { viewModel.onOpenFile(it) },
                        onDismissMessage = { viewModel.onDismissMessage() },
                        onToggleStretchSingleColumn = { viewModel.onToggleStretchSingleColumn(it) }
                    )
                }
            }
        }
    }
}
