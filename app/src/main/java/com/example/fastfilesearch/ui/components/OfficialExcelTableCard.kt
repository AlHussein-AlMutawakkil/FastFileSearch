package com.example.fastfilesearch.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.TableCellHelper
import com.example.fastfilesearch.ui.TableLayoutCalculator
import com.example.fastfilesearch.ui.TextHighlighter

private object ExcelTableColors {
    val topBarBg = Color(0xFF1C1F23)
    val colHeaderBg = Color(0xFF22262B)
    val tableBg = Color(0xFF131518)
    val rowAltBg = Color(0xFF16191D)
    val rowNumBg = Color(0xFF1B1E22)
    val gridBorderColor = Color(0xFF2D3137)
    val headerBorderColor = Color(0xFF33383F)
    val headerTextColor = Color(0xFFB5B9BF)
    val cellTextColor = Color(0xFFE4E7EB)
    val matchBg = Color(0xFF1A3822)
    val matchBorder = Color(0xFF388E3C)
    val popupBg = Color(0xFF202328)
    val sheetStripBg = Color(0xFF181B1F)
    val greenDot = Color(0xFF107C41)
}

private data class CellDetailDialogInfo(
    val columnName: String,
    val rowNumber: String,
    val fullText: String
)

/**
 * Exact replica of the data table screen:
 * - Edge-to-edge dark table layout.
 * - Top header bar with file type icon, file name, and Open File button.
 * - Accurate layout calculation with TextMeasurer.
 * - Sorted rows by rowNumber.
 * - Formatted copy and share (column: value - column: value).
 * - Full content viewer dialog on cell click.
 * - 20 rows pagination limit per sheet.
 * - Interactive sheet menu on top-corner icon.
 */
