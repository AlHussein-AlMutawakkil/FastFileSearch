package com.example.fastfilesearch.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.model.SupportedFileTypeGroups
import com.example.fastfilesearch.ui.FileTypeHelper
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppNavigationRow
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current

    val versionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "حول التطبيق",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Info Card - aligned to the right (RTL start)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDesignTokens.CardCornerRadius))
                    .background(colors.cardBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Line 1: 26.dp icon + App Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(colors.controlActiveBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.controlActiveContent,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "FastFileSearch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Line 2: 2-line description
                Text(
                    text = "محرك بحث محلي فائق السرعة وفهرسة متقدمة لمستندات أوفيس والجداول وملفات PDF والنصوص مع مطابقة عربية ذكية ودعم كامل للعمل دون اتصال بالإنترنت.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Line 3: App version title + version number directly below in secondary gray without background box
                Text(
                    text = "إصدار التطبيق",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = versionName,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            // Supported File Formats Card (clean single-line rows)
            AppSectionCard {
                SupportedFileTypeGroups.ALL_GROUPS.forEach { group ->
                    val visual = FileTypeHelper.getVisualForGroup(group)
                    item {
                        AppNavigationRow(
                            title = group.displayName,
                            value = group.extensions.joinToString("، ") { ".$it" },
                            icon = visual.icon,
                            iconTint = visual.color,
                            showChevron = false,
                            onClick = null
                        )
                    }
                }
            }
        }
    }
}
