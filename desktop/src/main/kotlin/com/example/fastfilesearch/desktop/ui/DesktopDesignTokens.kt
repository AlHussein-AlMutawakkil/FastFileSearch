@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.fastfilesearch.desktop.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
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
    const val DESKTOP_APP_VERSION = "6.0.0"

    val AppLogo: ImageVector by lazy {
        ImageVector.Builder(
            name = "SafeSearchLogo",
            defaultWidth = 48.dp,
            defaultHeight = 48.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).addPath(
            pathData = PathParser().parsePathString(
                "M24,44q-7,-1.75 -11.5,-8.13T8,21.9V10L24,4l16,6V21.9q0,3.9 -1.07,7.73t-3.78,7.13l-7,-6.7q-0.95,0.55 -2.01,0.9T24,31.3q-3.1,0 -5.27,-2.15t-2.18,-5.27t2.18,-5.3T24,16.4t5.28,2.18t2.18,5.32q0,1.05 -0.3,2.1T30.2,27.9l4.4,4.2q1.2,-2.15 1.8,-4.83T37,21.9V11.98L24,7.25L11,11.98V21.9q0,6.55 3.63,11.83t9.39,7.17q1.44,-0.4 3.51,-1.65t3.27,-2.4L32.9,39q-1.75,1.6 -4.17,3.02T24,44Zm3.15,-16.98q1.3,-1.27 1.3,-3.15T27.14,20.7t-3.15,-1.3t-3.14,1.3t-1.3,3.18t1.31,3.15t3.15,1.27t3.14,-1.27Zm-3.2,-3.23Z"
            ).toNodes(),
            fill = SolidColor(Color.White)
        ).build()
    }

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

    // Windows 11 / WinUI 3 Desktop Geometry
    val CardCornerRadius: Dp = 8.dp
    val SubCardCornerRadius: Dp = 6.dp
    val ButtonCornerRadius: Dp = 6.dp
    val ControlCornerRadius: Dp = 5.dp
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        !enabled -> colors.controlActiveBackground.copy(alpha = 0.35f)
        isHovered -> colors.controlActiveBackground.copy(alpha = 0.88f)
        else -> colors.controlActiveBackground
    }
    val fg = colors.controlActiveContent
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius))
            .background(bg)
            .hoverable(interactionSource, enabled = enabled)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Text(text = text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
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
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        !enabled -> colors.subCardBackground.copy(alpha = 0.6f)
        isHovered -> colors.divider.copy(alpha = if (colors.isDark) 0.55f else 0.45f)
        else -> colors.subCardBackground
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius))
            .background(bg)
            .border(
                DesktopDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(DesktopDesignTokens.ButtonCornerRadius)
            )
            .hoverable(interactionSource, enabled = enabled)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text,
            color = if (enabled) colors.textPrimary else colors.textSecondary.copy(alpha = 0.5f),
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

/**
 * Flat WinUI 3 Command Bar button (used in top Explorer-style toolbar).
 */
@Composable
fun FluentCommandButton(
    text: String? = null,
    icon: ImageVector,
    tooltip: String? = null,
    active: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        !enabled -> Color.Transparent
        active -> colors.controlActiveBackground
        isHovered -> colors.subCardBackground
        else -> Color.Transparent
    }
    val fg = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.45f)
        active -> colors.controlActiveContent
        else -> colors.textPrimary
    }

    val buttonContent: @Composable () -> Unit = {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                .background(bg)
                .then(
                    if (active) {
                        Modifier
                    } else if (isHovered && enabled) {
                        Modifier.border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                    } else {
                        Modifier
                    }
                )
                .hoverable(interactionSource, enabled = enabled)
                .clickable(enabled = enabled, interactionSource = interactionSource, indication = null, onClick = onClick)
                .padding(horizontal = if (text != null) 10.dp else 7.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = tooltip ?: text, tint = fg, modifier = Modifier.size(16.dp))
            if (!text.isNullOrBlank()) {
                Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = fg, maxLines = 1)
            }
        }
    }

    if (!tooltip.isNullOrBlank()) {
        FluentTooltip(text = tooltip, content = buttonContent)
    } else {
        buttonContent()
    }
}