@Composable
fun OfficialExcelTableCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    stretchSingleColumn: Boolean = true,
    onCopyRow: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }

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

    // Dialog showing full cell content on click
    if (selectedCellInfo != null) {
        val info = selectedCellInfo!!
        AlertDialog(
            onDismissRequest = { selectedCellInfo = null },
            title = {
                Text(
                    text = "${info.columnName} • صف ${info.rowNumber}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                SelectionContainer {
                    Text(
                        text = info.fullText,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboardManager.setText(AnnotatedString(info.fullText))
                    selectedCellInfo = null
                }) {
                    Text("نسخ", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCellInfo = null }) {
                    Text("إغلاق")
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(ExcelTableColors.tableBg)
    ) {
        // 1. Top Bar: File type icon, file name, and Open File button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ExcelTableColors.topBarBg)
                .padding(horizontal = 14.dp, vertical = 8.dp),
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
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            // Matches Count Badge
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(horizontal = 9.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${group.results.size} مطابقة",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onOpenFile,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "فتح الملف",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = ExcelTableColors.gridBorderColor, thickness = 1.dp)

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

            // Pagination limit: 20 rows per sheet initially
            var isExpandedAll by remember { mutableStateOf(false) }
            val displayedResults = if (isExpandedAll || sortedResults.size <= 20) {
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
                        .background(ExcelTableColors.sheetStripBg)
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ExcelTableColors.greenDot)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sheetName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = ExcelTableColors.headerTextColor
                    )
                }
                HorizontalDivider(color = ExcelTableColors.gridBorderColor, thickness = 0.5.dp)
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
                                .background(ExcelTableColors.colHeaderBg),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Top-left corner icon with interactive sheet options menu
                            Box(
                                modifier = Modifier
                                    .width(dims.rowNumWidth)
                                    .height(38.dp)
                                    .border(0.5.dp, ExcelTableColors.headerBorderColor)
                                    .background(ExcelTableColors.colHeaderBg)
                                    .clickable { sheetMenuExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Menu,
                                    contentDescription = "خيارات الورقة",
                                    tint = ExcelTableColors.headerTextColor,
                                    modifier = Modifier.size(19.dp)
                                )

                                DropdownMenu(
                                    expanded = sheetMenuExpanded,
                                    onDismissRequest = { sheetMenuExpanded = false },
                                    modifier = Modifier
                                        .background(ExcelTableColors.popupBg)
                                        .border(0.5.dp, ExcelTableColors.gridBorderColor, RoundedCornerShape(8.dp))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "نسخ جميع الصفوف المطابقة في هذه الورقة",
                                                color = Color.White,
                                                fontSize = 13.sp
                                            )
                                        },
                                        onClick = {
                                            sheetMenuExpanded = false
                                            val allText = rowsData.joinToString("\n") { (rNum, cells, _) ->
                                                "صف $rNum: " + formatRowText(cells, sheetHeaders)
                                            }
                                            clipboardManager.setText(AnnotatedString(allText))
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = null,
                                                tint = ExcelTableColors.headerTextColor,
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
                                        .border(0.5.dp, ExcelTableColors.headerBorderColor)
                                        .background(ExcelTableColors.colHeaderBg)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = colTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelTableColors.headerTextColor,
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
                            val rowBg = if (isEven) ExcelTableColors.tableBg else ExcelTableColors.rowAltBg

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
                                        .border(0.5.dp, ExcelTableColors.headerBorderColor)
                                        .background(ExcelTableColors.rowNumBg)
                                        .clickable { menuExpandedRowIndex = index }
                                        .padding(vertical = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rowNum,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExcelTableColors.headerTextColor,
                                        fontFamily = FontFamily.Monospace
                                    )

                                    // Popup Menu on Row Number click
                                    DropdownMenu(
                                        expanded = menuExpandedRowIndex == index,
                                        onDismissRequest = { menuExpandedRowIndex = null },
                                        modifier = Modifier
                                            .background(ExcelTableColors.popupBg)
                                            .border(0.5.dp, ExcelTableColors.gridBorderColor, RoundedCornerShape(8.dp))
                                    ) {
                                        val formattedRow = remember(cells, sheetHeaders) {
                                            formatRowText(cells, sheetHeaders)
                                        }

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "نسخ الصف",
                                                    color = Color.White,
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
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = "نسخ",
                                                    tint = ExcelTableColors.headerTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )

                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "مشاركة",
                                                    color = Color.White,
                                                    fontSize = 14.sp
                                                )
                                            },
                                            onClick = {
                                                menuExpandedRowIndex = null
                                                onShareRow(formattedRow)
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = "مشاركة",
                                                    tint = ExcelTableColors.headerTextColor,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // Cells in Row
                                for (colIndex in 0 until maxCols) {
                                    val cellText = cells.getOrNull(colIndex) ?: ""
                                    val colW = dims.columnWidths.getOrElse(colIndex) { 110.dp }
                                    val colTitle = sheetHeaders.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"

                                    val isMatch = remember(cellText, result.query) {
                                        TableCellHelper.isCellMatching(cellText, result.query)
                                    }

                                    val cellBg = if (isMatch) ExcelTableColors.matchBg else Color.Transparent
                                    val borderC = if (isMatch) ExcelTableColors.matchBorder else ExcelTableColors.gridBorderColor
                                    val borderW = if (isMatch) 1.dp else 0.5.dp

                                    // Align start for single column and long text cells (> 22 chars), center for short/numeric
                                    val isLong = cellText.length > 22 || maxCols == 1
                                    val contentAlignment = if (isLong) Alignment.CenterStart else Alignment.Center
                                    val textAlign = if (isLong) TextAlign.Start else TextAlign.Center

                                    Box(
                                        modifier = Modifier
                                            .width(colW)
                                            .fillMaxHeight()
                                            .defaultMinSize(minHeight = 42.dp)
                                            .border(borderW, borderC)
                                            .background(cellBg)
                                            .clickable {
                                                if (cellText.isNotBlank()) {
                                                    selectedCellInfo = CellDetailDialogInfo(
                                                        columnName = colTitle,
                                                        rowNumber = rowNum,
                                                        fullText = cellText
                                                    )
                                                }
                                            }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = contentAlignment
                                    ) {
                                        if (cellText.isNotEmpty()) {
                                            val highlightedText = remember(cellText, result.query) {
                                                TextHighlighter.highlight(cellText, result.query)
                                            }

                                            Text(
                                                text = highlightedText,
                                                fontSize = 13.sp,
                                                color = ExcelTableColors.cellTextColor,
                                                maxLines = 4,
                                                overflow = TextOverflow.Ellipsis,
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

            // Pagination button: Show more rows if exceeding 20
            if (remainingRowsCount > 0) {
                TextButton(
                    onClick = { isExpandedAll = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ExcelTableColors.rowAltBg)
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

            HorizontalDivider(color = ExcelTableColors.gridBorderColor, thickness = 1.dp)
        }
    }
}

/**
 * Formats row cell values cleanly for clipboard copy and system share.
 * Produces "Header1: Value1 - Header2: Value2" instead of raw tab-delimited text.
 */
private fun formatRowText(cells: List<String>, headers: List<String>): String {
    val pairs = cells.mapIndexedNotNull { index, cell ->
        if (cell.isNotBlank()) {
            val colName = headers.getOrNull(index)?.takeIf { it.isNotBlank() } ?: "عمود ${index + 1}"
            "$colName: $cell"
        } else null
    }
    return if (pairs.isNotEmpty()) {
        pairs.joinToString(" - ")
    } else {
        cells.filter { it.isNotBlank() }.joinToString(" - ")
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
