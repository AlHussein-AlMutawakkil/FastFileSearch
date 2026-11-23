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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.R
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

    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }

    val versionName = remember(context) {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0.0"
        } catch (_: Exception) {
            "1.0.0"
        }
    }

    BackHandler(onBack = onBack)

    if (activeDialogTitle != null && activeDialogContent != null) {
        AlertDialog(
            onDismissRequest = {
                activeDialogTitle = null
                activeDialogContent = null
            },
            title = {
                Text(
                    text = activeDialogTitle!!,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = activeDialogContent!!,
                    fontSize = 14.sp,
                    color = colors.textSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    activeDialogTitle = null
                    activeDialogContent = null
                }) {
                    Text("حسناً", color = colors.textPrimary)
                }
            },
            containerColor = colors.elevatedSurface,
            shape = RoundedCornerShape(AppDesignTokens.CardCornerRadius)
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "حول بحث آمن",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top App Info Card - strictly matching reference image (Qwen style)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(AppDesignTokens.CardCornerRadius))
                    .background(colors.cardBackground)
                    .padding(20.dp),
                horizontalAlignment = Alignment.Start
            ) {
                // Line 1: Logo icon + App Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.controlActiveBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_app_logo),
                            contentDescription = null,
                            tint = colors.controlActiveContent,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "بحث آمن",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Line 2: 2-line description
                Text(
                    text = "بحث آمن محرك بحث محلي فائق السرعة وفهرسة متقدمة لمستندات أوفيس والجداول وملفات PDF والنصوص مع مطابقة عربية ذكية ودعم كامل دون اتصال.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Line 3: App version label + version number directly below in gray
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

            // Bottom Policy Card - strictly matching reference image
            AppSectionCard {
                item {
                    AppNavigationRow(
                        title = "اتفاقية الخدمة",
                        showChevron = true,
                        onClick = {
                            activeDialogTitle = "اتفاقية الخدمة"
                            activeDialogContent = "تطبيق «بحث آمن» هو أداة مجانية للبحث المحلي في الملفات والمستندات على جهازك دون إرسال أي بيانات إلى خوادم خارجية."
                        }
                    )
                }

                item {
                    AppNavigationRow(
                        title = "سياسة الاستخدام",
                        showChevron = true,
                        onClick = {
                            activeDialogTitle = "سياسة الاستخدام"
                            activeDialogContent = "يتم استخدام التطبيق للبحث في المستندات والملفات المصرّح بقراءتها محلياً وفق صلاحيات الوصول المختارة من قبلك."
                        }
                    )
                }

                item {
                    AppNavigationRow(
                        title = "سياسة الخصوصية",
                        showChevron = true,
                        onClick = {
                            activeDialogTitle = "سياسة الخصوصية"
                            activeDialogContent = "خصوصيتك محمية 100%. التطبيق يعمل بالكامل دون اتصال بالإنترنت (Offline)، ولا يجمع أو يشارك أي معلومات شخصية أو ملفات."
                        }
                    )
                }
            }
        }
    }
}
