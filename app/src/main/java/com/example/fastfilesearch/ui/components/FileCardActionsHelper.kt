package com.example.fastfilesearch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.IndexedFileRecord
import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun FileHeaderActionMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onOpenFile: () -> Unit,
    onExportToExcel: () -> Unit,
    onShowDetails: () -> Unit
) {
    val colors = LocalAppColors.current

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .background(colors.elevatedSurface)
            .border(
                AppDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(AppDesignTokens.MenuCornerRadius)
            )
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = "فتح الملف الأصلي",
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onOpenFile()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = "تصدير إلى Excel",
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.TableChart,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onExportToExcel()
            }
        )

        DropdownMenuItem(
            text = {
                Text(
                    text = "تفاصيل الملف",
                    color = colors.textPrimary,
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(19.dp)
                )
            },
            onClick = {
                onDismissRequest()
                onShowDetails()
            }
        )
    }
}

@Composable
fun FileExportChoiceDialog(
    fileName: String,
    onDismissRequest: () -> Unit,
    onSaveToDevice: () -> Unit,
    onShareFile: () -> Unit
) {
    val colors = LocalAppColors.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(AppDesignTokens.CardCornerRadius),
            title = {
                Text(
                    text = "تصدير نتائج الملف إلى Excel",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = fileName,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                            .background(colors.subCardBackground)
                            .border(
                                AppDesignTokens.DividerThickness,
                                colors.divider,
                                RoundedCornerShape(AppDesignTokens.SubCardCornerRadius)
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissRequest()
                                    onSaveToDevice()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "حفظ كملف Excel (.xlsx)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }

                        HorizontalDivider(color = colors.divider, thickness = AppDesignTokens.DividerThickness)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onDismissRequest()
                                    onShareFile()
                                }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Share,
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "مشاركة كملف Excel (.xlsx)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "إلغاء",
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
fun FileDetailsDialog(
    fileName: String,
    matchesCount: Int,
    fileRecord: IndexedFileRecord?,
    workspaceName: String?,
    onOpenFile: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val colors = LocalAppColors.current
    val visual = FileTypeHelper.getVisual(fileName)
    val ext = FileParserDispatcher.getExtension(fileName).uppercase()
    val typeLabel = if (ext.isNotBlank()) "${visual.typeName} (.$ext)" else visual.typeName

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            containerColor = colors.cardBackground,
            shape = RoundedCornerShape(AppDesignTokens.CardCornerRadius),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.color,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "تفاصيل الملف",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                        .background(colors.subCardBackground)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FileInfoRow(label = "اسم الملف", value = fileName)
                    FileInfoRow(label = "الصيغة", value = typeLabel)
                    if (!workspaceName.isNullOrBlank()) {
                        FileInfoRow(label = "المجلد", value = workspaceName)
                    }
                    FileInfoRow(
                        label = "الحجم",
                        value = if (fileRecord != null) TableCellHelper.formatFileSize(fileRecord.size) else "غير متوفر"
                    )
                    FileInfoRow(
                        label = "آخر تعديل",
                        value = if (fileRecord != null) TableCellHelper.formatTimestamp(fileRecord.lastModified) else "غير متوفر"
                    )
                    if (fileRecord != null && fileRecord.indexedAt > 0L) {
                        FileInfoRow(
                            label = "تاريخ الفهرسة",
                            value = TableCellHelper.formatTimestamp(fileRecord.indexedAt)
                        )
                    }
                    FileInfoRow(label = "النتائج المطابقة", value = "$matchesCount")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDismissRequest()
                        onOpenFile()
                    }
                ) {
                    Text(
                        text = "فتح الملف",
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = onDismissRequest) {
                    Text(
                        text = "إغلاق",
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}

@Composable
private fun FileInfoRow(label: String, value: String) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}
