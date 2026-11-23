package com.example.fastfilesearch.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.TextHighlighter
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.selection.RowKey
import com.example.fastfilesearch.ui.selection.SelectedRowData
import com.example.fastfilesearch.ui.selection.SelectionMode
import com.example.fastfilesearch.ui.selection.SelectionState

data class FileResultGroup(
    val fileUri: String,
    val fileName: String,
    val isDocument: Boolean,
    val results: List<SearchResult>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WordPdfGroupedCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopySnippet: (String) -> Unit,
    isDark: Boolean = isSystemInDarkTheme(),
    showAllRows: Boolean = true,
    selectionState: SelectionState = SelectionState(),
    onToggleRowSelection: ((SelectedRowData) -> Unit)? = null,
    searchFilter: SearchFilter = SearchFilter(),
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val appColors = LocalAppColors.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }

    val topBarBg = remember(visual.color, isDark) {
        if (isDark) visual.color.copy(alpha = 0.16f) else visual.color.copy(alpha = 0.10f)
    }
    val topBarTextColor = if (isDark) Color(0xFFE4E7EB) else Color(0xFF1F2937)

    // Highlight text colors matching calm tint
    val highlightBg = if (isDark) Color(0xFF3D351A) else Color(0xFFFFECB3)
    val highlightFg = if (isDark) Color(0xFFFFE082) else Color(0xFF332600)

    var isCardExpanded by remember { mutableStateOf(true) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isCardExpanded) 0f else 180f,
        label = "wordPdfExpandArrow"
    )

    var showUnifiedDialog by remember { mutableStateOf(false) }

    val combinedText = remember(group.results) {
        group.results
            .map { it.snippet.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("\n\n")
    }

    val query = remember(group.results) {
        group.results.firstOrNull()?.query ?: ""
    }

    // Unified Text Detail Dialog when clicked
    if (showUnifiedDialog) {
        UnifiedTextDetailDialog(
            info = TextDetailInfo(
                title = group.fileName,
                subtitle = "${group.results.size} نتائج",
                text = combinedText,
                query = query,
                searchFilter = searchFilter
            ),
            onDismissRequest = { showUnifiedDialog = false },
            isDark = isDark
        )
    }

    // Full-screen width Edge-to-Edge Column without rounded corners or side gaps, matching Excel layout
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(appColors.cardBackground)
    ) {
        // Top Bar: file icon, file name, matches count badge, and collapse arrow
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

            // Matches Count Badge (numbers only)
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
                HorizontalDivider(color = appColors.divider, thickness = AppDesignTokens.DividerThickness)

                val highlightedCombinedText = remember(combinedText, query, isDark, searchFilter) {
                    if (query.isNotBlank() && combinedText.isNotBlank()) {
                        TextHighlighter.highlight(
                            text = combinedText,
                            query = query,
                            method = searchFilter.method,
                            ignoreDiacritics = searchFilter.ignoreDiacritics,
                            highlightColor = highlightBg,
                            highlightTextColor = highlightFg
                        )
                    } else {
                        AnnotatedString(combinedText)
                    }
                }

                val rowKey = remember(group.fileUri) {
                    RowKey(fileUri = group.fileUri, sheetName = "مستند", rowIndex = 0)
                }
                val isSelected = selectionState.isRowSelected(rowKey)
                val rowData = remember(rowKey, group.fileName, combinedText) {
                    SelectedRowData(
                        key = rowKey,
                        fileName = group.fileName,
                        sheetName = "مستند",
                        rowNumber = "1",
                        cells = emptyList(),
                        headers = emptyList(),
                        isDocument = true,
                        snippet = combinedText,
                        location = group.results.firstOrNull()?.location ?: ""
                    )
                }

                val bodyBg = if (isSelected) {
                    appColors.controlActiveBackground.copy(alpha = 0.12f)
                } else {
                    appColors.cardBackground
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bodyBg)
                        .combinedClickable(
                            onClick = {
                                if (selectionState.mode == SelectionMode.ROWS) {
                                    onToggleRowSelection?.invoke(rowData)
                                } else {
                                    showUnifiedDialog = true
                                }
                            },
                            onLongClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleRowSelection?.invoke(rowData)
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = highlightedCombinedText,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        color = appColors.textPrimary,
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                HorizontalDivider(color = appColors.divider, thickness = AppDesignTokens.DividerThickness)
            }
        }
    }
}
