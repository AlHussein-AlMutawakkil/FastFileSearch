package com.example.fastfilesearch.ui.licensing

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

/**
 * Displays the unified R2 device request code on a single LTR monospace line,
 * with Copy and Share action buttons placed underneath to keep full horizontal width for the code.
 */
@Composable
fun DeviceRequestCodeBlock(
    code: String,
    copyToastMessage: String,
    shareChooserTitle: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val colors = LocalAppColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.subCardBackground)
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SelectionContainer {
                    Text(
                        text = code,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        textAlign = TextAlign.Center,
                        color = colors.textPrimary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.subCardBackground)
                    .border(
                        AppDesignTokens.DividerThickness,
                        colors.divider,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        clipboardManager.setText(AnnotatedString(code))
                        Toast.makeText(context, copyToastMessage, Toast.LENGTH_SHORT).show()
                    }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "نسخ",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "نسخ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }

            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.subCardBackground)
                    .border(
                        AppDesignTokens.DividerThickness,
                        colors.divider,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable {
                        try {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, code)
                            }
                            context.startActivity(Intent.createChooser(intent, shareChooserTitle))
                        } catch (_: Exception) {}
                    }
                    .padding(vertical = 9.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "مشاركة",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(17.dp)
                )
                Text(
                    text = "مشاركة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }
        }
    }
}
