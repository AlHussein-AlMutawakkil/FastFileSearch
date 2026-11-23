package com.example.fastfilesearch.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.RecordDetailData
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.util.ValueInteractionHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecordDetailScreen(
    data: RecordDetailData,
    verticalCopyFormat: Boolean = true,
    isDark: Boolean = isSystemInDarkTheme(),
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val colors = LocalAppColors.current

    val highlightBg = if (isDark) Color(0xFF3D351A) else Color(0xFFFFECB3)
    val highlightFg = if (isDark) Color(0xFFFFE082) else Color(0xFF332600)

    BackHandler(onBack = onBack)

    val maxCols = maxOf(data.cells.size, data.headers.size).coerceAtLeast(1)

    val formattedRecord = remember(data, verticalCopyFormat) {
        ValueInteractionHelper.formatRow(data.cells, data.headers, vertical = verticalCopyFormat)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .navigationBarsPadding()
        ) {
            // Top Bar with Copy and Share on left (End in RTL)
            AppTopBar(
                title = "تفاصيل السجل",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(formattedRecord))
                            Toast.makeText(context, "تم نسخ بيانات السجل", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "نسخ السجل",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            try {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_TEXT, formattedRecord)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(sendIntent, "مشاركة السجل")
                                context.startActivity(shareIntent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "مشاركة السجل",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            )

            // Sub-header Banner: File name on right, sheet/row on left
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = data.fileName,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Text(
                    text = if (!data.sheetName.isNullOrBlank() && data.sheetName != "ورقة 1") {
                        "${data.sheetName} • صف ${data.rowNumber}"
                    } else {
                        "صف ${data.rowNumber}"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textSecondary
                )
            }

            // Scrollable Content inside AppSectionCard
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                AppSectionCard {
                    for (colIndex in 0 until maxCols) {
                        val colName = data.headers.getOrNull(colIndex)?.takeIf { it.isNotBlank() } ?: "عمود ${colIndex + 1}"
                        val cellValue = data.cells.getOrNull(colIndex).orEmpty()

                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {},
                                        onLongClick = {
                                            if (cellValue.isNotBlank()) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                clipboardManager.setText(AnnotatedString(cellValue))
                                                Toast.makeText(context, "تم نسخ $colName", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Clean Column Name (No icon)
                                Text(
                                    text = colName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Value with highlighting
                                val displayValue = cellValue.ifBlank { "—" }
                                val highlightedText = remember(displayValue, data.query, isDark, data.searchFilter) {
                                    if (data.query.isNotBlank() && cellValue.isNotBlank()) {
                                        TextHighlighter.highlight(
                                            text = displayValue,
                                            query = data.query,
                                            method = data.searchFilter.method,
                                            ignoreDiacritics = data.searchFilter.ignoreDiacritics,
                                            highlightColor = highlightBg,
                                            highlightTextColor = highlightFg
                                        )
                                    } else {
                                        AnnotatedString(displayValue)
                                    }
                                }

                                SelectionContainer {
                                    Text(
                                        text = highlightedText,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        color = colors.textPrimary
                                    )
                                }

                                // Interactive action chips (Phone, URL, Email)
                                val phones = remember(cellValue) { ValueInteractionHelper.extractPhoneNumbers(cellValue) }
                                val urls = remember(cellValue) { ValueInteractionHelper.extractUrls(cellValue) }
                                val emails = remember(cellValue) { ValueInteractionHelper.extractEmails(cellValue) }

                                if (phones.isNotEmpty() || urls.isNotEmpty() || emails.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        phones.forEach { phone ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.subCardBackground)
                                                    .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(8.dp))
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
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = phone,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        urls.forEach { url ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.subCardBackground)
                                                    .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(8.dp))
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
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "فتح الرابط",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }

                                        emails.forEach { email ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(colors.subCardBackground)
                                                    .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(8.dp))
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
                                                    tint = colors.textPrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = email,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colors.textPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
