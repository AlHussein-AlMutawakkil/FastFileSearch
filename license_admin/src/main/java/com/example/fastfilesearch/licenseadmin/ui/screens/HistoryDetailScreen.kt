package com.example.fastfilesearch.licenseadmin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.licenseadmin.history.IssuedLicenseItem
import com.example.fastfilesearch.licenseadmin.history.LicenseHistoryManager
import com.example.fastfilesearch.licenseadmin.ui.components.AppPrimaryButton
import com.example.fastfilesearch.licenseadmin.ui.components.AppSecondaryButton
import com.example.fastfilesearch.licenseadmin.ui.components.AppSectionCard
import com.example.fastfilesearch.licenseadmin.ui.components.AppTopBar
import com.example.fastfilesearch.licenseadmin.ui.theme.AdminDesignTokens
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryDetailScreen(
    item: IssuedLicenseItem,
    onBack: () -> Unit,
    onRenew: (IssuedLicenseItem) -> Unit,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAdminColors.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val issueDateStr = remember(item.iat) {
        SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()).format(Date(item.iat * 1000L))
    }

    val nowSec = remember { System.currentTimeMillis() / 1000L }
    val isExpired = item.exp > 0L && nowSec > item.exp

    val validityStr = remember(item.exp) {
        if (item.exp <= 0L) {
            "دائم (بلا انتهاء)"
        } else {
            val expDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(item.exp * 1000L))
            if (isExpired) {
                "منتهي في $expDate"
            } else {
                val daysLeft = ((item.exp - nowSec) / 86_400L).coerceAtLeast(0L)
                "$expDate (متبقي $daysLeft يوماً)"
            }
        }
    }

    val hasDeviceDetails = item.manufacturer.isNotBlank() ||
        item.model.isNotBlank() ||
        item.osVersion.isNotBlank() ||
        item.abi.isNotBlank() ||
        item.appVersion.isNotBlank()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.screenBackground)
                .navigationBarsPadding()
        ) {
            AppTopBar(
                title = "تفاصيل الترخيص",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Client & License Information
                Text(
                    text = "بيانات العميل والترخيص",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                AppSectionCard {
                    item {
                        DetailInfoRow(title = "العميل / الجهة", value = item.clientName ?: "عميل غير مسمى")
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "معرّف التثبيت",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = item.installationId,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Installation ID", item.installationId))
                                    Toast.makeText(context, "تم نسخ معرّف التثبيت", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "نسخ المعرّف",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    item {
                        DetailInfoRow(
                            title = "الحالة",
                            value = if (isExpired) "منتهي الصلاحية" else "نشط"
                        )
                    }
                    item {
                        DetailInfoRow(title = "تاريخ الإصدار", value = issueDateStr)
                    }
                    item {
                        DetailInfoRow(title = "الصلاحية", value = validityStr)
                    }
                    item {
                        DetailInfoRow(
                            title = "الحد الأقصى للمجلدات",
                            value = if (item.maxWorkspaces <= 0) "غير محدود" else "${item.maxWorkspaces} مجلدات"
                        )
                    }
                }

                // Card 2: Permissions
                Text(
                    text = "الصلاحيات الممنوحة",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                AppSectionCard {
                    item {
                        DetailPermissionRow(title = "تصدير ومشاركة Excel", enabled = item.excelExport)
                    }
                    item {
                        DetailPermissionRow(title = "قفل التطبيق بالبصمة", enabled = item.appLock)
                    }
                    item {
                        DetailPermissionRow(title = "المزامنة التلقائية للمجلدات", enabled = item.bgSync)
                    }
                }

                // Card 3: Client Device Info (if available)
                if (hasDeviceDetails) {
                    Text(
                        text = "بيانات جهاز العميل",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )

                    AppSectionCard {
                        if (item.manufacturer.isNotBlank()) {
                            item { DetailInfoRow(title = "نوع الهاتف", value = item.manufacturer) }
                        }
                        if (item.model.isNotBlank()) {
                            item { DetailInfoRow(title = "موديل الهاتف", value = item.model) }
                        }
                        if (item.osVersion.isNotBlank()) {
                            item { DetailInfoRow(title = "إصدار النظام", value = item.osVersion) }
                        }
                        if (item.abi.isNotBlank()) {
                            item { DetailInfoRow(title = "المعمارية", value = item.abi) }
                        }
                        if (item.appVersion.isNotBlank()) {
                            item { DetailInfoRow(title = "إصدار التطبيق", value = item.appVersion) }
                        }
                    }
                }

                // Card 4: Full License Code
                Text(
                    text = "كود الترخيص الرقمي",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
                        .background(colors.cardBackground)
                        .border(
                            AdminDesignTokens.DividerThickness,
                            colors.divider,
                            RoundedCornerShape(AdminDesignTokens.CardCornerRadius)
                        )
                        .padding(14.dp)
                ) {
                    Text(
                        text = item.licenseCode,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        lineHeight = 18.sp
                    )
                }

                // Actions
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                ) {
                    AppPrimaryButton(
                        text = "تجديد الترخيص بنفس البيانات",
                        icon = Icons.Outlined.Autorenew,
                        onClick = { onRenew(item) }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            AppSecondaryButton(
                                text = "نسخ الكود",
                                icon = Icons.Outlined.ContentCopy,
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("License Code", item.licenseCode))
                                    Toast.makeText(context, "تم نسخ الكود!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            AppSecondaryButton(
                                text = "مشاركة",
                                icon = Icons.Outlined.Share,
                                onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, item.licenseCode)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, "مشاركة كود الترخيص"))
                                }
                            )
                        }
                    }

                    AppSecondaryButton(
                        text = "حذف من السجل المحلي",
                        icon = Icons.Outlined.DeleteOutline,
                        contentColor = colors.statusError,
                        onClick = { showDeleteDialog = true }
                    )
                }
            }
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(AdminDesignTokens.CardCornerRadius),
                title = {
                    Text(
                        text = "حذف الترخيص من السجل",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من حذف هذا الترخيص من السجل المحلي؟ لن يؤثر الحذف على تشغيل التطبيق لدى العميل.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            LicenseHistoryManager.deleteLicense(context, item.internalId)
                            showDeleteDialog = false
                            onDeleted()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.statusError)
                    ) {
                        Text("حذف", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailInfoRow(title: String, value: String) {
    val colors = LocalAdminColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
    }
}

@Composable
private fun DetailPermissionRow(title: String, enabled: Boolean) {
    val colors = LocalAdminColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textPrimary
        )
        Icon(
            imageVector = if (enabled) Icons.Outlined.Check else Icons.Outlined.Close,
            contentDescription = if (enabled) "مفعّل" else "معطّل",
            tint = if (enabled) colors.textPrimary else colors.textSecondary,
            modifier = Modifier.size(19.dp)
        )
    }
}
