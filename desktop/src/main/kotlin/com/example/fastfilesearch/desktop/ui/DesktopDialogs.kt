package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fastfilesearch.desktop.licensing.WindowsDeviceIdProvider
import com.example.fastfilesearch.desktop.model.IndexedFileRecord
import com.example.fastfilesearch.desktop.model.SearchFilter
import com.example.fastfilesearch.desktop.model.SearchMethod
import com.example.fastfilesearch.desktop.model.SearchResult
import com.example.fastfilesearch.desktop.model.SupportedFileTypeGroups
import com.example.fastfilesearch.desktop.model.Workspace
import com.example.fastfilesearch.desktop.parser.DesktopFileParserDispatcher
import com.example.fastfilesearch.desktop.util.DesktopValueInteractionHelper

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopRowDetailModal(
    result: SearchResult,
    searchFilter: SearchFilter,
    verticalCopyFormat: Boolean,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val cells = remember(result) { DesktopTableCellHelper.splitIntoCells(result.snippet) }
    val isTabular = result.columnHeaders.isNotEmpty() || cells.size > 1
    val formattedText = remember(result, verticalCopyFormat) {
        if (isTabular) {
            DesktopValueInteractionHelper.formatRow(cells, result.columnHeaders, verticalCopyFormat)
        } else {
            result.snippet
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(620.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = result.fileName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (!result.sheetName.isNullOrBlank()) {
                                "${result.sheetName} • صف ${result.rowNumber ?: ""}"
                            } else {
                                result.location
                            },
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isTabular) {
                        val maxCols = maxOf(cells.size, result.columnHeaders.size)
                        for (idx in 0 until maxCols) {
                            val col = result.columnHeaders.getOrNull(idx)?.takeIf { it.isNotBlank() } ?: "عمود ${idx + 1}"
                            val v = cells.getOrNull(idx).orEmpty()
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.subCardBackground)
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.textSecondary
                                    )
                                    if (v.isNotBlank()) {
                                        IconButton(onClick = { onCopy(v) }, modifier = Modifier.size(24.dp)) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "نسخ الحقل",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                SelectionContainer {
                                    Text(
                                        text = if (v.isBlank()) {
                                            androidx.compose.ui.text.AnnotatedString("—")
                                        } else {
                                            DesktopTextHighlighter.highlight(
                                                text = v,
                                                query = result.query,
                                                method = searchFilter.method,
                                                ignoreDiacritics = searchFilter.ignoreDiacritics,
                                                highlightColor = colors.highlightBg,
                                                highlightTextColor = colors.highlightFg
                                            )
                                        },
                                        fontSize = 13.5.sp,
                                        lineHeight = 20.sp,
                                        color = colors.textPrimary
                                    )
                                }

                                InteractiveValueChips(text = v, onCopy = onCopy)
                            }
                        }
                    } else {
                        SelectionContainer {
                            Text(
                                text = DesktopTextHighlighter.highlight(
                                    text = result.snippet,
                                    query = result.query,
                                    method = searchFilter.method,
                                    ignoreDiacritics = searchFilter.ignoreDiacritics,
                                    highlightColor = colors.highlightBg,
                                    highlightTextColor = colors.highlightFg
                                ),
                                fontSize = 14.sp,
                                lineHeight = 22.sp,
                                color = colors.textPrimary
                            )
                        }
                        InteractiveValueChips(text = result.snippet, onCopy = onCopy)
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPrimaryButton(
                        text = "نسخ البيانات",
                        icon = Icons.Outlined.ContentCopy,
                        onClick = { onCopy(formattedText) },
                        modifier = Modifier.weight(1f)
                    )
                    DesktopSecondaryButton(
                        text = "إغلاق",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopCellDetailModal(
    title: String,
    subtitle: String?,
    text: String,
    query: String,
    searchFilter: SearchFilter,
    onCopy: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesktopColors.current
    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(560.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 580.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Text(text = subtitle, fontSize = 12.sp, color = colors.textSecondary)
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = if (query.isNotBlank() && text.isNotBlank()) {
                                DesktopTextHighlighter.highlight(
                                    text = text,
                                    query = query,
                                    method = searchFilter.method,
                                    ignoreDiacritics = searchFilter.ignoreDiacritics,
                                    highlightColor = colors.highlightBg,
                                    highlightTextColor = colors.highlightFg
                                )
                            } else {
                                androidx.compose.ui.text.AnnotatedString(text.ifBlank { "—" })
                            },
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            color = colors.textPrimary
                        )
                    }
                    InteractiveValueChips(text = text, onCopy = onCopy)
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPrimaryButton(
                        text = "نسخ النص",
                        icon = Icons.Outlined.ContentCopy,
                        onClick = { onCopy(text) },
                        modifier = Modifier.weight(1f)
                    )
                    DesktopSecondaryButton(
                        text = "إغلاق",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InteractiveValueChips(
    text: String,
    onCopy: (String) -> Unit
) {
    val colors = LocalDesktopColors.current
    val phones = remember(text) { DesktopValueInteractionHelper.extractPhoneNumbers(text) }
    val urls = remember(text) { DesktopValueInteractionHelper.extractUrls(text) }
    val emails = remember(text) { DesktopValueInteractionHelper.extractEmails(text) }

    if (phones.isEmpty() && urls.isEmpty() && emails.isEmpty()) return

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        phones.forEach { phone ->
            ActionChip(
                icon = Icons.Outlined.Phone,
                label = phone,
                onClick = { onCopy(phone) }
            )
        }
        urls.forEach { url ->
            ActionChip(
                icon = Icons.Outlined.Language,
                label = "فتح الرابط",
                onClick = { DesktopSystemActions.openUrl(url) }
            )
        }
        emails.forEach { email ->
            ActionChip(
                icon = Icons.Outlined.Email,
                label = email,
                onClick = {
                    val res = DesktopSystemActions.openMailto(email)
                    if (res.isFailure) onCopy(email)
                }
            )
        }
    }
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalDesktopColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(colors.cardBackground)
            .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 5.dp)
    ) {
        Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(14.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}

@Composable
fun DesktopFileDetailsDialog(
    fileName: String,
    fileUri: String,
    matchesCount: Int,
    fileRecord: IndexedFileRecord?,
    workspaceName: String?,
    onOpenFile: () -> Unit,
    onRevealInExplorer: () -> Unit,
    onCopyPath: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val visual = remember(fileName) { DesktopFileTypeHelper.getVisual(fileName) }
    val ext = DesktopFileParserDispatcher.getExtension(fileName).uppercase()
    val typeLabel = if (ext.isNotBlank()) "${visual.typeName} (.$ext)" else visual.typeName

    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(500.dp)) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(visual.icon, contentDescription = null, tint = visual.color, modifier = Modifier.size(22.dp))
                        Text("تفاصيل الملف", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.subCardBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileDetailRow("اسم الملف", fileName)
                    FileDetailRow("الصيغة", typeLabel)
                    if (!workspaceName.isNullOrBlank()) {
                        FileDetailRow("المجلد", workspaceName)
                    }
                    FileDetailRow(
                        "الحجم",
                        if (fileRecord != null) DesktopTableCellHelper.formatFileSize(fileRecord.size) else "غير متوفر"
                    )
                    FileDetailRow(
                        "آخر تعديل",
                        if (fileRecord != null) DesktopTableCellHelper.formatTimestamp(fileRecord.lastModified) else "غير متوفر"
                    )
                    if (fileRecord != null && fileRecord.indexedAt > 0L) {
                        FileDetailRow("تاريخ الفهرسة", DesktopTableCellHelper.formatTimestamp(fileRecord.indexedAt))
                    }
                    FileDetailRow("النتائج المطابقة", "$matchesCount")

                    HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("المسار", fontSize = 12.sp, color = colors.textSecondary)
                        IconButton(onClick = { onCopyPath(fileUri) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ المسار", tint = colors.textSecondary, modifier = Modifier.size(15.dp))
                        }
                    }
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        Text(
                            text = fileUri,
                            fontSize = 11.5.sp,
                            fontFamily = FontFamily.Monospace,
                            color = colors.textPrimary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopPrimaryButton(
                        text = "فتح الملف",
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = {
                            onDismiss()
                            onOpenFile()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DesktopSecondaryButton(
                        text = "فتح موقعه",
                        icon = Icons.Outlined.FolderOpen,
                        onClick = {
                            onDismiss()
                            onRevealInExplorer()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FileDetailRow(label: String, value: String) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(label, fontSize = 12.sp, color = colors.textSecondary)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopFilterDialog(
    currentFilter: SearchFilter,
    defaultMethod: SearchMethod,
    workspaces: List<Workspace>,
    activeWorkspace: Workspace?,
    indexedFiles: List<IndexedFileRecord>,
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalDesktopColors.current

    var tempMethod by remember(currentFilter) { mutableStateOf(currentFilter.method) }
    var tempIgnoreDiacritics by remember(currentFilter) { mutableStateOf(currentFilter.ignoreDiacritics) }
    var tempWorkspaceIds by remember(currentFilter) { mutableStateOf(currentFilter.selectedWorkspaceIds) }
    var tempExtensions by remember(currentFilter) { mutableStateOf(currentFilter.selectedExtensions) }
    var tempFileUris by remember(currentFilter) { mutableStateOf(currentFilter.selectedFileUris) }
    var fileQuery by remember { mutableStateOf("") }

    val availableFiles = remember(indexedFiles, activeWorkspace, tempExtensions, tempWorkspaceIds) {
        indexedFiles.filter { file ->
            val matchWs = if (activeWorkspace != null) {
                file.workspaceId == activeWorkspace.id
            } else {
                tempWorkspaceIds.isEmpty() || file.workspaceId in tempWorkspaceIds
            }
            val matchExt = tempExtensions.isEmpty() ||
                DesktopFileParserDispatcher.getExtension(file.fileName) in tempExtensions
            matchWs && matchExt
        }
    }

    val displayedFiles = remember(availableFiles, fileQuery) {
        if (fileQuery.isBlank()) availableFiles else availableFiles.filter {
            it.fileName.contains(fileQuery, ignoreCase = true)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(640.dp)) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 680.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FilterList, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                        Text("التصفية المتقدمة", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = colors.textSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 1. Search Method + Diacritics
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.subCardBackground)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("طريقة البحث", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchMethod.entries.forEach { m ->
                                val selected = tempMethod == m
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) colors.controlActiveBackground else colors.cardBackground)
                                        .border(1.dp, if (selected) colors.controlActiveBackground else colors.divider, RoundedCornerShape(8.dp))
                                        .clickable { tempMethod = m }
                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = m.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) colors.controlActiveContent else colors.textPrimary
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("تجاهل التشكيل والهمزات", fontSize = 13.5.sp, color = colors.textPrimary)
                            Switch(
                                checked = tempIgnoreDiacritics,
                                onCheckedChange = { tempIgnoreDiacritics = it }
                            )
                        }
                    }

                    // 2. File Types (8 groups)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.subCardBackground)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("نوع الملف", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                            if (tempExtensions.isNotEmpty()) {
                                Text(
                                    text = "مسح التحديد (الكل)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.clickable { tempExtensions = emptySet() }.padding(4.dp)
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SupportedFileTypeGroups.ALL_GROUPS.forEach { grp ->
                                val selected = grp.extensions.any { it in tempExtensions }
                                val visual = DesktopFileTypeHelper.getVisualForGroup(grp)
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selected) colors.cardBackground else Color.Transparent)
                                        .border(
                                            width = if (selected) 1.5.dp else 1.dp,
                                            color = if (selected) colors.controlActiveBackground else colors.divider,
                                            shape = RoundedCornerShape(9.dp)
                                        )
                                        .clickable {
                                            tempExtensions = if (selected) {
                                                tempExtensions - grp.extensions
                                            } else {
                                                tempExtensions + grp.extensions
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(15.dp))
                                    Text(
                                        text = grp.displayName,
                                        fontSize = 12.5.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    if (selected) {
                                        Icon(Icons.Default.Check, null, tint = colors.textPrimary, modifier = Modifier.size(13.dp))
                                    }
                                }
                            }
                        }
                    }

                    // 3. Workspaces (when activeWorkspace is All and workspaces > 1)
                    if (activeWorkspace == null && workspaces.size > 1) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.subCardBackground)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("المجلدات", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                                if (tempWorkspaceIds.isNotEmpty()) {
                                    Text(
                                        text = "مسح التحديد (الكل)",
                                        fontSize = 12.sp,
                                        color = colors.textSecondary,
                                        modifier = Modifier.clickable { tempWorkspaceIds = emptySet() }.padding(4.dp)
                                    )
                                }
                            }
                            workspaces.forEach { ws ->
                                val selected = ws.id in tempWorkspaceIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selected) colors.cardBackground else Color.Transparent)
                                        .clickable {
                                            tempWorkspaceIds = if (selected) tempWorkspaceIds - ws.id else tempWorkspaceIds + ws.id
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Folder, null, tint = colors.textPrimary, modifier = Modifier.size(17.dp))
                                        Text(ws.name, fontSize = 13.sp, color = colors.textPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (selected) {
                                        Box(
                                            modifier = Modifier.size(18.dp).clip(CircleShape).background(colors.controlActiveBackground),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = colors.controlActiveContent, modifier = Modifier.size(11.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 4. Specific Files Selection
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.subCardBackground)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختيار الملفات (${availableFiles.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            if (tempFileUris.isNotEmpty()) {
                                Text(
                                    text = "إلغاء التحديد (${tempFileUris.size})",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
                                    modifier = Modifier.clickable { tempFileUris = emptySet() }.padding(4.dp)
                                )
                            }
                        }

                        if (availableFiles.isEmpty()) {
                            Text(
                                text = "لا توجد ملفات مفهرسة تطابق التصفية المختارة.",
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        } else {
                            if (availableFiles.size > 5) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.cardBackground)
                                        .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Search, null, tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                                    BasicTextField(
                                        value = fileQuery,
                                        onValueChange = { fileQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = colors.textPrimary, fontSize = 13.sp),
                                        cursorBrush = SolidColor(colors.textPrimary),
                                        modifier = Modifier.weight(1f),
                                        decorationBox = { inner ->
                                            if (fileQuery.isEmpty()) {
                                                Text("تصفية أسماء الملفات...", fontSize = 12.5.sp, color = colors.textSecondary)
                                            }
                                            inner()
                                        }
                                    )
                                    if (fileQuery.isNotEmpty()) {
                                        IconButton(onClick = { fileQuery = "" }, modifier = Modifier.size(20.dp)) {
                                            Icon(Icons.Default.Close, null, tint = colors.textSecondary, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                displayedFiles.forEach { file ->
                                    val selected = file.uri in tempFileUris
                                    val visual = DesktopFileTypeHelper.getVisual(file.fileName)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) colors.cardBackground else Color.Transparent)
                                            .clickable {
                                                tempFileUris = if (selected) tempFileUris - file.uri else tempFileUris + file.uri
                                            }
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(visual.icon, null, tint = visual.color, modifier = Modifier.size(17.dp))
                                        Text(
                                            text = file.fileName,
                                            fontSize = 12.5.sp,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(17.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(if (selected) colors.controlActiveBackground else Color.Transparent)
                                                .border(
                                                    1.5.dp,
                                                    if (selected) colors.controlActiveBackground else colors.divider,
                                                    RoundedCornerShape(4.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (selected) {
                                                Icon(Icons.Default.Check, null, tint = colors.controlActiveContent, modifier = Modifier.size(11.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopSecondaryButton(
                        text = "إعادة ضبط",
                        onClick = {
                            onApply(SearchFilter(method = defaultMethod))
                        },
                        modifier = Modifier.weight(0.4f)
                    )
                    DesktopPrimaryButton(
                        text = "تطبيق التصفية",
                        onClick = {
                            onApply(
                                SearchFilter(
                                    method = tempMethod,
                                    ignoreDiacritics = tempIgnoreDiacritics,
                                    selectedWorkspaceIds = tempWorkspaceIds,
                                    selectedExtensions = tempExtensions,
                                    selectedFileUris = tempFileUris
                                )
                            )
                        },
                        modifier = Modifier.weight(0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopAboutScreen(
    onStatusMessage: (String) -> Unit = {}
) {
    val colors = LocalDesktopColors.current
    var policyTitle by remember { mutableStateOf<String?>(null) }
    var policyBody by remember { mutableStateOf<String?>(null) }
    var localToast by remember { mutableStateOf<String?>(null) }

    fun notify(msg: String) {
        localToast = msg
        onStatusMessage(msg)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "حول بحث آمن",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary
        )

        localToast?.let { msg ->
            DesktopSectionCard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(msg, fontSize = 13.sp, color = colors.textPrimary)
                    Text(
                        text = "إغلاق",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        modifier = Modifier.clickable { localToast = null }
                    )
                }
            }
        }

        DesktopSectionCard {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "بحث آمن — إصدار سطح المكتب",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Text(
                    text = "بحث آمن محرك بحث محلي فائق السرعة وفهرسة متقدمة لمستندات أوفيس والجداول وقواعد بيانات Access وملفات PDF والنصوص مع مطابقة عربية ذكية ودعم كامل دون اتصال.",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp,
                    color = colors.textSecondary
                )
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("إصدار التطبيق", fontSize = 13.sp, color = colors.textSecondary)
                    Text(WindowsDeviceIdProvider.DESKTOP_APP_VERSION, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
                }
            }
        }

        DesktopSectionCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Eng. Al-Hussein Al-Mutawakel", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                Text("تطوير وتصميم", fontSize = 12.5.sp, color = colors.textSecondary)
                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                ContactPhoneRow("776472265", "https://wa.me/967776472265", onCopy = {
                    DesktopSystemActions.copyToClipboard(it)
                    notify("تم نسخ الرقم $it إلى الحافظة.")
                })
                ContactPhoneRow("730118144", "https://wa.me/967730118144", onCopy = {
                    DesktopSystemActions.copyToClipboard(it)
                    notify("تم نسخ الرقم $it إلى الحافظة.")
                })

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val email = "alhussein.ali.almutawakel@gmail.com"
                            DesktopSystemActions.copyToClipboard(email)
                            notify("تم نسخ البريد الإلكتروني إلى الحافظة.")
                        }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Email, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                        Text("alhussein.ali.almutawakel@gmail.com", fontSize = 13.5.sp, color = colors.textPrimary)
                    }
                    IconButton(
                        onClick = { DesktopSystemActions.openMailto("alhussein.ali.almutawakel@gmail.com") },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "فتح البريد", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        DesktopSectionCard {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PolicyRow("اتفاقية الخدمة") {
                    policyTitle = "اتفاقية الخدمة"
                    policyBody = "تطبيق «بحث آمن» هو أداة للبحث المحلي في الملفات والمستندات على جهازك دون إرسال أي بيانات إلى خوادم خارجية."
                }
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                PolicyRow("سياسة الاستخدام") {
                    policyTitle = "سياسة الاستخدام"
                    policyBody = "يتم استخدام التطبيق للبحث في المستندات والملفات الموجودة في المجلدات التي تحددها محلياً على جهازك."
                }
                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)
                PolicyRow("سياسة الخصوصية") {
                    policyTitle = "سياسة الخصوصية"
                    policyBody = "خصوصيتك محمية 100%. التطبيق يعمل بالكامل دون اتصال بالإنترنت (Offline)، ولا يجمع أو يشارك أي معلومات شخصية أو ملفات."
                }
            }
        }

        Text(
            text = "© 2026 Eng. Al-Hussein Al-Mutawakel. جميع الحقوق محفوظة.",
            fontSize = 12.sp,
            color = colors.textSecondary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }

    if (policyTitle != null && policyBody != null) {
        Dialog(onDismissRequest = { policyTitle = null; policyBody = null }) {
            DesktopSectionCard(modifier = Modifier.width(440.dp)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(policyTitle!!, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                    Text(policyBody!!, fontSize = 13.5.sp, lineHeight = 20.sp, color = colors.textSecondary)
                    DesktopPrimaryButton(
                        text = "حسناً",
                        onClick = { policyTitle = null; policyBody = null },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactPhoneRow(
    phone: String,
    whatsappUrl: String,
    onCopy: (String) -> Unit
) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCopy(phone) }
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Outlined.Call, null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
            Text(phone, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = { onCopy(phone) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ الرقم", tint = colors.textSecondary, modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = { DesktopSystemActions.openUrl(whatsappUrl) }, modifier = Modifier.size(28.dp)) {
                Icon(Icons.AutoMirrored.Outlined.Chat, contentDescription = "واتساب", tint = colors.textSecondary, modifier = Modifier.size(17.dp))
            }
        }
    }
}

@Composable
private fun PolicyRow(title: String, onClick: () -> Unit) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Text("عرض", fontSize = 12.sp, color = colors.textSecondary)
    }
}
