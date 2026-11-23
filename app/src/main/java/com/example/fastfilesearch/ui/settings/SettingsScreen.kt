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
import androidx.compose.material.icons.automirrored.filled.WrapText
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TableRows
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
    onToggleShowAllRows: (Boolean) -> Unit,
    onToggleVerticalCopyFormat: (Boolean) -> Unit,
    onToggleExpandFullCellText: (Boolean) -> Unit,
    onNavigateToWorkspaces: () -> Unit = {},
    onToggleBackgroundSync: (Boolean) -> Unit = {},
    onNavigateToTheme: () -> Unit,
    onNavigateToPermissions: () -> Unit,
    onNavigateToSupportedFormats: () -> Unit,
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

    val folderDisplayName = if (uiState.activeWorkspace != null) {
        uiState.activeWorkspace.name
    } else if (uiState.workspaces.isNotEmpty()) {
        "الكل (${uiState.workspaces.size})"
    } else {
        "لم يُحدد بعد"
    }

    val statsText = "${uiState.filesCount} ملف، ${uiState.segmentsCount} مقطع"

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
            // CARD 1: المجلدات والمزامنة وتحديث الفهرس
            AppSectionCard {
                item {
                    AppNavigationRow(
                        title = "المجلدات",
                        value = folderDisplayName,
                        icon = Icons.Default.FolderOpen,
                        showChevron = false,
                        onClick = onNavigateToWorkspaces
                    )
                }

                item {
                    AppToggleRow(
                        title = "المزامنة التلقائية في الخلفية",
                        checked = uiState.isBackgroundSyncEnabled,
                        onCheckedChange = onToggleBackgroundSync,
                        icon = Icons.Default.Sync
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

            // CARD 2: إعدادات الجداول الأربعة مع الشروحات
            AppSectionCard {
                item {
                    AppToggleRow(
                        title = "تمديد العمود الواحد في الجداول",
                        subtitle = "ملء عرض الشاشة بالكامل عند وجود عمود واحد",
                        checked = uiState.stretchSingleColumn,
                        onCheckedChange = onToggleStretchSingleColumn,
                        icon = Icons.AutoMirrored.Filled.FormatAlignRight
                    )
                }

                item {
                    AppToggleRow(
                        title = "عرض جميع الصفوف مباشرة",
                        subtitle = "إظهار كامل صفوف الجدول دون الحاجة لزر عرض المزيد",
                        checked = uiState.showAllRows,
                        onCheckedChange = onToggleShowAllRows,
                        icon = Icons.Default.TableRows
                    )
                }

                item {
                    AppToggleRow(
                        title = "نسخ عمودي بأسماء الأعمدة",
                        subtitle = "نسخ الصفوف بتنسيق رأسي (اسم العمود: القيمة)",
                        checked = uiState.verticalCopyFormat,
                        onCheckedChange = onToggleVerticalCopyFormat,
                        icon = Icons.Default.ContentCopy
                    )
                }

                item {
                    AppToggleRow(
                        title = "توسيع نص الخلية بالكامل",
                        subtitle = "عرض النص كاملاً داخل الخلية دون اقتصاره على أربعة أسطر",
                        checked = uiState.expandFullCellText,
                        onCheckedChange = onToggleExpandFullCellText,
                        icon = Icons.AutoMirrored.Filled.WrapText
                    )
                }
            }

            // CARD 3: المجموعة الأخيرة: المظهر، الأذونات، الصيغ المدعومة، حول التطبيق
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
                    AppNavigationRow(
                        title = "الأذونات",
                        icon = Icons.Default.Security,
                        showChevron = false,
                        onClick = onNavigateToPermissions
                    )
                }

                item {
                    AppNavigationRow(
                        title = "الصيغ المدعومة",
                        icon = Icons.Default.Description,
                        showChevron = false,
                        onClick = onNavigateToSupportedFormats
                    )
                }

                item {
                    AppNavigationRow(
                        title = "حول التطبيق",
                        icon = Icons.Default.Info,
                        showChevron = false,
                        onClick = onNavigateToAbout
                    )
                }
            }
        }
    }
}
