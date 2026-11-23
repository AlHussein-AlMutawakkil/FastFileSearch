package com.example.fastfilesearch.ui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class AppColors(
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
    val segmentedContainer: Color,
    val segmentedActive: Color,
    val permissionGranted: Color = Color(0xFF1E8E3E),
    val permissionDenied: Color = Color(0xFF8E9BAE),
    val onStatusIcon: Color = Color.White
)

object AppDesignTokens {
    val LightColors = AppColors(
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
        switchThumb = Color(0xFFFFFFFF),
        segmentedContainer = Color(0xFFEAECEF),
        segmentedActive = Color(0xFFFFFFFF)
    )

    val DarkColors = AppColors(
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
        switchThumb = Color(0xFFFFFFFF),
        segmentedContainer = Color(0xFF141416),
        segmentedActive = Color(0xFF242426)
    )

    // Geometry and Tokens
    val CardCornerRadius: Dp = 18.dp
    val SubCardCornerRadius: Dp = 16.dp
    val BottomSheetCornerRadius: Dp = 24.dp
    val MenuCornerRadius: Dp = 18.dp
    val SegmentedCornerRadius: Dp = 12.dp
    val ButtonCornerRadius: Dp = 16.dp
    val DividerThickness: Dp = 0.7.dp
    val DividerHorizontalPadding: Dp = 16.dp
}

val LocalAppColors = staticCompositionLocalOf { AppDesignTokens.LightColors }

@Composable
fun ProvideAppDesignTokens(
    isDark: Boolean,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) AppDesignTokens.DarkColors else AppDesignTokens.LightColors
    CompositionLocalProvider(LocalAppColors provides colors) {
        content()
    }
}
