package com.example.fastfilesearch.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState

@Composable
fun SettingsDialog(
    uiState: SearchUiState,
    onSelectFolderClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onToggleStretchSingleColumn: (Boolean) -> Unit,
    onToggleShowAllRows: (Boolean) -> Unit,
    onToggleVerticalCopyFormat: (Boolean) -> Unit,
    onToggleExpandFullCellText: (Boolean) -> Unit,
    onSetAppTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "الإعدادات",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Section 1: Folder Selection & Management
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مجلد البحث المفهرس",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = if (uiState.folderName.isNotBlank())
                            "المجلد الحالي: ${uiState.folderName} (${uiState.filesCount} ملف مفهرس)"
                        else
                            "لم يتم اختيار مجلد بعد",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = {
                                onDismiss()
                                onSelectFolderClick()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "تغيير المجلد", fontSize = 12.sp)
                        }

                        if (uiState.folderUri != null) {
                            FilledTonalButton(
                                onClick = onRefreshClick,
                                enabled = !uiState.indexProgress.isIndexing
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "تحديث الفهرس",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Section 2: Options 1 to 4 (Display and Copy Flexibility)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "خيارات عرض الجداول والنسخ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Option 1: Single column stretch toggle
                    SettingSwitchItem(
                        title = "1. تمديد ملفات العمود الواحد",
                        description = if (uiState.stretchSingleColumn)
                            "مفعل: يتمدد لملء 100% من عرض الشاشة دون تمرير أفقي"
                        else
                            "معطل: العرض حسب طول المحتوى مع التمرير كالإكسل",
                        checked = uiState.stretchSingleColumn,
                        onCheckedChange = onToggleStretchSingleColumn
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Option 2: Show all rows directly toggle
                    SettingSwitchItem(
                        title = "2. عرض جميع الصفوف مباشرة",
                        description = if (uiState.showAllRows)
                            "مفعل: عرض كافة النتائج كاملة ومباشرة بدون حد الـ 20 صفاً"
                        else
                            "معطل: عرض أول 20 صفاً مبدئياً مع زر (عرض المزيد)",
                        checked = uiState.showAllRows,
                        onCheckedChange = onToggleShowAllRows
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Option 3: Vertical copy format toggle
                    SettingSwitchItem(
                        title = "3. تنسيق النسخ الرأسي المنظم",
                        description = if (uiState.verticalCopyFormat)
                            "مفعل: كل حقل في سطر (اسم العمود: القيمة) مع فاصل بين الصفوف"
                        else
                            "معطل: سطر واحد مضغوط مفصول بشرطات (قيمة - قيمة)",
                        checked = uiState.verticalCopyFormat,
                        onCheckedChange = onToggleVerticalCopyFormat
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Option 4: Full cell text toggle
                    SettingSwitchItem(
                        title = "4. إظهار كامل محتوى الخلايا",
                        description = if (uiState.expandFullCellText)
                            "مفعل: يتمدد الصف ليظهر النص كاملاً بدون اقتطاع (...)"
                        else
                            "معطل: اقتطاع النص بعد 4 أسطر مع إمكانية النقر للعرض الكامل",
                        checked = uiState.expandFullCellText,
                        onCheckedChange = onToggleExpandFullCellText
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                // Section 3: Option 7 - App Theme (System, Dark, Light)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "مظهر التطبيق (الوضع الداكن والفاتح)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )

                    ThemeOptionRow(
                        title = "تلقائي (حسب النظام)",
                        icon = Icons.Default.BrightnessAuto,
                        selected = uiState.appTheme == "system",
                        onClick = { onSetAppTheme("system") }
                    )

                    ThemeOptionRow(
                        title = "الوضع الداكن (Dark Mode)",
                        icon = Icons.Default.DarkMode,
                        selected = uiState.appTheme == "dark",
                        onClick = { onSetAppTheme("dark") }
                    )

                    ThemeOptionRow(
                        title = "الوضع الفاتح (Light Mode)",
                        icon = Icons.Default.LightMode,
                        selected = uiState.appTheme == "light",
                        onClick = { onSetAppTheme("light") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SettingSwitchItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun ThemeOptionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else androidx.compose.ui.graphics.Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }

            RadioButton(
                selected = selected,
                onClick = onClick
            )
        }
    }
}
