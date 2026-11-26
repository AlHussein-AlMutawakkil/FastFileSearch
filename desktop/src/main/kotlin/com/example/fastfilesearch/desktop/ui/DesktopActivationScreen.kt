package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.licensing.DesktopLicenseManager
import com.example.fastfilesearch.desktop.licensing.LicenseStatus

@Composable
fun DesktopActivationScreen(
    licenseManager: DesktopLicenseManager,
    onActivated: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val r2Code = remember { licenseManager.buildR2ActivationRequest() }
    val currentStatus = remember { licenseManager.getLicenseStatus() }

    var licenseInput by remember { mutableStateOf("") }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val subtitle = when (currentStatus) {
        LicenseStatus.EXPIRED -> "انتهت صلاحية الترخيص الحالي"
        LicenseStatus.CLOCK_TAMPERED -> "يرجى تصحيح ساعة النظام وإعادة التحقق"
        else -> "تفعيل نسخة سطح المكتب (ويندوز)"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.VerifiedUser,
                contentDescription = "شعار التفعيل",
                tint = colors.textPrimary,
                modifier = Modifier.size(54.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بحث آمن",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            DesktopSectionCard(modifier = Modifier.padding(2.dp)) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "كود طلب التفعيل (معرّف الجهاز)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DesktopR2CodeBox(
                        r2Code = r2Code,
                        onCopied = { feedbackMessage = "تم نسخ كود الطلب إلى الحافظة." }
                    )
                }
            }

            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "كود التفعيل (16 حرفاً)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.subCardBackground)
                            .border(1.dp, colors.divider, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                            BasicTextField(
                                value = licenseInput,
                                onValueChange = {
                                    licenseInput = it
                                    feedbackMessage = null
                                },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 15.sp,
                                    fontFamily = FontFamily.Monospace,
                                    textAlign = TextAlign.Center
                                ),
                                cursorBrush = SolidColor(colors.textPrimary),
                                modifier = Modifier.weight(1f),
                                decorationBox = { inner ->
                                    if (licenseInput.isEmpty()) {
                                        Text(
                                            text = "XXXX-XXXX-XXXX-XXXX",
                                            color = colors.textSecondary.copy(alpha = 0.55f),
                                            fontSize = 14.sp,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    inner()
                                }
                            )
                        }

                        IconButton(
                            onClick = {
                                val pasted = DesktopSystemActions.pasteFromClipboard().trim()
                                if (pasted.isNotEmpty()) {
                                    licenseInput = pasted
                                    feedbackMessage = null
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ContentPaste,
                                contentDescription = "لصق الكود",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    feedbackMessage?.let { msg ->
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            color = if (msg.contains("نسخ")) colors.textSecondary else colors.dangerText
                        )
                    }

                    DesktopPrimaryButton(
                        text = "تفعيل التطبيق",
                        icon = Icons.Outlined.Key,
                        enabled = licenseInput.isNotBlank(),
                        onClick = {
                            val result = licenseManager.activateLicense(licenseInput)
                            if (result.status == LicenseStatus.ACTIVE) {
                                onActivated()
                            } else {
                                feedbackMessage = result.errorMessage ?: "كود التفعيل غير صالح"
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
