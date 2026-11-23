package com.example.fastfilesearch.ui.licensing

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.fastfilesearch.licensing.LicenseManager
import com.example.fastfilesearch.licensing.LicenseStatus
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppNavigationRow
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.AppSecondaryButton
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens
import com.example.fastfilesearch.util.DeviceInfoHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LicenseSettingsScreen(
    licenseManager: LicenseManager,
    onBack: () -> Unit,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    BackHandler {
        onBack()
    }

    ProvideAppDesignTokens(isDark = isDark) {
        val colors = LocalAppColors.current

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val installationId = remember { licenseManager.installationId }
            var licenseStatus by remember { mutableStateOf(licenseManager.getLicenseStatus()) }
            var payload by remember { mutableStateOf(licenseManager.getActivePayload()) }
            var showUpdateDialog by remember { mutableStateOf(false) }

            val statusLabel = when (licenseStatus) {
                LicenseStatus.ACTIVE -> "نشط"
                LicenseStatus.EXPIRED -> "منتهي الصلاحية"
                LicenseStatus.CLOCK_TAMPERED -> "تاريخ الجهاز غير صحيح"
                LicenseStatus.NOT_ACTIVATED -> "غير مفعّل"
                LicenseStatus.INVALID -> "كود غير صالح"
            }

            val validityLabel = remember(payload) {
                val p = payload
                if (p == null) {
                    "—"
                } else if (p.isPermanent) {
                    "دائم (بلا انتهاء)"
                } else {
                    val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                    val dateStr = sdf.format(Date(p.exp * 1000L))
                    val daysRemaining = ((p.exp * 1000L - System.currentTimeMillis()) / (24 * 3600 * 1000L)).toInt().coerceAtLeast(0)
                    "$dateStr (متبقي $daysRemaining يوماً)"
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.screenBackground)
                    .navigationBarsPadding()
            ) {
                AppTopBar(
                    title = "الترخيص",
                    onBack = onBack
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // CARD 1: معلومات الترخيص والجهاز
                    AppSectionCard {
                        item {
                            AppNavigationRow(
                                title = "حالة الترخيص",
                                value = statusLabel,
                                icon = Icons.Outlined.VerifiedUser,
                                showChevron = false
                            )
                        }

                        if (!payload?.n.isNullOrBlank()) {
                            item {
                                AppNavigationRow(
                                    title = "المرخّص له",
                                    value = payload?.n ?: "",
                                    showChevron = false
                                )
                            }
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "معرّف التثبيت",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = installationId,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }

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
                            }
                        }

                        item {
                            AppNavigationRow(
                                title = "صلاحية الترخيص",
                                value = validityLabel,
                                showChevron = false
                            )
                        }
                    }

                    // CARD 2: الصلاحيات المتقدمة
                    AppSectionCard {
                        item {
                            val maxWorkspacesLabel = remember(payload) {
                                val p = payload
                                if (p == null || p.maxWorkspaces <= 0) "غير محدود" else "${p.maxWorkspaces} مجلدات"
                            }
                            AppNavigationRow(
                                title = "الحد الأقصى للمجلدات",
                                value = maxWorkspacesLabel,
                                icon = Icons.Outlined.Folder,
                                showChevron = false
                            )
                        }

                        item {
                            LicensePermissionRow(
                                title = "تصدير ومشاركة Excel",
                                icon = Icons.Outlined.TableChart,
                                isEnabled = payload?.allowExcelExport == true
                            )
                        }

                        item {
                            LicensePermissionRow(
                                title = "قفل التطبيق بالبصمة",
                                icon = Icons.Outlined.Lock,
                                isEnabled = payload?.allowAppLock == true
                            )
                        }

                        item {
                            LicensePermissionRow(
                                title = "المزامنة التلقائية بالخلفية",
                                icon = Icons.Outlined.Sync,
                                isEnabled = payload?.allowBackgroundSync == true
                            )
                        }
                    }

                    // CARD 3: بيانات الجهاز والإصدار
                    AppSectionCard {
                        item {
                            AppNavigationRow(
                                title = "إصدار التطبيق",
                                value = DeviceInfoHelper.getAppVersion(context),
                                showChevron = false
                            )
                        }

                        item {
                            AppNavigationRow(
                                title = "نوع الهاتف",
                                value = DeviceInfoHelper.getManufacturer(),
                                showChevron = false
                            )
                        }

                        item {
                            AppNavigationRow(
                                title = "موديل الهاتف",
                                value = DeviceInfoHelper.getModel(),
                                showChevron = false
                            )
                        }

                        item {
                            AppNavigationRow(
                                title = "إصدار النظام",
                                value = DeviceInfoHelper.getOsVersion(),
                                showChevron = false
                            )
                        }

                        item {
                            AppNavigationRow(
                                title = "معمارية الجهاز",
                                value = DeviceInfoHelper.getCpuAbi(),
                                showChevron = false
                            )
                        }
                    }

                    // CARD 4: خيار تحديث / تجديد كود الترخيص
                    AppSectionCard {
                        item {
                            AppNavigationRow(
                                title = "تحديث كود الترخيص",
                                icon = Icons.Outlined.Upgrade,
                                showChevron = true,
                                onClick = { showUpdateDialog = true }
                            )
                        }
                    }
                }
            }

            // Dialog for Updating / Upgrading License Code
            if (showUpdateDialog) {
                var updateCodeInput by remember { mutableStateOf("") }
                var updateError by remember { mutableStateOf<String?>(null) }

                Dialog(
                    onDismissRequest = { showUpdateDialog = false },
                    properties = DialogProperties(usePlatformDefaultWidth = false)
                ) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .clip(RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                                .background(colors.cardBackground)
                                .border(
                                    AppDesignTokens.DividerThickness,
                                    colors.divider,
                                    RoundedCornerShape(AppDesignTokens.MenuCornerRadius)
                                )
                                .imePadding()
                                .padding(20.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "تحديث كود الترخيص",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "أدخل كود الترخيص الجديد لتجديد أو ترقية الصلاحيات",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                                        .background(colors.subCardBackground)
                                        .border(
                                            width = AppDesignTokens.DividerThickness,
                                            color = if (updateError != null) colors.textPrimary else colors.divider,
                                            shape = RoundedCornerShape(AppDesignTokens.SubCardCornerRadius)
                                        )
                                        .padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = updateCodeInput,
                                            onValueChange = {
                                                updateCodeInput = it
                                                updateError = null
                                            },
                                            textStyle = TextStyle(
                                                color = colors.textPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = FontFamily.Monospace,
                                                textAlign = TextAlign.Start
                                            ),
                                            cursorBrush = SolidColor(colors.textPrimary),
                                            modifier = Modifier.weight(1f),
                                            decorationBox = { innerTextField ->
                                                if (updateCodeInput.isEmpty()) {
                                                    Text(
                                                        text = "الصق الكود الجديد هنا...",
                                                        color = colors.textSecondary.copy(alpha = 0.6f),
                                                        fontSize = 13.sp
                                                    )
                                                }
                                                innerTextField()
                                            }
                                        )

                                        IconButton(
                                            onClick = {
                                                val clipText = clipboardManager.getText()?.text
                                                if (!clipText.isNullOrBlank()) {
                                                    updateCodeInput = clipText.trim()
                                                    updateError = null
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentPaste,
                                                contentDescription = "لصق",
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }

                                if (updateError != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = updateError ?: "",
                                        fontSize = 12.sp,
                                        color = colors.textPrimary
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AppPrimaryButton(
                                        text = "تحديث",
                                        onClick = {
                                            val res = licenseManager.activateLicense(updateCodeInput)
                                            if (res.status == LicenseStatus.ACTIVE) {
                                                licenseStatus = res.status
                                                payload = res.payload
                                                showUpdateDialog = false
                                                Toast.makeText(context, "تم تحديث الترخيص بنجاح", Toast.LENGTH_SHORT).show()
                                            } else {
                                                updateError = res.errorMessage ?: "كود الترخيص غير صالح"
                                            }
                                        },
                                        enabled = updateCodeInput.isNotBlank(),
                                        modifier = Modifier.weight(1f)
                                    )

                                    AppSecondaryButton(
                                        text = "إلغاء",
                                        onClick = { showUpdateDialog = false },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LicensePermissionRow(
    title: String,
    icon: ImageVector,
    isEnabled: Boolean
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
        }
        Icon(
            imageVector = if (isEnabled) Icons.Outlined.Check else Icons.Outlined.Close,
            contentDescription = if (isEnabled) "مفعّل" else "غير مفعّل",
            tint = if (isEnabled) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}
