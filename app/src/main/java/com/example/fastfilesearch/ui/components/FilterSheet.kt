package com.example.fastfilesearch.ui.components

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.SearchUiState
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchMethod
import com.example.fastfilesearch.model.SupportedFileTypeGroups
import com.example.fastfilesearch.parser.FileParserDispatcher
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppMonochromeSwitch
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterSheet(
    uiState: SearchUiState,
    onApply: (SearchFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val colors = LocalAppColors.current

    // Local copy of filter to allow cancel / apply
    val initialFilter = uiState.searchFilter
    var tempMethod by remember(initialFilter) { mutableStateOf(initialFilter.method) }
    var tempIgnoreDiacritics by remember(initialFilter) { mutableStateOf(initialFilter.ignoreDiacritics) }
    var tempSelectedExtensions by remember(initialFilter) { mutableStateOf(initialFilter.selectedExtensions) }
    var tempSelectedFileUris by remember(initialFilter) { mutableStateOf(initialFilter.selectedFileUris) }
    var fileSearchQuery by remember { mutableStateOf("") }

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
        containerColor = colors.elevatedSurface,
        shape = RoundedCornerShape(
            topStart = AppDesignTokens.BottomSheetCornerRadius,
            topEnd = AppDesignTokens.BottomSheetCornerRadius
        ),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(42.dp)
                    .height(4.5.dp)
                    .clip(CircleShape)
                    .background(colors.divider)
            )
        }
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                // Header: Title on right, Close on left
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
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "التصفية",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "إغلاق",
                            tint = colors.textSecondary,
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
                            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                            .background(colors.subCardBackground)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "طريقة البحث",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = colors.textPrimary
                        )

                        SearchMethod.values().forEach { method ->
                            val isSelected = tempMethod == method
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) colors.cardBackground else Color.Transparent)
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
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = method.description,
                                        fontSize = 12.sp,
                                        color = colors.textSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
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
                                            modifier = Modifier.size(12.dp),
                                            tint = colors.controlActiveContent
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            color = colors.divider,
                            thickness = AppDesignTokens.DividerThickness,
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
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "مطابقة (أ / إ / آ / ا) والحركات كحرف واحد",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            AppMonochromeSwitch(
                                checked = tempIgnoreDiacritics,
                                onCheckedChange = { tempIgnoreDiacritics = it }
                            )
                        }
                    }

                    // SECTION 2: نوع الملف (Grouped File Types)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                            .background(colors.subCardBackground)
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
                                color = colors.textPrimary
                            )
                            if (tempSelectedExtensions.isNotEmpty()) {
                                Text(
                                    text = "مسح التحديد (الكل)",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
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
                            color = colors.textSecondary
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SupportedFileTypeGroups.ALL_GROUPS.forEach { group ->
                                val isGroupSelected = group.extensions.any { tempSelectedExtensions.contains(it) }
                                val visual = FileTypeHelper.getVisualForGroup(group)

                                val chipBg = if (isGroupSelected) colors.cardBackground else colors.elevatedSurface

                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(
                                            width = if (isGroupSelected) 1.5.dp else 0.7.dp,
                                            color = if (isGroupSelected) colors.controlActiveBackground else colors.divider,
                                            shape = RoundedCornerShape(12.dp)
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
                                        color = colors.textPrimary
                                    )
                                    if (isGroupSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.textPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // SECTION 3: اختيار الملفات (Specific Files Selection)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                            .background(colors.subCardBackground)
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
                                color = colors.textPrimary
                            )

                            if (tempSelectedFileUris.isNotEmpty()) {
                                Text(
                                    text = "إلغاء التحديد (${tempSelectedFileUris.size})",
                                    fontSize = 12.sp,
                                    color = colors.textSecondary,
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
                                color = colors.textSecondary,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )
                        } else {
                            if (availableFiles.size > 5) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.cardBackground)
                                        .padding(horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    BasicTextField(
                                        value = fileSearchQuery,
                                        onValueChange = { fileSearchQuery = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = colors.textPrimary,
                                            fontSize = 13.sp
                                        ),
                                        decorationBox = { innerTextField ->
                                            if (fileSearchQuery.isEmpty()) {
                                                Text(
                                                    text = "تصفية أسماء الملفات...",
                                                    color = colors.textSecondary,
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
                                                tint = colors.textSecondary,
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
                                            .background(if (isFileSelected) colors.cardBackground else Color.Transparent)
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
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                                .background(if (isFileSelected) colors.controlActiveBackground else Color.Transparent)
                                                .border(
                                                    width = 1.5.dp,
                                                    color = if (isFileSelected) colors.controlActiveBackground else colors.divider,
                                                    shape = RoundedCornerShape(5.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isFileSelected) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(12.dp),
                                                    tint = colors.controlActiveContent
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom Action Bar: Reset & Apply
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
                        shape = RoundedCornerShape(AppDesignTokens.ButtonCornerRadius),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = colors.textPrimary
                        ),
                        modifier = Modifier
                            .weight(0.38f)
                            .height(50.dp)
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
                        shape = RoundedCornerShape(AppDesignTokens.ButtonCornerRadius),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.controlActiveBackground,
                            contentColor = colors.controlActiveContent
                        ),
                        modifier = Modifier
                            .weight(0.62f)
                            .height(50.dp)
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
