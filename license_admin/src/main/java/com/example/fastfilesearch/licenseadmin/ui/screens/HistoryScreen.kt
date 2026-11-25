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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.text.style.TextOverflow
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

enum class HistoryStatusFilter(val label: String) {
    ALL("الكل"),
    ACTIVE("النشطة"),
    EXPIRING_SOON("قريب الانتهاء"),
    EXPIRED("المنتهية")
}

internal fun matchesHistoryFilter(
    item: IssuedLicenseItem,
    filter: HistoryStatusFilter,
    nowSeconds: Long = System.currentTimeMillis() / 1000L
): Boolean {
    return when (filter) {
        HistoryStatusFilter.ALL -> true
        HistoryStatusFilter.ACTIVE -> item.exp <= 0L || item.exp > nowSeconds
        HistoryStatusFilter.EXPIRING_SOON -> {
            item.exp > nowSeconds && (item.exp - nowSeconds) <= 7L * 86_400L
        }
        HistoryStatusFilter.EXPIRED -> item.exp > 0L && nowSeconds > item.exp
    }
}

@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenDetail: (IssuedLicenseItem) -> Unit,
    onRenew: (IssuedLicenseItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAdminColors.current

    var historyList by remember { mutableStateOf(LicenseHistoryManager.getHistory(context)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryStatusFilter.ALL) }
    var itemToDelete by remember { mutableStateOf<IssuedLicenseItem?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }

    val filteredList = remember(historyList, searchQuery, selectedFilter) {
        val nowSec = System.currentTimeMillis() / 1000L
        val q = searchQuery.trim().lowercase(Locale.ROOT)
        historyList.filter { item ->
            val matchesStatus = matchesHistoryFilter(item, selectedFilter, nowSec)
            val matchesSearch = if (q.isBlank()) {
                true
            } else {
                (item.clientName?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                    item.installationId.lowercase(Locale.ROOT).contains(q) ||
                    item.manufacturer.lowercase(Locale.ROOT).contains(q) ||
                    item.model.lowercase(Locale.ROOT).contains(q)
            }
            matchesStatus && matchesSearch
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

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HistoryStatusFilter.entries, key = { it.name }) { filterOption ->
                    val isSelected = selectedFilter == filterOption
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(AdminDesignTokens.ChipCornerRadius))
                            .background(if (isSelected) colors.controlActiveBackground else colors.cardBackground)
                            .border(
                                AdminDesignTokens.DividerThickness,
                                if (isSelected) colors.controlActiveBackground else colors.divider,
                                RoundedCornerShape(AdminDesignTokens.ChipCornerRadius)
                            )
                            .clickable { selectedFilter = filterOption }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = filterOption.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.controlActiveContent else colors.textPrimary
                        )
                    }
                }
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
                            text = if (searchQuery.isBlank() && selectedFilter == HistoryStatusFilter.ALL) {
                                "لا توجد تراخيص مصدّرة بعد"
                            } else {
                                "لا توجد نتائج مطابقة"
                            },
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList, key = { it.internalId }) { item ->
                        CompactHistoryItemCard(
                            item = item,
                            onClick = { onOpenDetail(item) },
                            onRenew = { onRenew(item) },
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
private fun CompactHistoryItemCard(
    item: IssuedLicenseItem,
    onClick: () -> Unit,
    onRenew: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAdminColors.current
    var menuExpanded by remember { mutableStateOf(false) }

    val expiryStr = remember(item.exp) {
        if (item.exp <= 0L) {
            "دائم"
        } else {
            val expDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date(item.exp * 1000L))
            val isExpired = System.currentTimeMillis() / 1000L > item.exp
            if (isExpired) "منتهي $expDate" else expDate
        }
    }

    val devName = remember(item.manufacturer, item.model) {
        listOf(item.manufacturer, item.model).filter { it.isNotBlank() }.joinToString(" ")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AdminDesignTokens.CardCornerRadius))
            .background(colors.cardBackground)
            .border(
                AdminDesignTokens.DividerThickness,
                colors.divider,
                RoundedCornerShape(AdminDesignTokens.CardCornerRadius)
            )
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = item.clientName ?: "عميل غير مسمى",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.subCardBackground)
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = expiryStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                }
            }

            val subtitle = if (devName.isNotBlank()) {
                "${item.installationId} • $devName"
            } else {
                item.installationId
            }

            Text(
                text = subtitle,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(
                onClick = { menuExpanded = true },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "خيارات",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                modifier = Modifier
                    .background(colors.cardBackground)
                    .border(
                        AdminDesignTokens.DividerThickness,
                        colors.divider,
                        RoundedCornerShape(10.dp)
                    )
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "تجديد الترخيص",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Autorenew,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onRenew()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "نسخ الكود",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onCopy()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "مشاركة",
                            color = colors.textPrimary,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onShare()
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(
                            text = "حذف",
                            color = colors.statusError,
                            fontSize = 14.sp
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = colors.statusError,
                            modifier = Modifier.size(19.dp)
                        )
                    },
                    onClick = {
                        menuExpanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}
