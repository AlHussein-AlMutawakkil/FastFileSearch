package com.example.fastfilesearch.ui.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.licensing.LicenseManager
import com.example.fastfilesearch.model.Workspace
import com.example.fastfilesearch.security.AppLockManager
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppSelectableRow
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun WorkspacesScreen(
    uiState: SearchUiState,
    onSelectWorkspace: (String?) -> Unit,
    onAddWorkspace: (Uri, String) -> Unit,
    onRenameWorkspace: (String, String) -> Unit,
    onDeleteWorkspace: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current

    BackHandler(onBack = onBack)

    var workspaceToRename by remember { mutableStateOf<Workspace?>(null) }
    var renameText by remember { mutableStateOf("") }
    var workspaceToDelete by remember { mutableStateOf<Workspace?>(null) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            val docFile = DocumentFile.fromTreeUri(context, uri)
            val folderName = docFile?.name ?: uri.lastPathSegment ?: "مجلد جديد"
            onAddWorkspace(uri, folderName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "المجلدات",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSectionCard {
                // Fixed option: "جميع المجلدات"
                item {
                    AppSelectableRow(
                        title = "جميع المجلدات",
                        selected = uiState.activeWorkspace == null,
                        onClick = { onSelectWorkspace(null) }
                    )
                }

                // Added workspaces list
                uiState.workspaces.forEach { ws ->
                    item {
                        WorkspaceRowItem(
                            workspace = ws,
                            isSelected = uiState.activeWorkspace?.id == ws.id,
                            onSelect = { onSelectWorkspace(ws.id) },
                            onRenameClick = {
                                workspaceToRename = ws
                                renameText = ws.name
                            },
                            onDeleteClick = {
                                workspaceToDelete = ws
                            }
                        )
                    }
                }
            }
        }

        // Bottom Add Folder Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            AppPrimaryButton(
                text = "إضافة مجلد",
                onClick = {
                    val licenseManager = LicenseManager.getInstance(context)
                    val lockManager = AppLockManager.getInstance(context)
                    if (!licenseManager.canAddWorkspace(uiState.workspaces.size)) {
                        val maxAllowed = licenseManager.getActivePayload()?.maxWorkspaces ?: 0
                        Toast.makeText(
                            context,
                            "تم الوصول إلى الحد الأقصى للمجلدات المسموح بها في ترخيصك ($maxAllowed)",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        lockManager.suppressNextLockOnce()
                        try {
                            folderPickerLauncher.launch(null)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح منتقي المجلدات: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            )
        }
    }

    // Rename Dialog
    if (workspaceToRename != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { workspaceToRename = null },
                title = {
                    Text(
                        text = "إعادة تسمية المجلد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    OutlinedTextField(
                        value = renameText,
                        onValueChange = { renameText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.textPrimary,
                            unfocusedBorderColor = colors.divider,
                            cursorColor = colors.textPrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = workspaceToRename
                            if (target != null && renameText.isNotBlank()) {
                                onRenameWorkspace(target.id, renameText.trim())
                            }
                            workspaceToRename = null
                        }
                    ) {
                        Text(
                            text = "حفظ",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workspaceToRename = null }) {
                        Text(
                            text = "إلغاء",
                            color = colors.textSecondary
                        )
                    }
                },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }

    // Delete Confirmation Dialog
    if (workspaceToDelete != null) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            AlertDialog(
                onDismissRequest = { workspaceToDelete = null },
                title = {
                    Text(
                        text = "حذف المجلد",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من حذف هذا المجلد من التطبيق؟ لن يتم حذف أي ملفات من جهازك.",
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = workspaceToDelete
                            if (target != null) {
                                onDeleteWorkspace(target.id)
                            }
                            workspaceToDelete = null
                        }
                    ) {
                        Text(
                            text = "حذف",
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { workspaceToDelete = null }) {
                        Text(
                            text = "إلغاء",
                            color = colors.textSecondary
                        )
                    }
                },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun WorkspaceRowItem(
    workspace: Workspace,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Folder,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = workspace.name,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
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

            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(colors.elevatedSurface)
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "إعادة التسمية",
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onRenameClick()
                        }
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "حذف",
                                color = colors.textPrimary,
                                fontSize = 14.sp
                            )
                        },
                        onClick = {
                            menuExpanded = false
                            onDeleteClick()
                        }
                    )
                }
            }
        }
    }
}
