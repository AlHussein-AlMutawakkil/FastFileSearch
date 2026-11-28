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
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockMode
import com.example.fastfilesearch.desktop.licensing.DesktopAutoLockTimeout
import com.example.fastfilesearch.desktop.model.SearchMethod

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DesktopSettingsScreen(
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
    onOpenAbout: () -> Unit
) {
    val colors = LocalDesktopColors.current

    val currentLockMode by appLockManager.lockModeFlow.collectAsState()
    val currentTimeout by appLockManager.autoLockTimeoutFlow.collectAsState()

    var statusToast by remember { mutableStateOf<String?>(null) }
    var showSetPasscodeDialog by remember { mutableStateOf(false) }
    var targetLockModeAfterSet by remember { mutableStateOf<DesktopAppLockMode?>(null) }
    var showVerifyPasscodeDialog by remember { mutableStateOf(false) }
    var pendingVerifiedAction by remember { mutableStateOf<(() -> Unit)?>(null) }

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

        // Security (App Lock), Auto-Sync, & Search History
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
                    onToggle = onToggleBackgroundSync
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
                                    if (!appLockManager.hasPasscode()) {
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
                InfoRow("إصدار سطح المكتب", "بحث آمن ${DesktopDesignTokens.DESKTOP_APP_VERSION}")
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
