package com.example.fastfilesearch.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.model.FilteredFileType
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.model.SupportedFileTypeGroups
import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.ui.FileTypeHelper

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    uiState: SearchUiState,
    isDark: Boolean,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit
) {
    // Local copy of filter to allow cancel / apply
    val initialFilter = uiState.searchFilter
    var tempMethod by remember(initialFilter) { mutableStateOf(initialFilter.method) }
    var tempIgnoreDiacritics by remember(initialFilter) { mutableStateOf(initialFilter.ignoreDiacritics) }
    var tempSelectedExtensions by remember(initialFilter) { mutableStateOf(initialFilter.selectedExtensions) }
    var tempSelectedFileUris by remember(initialFilter) { mutableStateOf(initialFilter.selectedFileUris) }
    var fileSearchQuery by remember { mutableStateOf("") }

    // Monochromatic theme palette
    val surfaceCardColor = if (isDark) Color(0xFF242426) else Color(0xFFFFFFFF)
    val sheetContainerColor = if (isDark) Color(0xFF18181A) else Color(0xFFF7F7F9)
    val borderColor = if (isDark) Color(0xFF333336) else Color(0xFFE5E5EA)
    val itemBg = if (isDark) Color(0xFF2C2C2E) else Color(0xFFF2F2F7)
    val textPrimary = if (isDark) Color(0xFFEDEDED) else Color(0xFF1C1C1E)
    val textSecondary = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73)

    // Filter indexed files by selected file type extensions
    val availableFiles = remember(uiState.indexedFiles, tempSelectedExtensions) {
        if (tempSelectedExtensions.isEmpty()) {
            uiState.indexedFiles
        } else {
            uiState.indexedFiles.filter { file ->
                val ext = FileParserDispatcher.getExtension(file.fileName)
                tempSelectedExtensions.contains(ext)
            }
        }
    }

    val displayedFiles = remember(availableFiles, fileSearchQuery) {
        if (fileSearchQuery.isBlank()) {
            availableFiles
        } else {
            availableFiles.filter { it.fileName.contains(fileSearchQuery, ignoreCase = true) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetContainerColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color(0xFF48484A) else Color(0xFFD1D1D6))
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Header: Title and Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = null,
                            tint = textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "التصفية",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Scrollable content
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = 520.dp)
                        .verticalScroll(scrollState),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // SECTION 1: طريقة البحث (Search Method)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceCardColor)
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "طريقة البحث",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = textPrimary
                        )

                        SearchMethod.values().forEach { method ->
                            val isSelected = tempMethod == method
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) itemBg else Color.Transparent)
                                    .clickable { tempMethod = method }
                                    .padding(horizontal = 10.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = method.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = method.description,
                                        fontSize = 12.sp,
                                        color = textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                CircularCheckBadge(
                                    selected = isSelected,
                                    isDark = isDark
                                )
                            }
                        }

                        HorizontalDivider(
                            color = borderColor,
                            thickness = 0.8.dp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // Hamza and Diacritics Toggle
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تجاهل التشكيل والهمزات",
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "مطابقة (أ / إ / آ / ا) والحركات كحرف واحد",
                                    fontSize = 12.sp,
                                    color = textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Switch(
                                checked = tempIgnoreDiacritics,
                                onCheckedChange = { tempIgnoreDiacritics = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = if (isDark) Color(0xFF1C1C1E) else Color.White,
                                    checkedTrackColor = if (isDark) Color(0xFFEDEDED) else Color(0xFF2C2C2E),
                                    uncheckedThumbColor = if (isDark) Color(0xFF8E8E93) else Color(0xFF6E6E73),
                                    uncheckedTrackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA),
                                    checkedBorderColor = Color.Transparent,
                                    uncheckedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // SECTION 2: نوع الملف (File Types Grouped)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceCardColor)
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "نوع الملف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )
                            if (tempSelectedExtensions.isNotEmpty()) {
                                Text(
                                    text = "مسح التحديد (الكل)",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { tempSelectedExtensions = emptySet() }
                                        .padding(4.dp)
                                )
                            }
                        }

                        Text(
                            text = if (tempSelectedExtensions.isEmpty())
                                "البحث في جميع أنواع الملفات المدعومة"
                            else
                                "تم تحديد ${tempSelectedExtensions.size} امتداد",
                            fontSize = 12.sp,
                            color = textSecondary
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SupportedFileTypeGroups.ALL_GROUPS.forEach { group ->
                                val isGroupSelected = group.extensions.any { tempSelectedExtensions.contains(it) }
                                val visual = FileTypeHelper.getVisualForGroup(group)

                                val chipBg = if (isGroupSelected) {
                                    if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
                                } else {
                                    itemBg
                                }

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(
                                            BorderStroke(
                                                width = if (isGroupSelected) 1.5.dp else 0.8.dp,
                                                color = if (isGroupSelected) textPrimary else borderColor
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            tempSelectedExtensions = if (isGroupSelected) {
                                                tempSelectedExtensions - group.extensions
                                            } else {
                                                tempSelectedExtensions + group.extensions
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = visual.icon,
                                        contentDescription = null,
                                        tint = visual.color,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = group.displayName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = textPrimary
                                    )
                                    if (isGroupSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = textPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: اختيار الملفات (Specific File Selection)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceCardColor)
                            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختيار الملفات (${availableFiles.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = textPrimary
                            )

                            if (tempSelectedFileUris.isNotEmpty()) {
                                Text(
                                    text = "إلغاء التحديد (${tempSelectedFileUris.size})",
                                    fontSize = 12.sp,
                                    color = textSecondary,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier
                                        .clickable { tempSelectedFileUris = emptySet() }
                                        .padding(4.dp)
                                )
                            }
                        }

                        if (availableFiles.isEmpty()) {
                            Text(
                                text = "لا توجد ملفات مفهرسة تطابق النوع المختار.",
                                fontSize = 13.sp,
                                color = textSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            // File search filter input
                            if (availableFiles.size > 5) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(itemBg)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = fileSearchQuery,
                                        onValueChange = { fileSearchQuery = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = textPrimary,
                                            fontSize = 13.sp
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (fileSearchQuery.isEmpty()) {
                                                Text(
                                                    text = "تصفية أسماء الملفات...",
                                                    color = textSecondary,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            innerTextField()
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (fileSearchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = { fileSearchQuery = "" },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "مسح",
                                                tint = textSecondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            // Files List
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                displayedFiles.forEach { file ->
                                    val isFileSelected = tempSelectedFileUris.contains(file.uri)
                                    val visual = FileTypeHelper.getVisual(file.fileName)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isFileSelected) itemBg else Color.Transparent)
                                            .clickable {
                                                tempSelectedFileUris = if (isFileSelected) {
                                                    tempSelectedFileUris - file.uri
                                                } else {
                                                    tempSelectedFileUris + file.uri
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = visual.icon,
                                            contentDescription = null,
                                            tint = visual.color,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Text(
                                            text = file.fileName,
                                            fontSize = 13.sp,
                                            color = textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        MonochromeCheckboxBadge(
                                            checked = isFileSelected,
                                            isDark = isDark
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Bar: Reset & Apply Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            tempMethod = SearchMethod.CONTAINS
                            tempIgnoreDiacritics = true
                            tempSelectedExtensions = emptySet()
                            tempSelectedFileUris = emptySet()
                            fileSearchQuery = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, borderColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textPrimary
                        ),
                        modifier = Modifier
                            .weight(0.38f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "إعادة ضبط",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Button(
                        onClick = {
                            onApply(
                                SearchFilter(
                                    method = tempMethod,
                                    ignoreDiacritics = tempIgnoreDiacritics,
                                    selectedExtensions = tempSelectedExtensions,
                                    selectedFileUris = tempSelectedFileUris
                                )
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFFEDEDED) else Color(0xFF1C1C1E),
                            contentColor = if (isDark) Color(0xFF1C1C1E) else Color.White
                        ),
                        modifier = Modifier
                            .weight(0.62f)
                            .height(48.dp)
                    ) {
                        Text(
                            text = "تطبيق",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CircularCheckBadge(
    selected: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val circleColor = if (selected) {
        if (isDark) Color(0xFFF0F0F2) else Color(0xFF1C1C1E)
    } else {
        Color.Transparent
    }
    val borderCol = if (selected) {
        circleColor
    } else {
        if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC)
    }
    val iconTint = if (isDark) Color(0xFF1C1C1E) else Color.White

    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(circleColor)
            .border(width = 1.5.dp, color = borderCol, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = iconTint
            )
        }
    }
}

@Composable
fun MonochromeCheckboxBadge(
    checked: Boolean,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val boxBg = if (checked) {
        if (isDark) Color(0xFFF0F0F2) else Color(0xFF1C1C1E)
    } else {
        Color.Transparent
    }
    val borderCol = if (checked) {
        boxBg
    } else {
        if (isDark) Color(0xFF48484A) else Color(0xFFC7C7CC)
    }
    val iconTint = if (isDark) Color(0xFF1C1C1E) else Color.White

    Box(
        modifier = modifier
            .size(18.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(boxBg)
            .border(width = 1.5.dp, color = borderCol, shape = RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = iconTint
            )
        }
    }
}
