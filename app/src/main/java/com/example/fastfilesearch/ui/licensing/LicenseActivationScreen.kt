package com.example.fastfilesearch.ui.licensing

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.R
import com.example.fastfilesearch.licensing.LicenseManager
import com.example.fastfilesearch.licensing.LicenseStatus
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens

@Composable
fun LicenseActivationScreen(
    licenseManager: LicenseManager,
    isDark: Boolean = isSystemInDarkTheme(),
    onActivationSuccess: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val clipboardManager = LocalClipboardManager.current

    BackHandler {
        activity?.moveTaskToBack(true)
    }

    ProvideAppDesignTokens(isDark = isDark) {
        val colors = LocalAppColors.current

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val installationId = remember { licenseManager.installationId }
            val currentStatus = remember { licenseManager.getLicenseStatus() }

            var licenseInput by remember { mutableStateOf("") }
            var errorMessage by remember { mutableStateOf<String?>(null) }

            val reasonSubtitle = when (currentStatus) {
                LicenseStatus.EXPIRED -> "انتهت صلاحية الترخيص"
                LicenseStatus.CLOCK_TAMPERED -> "يرجى تصحيح تاريخ الجهاز"
                else -> "تفعيل التطبيق"
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.screenBackground)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // App Logo (No white background card) + App Name
                Icon(
                    painter = painterResource(R.drawable.ic_app_logo),
                    contentDescription = "شعار التطبيق",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(60.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "بحث آمن",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = reasonSubtitle,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Card displaying Installation ID with Copy and Share icons
                AppSectionCard {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "معرّف التثبيت الخاص بجهازك",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = installationId,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = colors.textPrimary
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            clipboardManager.setText(AnnotatedString(installationId))
                                            Toast.makeText(context, "تم نسخ معرّف التثبيت", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = "نسخ المعرّف",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            try {
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(
                                                        Intent.EXTRA_TEXT,
                                                        "معرّف التثبيت لتطبيق بحث آمن:\n$installationId"
                                                    )
                                                }
                                                context.startActivity(Intent.createChooser(intent, "مشاركة معرّف التثبيت"))
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Share,
                                            contentDescription = "مشاركة المعرّف",
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(19.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // License Code Input Box with Paste Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                        .background(colors.cardBackground)
                        .border(
                            width = AppDesignTokens.DividerThickness,
                            color = if (errorMessage != null) colors.textPrimary else colors.divider,
                            shape = RoundedCornerShape(AppDesignTokens.SubCardCornerRadius)
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = licenseInput,
                            onValueChange = {
                                licenseInput = it
                                errorMessage = null
                            },
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Start
                            ),
                            cursorBrush = SolidColor(colors.textPrimary),
                            modifier = Modifier.weight(1f),
                            decorationBox = { innerTextField ->
                                if (licenseInput.isEmpty()) {
                                    Text(
                                        text = "أدخل أو الصق كود الترخيص هنا...",
                                        color = colors.textSecondary.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                innerTextField()
                            }
                        )

                        IconButton(
                            onClick = {
                                val clipText = clipboardManager.getText()?.text
                                if (!clipText.isNullOrBlank()) {
                                    licenseInput = clipText.trim()
                                    errorMessage = null
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "لصق من الحافظة",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = errorMessage ?: "",
                        fontSize = 13.sp,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                AppPrimaryButton(
                    text = "تفعيل",
                    onClick = {
                        val result = licenseManager.activateLicense(licenseInput)
                        if (result.status == LicenseStatus.ACTIVE) {
                            Toast.makeText(context, "تم تفعيل الترخيص بنجاح", Toast.LENGTH_SHORT).show()
                            onActivationSuccess()
                        } else {
                            errorMessage = result.errorMessage ?: "كود الترخيص غير صالح"
                        }
                    },
                    enabled = licenseInput.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
