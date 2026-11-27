package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockMode
import com.example.fastfilesearch.desktop.licensing.DesktopAutoLockTimeout
import com.example.fastfilesearch.desktop.licensing.DesktopLicenseManager
import com.example.fastfilesearch.desktop.licensing.LicenseStatus
import com.example.fastfilesearch.desktop.licensing.WindowsDeviceIdProvider
import com.example.fastfilesearch.desktop.model.SearchMethod
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopSettingsScreen(
    licenseManager: DesktopLicenseManager,
    appLockManager: DesktopAppLockManager,
    themeMode: String,
    defaultSearchMethod: SearchMethod,
    showAllRows: Boolean,
    stretchSingleColumn: Boolean,
    verticalCopyFormat: Boolean,
    expandFullCellText: Boolean,
    backgroundSyncEnabled: Boolean,
    searchHistoryEnabled: Boolean,
    searchHistoryCount: Int,
    onChangeTheme: (String) -> Unit,
    onChangeDefaultMethod: (SearchMethod) -> Unit,
    onToggleShowAllRows: (Boolean) -> Unit,
    onToggleStretchSingleColumn: (Boolean) -> Unit,
    onToggleVerticalCopyFormat: (Boolean) -> Unit,
    onToggleExpandFullCellText: (Boolean) -> Unit,
    onToggleBackgroundSync: (Boolean) -> Unit,
    onToggleSearchHistory: (Boolean) -> Unit,
    onClearSearchHistory: () -> Unit,
    onOpenAbout: () -> Unit,
    onLicenseUpdated: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val r2Code = remember { licenseManager.buildR2ActivationRequest() }
    val status = licenseManager.getLicenseStatus()
    val payload = licenseManager.getActivePayload()

    val currentLockMode by appLockManager.lockModeFlow.collectAsState()
    val currentTimeout by appLockManager.autoLockTimeoutFlow.collectAsState()

    var statusToast by remember { mutableStateOf<String?>(null) }
    var showRenewDialog by remember { mutableStateOf(false) }
    var showSetPasscodeDialog by remember { mutableStateOf(false) }
    var targetLockModeAfterSet by remember { mutableStateOf<DesktopAppLockMode?>(null) }
    var showVerifyPasscodeDialog by remember { mutableStateOf(false) }
    var pendingVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val statusText = when (status) {
        LicenseStatus.ACTIVE -> "نشط"
        LicenseStatus.EXPIRED -> "منتهي الصلاحية"
        LicenseStatus.CLOCK_TAMPERED -> "خلل في ساعة النظام"
        LicenseStatus.NOT_ACTIVATED -> "غير مفعّل"
        LicenseStatus.INVALID -> "غير صالح"
    }

    val validityText = remember(payload) {
        if (payload == null) {
            "—"
        } else if (payload.isPermanent) {
            "دائم (بلا انتهاء)"
        } else {
            val sdf = SimpleDateFormat("yyyy/MM/dd", Locale.US)
            val dateStr = sdf.format(Date(payload.exp * 1000L))
            val daysLeft = ((payload.exp * 1000L - System.currentTimeMillis()) / 86_400_000L)
                .toInt()
                .coerceAtLeast(0)
            "$dateStr (متبقي $daysLeft يوماً)"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 860.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "الإعدادات",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

        statusToast?.let { msg ->
            DesktopSectionCard {
                Text(
                    text = msg,
                    fontSize = 13.sp,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }

        // Card 1: Unified R2 Device Code & License Details
        DesktopSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "معرّف الجهاز (R2)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                DesktopR2CodeBox(
                    r2Code = r2Code,
                    onCopied = { statusToast = "تم نسخ معرّف الجهاز R2 إلى الحافظة." }
                )

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                InfoRow(title = "حالة الترخيص", value = statusText)
                InfoRow(title = "صلاحية الترخيص", value = validityText)
                InfoRow(
                    title = "الحد الأقصى للمجلدات",
                    value = if (payload == null || payload.maxWorkspaces <= 0) "غير محدود" else "${payload.maxWorkspaces} مجلدات"
                )
                InfoRow(
                    title = "تصدير Excel (.xlsx)",
                    value = if (payload?.allowExcelExport == true) "مفعّل" else "غير مفعّل"
                )
                InfoRow(
                    title = "قفل التطبيق بكلمة مرور",
                    value = if (payload?.allowAppLock == true) "مفعّل" else "غير مفعّل"
                )
                InfoRow(
                    title = "المزامنة التلقائية بالخلفية",
                    value = if (payload?.allowAccess == true) "مفعّل" else "غير مفعّل"
                )

                Spacer(modifier = Modifier.height(4.dp))
                DesktopSecondaryButton(
                    text = "تحديث أو تجديد كود التفعيل",
                    icon = Icons.Outlined.Key,
                    onClick = { showRenewDialog = true }
                )
            }
        }

        // Card 2: Security (App Lock), Auto-Sync, & Search History
        DesktopSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "الأمان والمزامنة وسجل البحث",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                ToggleSettingRow(
                    title = "المزامنة التلقائية ومراقبة المجلدات",
                    subtitle = "فهرسة التغييرات تلقائياً أثناء تشغيل التطبيق",
                    checked = backgroundSyncEnabled,
                    onToggle = { enabled ->
                        if (enabled && !licenseManager.canUseBackgroundSync()) {
                            statusToast = "ميزة المزامنة التلقائية غير مفعّلة في الترخيص الحالي."
                        } else {
                            onToggleBackgroundSync(enabled)
                        }
                    }
                )

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                ToggleSettingRow(
                    title = "حفظ سجل البحث",
                    subtitle = "حفظ آخر 10 كلمات بحث للوصول السريع",
                    checked = searchHistoryEnabled,
                    onToggle = onToggleSearchHistory
                )

                if (searchHistoryEnabled && searchHistoryCount > 0) {
                    DesktopSecondaryButton(
                        text = "مسح سجل البحث ($searchHistoryCount عنصر)",
                        icon = Icons.Outlined.DeleteSweep,
                        onClick = {
                            onClearSearchHistory()
                            statusToast = "تم مسح سجل البحث السريع."
                        }
                    )
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Text(
                    text = "قفل التطبيق بكلمة مرور",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopAppLockMode.entries.forEach { mode ->
                        SelectableChip(
                            label = mode.titleArabic,
                            selected = currentLockMode == mode,
                            onClick = {
                                if (mode == currentLockMode) return@SelectableChip
                                if (mode == DesktopAppLockMode.DISABLED) {
                                    if (appLockManager.hasPasscode()) {
                                        pendingVerifiedAction = {
                                            appLockManager.setLockMode(DesktopAppLockMode.DISABLED)
                                            statusToast = "تم تعطيل قفل التطبيق."
                                        }
                                        showVerifyPasscodeDialog = true
                                    } else {
                                        appLockManager.setLockMode(DesktopAppLockMode.DISABLED)
                                    }
                                } else {
                                    if (!licenseManager.canUseAppLock()) {
                                        statusToast = "ميزة قفل التطبيق غير مفعّلة في الترخيص الحالي."
                                    } else if (!appLockManager.hasPasscode()) {
                                        targetLockModeAfterSet = mode
                                        showSetPasscodeDialog = true
                                    } else {
                                        appLockManager.setLockMode(mode)
                                        statusToast = "تم تفعيل قفل التطبيق بكلمة مرور."
                                    }
                                }
                            }
                        )
                    }
                }

                if (currentLockMode != DesktopAppLockMode.DISABLED) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DesktopSecondaryButton(
                            text = "تغيير كلمة المرور",
                            icon = Icons.Outlined.Lock,
                            onClick = {
                                pendingVerifiedAction = {
                                    targetLockModeAfterSet = currentLockMode
                                    showSetPasscodeDialog = true
                                }
                                showVerifyPasscodeDialog = true
                            }
                        )
                        DesktopSecondaryButton(
                            text = "قفل الآن (Ctrl+L)",
                            icon = Icons.Outlined.Lock,
                            onClick = { appLockManager.lockNow() }
                        )
                    }

                    Text(
                        text = "القفل التلقائي عند عدم النشاط",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DesktopAutoLockTimeout.entries.forEach { timeout ->
                            SelectableChip(
                                label = timeout.titleArabic,
                                selected = currentTimeout == timeout,
                                onClick = { appLockManager.setAutoLockTimeout(timeout) }
                            )
                        }
                    }
                }
            }
        }

        // Card 3: Appearance, Search Defaults, and Table Display
        DesktopSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "المظهر (الوضع الفاتح / الداكن)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val themes = listOf("system" to "حسب النظام", "light" to "فاتح", "dark" to "داكن")
                    themes.forEach { (key, label) ->
                        SelectableChip(
                            label = label,
                            selected = themeMode == key,
                            onClick = { onChangeTheme(key) }
                        )
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                Text(
                    text = "طريقة البحث الافتراضية",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchMethod.entries.forEach { m ->
                        SelectableChip(
                            label = m.displayName,
                            selected = defaultSearchMethod == m,
                            onClick = { onChangeDefaultMethod(m) }
                        )
                    }
                }

                HorizontalDivider(color = colors.divider, thickness = 1.dp)

                ToggleSettingRow(
                    title = "تمديد العمود الواحد في الجداول",
                    subtitle = "ملء عرض الجدول بالكامل عند وجود عمود واحد",
                    checked = stretchSingleColumn,
                    onToggle = onToggleStretchSingleColumn
                )

                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

                ToggleSettingRow(
                    title = "عرض جميع صفوف الجداول مباشرة بدون طي",
                    subtitle = "إظهار كامل الصفوف دون الحاجة لزر عرض المزيد",
                    checked = showAllRows,
                    onToggle = onToggleShowAllRows
                )

                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

                ToggleSettingRow(
                    title = "نسخ عمودي بأسماء الأعمدة",
                    subtitle = "نسخ الصفوف بتنسيق رأسي (اسم العمود: القيمة) عند نسخ صفوف كاملة",
                    checked = verticalCopyFormat,
                    onToggle = onToggleVerticalCopyFormat
                )

                HorizontalDivider(color = colors.divider, thickness = 0.5.dp)

                ToggleSettingRow(
                    title = "توسيع نص الخلية بالكامل",
                    subtitle = "عرض النص كاملاً داخل الخلية دون اقتصاره على 4 أسطر",
                    checked = expandFullCellText,
                    onToggle = onToggleExpandFullCellText
                )
            }
        }

        // Card 4: Supported Formats & About
        DesktopSectionCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "الصيغ المدعومة محلياً بدون إنترنت",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary
                )
                InfoRow("Excel", ".xlsx, .xls")
                InfoRow("قواعد بيانات Access", ".mdb, .accdb (للقراءة فقط)")
                InfoRow("Word", ".docx, .doc")
                InfoRow("PDF", ".pdf")
                InfoRow("نصوص", ".txt, .log (UTF-8 / Windows-1256)")
                InfoRow("CSV", ".csv")
                InfoRow("JSON", ".json")
                InfoRow("Markdown", ".md")
                HorizontalDivider(color = colors.divider, thickness = 1.dp)
                InfoRow("إصدار سطح المكتب", "بحث آمن ${WindowsDeviceIdProvider.DESKTOP_APP_VERSION}")
                Spacer(modifier = Modifier.height(4.dp))
                DesktopSecondaryButton(
                    text = "حول التطبيق والتواصل والسياسات",
                    icon = Icons.Outlined.Info,
                    onClick = onOpenAbout
                )
            }
        }
        }
    }

    if (showRenewDialog) {
        var newCode by remember { mutableStateOf("") }
        var renewErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showRenewDialog = false }) {
            DesktopSectionCard(modifier = Modifier.width(440.dp)) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تحديث كود الترخيص",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                        FluentTextBox(
                            value = newCode,
                            onValueChange = {
                                newCode = it
                                renewErr = null
                            },
                            placeholder = "XXXX-XXXX-XXXX-XXXX",
                            isError = renewErr != null,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    renewErr?.let { err ->
                        Text(text = err, color = colors.dangerText, fontSize = 13.sp)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DesktopPrimaryButton(
                            text = "تفعيل",
                            enabled = newCode.isNotBlank(),
                            onClick = {
                                val res = licenseManager.activateLicense(newCode)
                                if (res.status == LicenseStatus.ACTIVE) {
                                    showRenewDialog = false
                                    statusToast = "تم تحديث كود التفعيل بنجاح."
                                    onLicenseUpdated()
                                } else {
                                    renewErr = res.errorMessage ?: "كود غير صالح"
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                        DesktopSecondaryButton(
                            text = "إلغاء",
                            onClick = { showRenewDialog = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showSetPasscodeDialog) {
        DesktopSetPasscodeDialog(
            onDismiss = {
                showSetPasscodeDialog = false
                targetLockModeAfterSet = null
            },
            onConfirm = { newPass ->
                appLockManager.setPasscode(newPass)
                targetLockModeAfterSet?.let { appLockManager.setLockMode(it) }
                showSetPasscodeDialog = false
                targetLockModeAfterSet = null
                statusToast = "تم حفظ كلمة المرور بنجاح."
            }
        )
    }

    if (showVerifyPasscodeDialog) {
        DesktopVerifyPasscodeDialog(
            appLockManager = appLockManager,
            onDismiss = {
                showVerifyPasscodeDialog = false
                pendingVerifiedAction = null
            },
            onVerified = {
                showVerifyPasscodeDialog = false
                val action = pendingVerifiedAction
                pendingVerifiedAction = null
                action?.invoke()
            }
        )
    }
}

@Composable
private fun DesktopSetPasscodeDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalDesktopColors.current
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var show1 by remember { mutableStateOf(false) }
    var show2 by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(420.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("تعيين كلمة المرور", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                PasscodeFieldBox(p1, { p1 = it; err = null }, "كلمة المرور الجديدة (4 خانات على الأقل)", show1) { show1 = !show1 }
                PasscodeFieldBox(p2, { p2 = it; err = null }, "تأكيد كلمة المرور", show2) { show2 = !show2 }
                err?.let { Text(it, fontSize = 12.5.sp, color = colors.dangerText) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPrimaryButton(
                        text = "حفظ",
                        enabled = p1.isNotEmpty() && p2.isNotEmpty(),
                        onClick = {
                            when {
                                p1.length < 4 -> err = "كلمة المرور يجب أن تكون 4 خانات على الأقل."
                                p1 != p2 -> err = "كلمتا المرور غير متطابقتين."
                                else -> onConfirm(p1)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DesktopSecondaryButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DesktopVerifyPasscodeDialog(
    appLockManager: DesktopAppLockManager,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val colors = LocalDesktopColors.current
    var pass by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    var err by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        DesktopSectionCard(modifier = Modifier.width(420.dp)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("التحقق من كلمة المرور الحالية", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)
                PasscodeFieldBox(pass, { pass = it; err = null }, "أدخل كلمة المرور الحالية", visible) { visible = !visible }
                err?.let { Text(it, fontSize = 12.5.sp, color = colors.dangerText) }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DesktopPrimaryButton(
                        text = "تأكيد",
                        enabled = pass.isNotEmpty(),
                        onClick = {
                            if (appLockManager.verifyPasscode(pass)) {
                                onVerified()
                            } else {
                                err = "كلمة المرور غير صحيحة"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    DesktopSecondaryButton(text = "إلغاء", onClick = onDismiss, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PasscodeFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    visible: Boolean,
    onToggleVisible: () -> Unit
) {
    val colors = LocalDesktopColors.current
    FluentTextBox(
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        textStyle = TextStyle(color = colors.textPrimary, fontSize = 14.sp),
        modifier = Modifier.fillMaxWidth(),
        trailingContent = {
            IconButton(onClick = onToggleVisible, modifier = Modifier.size(26.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    )
}

@Composable
private fun ToggleSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    FluentSettingRow(
        title = title,
        subtitle = subtitle,
        onClick = { onToggle(!checked) }
    ) {
        FluentToggleSwitch(
            checked = checked,
            onCheckedChange = onToggle
        )
    }
}

@Composable
private fun InfoRow(title: String, value: String) {
    val colors = LocalDesktopColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, fontSize = 13.sp, color = colors.textSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalDesktopColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bg = when {
        selected -> colors.controlActiveBackground
        isHovered -> colors.cardBackground
        else -> colors.subCardBackground
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius))
            .background(bg)
            .border(
                1.dp,
                if (selected) colors.controlActiveBackground else colors.divider,
                RoundedCornerShape(DesktopDesignTokens.ControlCornerRadius)
            )
            .hoverable(interactionSource = interactionSource)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) colors.controlActiveContent else colors.textPrimary
        )
    }
}
