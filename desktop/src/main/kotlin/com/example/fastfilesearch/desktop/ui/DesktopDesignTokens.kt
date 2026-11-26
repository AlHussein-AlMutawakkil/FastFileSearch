package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.File
import java.net.URI
import javax.swing.JFileChooser

@Immutable
data class DesktopColors(
    val isDark: Boolean,
    val screenBackground: Color,
    val sidebarBackground: Color,
    val cardBackground: Color,
    val subCardBackground: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val controlActiveBackground: Color,
    val controlActiveContent: Color,
    val dangerText: Color,
    val highlightBg: Color,
    val highlightFg: Color
)

object DesktopDesignTokens {
    val LightColors = DesktopColors(
        isDark = false,
        screenBackground = Color(0xFFF5F6F8),
        sidebarBackground = Color(0xFFFFFFFF),
        cardBackground = Color(0xFFFFFFFF),
        subCardBackground = Color(0xFFF5F6F8),
        divider = Color(0xFFE5E6EB),
        textPrimary = Color(0xFF141416),
        textSecondary = Color(0xFF6E717A),
        controlActiveBackground = Color(0xFF1C1C1E),
        controlActiveContent = Color(0xFFFFFFFF),
        dangerText = Color(0xFFB3261E),
        highlightBg = Color(0xFFFFECB3),
        highlightFg = Color(0xFF332600)
    )

    val DarkColors = DesktopColors(
        isDark = true,
        screenBackground = Color(0xFF0E0F12),
        sidebarBackground = Color(0xFF141519),
        cardBackground = Color(0xFF18191D),
        subCardBackground = Color(0xFF202228),
        divider = Color(0xFF2A2C33),
        textPrimary = Color(0xFFF4F5F7),
        textSecondary = Color(0xFF9EA2AC),
        controlActiveBackground = Color(0xFFF4F5F7),
        controlActiveContent = Color(0xFF141416),
        dangerText = Color(0xFFF2B8B5),
        highlightBg = Color(0xFF3D351A),
        highlightFg = Color(0xFFFFE082)
    )

    val CardCornerRadius: Dp = 14.dp
    val SubCardCornerRadius: Dp = 10.dp
    val ButtonCornerRadius: Dp = 10.dp
    val DividerThickness: Dp = 1.dp
}

val LocalDesktopColors = staticCompositionLocalOf { DesktopDesignTokens.LightColors }

@Composable
fun ProvideDesktopTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) DesktopDesignTokens.DarkColors else DesktopDesignTokens.LightColors
    val m3Scheme = if (isDark) {
        darkColorScheme(
            primary = colors.controlActiveBackground,
            onPrimary = colors.controlActiveContent,
            background = colors.screenBackground,
            onBackground = colors.textPrimary,
            surface = colors.cardBackground,
            onSurface = colors.textPrimary
        )
    } else {
        lightColorScheme(
            primary = colors.controlActiveBackground,
            onPrimary = colors.controlActiveContent,
            background = colors.screenBackground,
            onBackground = colors.textPrimary,
            surface = colors.cardBackground,
            onSurface = colors.textPrimary
        )
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalDesktopColors provides colors
    ) {
        MaterialTheme(colorScheme = m3Scheme) {
            content()
        }
    }
}

@Composable
fun DesktopSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalDesktopColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesktopDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(
                DesktopDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(DesktopDesignTokens.CardCornerRadius)
            ),
        content = content
    )
}

@Composable
fun DesktopPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesktopColors.current
    val bg = if (enabled) colors.controlActiveBackground else colors.controlActiveBackground.copy(alpha = 0.35f)
    val fg = colors.controlActiveContent
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius))
            .background(bg)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(18.dp))
        }
        Text(text = text, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun DesktopSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius))
            .background(colors.subCardBackground)
            .border(
                DesktopDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(17.dp)
            )
        }
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Displays unified R2 code on a single LTR monospace line with Copy button underneath.
 */
@Composable
fun DesktopR2CodeBox(
    r2Code: String,
    onCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesktopColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(colors.subCardBackground)
                .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SelectionContainer {
                    Text(
                        text = r2Code,
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

        DesktopSecondaryButton(
            text = "نسخ كود الطلب R2",
            icon = Icons.Outlined.ContentCopy,
            onClick = {
                DesktopSystemActions.copyToClipboard(r2Code)
                onCopied()
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

object DesktopSystemActions {

    fun copyToClipboard(text: String) {
        try {
            val selection = StringSelection(text)
            Toolkit.getDefaultToolkit().systemClipboard.setContents(selection, null)
        } catch (_: Throwable) {
        }
    }

    fun pasteFromClipboard(): String {
        return try {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            if (clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)) {
                clipboard.getData(DataFlavor.stringFlavor) as? String ?: ""
            } else {
                ""
            }
        } catch (_: Throwable) {
            ""
        }
    }

    fun pickFolder(initialDir: File? = null): File? {
        return try {
            val chooser = JFileChooser(initialDir).apply {
                dialogTitle = "اختر مجلداً للفهرسة"
                fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
                isAcceptAllFileFilterUsed = false
            }
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFile
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun pickSaveExcelFile(defaultFileName: String, parentFrame: Frame? = null): File? {
        return try {
            val dialog = FileDialog(parentFrame, "حفظ نتائج البحث كملف Excel (.xlsx)", FileDialog.SAVE).apply {
                file = defaultFileName
            }
            dialog.isVisible = true
            val dir = dialog.directory
            val file = dialog.file
            if (dir != null && file != null) {
                File(dir, file)
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }

    fun openFile(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IllegalStateException("الملف غير موجود: $filePath"))
            }
            Desktop.getDesktop().open(file)
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun revealInExplorer(filePath: String): Result<Unit> {
        return try {
            val file = File(filePath)
            val target = if (file.exists()) file.absolutePath else file.parentFile?.absolutePath ?: filePath
            ProcessBuilder("explorer.exe", "/select,$target").start()
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun openUrl(url: String): Result<Unit> {
        return try {
            Desktop.getDesktop().browse(URI(url))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    fun openMailto(email: String): Result<Unit> {
        return try {
            Desktop.getDesktop().mail(URI("mailto:$email"))
            Result.success(Unit)
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}
