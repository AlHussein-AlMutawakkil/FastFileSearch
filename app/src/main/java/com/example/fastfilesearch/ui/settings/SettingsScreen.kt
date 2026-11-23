package com.example.fastfilesearch.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.ui.designsystem.AppNavigationRow
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppToggleRow
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun SettingsScreen(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleStretchSingleColumn: (Boolean) -> Unit,
    onToggleShowAllRows: (Boolean) -> Unit = {},
    onToggleVerticalCopyFormat: (Boolean) -> Unit = {},
    onToggleExpandFullCellText: (Boolean) -> Unit = {},
    onNavigateToTheme: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    BackHandler(onBack = onBack)

    val themeLabel = when (uiState.appTheme) {
        "light" -> "فاتح"
        "dark" -> "مظلم"
        else -> "النظام"
    }

    val folderDisplayName = if (uiState.folderName.isNotBlank()) {
        uiState.folderName
    } else {
        "لم يُحدد بعد"
    }

    val statsText = "${uiState.filesCount} ملف، ${uiState.segmentsCount} مقطع"
    val permissionStatusText = if (uiState.folderUri != null) "ممنوح" else "مطلوب"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "الإعدادات",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // CARD 1: مجلد البحث وتحديث الفهرس
            AppSectionCard {
                item {
                    AppNavigationRow(
                        title = "مجلد البحث",
                        value = folderDisplayName,
                        icon = Icons.Default.FolderOpen,
                        showChevron = false,
                        onClick = onSelectFolderClick
                    )
                }

                item {
                    AppNavigationRow(
                        title = "تحديث الفهرس",
                        value = statsText,
                        icon = Icons.Default.Refresh,
                        showChevron = false,
                        onClick = onRefreshClick
                    )
                }
            }

            // CARD 2: المظهر وتمديد العمود الواحد
            AppSectionCard {
                item {
                    AppNavigationRow(
                        title = "المظهر",
                        value = themeLabel,
                        icon = Icons.Default.BrightnessMedium,
                        showChevron = false,
                        onClick = onNavigateToTheme
                    )
                }

                item {
                    AppToggleRow(
                        title = "تمديد العمود الواحد",
                        checked = uiState.stretchSingleColumn,
                        onCheckedChange = onToggleStretchSingleColumn,
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight
                    )
                }
            }

            // CARD 3: الأذونات وحول التطبيق
            AppSectionCard {
                item {
                    AppNavigationRow(
                        title = "الأذونات",
                        value = permissionStatusText,
                        icon = Icons.Default.Security,
                        showChevron = false,
                        onClick = onNavigateToPermissions
                    )
                }

                item {
                    AppNavigationRow(
                        title = "حول التطبيق",
                        icon = Icons.Default.Info,
                        showChevron = true,
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }
}
