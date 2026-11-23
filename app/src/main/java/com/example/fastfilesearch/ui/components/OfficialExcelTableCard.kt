package com.example.fastfilesearch.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fastfilesearch.model.RecordDetailData
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.ui.TableLayoutCalculator
import com.example.fastfilesearch.ui.TextHighlighter
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.AppSecondaryButton
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.util.ValueInteractionHelper

private class ExcelTableThemeColors(
    val tableBg: Color,
    val rowAltBg: Color,
    val colHeaderBg: Color,
    val rowNumBg: Color,
    val gridBorderColor: Color,
    val headerBorderColor: Color,
    val headerTextColor: Color,
    val cellTextColor: Color,
    val matchBg: Color,
    val popupBg: Color,
    val sheetStripBg: Color
)

@Composable
private fun rememberExcelTableColors(isDark: Boolean): ExcelTableThemeColors {
    return remember(isDark) {
        if (isDark) {
            ExcelTableThemeColors(
                tableBg = Color(0xFF131518),
                rowAltBg = Color(0xFF16191D),
                colHeaderBg = Color(0xFF1F2328),
                rowNumBg = Color(0xFF1B1E22),
                gridBorderColor = Color(0xFF2D3137),
                headerBorderColor = Color(0xFF33383F),
                headerTextColor = Color(0xFFB5B9BF),
                cellTextColor = Color(0xFFE4E7EB),
                matchBg = Color(0xFF28251C), // Ultra-calm, subtle warm glow in dark mode
                popupBg = Color(0xFF202328),
                sheetStripBg = Color(0xFF181B1F)
            )
        } else {
            ExcelTableThemeColors(
                tableBg = Color(0xFFFFFFFF),
                rowAltBg = Color(0xFFF9FAFB),
                colHeaderBg = Color(0xFFF1F3F5),
                rowNumBg = Color(0xFFEDEDF0),
                gridBorderColor = Color(0xFFE2E4E8),
                headerBorderColor = Color(0xFFD0D3D9),
                headerTextColor = Color(0xFF495057),
                cellTextColor = Color(0xFF212529),
                matchBg = Color(0xFFFFF8E7), // Ultra-calm, delicate soft cream glow in light mode
                popupBg = Color(0xFFFFFFFF),
                sheetStripBg = Color(0xFFF1F3F5)
            )
        }
    }
}

private data class CellDetailDialogInfo(
    val columnName: String,
    val rowNumber: String,
    val fullText: String,
    val query: String = ""
)

