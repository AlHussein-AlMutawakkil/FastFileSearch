package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fastfilesearch.desktop.model.IndexProgress
import com.example.fastfilesearch.desktop.model.Workspace

@Composable
fun DesktopWorkspacesScreen(
    workspaces: List<Workspace>,
    activeWorkspaceId: String? = null,
    indexProgress: IndexProgress,
    filesCount: Int,
    segmentsCount: Int,
    statusBanner: String?,
    onSelectWorkspace: (String?) -> Unit = {},
    onAddFolderClick: () -> Unit,
    onRenameWorkspace: (Workspace, String) -> Unit = { _, _ -> },
    onDeleteWorkspace: (Workspace) -> Unit,
    onRefreshIndex: () -> Unit,
    onCancelIndexing: () -> Unit
) {
    val colors = LocalDesktopColors.current

    var workspaceToRename by remember { mutableStateOf<Workspace?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var workspaceToDelete by remember { mutableStateOf<Workspace?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "إدارة المجلدات والفهرسة",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "$filesCount ملف مفهرس • $segmentsCount مقطع",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (indexProgress.isIndexing) {
                    DesktopSecondaryButton(
                        text = "إلغاء الفهرسة",
                        icon = Icons.Outlined.Close,
                        onClick = onCancelIndexing
                    )
                } else {
                    DesktopSecondaryButton(
                        text = "تحديث الفهرس",
                        icon = Icons.Outlined.Refresh,
                        enabled = workspaces.isNotEmpty(),
                        onClick = onRefreshIndex
                    )
                }

                DesktopPrimaryButton(
                    text = "إضافة مجلد",
                    icon = Icons.Outlined.CreateNewFolder,
                    enabled = !indexProgress.isIndexing,
                    onClick = onAddFolderClick
                )
            }
        }

        // Progress card during indexing
        if (indexProgress.isIndexing) {
            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = indexProgress.statusMessage.ifBlank { "جارٍ الفهرسة..." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (indexProgress.total > 0) {
                            Text(
                                text = "${indexProgress.current} / ${indexProgress.total}",
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }

                    if (indexProgress.total > 0) {
                        LinearProgressIndicator(
                            progress = { (indexProgress.current.toFloat() / indexProgress.total).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        statusBanner?.let { msg ->
            DesktopSectionCard {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }

        if (workspaces.isEmpty()) {
            DesktopSectionCard(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لم تتم إضافة أي مجلد بعد",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "اضغط على «إضافة مجلد» لاختيار مجلد ملفات من جهازك.",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            }
        } else {
            DesktopSectionCard(modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item(key = "__all_workspaces__") {
                        val allSelected = activeWorkspaceId == null
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !indexProgress.isIndexing) { onSelectWorkspace(null) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "جميع المجلدات (${workspaces.size})",
                                fontSize = 14.5.sp,
                                fontWeight = if (allSelected) FontWeight.Bold else FontWeight.Medium,
                                color = colors.textPrimary
                            )
                            if (allSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.controlActiveBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.controlActiveContent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = colors.divider, thickness = DesktopDesignTokens.DividerThickness)
                    }

                    items(workspaces, key = { it.id }) { ws ->
                        val isSelected = activeWorkspaceId == ws.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !indexProgress.isIndexing) { onSelectWorkspace(ws.id) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ws.name,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        text = ws.path,
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = colors.textSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(colors.controlActiveBackground),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = colors.controlActiveContent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            FluentTooltip("فتح المجلد في مستكشف Windows") {
                                IconButton(
                                    onClick = { DesktopSystemActions.openFile(ws.path) }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                        contentDescription = "فتح المجلد في Explorer",
                                        tint = colors.textSecondary
                                    )
                                }
                            }

                            FluentTooltip("إعادة تسمية المجلد") {
                                IconButton(
                                    onClick = {
                                        workspaceToRename = ws
                                        renameInput = ws.name
                                    },
                                    enabled = !indexProgress.isIndexing
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "إعادة تسمية المجلد",
                                        tint = colors.textSecondary
                                    )
                                }
                            }

                            FluentTooltip("حذف المجلد من الفهرس") {
                                IconButton(
                                    onClick = { workspaceToDelete = ws },
                                    enabled = !indexProgress.isIndexing
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DeleteOutline,
                                        contentDescription = "حذف المجلد",
                                        tint = colors.dangerText
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = colors.divider, thickness = DesktopDesignTokens.DividerThickness)
                    }
                }
            }
        }
    }

    // Rename Dialog
    workspaceToRename?.let { targetWs ->
        Dialog(onDismissRequest = { workspaceToRename = null }) {
            DesktopSectionCard(modifier = Modifier.width(420.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "إعادة تسمية المجلد",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    FluentTextBox(
                        value = renameInput,
                        onValueChange = { renameInput = it },
                        placeholder = "أدخل اسم المجلد...",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopPrimaryButton(
                            text = "حفظ",
                            enabled = renameInput.isNotBlank(),
                            onClick = {
                                onRenameWorkspace(targetWs, renameInput.trim())
                                workspaceToRename = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DesktopSecondaryButton(
                            text = "إلغاء",
                            onClick = { workspaceToRename = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    workspaceToDelete?.let { targetWs ->
        Dialog(onDismissRequest = { workspaceToDelete = null }) {
            DesktopSectionCard(modifier = Modifier.width(440.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "حذف المجلد",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "هل أنت متأكد من حذف المجلد «${targetWs.name}» من الفهرس؟ لن يتم حذف أي ملفات من جهازك.",
                        fontSize = 13.5.sp,
                        lineHeight = 20.sp,
                        color = colors.textSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopPrimaryButton(
                            text = "حذف",
                            onClick = {
                                workspaceToDelete = null
                                onDeleteWorkspace(targetWs)
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DesktopSecondaryButton(
                            text = "إلغاء",
                            onClick = { workspaceToDelete = null },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
