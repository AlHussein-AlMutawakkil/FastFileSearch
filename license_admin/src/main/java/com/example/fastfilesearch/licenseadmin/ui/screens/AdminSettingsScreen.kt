package com.example.fastfilesearch.licenseadmin.ui.screens

import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.licenseadmin.R
import com.example.fastfilesearch.licenseadmin.security.AdminLockManager
import com.example.fastfilesearch.licenseadmin.security.KeyStorageManager
import com.example.fastfilesearch.licenseadmin.ui.components.AppNavigationRow
import com.example.fastfilesearch.licenseadmin.ui.components.AppSectionCard
import com.example.fastfilesearch.licenseadmin.ui.components.AppTextField
import com.example.fastfilesearch.licenseadmin.ui.components.AppToggleRow
import com.example.fastfilesearch.licenseadmin.ui.components.AppTopBar
import com.example.fastfilesearch.licenseadmin.ui.theme.AdminDesignTokens
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors

@Composable
fun AdminSettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalAdminColors.current

    var hasKey by remember { mutableStateOf(KeyStorageManager.hasKey(context)) }
    var isBiometricEnabled by remember { mutableStateOf(AdminLockManager.isLockEnabled(context)) }
    var showDeleteKeyDialog by remember { mutableStateOf(false) }
    var showPasteDialog by remember { mutableStateOf(false) }
    var pastedKeyText by remember { mutableStateOf("") }

    fun processUri(uri: Uri?) {
        if (uri == null) return
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val result = KeyStorageManager.importKeyFromStream(context, stream)
                if (result.isSuccess) {
                    hasKey = true
                    Toast.makeText(context, "تم استيراد المفتاح الخاص وتوثيقه بنجاح!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, result.exceptionOrNull()?.message ?: "فشل الاستيراد", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Toast.makeText(context, "تعذر فتح ملف المفتاح", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            processUri(uri)
        }
    }

    fun launchSafeFilePicker() {
        try {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            val chooser = Intent.createChooser(intent, "اختر ملف المفتاح (private_key.pem)")
            filePickerLauncher.launch(chooser)
        } catch (_: Exception) {
            try {
                val openDocIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                filePickerLauncher.launch(openDocIntent)
            } catch (e2: Exception) {
                Toast.makeText(context, "تعذر فتح مدير الملفات، استخدم خيار لصق النص.", Toast.LENGTH_LONG).show()
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
                title = "إعدادات المفتاح والأمان",
                onBack = onBack
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // Section 1: Key Management
                Text(
                    text = "إدارة المفتاح الخاص للتوقيع",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                AppSectionCard {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (hasKey) colors.statusSuccess.copy(alpha = 0.12f) else colors.statusError.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (hasKey) Icons.Outlined.Check else Icons.Outlined.Warning,
                                        contentDescription = null,
                                        tint = if (hasKey) colors.statusSuccess else colors.statusError,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = if (hasKey) "المفتاح الخاص مشفّراً ومحفوظ" else "المفتاح الخاص غير موجود",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (hasKey) "تم التحقق ومطابقته مع المفتاح العام بنجاح" else "يجب استيراد private_key.pem لتوليد التراخيص",
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }

                    item {
                        AppNavigationRow(
                            title = if (hasKey) "استبدال المفتاح باختيار ملف" else "استيراد المفتاح باختيار ملف",
                            subtitle = "اختيار ملف private_key.pem جديد من الهاتف",
                            icon = Icons.Outlined.FileOpen,
                            onClick = {
                                launchSafeFilePicker()
                            }
                        )
                    }

                    item {
                        AppNavigationRow(
                            title = "لصق نص المفتاح مباشرة",
                            subtitle = "لصق محتوى المفتاح الخاص من الحافظة",
                            icon = Icons.Outlined.ContentPaste,
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clipItem = clipboard?.primaryClip?.getItemAt(0)
                                val text = clipItem?.text?.toString()?.trim()
                                pastedKeyText = text ?: ""
                                showPasteDialog = true
                            }
                        )
                    }

                    if (hasKey) {
                        item {
                            AppNavigationRow(
                                title = "حذف المفتاح الخاص من الجهاز",
                                subtitle = "مسح المفتاح المشفر نهائياً من التخزين الآمن",
                                icon = Icons.Outlined.DeleteOutline,
                                onClick = {
                                    showDeleteKeyDialog = true
                                }
                            )
                        }
                    }
                }

                // Section 2: Security & Lock
                Text(
                    text = "حماية التطبيق",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                AppSectionCard {
                    item {
                        AppToggleRow(
                            title = "قفل التطبيق بالبصمة أو رمز الشاشة",
                            subtitle = "طلب تأكيد الهوية عند كل فتح لتطبيق مولّد التراخيص",
                            icon = Icons.Outlined.Fingerprint,
                            checked = isBiometricEnabled,
                            onCheckedChange = { enable ->
                                if (enable) {
                                    if (AdminLockManager.canAuthenticate(context)) {
                                        AdminLockManager.setLockEnabled(context, true)
                                        isBiometricEnabled = true
                                        Toast.makeText(context, "تم تفعيل القفل بنجاح", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "الجهاز لا يدعم أو لم يتم ضبط بصمة أو رمز شاشة", Toast.LENGTH_LONG).show()
                                    }
                                } else {
                                    AdminLockManager.setLockEnabled(context, false)
                                    isBiometricEnabled = false
                                }
                            }
                        )
                    }
                }

                // Section 3: About
                Text(
                    text = "معلومات النظام",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )

                AppSectionCard {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_encrypted_shield),
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                            Column {
                                Text(
                                    text = "مولّد تراخيص بحث آمن",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "الإصدار 1.0 (ECDSA P-256 + KeyStore AES-256)",
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "لا يتم إرسال أي بيانات أو مفاتيح عبر الشبكة؛ كافة عمليات التوقيع والتشفير تتم محلياً بالكامل على جهازك.",
                                fontSize = 12.sp,
                                color = colors.textSecondary,
                                lineHeight = 17.sp
                            )
                        }
                    }
                }
            }
        }

        // Paste Key Dialog
        if (showPasteDialog) {
            AlertDialog(
                onDismissRequest = { showPasteDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(AdminDesignTokens.CardCornerRadius),
                title = {
                    Text(
                        text = "لصق نص المفتاح الخاص (private_key.pem)",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "الصق نص المفتاح بما يشمل -----BEGIN PRIVATE KEY-----:",
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                        AppTextField(
                            value = pastedKeyText,
                            onValueChange = { pastedKeyText = it },
                            placeholder = "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----",
                            singleLine = false,
                            isMonospace = true,
                            modifier = Modifier.height(150.dp)
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (pastedKeyText.isNotBlank()) {
                                val res = KeyStorageManager.importKeyFromPem(context, pastedKeyText)
                                if (res.isSuccess) {
                                    hasKey = true
                                    showPasteDialog = false
                                    Toast.makeText(context, "تم استيراد المفتاح وتوثيقه بنجاح!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, res.exceptionOrNull()?.message ?: "فشل الاستيراد", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("استيراد وتوثيق", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showPasteDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textSecondary)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }

        // Delete Key Confirmation Dialog
        if (showDeleteKeyDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteKeyDialog = false },
                containerColor = colors.cardBackground,
                shape = RoundedCornerShape(AdminDesignTokens.CardCornerRadius),
                title = {
                    Text(
                        text = "حذف المفتاح الخاص",
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Text(
                        text = "هل أنت متأكد من حذف المفتاح الخاص من الجهاز؟ لن تتمكن من توليد أي تراخيص جديدة حتى تعيد استيراد ملف المفتاح من ذاكرة هاتفك.",
                        color = colors.textSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            KeyStorageManager.deleteKey(context)
                            hasKey = false
                            showDeleteKeyDialog = false
                            Toast.makeText(context, "تم حذف المفتاح الخاص بنجاح", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.statusError)
                    ) {
                        Text("حذف المفتاح", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDeleteKeyDialog = false },
                        colors = ButtonDefaults.textButtonColors(contentColor = colors.textPrimary)
                    ) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }
}