@Composable
fun OfficialExcelTableCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme(),
    stretchSingleColumn: Boolean = true,
    showAllRows: Boolean = true,
    verticalCopyFormat: Boolean = true,
    expandFullCellText: Boolean = true,
    onCopyRow: ((String) -> Unit)? = null,
    onViewRecord: ((RecordDetailData) -> Unit)? = null,
    searchFilter: SearchFilter = SearchFilter(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }
    val colors = rememberExcelTableColors(isDark)
    val appColors = LocalAppColors.current

    // Calm, soft pastel top bar color matching the file type
    val topBarBg = remember(visual.color, isDark) {
        if (isDark) visual.color.copy(alpha = 0.16f) else visual.color.copy(alpha = 0.10f)
    }
    val topBarTextColor = if (isDark) Color(0xFFE4E7EB) else Color(0xFF1F2937)

    // Highlight text colors matching calm tint
    val highlightBg = if (isDark) Color(0xFF3D351A) else Color(0xFFFFECB3)
    val highlightFg = if (isDark) Color(0xFFFFE082) else Color(0xFF332600)

    // Multi-sheet support: Use result.sheetName first, fallback to extractSheetAndRow
    val sheetGroups = remember(group.results) {
        group.results.groupBy { result ->
            result.sheetName ?: extractSheetAndRow(result.location).first
        }
    }

    val onShareRow: (String) -> Unit = remember(context) {
        { text ->
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "مشاركة بيانات الصف")
                context.startActivity(shareIntent)
            } catch (_: Exception) {}
        }
    }

    var selectedCellInfo by remember { mutableStateOf<CellDetailDialogInfo?>(null) }
    var isCardExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isCardExpanded) 0f else 180f,
        label = "cardExpandArrow"
    )

    // Dialog showing full cell content on click - strictly Dialog matching AppDesignTokens
    if (selectedCellInfo != null) {
        val info = selectedCellInfo!!

        Dialog(
            onDismissRequest = { selectedCellInfo = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .clip(RoundedCornerShape(AppDesignTokens.CardCornerRadius))
                        .background(appColors.cardBackground)
                        .border(1.dp, appColors.divider, RoundedCornerShape(AppDesignTokens.CardCornerRadius))
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Title: Column name + Row number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = info.columnName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = appColors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            Text(
                                text = "صف ${info.rowNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = appColors.textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = appColors.divider, thickness = AppDesignTokens.DividerThickness)
                        Spacer(modifier = Modifier.height(14.dp))

                        // Scrollable Body with TextHighlighter
                        val highlightedText = remember(info.fullText, info.query, isDark, searchFilter) {
                            if (info.query.isNotBlank()) {
                                TextHighlighter.highlight(
                                    text = info.fullText,
                                    query = info.query,
                                    method = searchFilter.method,
                                    ignoreDiacritics = searchFilter.ignoreDiacritics,
                                    highlightColor = highlightBg,
                                    highlightTextColor = highlightFg
                                )
                            } else {
                                AnnotatedString(info.fullText)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            SelectionContainer {
                                Text(
                                    text = highlightedText,
                                    fontSize = 14.sp,
                                    lineHeight = 21.sp,
                                    color = appColors.textPrimary
                                )
                            }

                            // Interactive quick action chips (Phone, URL, Email)
                            val phones = remember(info.fullText) { ValueInteractionHelper.extractPhoneNumbers(info.fullText) }
                            val urls = remember(info.fullText) { ValueInteractionHelper.extractUrls(info.fullText) }
                            val emails = remember(info.fullText) { ValueInteractionHelper.extractEmails(info.fullText) }

                            if (phones.isNotEmpty() || urls.isNotEmpty() || emails.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    phones.forEach { phone ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(appColors.subCardBackground)
                                                .border(AppDesignTokens.DividerThickness, appColors.divider, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Phone,
                                                contentDescription = null,
                                                tint = appColors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = phone,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = appColors.textPrimary
                                            )
                                        }
                                    }

                                    urls.forEach { url ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(appColors.subCardBackground)
                                                .border(AppDesignTokens.DividerThickness, appColors.divider, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Language,
                                                contentDescription = null,
                                                tint = appColors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "فتح الرابط",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = appColors.textPrimary
                                            )
                                        }
                                    }

                                    emails.forEach { email ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(appColors.subCardBackground)
                                                .border(AppDesignTokens.DividerThickness, appColors.divider, RoundedCornerShape(8.dp))
                                                .clickable {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                                                        context.startActivity(intent)
                                                    } catch (_: Exception) {}
                                                }
                                                .padding(horizontal = 8.dp, vertical = 5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Email,
                                                contentDescription = null,
                                                tint = appColors.textPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = email,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = appColors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Bottom Buttons: Copy & Close in monochrome AppDesignTokens
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppPrimaryButton(
                                text = "نسخ",
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(info.fullText))
                                    selectedCellInfo = null
                                },
                                modifier = Modifier.weight(1f)
                            )

                            AppSecondaryButton(
                                text = "إغلاق",
                                onClick = { selectedCellInfo = null },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.tableBg)
    ) {
        // 1. Top Bar: Calm file-specific tint, icon, file name, matches count, and collapse/expand arrow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(topBarBg)
                .clickable { isCardExpanded = !isCardExpanded }
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = visual.icon,
                contentDescription = visual.typeName,
                tint = visual.color,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = group.fileName,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = topBarTextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Matches Count Badge (numbers only, in place of open button)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(visual.color.copy(alpha = if (isDark) 0.22f else 0.16f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${group.results.size}",
                    color = if (isDark) Color(0xFFE4E7EB) else visual.color,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (isCardExpanded) "طيّ البطاقة" else "فتح البطاقة",
                tint = if (isDark) Color(0xFF8E8E93) else Color(0xFF86868B),
                modifier = Modifier
                    .size(19.dp)
                    .rotate(arrowRotation)
            )
        }

        AnimatedVisibility(
            visible = isCardExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                HorizontalDivider(color = colors.gridBorderColor, thickness = 1.dp)

        // 2. Render Sheet Tables
        sheetGroups.forEach { (sheetName, sheetResults) ->
            // Sort rows ascending by rowNumber before rendering
            val sortedResults = remember(sheetResults) {
                sheetResults.sortedBy { res ->
                    res.rowNumber ?: extractSheetAndRow(res.location).second.toIntOrNull() ?: 0
                }
            }

            val sheetHeaders = remember(sortedResults) {
                sortedResults.firstOrNull { it.columnHeaders.isNotEmpty() }?.columnHeaders ?: emptyList()
            }

            // Pagination limit: Respects showAllRows setting
            var isExpandedAll by remember { mutableStateOf(false) }
            val displayedResults = if (showAllRows || isExpandedAll || sortedResults.size <= 20) {
                sortedResults
            } else {
                sortedResults.take(20)
            }
            val remainingRowsCount = sortedResults.size - displayedResults.size

            val rowsData = remember(displayedResults) {
                displayedResults.map { res ->
                    val rowNum = res.rowNumber?.toString() ?: extractSheetAndRow(res.location).second
                    val cells = TableCellHelper.splitIntoCells(res.snippet)
                    Triple(rowNum, cells, res)
                }
            }

            val maxCols = remember(rowsData) {
                rowsData.maxOfOrNull { it.second.size }?.coerceAtLeast(1) ?: 1
            }

            // Sheet Name Strip if more than 1 sheet
            if (sheetGroups.size > 1 || sheetName != "ورقة 1") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.sheetStripBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(visual.color)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sheetName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.headerTextColor
                    )
                }
                HorizontalDivider(color = colors.gridBorderColor, thickness = 0.5.dp)
            }

            var menuExpandedRowIndex by remember { mutableStateOf<Int?>(null) }
            var sheetMenuExpanded by remember { mutableStateOf(false) }

            // 3. Exact Data Grid with Dynamic TextMeasurer Calculations
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val totalWidth = maxWidth
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()

                val cellTextStyle = remember { TextStyle(fontSize = 13.sp) }
                val headerTextStyle = remember { TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold) }
                val rowNumTextStyle = remember {
                    TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                val dims = remember(
                    rowsData,
                    sheetHeaders,
                    maxCols,
                    totalWidth,
                    stretchSingleColumn,
                    density
                ) {
                    TableLayoutCalculator.calculate(
                        rowsData = rowsData,
                        columnHeaders = sheetHeaders,
                        maxCols = maxCols,
                        totalContainerWidth = totalWidth,
                        stretchSingleColumn = stretchSingleColumn,
                        textMeasurer = textMeasurer,
                        density = density,
                        cellTextStyle = cellTextStyle,
                        headerTextStyle = headerTextStyle,
                        rowNumTextStyle = rowNumTextStyle
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    Column(modifier = Modifier.width(dims.totalTableWidth)) {
                        // Header Row
                        Row(
                            modifier = Modifier
                                .height(38.dp)
                                .background(colors.colHeaderBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-left corner icon with interactive sheet options menu
                            Box(
                                modifier = Modifier
                                    .width(dims.rowNumWidth)
                                    .height(38.dp)
                                    .border(0.5.dp, colors.headerBorderColor)
                                    .background(colors.colHeaderBg)
                                    .clickable { sheetMenuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "خيارات الورقة",
                                    tint = colors.headerTextColor,
                                    modifier = Modifier.size(19.dp)
                                )

                                DropdownMenu(
                                    expanded = sheetMenuExpanded,
                                    onDismissRequest = { sheetMenuExpanded = false },
                                    modifier = Modifier
                                        .background(colors.popupBg)
                                        .border(0.5.dp, colors.gridBorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "نسخ جميع الصفوف",
                                                color = colors.cellTextColor,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            sheetMenuExpanded = false
                                            val allText = if (verticalCopyFormat) {
                                                rowsData.joinToString("\n----------------------------------------\n") { (rNum, cells, _) ->
                                                    "صف $rNum:\n" + formatRowText(cells, sheetHeaders, vertical = true)
                                                }
                                            } else {
                                                rowsData.joinToString("\n") { (rNum, cells, _) ->
                                                    "صف $rNum: " + formatRowText(cells, sheetHeaders, vertical = false)
                                                }
                                            }
                                            clipboardManager.setText(AnnotatedString(allText))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = colors.headerTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }

                            // Column Titles
                            for (colIndex in 0 until maxCols) {
                                val colW = dims.columnWidths.getOrElse(colIndex) { 110.dp }
                                val colTitle = sheetHeaders.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"
                                Box(
                                    modifier = Modifier
                                        .width(colW)
                                        .height(38.dp)
                                        .border(0.5.dp, colors.headerBorderColor)
                                        .background(colors.colHeaderBg)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.headerTextColor,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        // Data Rows
                        rowsData.forEachIndexed { index, (rowNum, cells, result) ->
                            val isEven = index % 2 == 0
                            val rowBg = if (isEven) colors.tableBg else colors.rowAltBg

                            Row(
                                modifier = Modifier
                                    .background(rowBg)
                                    .height(IntrinsicSize.Min),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Row Number Cell
                                Box(
                                    modifier = Modifier
                                        .width(dims.rowNumWidth)
                                        .fillMaxHeight()
                                        .defaultMinSize(minHeight = 42.dp)
                                        .border(0.5.dp, colors.headerBorderColor)
                                        .background(colors.rowNumBg)
                                        .clickable { menuExpandedRowIndex = index }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowNum,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.headerTextColor,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Popup Menu on Row Number click
                                    DropdownMenu(
                                        expanded = menuExpandedRowIndex == index,
                                        onDismissRequest = { menuExpandedRowIndex = null },
                                        modifier = Modifier
                                            .background(appColors.elevatedSurface)
                                            .border(AppDesignTokens.DividerThickness, appColors.divider, RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                                    ) {
                                        val formattedRow = remember(cells, sheetHeaders, verticalCopyFormat) {
                                            formatRowText(cells, sheetHeaders, vertical = verticalCopyFormat)
                                        }

                                        // 1. عرض
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "عرض",
                                                    color = appColors.textPrimary,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                menuExpandedRowIndex = null
                                                onViewRecord?.invoke(
                                                    RecordDetailData(
                                                        fileName = group.fileName,
                                                        sheetName = sheetName,
                                                        rowNumber = rowNum,
                                                        cells = cells,
                                                        headers = sheetHeaders,
                                                        query = result.query,
                                                        searchFilter = searchFilter
                                                    )
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Visibility,
                                                    contentDescription = "عرض",
                                                    tint = appColors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )

                                        // 2. نسخ
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "نسخ",
                                                    color = appColors.textPrimary,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                menuExpandedRowIndex = null
                                                if (onCopyRow != null) {
                                                    onCopyRow(formattedRow)
                                                } else {
                                                    clipboardManager.setText(AnnotatedString(formattedRow))
                                                }
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = appColors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )

                                        // 3. مشاركة
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "مشاركة",
                                                    color = appColors.textPrimary,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                menuExpandedRowIndex = null
                                                onShareRow(formattedRow)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Outlined.Share,
                                                    contentDescription = "مشاركة",
                                                    tint = appColors.textSecondary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // Cells in Row: ONLY background is tinted on match, borders remain identical to all cells
                                for (colIndex in 0 until maxCols) {
                                    val cellText = cells.getOrNull(colIndex) ?: ""
                                    val colW = dims.columnWidths.getOrElse(colIndex) { 110.dp }
                                    val colTitle = sheetHeaders.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"

                                    val isMatch = remember(cellText, result.query) {
                                        TableCellHelper.isCellMatching(cellText, result.query)
                                    }

                                    val cellBg = if (isMatch) colors.matchBg else Color.Transparent

                                    // Align start for single column and long text cells (> 22 chars), center for short/numeric
                                    val isLong = cellText.length > 22 || maxCols == 1
                                    val contentAlignment = if (isLong) Alignment.CenterStart else Alignment.Center
                                    val textAlign = if (isLong) TextAlign.Start else TextAlign.Center

                                    Box(
                                        modifier = Modifier
                                            .width(colW)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 42.dp)
                                            .border(0.5.dp, colors.gridBorderColor) // Uniform grid line for ALL cells
                                            .background(cellBg)
                                            .clickable {
                                                if (cellText.isNotBlank()) {
                                                    selectedCellInfo = CellDetailDialogInfo(
                                                        columnName = colTitle,
                                                        rowNumber = rowNum,
                                                        fullText = cellText,
                                                        query = result.query
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = contentAlignment
                                    ) {
                                        if (cellText.isNotEmpty()) {
                                            val highlightedText = remember(cellText, result.query, isDark, searchFilter) {
                                                TextHighlighter.highlight(
                                                    text = cellText,
                                                    query = result.query,
                                                    method = searchFilter.method,
                                                    ignoreDiacritics = searchFilter.ignoreDiacritics,
                                                    highlightColor = highlightBg,
                                                    highlightTextColor = highlightFg
                                                )
                                            }

                                            Text(
                                                text = highlightedText,
                                                fontSize = 13.sp,
                                                color = colors.cellTextColor,
                                                maxLines = if (expandFullCellText) Int.MAX_VALUE else 4,
                                                overflow = if (expandFullCellText) TextOverflow.Clip else TextOverflow.Ellipsis,
                                                textAlign = textAlign,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Pagination button: Show more rows if exceeding 20 when showAllRows is disabled
            if (!showAllRows && remainingRowsCount > 0) {
                TextButton(
                    onClick = { isExpandedAll = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.rowAltBg)
                        .padding(vertical = 4.dp)
                ) {
                    Text(
                        text = "عرض المزيد (+$remainingRowsCount صف)",
                        color = Color(0xFF64B5F6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = colors.gridBorderColor, thickness = 1.dp)
        }
    }
}
}
}

/**
 * Formats row cell values cleanly for clipboard copy and system share.
 * When vertical is true: produces "Header: Value\nHeader: Value"
 * When vertical is false: produces "Header: Value - Header: Value"
 */
private fun formatRowText(cells: List<String>, headers: List<String>, vertical: Boolean): String {
    val pairs = cells.mapIndexedNotNull { index, cell ->
        if (cell.isNotBlank()) {
            val colName = headers.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "عمود ${index + 1}"
            "$colName: $cell"
        } else null
    }

    val delimiter = if (vertical) "\n" else " - "
    return if (pairs.isNotEmpty()) {
        pairs.joinToString(delimiter)
    } else {
        cells.filter { it.isNotBlank() }.joinToString(delimiter)
    }
}

/**
 * Fallback parser for location string when sheetName or rowNumber are null.
 */
private fun extractSheetAndRow(location: String): Pair<String, String> {
    var sheet = "ورقة 1"
    var row = "1"

    if (location.contains("ورقة:")) {
        val parts = location.split("،")
        sheet = parts.getOrNull(0)?.substringAfter("ورقة:")?.trim() ?: "ورقة 1"
        row = parts.getOrNull(1)?.replace("صف:", "")?.replace("صف", "")?.trim() ?: "1"
    } else if (location.contains("سطر")) {
        sheet = "بيانات جدولية"
        row = location.replace("سطر", "").replace(":", "").trim()
    } else {
        row = location.filter { it.isDigit() }.ifEmpty { "1" }
    }

    return Pair(sheet, row)
}
