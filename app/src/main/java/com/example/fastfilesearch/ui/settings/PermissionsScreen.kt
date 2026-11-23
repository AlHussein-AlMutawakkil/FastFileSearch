package com.example.fastfilesearch.ui.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fastfilesearch.ui.designsystem.AppPermissionRow
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun PermissionsScreen(
    folderUri: Uri?,
    onSelectFolderClick: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasPersistedPermission by remember {
        mutableStateOf(
            context.contentResolver.persistedUriPermissions.isNotEmpty() || folderUri != null
        )
    }

    // Auto-refresh permission status on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPersistedPermission =
                    context.contentResolver.persistedUriPermissions.isNotEmpty() || folderUri != null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "الأذونات",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSectionCard {
                item {
                    AppPermissionRow(
                        title = "الوصول إلى الملفات",
                        isGranted = hasPersistedPermission,
                        icon = Icons.Default.FolderOpen,
                        onClick = onSelectFolderClick
                    )
                }
            }
        }
    }
}