@Composable
fun FluentTooltip(
    text: String,
    content: @Composable () -> Unit
) {
    val colors = LocalDesktopColors.current
    TooltipArea(
        tooltip = {
            Surface(
                color = colors.cardBackground,
                shape = RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius),
                shadowElevation = 4.dp,
                modifier = Modifier.border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            ) {
                Text(
                    text = text,
                    fontSize = 11.5.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                )
            }
        },
        delayMillis = 450,
        tooltipPlacement = TooltipPlacement.CursorPoint(offset = DpOffset(0.dp, 18.dp)),
        content = content
    )
}

/**
 * Authentic WinUI 3 capsule ToggleSwitch using the app's existing primary color.
 */
@Composable
fun FluentToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    val trackColor by animateColorAsState(
        targetValue = when {
            checked && enabled -> colors.controlActiveBackground
            checked && !enabled -> colors.controlActiveBackground.copy(alpha = 0.4f)
            isHovered && enabled -> colors.divider
            else -> colors.subCardBackground
        }
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) colors.controlActiveContent else colors.textSecondary
    )
    val thumbOffset by animateDpAsState(targetValue = if (checked) 20.dp else 4.dp)
    val thumbSize by animateDpAsState(targetValue = if (isHovered && enabled) 14.dp else 12.dp)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            .hoverable(interactionSource, enabled = enabled)
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) {
                onCheckedChange(!checked)
            }
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(20.dp)
                .clip(CircleShape)
                .background(trackColor)
                .then(
                    if (!checked) {
                        Modifier.border(1.dp, colors.textSecondary.copy(alpha = 0.55f), CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(thumbColor)
            )
        }
    }
}

/**
 * WinUI 3 TextBox with subtle 1dp frame and 2dp bottom focus accent line using existing primary color.
 */
@Composable
fun FluentTextBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldModifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    isError: Boolean = false,
    textStyle: TextStyle? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    leadingContent: (@Composable () -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val isHovered by interactionSource.collectIsHoveredAsState()

    val effectiveStyle = textStyle ?: TextStyle(color = colors.textPrimary, fontSize = 13.5.sp)
    val bottomBarColor = when {
        isError -> colors.dangerText
        isFocused -> colors.controlActiveBackground
        else -> colors.divider
    }
    val bottomBarThickness = if (isFocused || isError) 2.dp else 1.dp

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            .background(
                if (isFocused) colors.cardBackground
                else if (isHovered) colors.cardBackground
                else colors.subCardBackground
            )
            .border(1.dp, colors.divider, RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            .hoverable(interactionSource, enabled = enabled)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (leadingContent != null) leadingContent()

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                enabled = enabled,
                interactionSource = interactionSource,
                visualTransformation = visualTransformation,
                textStyle = effectiveStyle,
                cursorBrush = SolidColor(colors.controlActiveBackground),
                modifier = Modifier.weight(1f).then(fieldModifier),
                decorationBox = { inner ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            fontSize = effectiveStyle.fontSize,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    inner()
                }
            )

            if (trailingContent != null) trailingContent()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(bottomBarThickness)
                .background(bottomBarColor)
        )
    }
}

/**
 * Windows 11 Settings horizontal card row.
 */
@Composable
fun FluentSettingRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = if (onClick != null && isHovered) {
        colors.subCardBackground.copy(alpha = 0.75f)
    } else {
        colors.cardBackground
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .then(
                if (onClick != null) {
                    Modifier
                        .hoverable(interactionSource)
                        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
                        .background(colors.subCardBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = title, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                if (!subtitle.isNullOrBlank()) {
                    Text(text = subtitle, fontSize = 12.sp, color = colors.textSecondary)
                }
            }
        }
        trailing()
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
