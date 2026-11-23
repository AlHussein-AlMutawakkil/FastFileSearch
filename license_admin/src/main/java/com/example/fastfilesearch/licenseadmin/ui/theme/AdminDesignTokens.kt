package com.example.fastfilesearch.licenseadmin.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AdminColors(
    val isDark: Boolean,
    val screenBackground: Color,
    val cardBackground: Color,
    val elevatedSurface: Color,
    val subCardBackground: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val controlActiveBackground: Color,
    val controlActiveContent: Color,
    val switchInactiveTrack: Color,
    val switchThumb: Color,
    val statusSuccess: Color = Color(0xFF1E8E3E),
    val statusError: Color = Color(0xFFD93025)
)

object AdminDesignTokens {
    val LightColors = AdminColors(
        isDark = false,
        screenBackground = Color(0xFFF5F6F8),
        cardBackground = Color(0xFFFFFFFF),
        elevatedSurface = Color(0xFFFFFFFF),
        subCardBackground = Color(0xFFF5F6F8),
        divider = Color(0xFFEFEFF2),
        textPrimary = Color(0xFF141416),
        textSecondary = Color(0xFF86868B),
        controlActiveBackground = Color(0xFF1C1C1E),
        controlActiveContent = Color(0xFFFFFFFF),
        switchInactiveTrack = Color(0xFFE5E5EA),
        switchThumb = Color(0xFFFFFFFF)
    )

    val DarkColors = AdminColors(
        isDark = true,
        screenBackground = Color(0xFF000000),
        cardBackground = Color(0xFF161618),
        elevatedSurface = Color(0xFF242426),
        subCardBackground = Color(0xFF1A1A1C),
        divider = Color(0xFF262629),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFF8E8E93),
        controlActiveBackground = Color(0xFFFFFFFF),
        controlActiveContent = Color(0xFF141416),
        switchInactiveTrack = Color(0xFF3A3A3C),
        switchThumb = Color(0xFFFFFFFF)
    )

    val CardCornerRadius: Dp = 18.dp
    val SubCardCornerRadius: Dp = 14.dp
    val ButtonCornerRadius: Dp = 16.dp
    val ChipCornerRadius: Dp = 12.dp
    val DividerThickness: Dp = 0.7.dp
    val DividerHorizontalPadding: Dp = 16.dp
}

val LocalAdminColors = staticCompositionLocalOf { AdminDesignTokens.LightColors }

@Composable
fun ProvideAdminTheme(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) AdminDesignTokens.DarkColors else AdminDesignTokens.LightColors
    CompositionLocalProvider(LocalAdminColors provides colors) {
        content()
    }
}
