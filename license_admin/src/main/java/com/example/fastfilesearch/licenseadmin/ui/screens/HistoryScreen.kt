package com.example.fastfilesearch.licenseadmin.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
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
import com.example.fastfilesearch.licenseadmin.ui.components.AppTextField
import com.example.fastfilesearch.licenseadmin.ui.components.AppTopBar
import com.example.fastfilesearch.licenseadmin.ui.theme.AdminDesignTokens
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAdminColors.current

    var historyList by remember { mutableStateOf(LicenseHistoryManager.getHistory(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var itemToDelete by remember { mutableStateOf<IssuedLicenseItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val filteredList = remember(historyList, searchQuery) {
        if (searchQuery.isBlank()) {
            historyList
        } else {
            val q = searchQuery.trim().lowercase(Locale.ROOT)
            historyList.filter {
                (it.clientName?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                        it.installationId.lowercase(Locale.ROOT).contains(q)
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(colors.screenBackground)
        ) {
            // Top Bar: Back button on the right (Start in RTL)
            AppTopBar(
                title = "سجل التراخيص المصدّرة",
                onBack = onBack,
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(
                            onClick = { showClearAllDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteSweep,
                                contentDescription = "مسح السجل بالكامل",
                                tint = colors.statusError,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            )

            // Search Box
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                AppTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "ابحث باسم العميل أو معرّف التثبيت...",
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                )
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = null,
                            tint = colors.textSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = if (searchQuery.isBlank()) "لا توجد تراخيص مصدّرة بعد" else "لا توجد نتائج مطابقة للبحث",
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredList, key = { it.internalId }) { item ->
                        HistoryItemCard(
                            item = item,
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("License Code", item.licenseCode)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "تم نسخ الكود!", Toast.LENGTH_SHORT).show()
                            },
                            onShare = {
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, item.licenseCode)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "مشاركة كود الترخيص"))
                            },
                            onDelete = {
                                itemToDelete = item
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }

        // Delete Single Item Confirmation Dialog
        if (itemToDelete != null) {
            AlertDialog(
                onDismissRequest = { itemToDelete = null },
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
                            itemToDelete?.let {
                                LicenseHistoryManager.deleteLicense(context, it.internalId)
                                historyList = LicenseHistoryManager.getHistory(context)
                            }
                            itemToDelete = null
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.statusError)
                    ) {
                        Text("حذف", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { itemToDelete = null },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Clear All Dialog
        if (showClearAllDialog) {
            AlertDialog(
                onDismissRequest = { showClearAllDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(AdminDesignTokens.CardCornerRadius),
                title = {
                    Text(
                        text = "مسح سجل التراخيص بالكامل",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من رغبتك في حذف جميع التراخيص المصدّرة المحفوظة في هذا الهاتف؟ لا يمكن التراجع عن هذه الخطوة.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            LicenseHistoryManager.clearHistory(context)
                            historyList = emptyList()
                            showClearAllDialog = false
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.statusError)
                    ) {
                        Text("مسح الكل", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showClearAllDialog = false },
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
fun HistoryItemCard(
    item: IssuedLicenseItem,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAdminColors.current
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault()) }
    val issueDateStr = remember(item.iat) { dateFormat.format(Date(item.iat * 1000L)) }

    val expiryStr = remember(item.exp) {
        if (item.exp <= 0L) {
            "دائم (بلا انتهاء)"
        } else {
            val expDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(item.exp * 1000L))
            val isExpired = System.currentTimeMillis() / 1000L > item.exp
            if (isExpired) "منتهي في $expDate" else "ينتهي في $expDate"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(
                AdminDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(AdminDesignTokens.CardCornerRadius)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header: Client Name + Installation ID
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.clientName ?: "عميل غير مسمى",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.installationId,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            // Expiry Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.subCardBackground)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = expiryStr,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )
            }
        }

        // Details Row: Date + Features
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "تاريخ الإصدار: $issueDateStr",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (item.excelExport) PermissionTag(text = "Excel")
                if (item.appLock) PermissionTag(text = "قفل")
                if (item.bgSync) PermissionTag(text = "مزامنة")
            }
        }

        // Code Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(AdminDesignTokens.SubCardCornerRadius))
                .background(colors.subCardBackground)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(
                text = item.licenseCode,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 2
            )
        }

        // Actions: Copy, Share, Delete
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "نسخ",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onShare,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "مشاركة",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "حذف",
                    tint = colors.statusError,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionTag(text: String) {
    val colors = LocalAdminColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.divider)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textSecondary
        )
    }
}
