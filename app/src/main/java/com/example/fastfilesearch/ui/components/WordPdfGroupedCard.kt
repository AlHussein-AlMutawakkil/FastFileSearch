package com.example.fastfilesearch.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.SearchFilter
import com.example.fastfilesearch.model.SearchResult
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.TextHighlighter

data class FileResultGroup(
    val fileUri: String,
    val fileName: String,
    val isDocument: Boolean,
    val results: List<SearchResult>
)

@Composable
fun WordPdfGroupedCard(
    group: FileResultGroup,
    onOpenFile: () -> Unit,
    onCopySnippet: (String) -> Unit,
    isDark: Boolean = isSystemInDarkTheme(),
    showAllRows: Boolean = true,
    searchFilter: SearchFilter = SearchFilter(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visual = remember(group.fileName) { FileTypeHelper.getVisual(group.fileName) }

    val onShareSnippet: (String) -> Unit = remember(context) {
        { text ->
            try {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    putExtra(Intent.EXTRA_TEXT, text)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "مشاركة النص")
                context.startActivity(shareIntent)
            } catch (_: Exception) {}
        }
    }

    // Pagination: Respects showAllRows
    var isExpandedAll by remember { mutableStateOf(false) }
    val displayedResults = if (showAllRows || isExpandedAll || group.results.size <= 15) {
        group.results
    } else {
        group.results.take(15)
    }
    val remainingCount = group.results.size - displayedResults.size

    val headerBg = remember(visual.color, isDark) {
        if (isDark) visual.color.copy(alpha = 0.16f) else visual.color.copy(alpha = 0.10f)
    }
    val headerTextColor = if (isDark) Color(0xFFE4E7EB) else Color(0xFF1F2937)

    // Highlight text colors matching calm tint
    val highlightBg = if (isDark) Color(0xFF3D351A) else Color(0xFFFFECB3)
    val highlightFg = if (isDark) Color(0xFFFFE082) else Color(0xFF332600)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Document Header with calm, file-specific tint
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFile)
                    .background(headerBg)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(visual.color),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = visual.typeName,
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = headerTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${group.results.size} مطابقة في هذا المستند",
                        fontSize = 12.sp,
                        color = visual.color,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = onOpenFile,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "فتح الملف",
                        tint = visual.color,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(color = visual.color.copy(alpha = 0.2f), thickness = 1.dp)

            // Document Snippets List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                displayedResults.forEach { result ->
                    var isExpanded by remember { mutableStateOf(false) }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { isExpanded = !isExpanded }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(visual.color.copy(alpha = 0.12f))
                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = visual.color,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = result.location,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = visual.color
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { onCopySnippet(result.snippet) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "نسخ الفقرة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onShareSnippet(result.snippet) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "مشاركة الفقرة",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        val highlightedSnippet = remember(result.snippet, result.query, isDark, searchFilter) {
                            TextHighlighter.highlight(
                                text = result.snippet,
                                query = result.query,
                                method = searchFilter.method,
                                ignoreDiacritics = searchFilter.ignoreDiacritics,
                                highlightColor = highlightBg,
                                highlightTextColor = highlightFg
                            )
                        }

                        Text(
                            text = highlightedSnippet,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Show more button if more than 15 paragraphs and showAllRows is false
                if (!showAllRows && remainingCount > 0) {
                    TextButton(
                        onClick = { isExpandedAll = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    ) {
                        Text(
                            text = "عرض المزيد (+$remainingCount فقرة)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = visual.color
                        )
                    }
                }
            }
        }
    }
}
