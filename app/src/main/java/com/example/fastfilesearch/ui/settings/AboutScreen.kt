package com.example.fastfilesearch.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.R
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppNavigationRow
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.util.DeviceInfoHelper

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAppColors.current
    val clipboardManager = LocalClipboardManager.current

    var activeDialogTitle by remember { mutableStateOf<String?>(null) }
    var activeDialogContent by remember { mutableStateOf<String?>(null) }

    val appVersion = remember(context) {
        DeviceInfoHelper.getAppVersion(context)
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
            // Top App Info Card
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

                // Line 2: Description
                Text(
                    text = "بحث آمن محرك بحث محلي فائق السرعة وفهرسة متقدمة لمستندات أوفيس والجداول وملفات PDF والنصوص مع مطابقة عربية ذكية ودعم كامل دون اتصال.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    lineHeight = 19.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Line 3: App version
                Text(
                    text = "إصدار التطبيق",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appVersion,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            // Developer and Contact Merged Card
            AppSectionCard {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 15.dp)
                    ) {
                        Text(
                            text = "Eng. Al-Hussein Al-Mutawakel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "تطوير وتصميم",
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                item {
                    HorizontalDivider(
                        thickness = AppDesignTokens.DividerThickness,
                        color = colors.divider
                    )
                }

                item {
                    PhoneContactRow(
                        phoneNumber = "776472265",
                        internationalPhone = "+967776472265",
                        whatsappUrl = "https://wa.me/967776472265",
                        context = context
                    )
                }

                item {
                    PhoneContactRow(
                        phoneNumber = "730118144",
                        internationalPhone = "+967730118144",
                        whatsappUrl = "https://wa.me/967730118144",
                        context = context
                    )
                }

                item {
                    EmailContactRow(
                        email = "alhussein.ali.almutawakel@gmail.com",
                        context = context,
                        clipboardManager = clipboardManager
                    )
                }
            }

            // Policies Card
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

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Copyright
            Text(
                text = "© 2026 Eng. Al-Hussein Al-Mutawakel. جميع الحقوق محفوظة.",
                fontSize = 12.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
    }
}

@Composable
private fun PhoneContactRow(
    phoneNumber: String,
    internationalPhone: String,
    whatsappUrl: String,
    context: Context
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$internationalPhone"))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "تعذر فتح تطبيق الاتصال", Toast.LENGTH_SHORT).show()
                }
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.Call,
                contentDescription = "اتصال",
                tint = colors.textPrimary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = phoneNumber,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }

        IconButton(
            onClick = {
                try {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(whatsappUrl))
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "تعذر فتح تطبيق واتساب", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.Chat,
                contentDescription = "مراسلة عبر واتساب",
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EmailContactRow(
    email: String,
    context: Context,
    clipboardManager: androidx.compose.ui.platform.ClipboardManager
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$email"))
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        clipboardManager.setText(AnnotatedString(email))
                        Toast.makeText(context, "تم نسخ البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                    }
                },
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(email))
                    Toast.makeText(context, "تم نسخ البريد الإلكتروني", Toast.LENGTH_SHORT).show()
                }
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = Icons.Outlined.Email,
            contentDescription = "البريد الإلكتروني",
            tint = colors.textPrimary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = email,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
