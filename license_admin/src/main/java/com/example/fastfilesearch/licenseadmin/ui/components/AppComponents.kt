package com.example.fastfilesearch.licenseadmin.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.licenseadmin.ui.theme.AdminDesignTokens
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors

/**
 * AppTopBar:
 * Monochromatic top bar with screen background color.
 * Back arrow on the right (Start in RTL), title next to it, actions on the left.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    val colors = LocalAdminColors.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .background(colors.screenBackground)
                .statusBarsPadding()
                .height(56.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right in RTL: Back arrow + Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                if (onBack != null) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "رجوع",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Left in RTL: Action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                actions()
            }
        }
    }
}

class AppSectionCardScope {
    val items = mutableListOf<@Composable () -> Unit>()

    fun item(content: @Composable () -> Unit) {
        items.add(content)
    }
}

/**
 * AppSectionCard:
 * 18.dp corners, no shadow, clean divider between items.
 */
@Composable
fun AppSectionCard(
    modifier: Modifier = Modifier,
    content: AppSectionCardScope.() -> Unit
) {
    val colors = LocalAdminColors.current
    val scope = remember(content) { AppSectionCardScope().apply(content) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
                .background(colors.cardBackground)
        ) {
            scope.items.forEachIndexed { index, itemContent ->
                itemContent()
                if (index < scope.items.size - 1) {
                    HorizontalDivider(
                        color = colors.divider,
                        thickness = AdminDesignTokens.DividerThickness,
                        modifier = Modifier.padding(horizontal = AdminDesignTokens.DividerHorizontalPadding)
                    )
                }
            }
        }
    }
}

/**
 * AppNavigationRow:
 * Monochromatic navigation row.
 */
@Composable
fun AppNavigationRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    value: String? = null,
    icon: ImageVector? = null,
    showChevron: Boolean = true,
    onClick: (() -> Unit)? = null
) {
    val colors = LocalAdminColors.current
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp)
    } else {
        modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (value != null) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }
            if (onClick != null && showChevron) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * AppMonochromeSwitch:
 * Minimalist monochrome switch.
 */
@Composable
fun AppMonochromeSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colors = LocalAdminColors.current
    val trackBg by animateColorAsState(
        targetValue = if (checked) colors.controlActiveBackground else colors.switchInactiveTrack,
        animationSpec = tween(durationMillis = 200),
        label = "trackBg"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = tween(durationMillis = 200),
        label = "thumbOffset"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onCheckedChange != null) {
        modifier.clickable(
            interactionSource = interactionSource,
            indication = null
        ) { onCheckedChange(!checked) }
    } else {
        modifier
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = clickableModifier
                .size(width = 42.dp, height = 24.dp)
                .clip(CircleShape)
                .background(trackBg),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .offset(x = thumbOffset)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(if (checked) colors.controlActiveContent else colors.switchThumb)
            )
        }
    }
}

/**
 * AppToggleRow:
 * Toggle row inside an AppSectionCard.
 */
@Composable
fun AppToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null
) {
    val colors = LocalAdminColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Column(modifier = Modifier.padding(end = 12.dp)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }
        }

        AppMonochromeSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

/**
 * AppPrimaryButton:
 * Full-width high-contrast monochrome button.
 */
@Composable
fun AppPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    val colors = LocalAdminColors.current

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(AdminDesignTokens.ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.controlActiveBackground,
            contentColor = colors.controlActiveContent,
            disabledContainerColor = colors.divider,
            disabledContentColor = colors.textSecondary
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * AppSecondaryButton:
 * Subdued monochrome button with border.
 */
@Composable
fun AppSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    contentColor: Color? = null
) {
    val colors = LocalAdminColors.current
    val effectiveContentColor = contentColor ?: colors.textPrimary

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(AdminDesignTokens.ButtonCornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.subCardBackground,
            contentColor = effectiveContentColor,
            disabledContainerColor = colors.divider,
            disabledContentColor = colors.textSecondary
        ),
        border = androidx.compose.foundation.BorderStroke(AdminDesignTokens.DividerThickness, colors.divider),
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = effectiveContentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = effectiveContentColor
            )
        }
    }
}

/**
 * AppTextField:
 * Minimalist monochrome text input field with optional end action (e.g. paste or clear).
 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    singleLine: Boolean = true,
    isMonospace: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    val colors = LocalAdminColors.current

    Column(modifier = modifier.fillMaxWidth()) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            textStyle = TextStyle(
                color = colors.textPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
            ),
            cursorBrush = SolidColor(colors.textPrimary),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AdminDesignTokens.SubCardCornerRadius))
                        .background(colors.cardBackground)
                        .border(
                            AdminDesignTokens.DividerThickness,
                            colors.divider,
                            RoundedCornerShape(AdminDesignTokens.SubCardCornerRadius)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        if (value.isEmpty() && placeholder.isNotEmpty()) {
                            Text(
                                text = placeholder,
                                color = colors.textSecondary.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default
                            )
                        }
                        innerTextField()
                    }
                    if (trailingIcon != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        trailingIcon()
                    }
                }
            }
        )
    }
}
