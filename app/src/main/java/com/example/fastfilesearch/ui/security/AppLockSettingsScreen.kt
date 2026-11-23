package com.example.fastfilesearch.ui.security

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.FragmentActivity
import com.example.fastfilesearch.licensing.LicenseManager
import com.example.fastfilesearch.security.AppLockManager
import com.example.fastfilesearch.security.AppLockMode
import com.example.fastfilesearch.security.AutoLockTimeout
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppNavigationRow
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppSelectableRow
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun AppLockSettingsScreen(
    lockManager: AppLockManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val colors = LocalAppColors.current

    val currentLockMode by lockManager.lockModeFlow.collectAsState()
    val currentTimeout by lockManager.autoLockTimeoutFlow.collectAsState()

    var showSetPasscodeDialog by remember { mutableStateOf(false) }
    var targetModeAfterPasscodeSet by remember { mutableStateOf<AppLockMode?>(null) }
    var showVerifyCurrentDialog by remember { mutableStateOf(false) }
    var pendingActionAfterVerify by remember { mutableStateOf<(() -> Unit)?>(null) }
    var showTimeoutDialog by remember { mutableStateOf(false) }

    BackHandler(onBack = onBack)

    val handleModeSelection: (AppLockMode) -> Unit = { targetMode ->
        if (targetMode == currentLockMode) {
            // Already selected
        } else if (targetMode == AppLockMode.DISABLED) {
            // Disabling lock requires verification if current lock is active
            if (currentLockMode != AppLockMode.DISABLED) {
                if (currentLockMode == AppLockMode.BIOMETRIC_ONLY && activity != null && AppLockManager.canAuthenticateBiometric(context)) {
                    AppLockManager.showBiometricPrompt(
                        activity = activity,
                        title = "تأكيد إلغاء القفل",
                        onSuccess = { lockManager.setLockMode(AppLockMode.DISABLED) },
                        onError = { Toast.makeText(context, "فشل التحقق من البصمة", Toast.LENGTH_SHORT).show() }
                    )
                } else if (lockManager.hasPasscode()) {
                    pendingActionAfterVerify = { lockManager.setLockMode(AppLockMode.DISABLED) }
                    showVerifyCurrentDialog = true
                } else {
                    lockManager.setLockMode(AppLockMode.DISABLED)
                }
            } else {
                lockManager.setLockMode(AppLockMode.DISABLED)
            }
        } else {
            // Switching to an active mode
            val licenseManager = LicenseManager.getInstance(context)
            if (!licenseManager.canUseAppLock()) {
                Toast.makeText(context, "ميزة قفل التطبيق غير مشمولة في الترخيص الحالي", Toast.LENGTH_SHORT).show()
            } else {
                val requiresBiometric = (targetMode == AppLockMode.BIOMETRIC_ONLY || targetMode == AppLockMode.BOTH)
                val requiresPasscode = (targetMode == AppLockMode.PASSCODE_ONLY || targetMode == AppLockMode.BOTH)

                val proceedWithBiometricAndPasscode = {
                    if (requiresPasscode && !lockManager.hasPasscode()) {
                        targetModeAfterPasscodeSet = targetMode
                        showSetPasscodeDialog = true
                    } else if (currentLockMode != AppLockMode.DISABLED && lockManager.hasPasscode()) {
                        // Changing mode when active: verify current passcode first
                        pendingActionAfterVerify = { lockManager.setLockMode(targetMode) }
                        showVerifyCurrentDialog = true
                    } else {
                        lockManager.setLockMode(targetMode)
                    }
                }

                if (requiresBiometric) {
                    if (!AppLockManager.canAuthenticateBiometric(context)) {
                        Toast.makeText(context, "البصمة غير متوفرة أو لم يتم تسجيل بصمة على هذا الجهاز", Toast.LENGTH_LONG).show()
                    } else if (activity != null) {
                        AppLockManager.showBiometricPrompt(
                            activity = activity,
                            title = "تأكيد تفعيل البصمة",
                            onSuccess = { proceedWithBiometricAndPasscode() },
                            onError = { Toast.makeText(context, "لم يتم تفعيل البصمة: تم إلغاء التحقق", Toast.LENGTH_SHORT).show() }
                        )
                    }
                } else {
                    proceedWithBiometricAndPasscode()
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "قفل التطبيق",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Lock Type
            AppSectionCard {
                item {
                    AppSelectableRow(
                        title = "معطّل",
                        selected = currentLockMode == AppLockMode.DISABLED,
                        onClick = { handleModeSelection(AppLockMode.DISABLED) }
                    )
                }
                item {
                    AppSelectableRow(
                        title = "البصمة فقط",
                        selected = currentLockMode == AppLockMode.BIOMETRIC_ONLY,
                        onClick = { handleModeSelection(AppLockMode.BIOMETRIC_ONLY) }
                    )
                }
                item {
                    AppSelectableRow(
                        title = "كلمة مرور فقط",
                        selected = currentLockMode == AppLockMode.PASSCODE_ONLY,
                        onClick = { handleModeSelection(AppLockMode.PASSCODE_ONLY) }
                    )
                }
                item {
                    AppSelectableRow(
                        title = "البصمة وكلمة المرور",
                        selected = currentLockMode == AppLockMode.BOTH,
                        onClick = { handleModeSelection(AppLockMode.BOTH) }
                    )
                }
            }

            // Card 2: Options (only if lock is not disabled)
            if (currentLockMode != AppLockMode.DISABLED) {
                AppSectionCard {
                    if (currentLockMode == AppLockMode.PASSCODE_ONLY || currentLockMode == AppLockMode.BOTH) {
                        item {
                            AppNavigationRow(
                                title = "تغيير كلمة المرور",
                                icon = Icons.Outlined.Lock,
                                showChevron = false,
                                onClick = {
                                    pendingActionAfterVerify = {
                                        targetModeAfterPasscodeSet = currentLockMode
                                        showSetPasscodeDialog = true
                                    }
                                    showVerifyCurrentDialog = true
                                }
                            )
                        }
                    }

                    item {
                        AppNavigationRow(
                            title = "القفل التلقائي",
                            value = currentTimeout.titleArabic,
                            icon = Icons.Outlined.Timer,
                            showChevron = false,
                            onClick = { showTimeoutDialog = true }
                        )
                    }
                }
            }
        }
    }

    // Dialog: Set/Change Passcode
    if (showSetPasscodeDialog) {
        PasscodeEditDialog(
            title = "تعيين كلمة المرور",
            onDismiss = {
                showSetPasscodeDialog = false
                targetModeAfterPasscodeSet = null
            },
            onConfirm = { newPasscode ->
                lockManager.setPasscode(newPasscode)
                targetModeAfterPasscodeSet?.let { lockManager.setLockMode(it) }
                showSetPasscodeDialog = false
                targetModeAfterPasscodeSet = null
                Toast.makeText(context, "تم حفظ كلمة المرور بنجاح", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Dialog: Verify Current Passcode
    if (showVerifyCurrentDialog) {
        PasscodeVerifyDialog(
            onDismiss = {
                showVerifyCurrentDialog = false
                pendingActionAfterVerify = null
            },
            onVerified = {
                showVerifyCurrentDialog = false
                val action = pendingActionAfterVerify
                pendingActionAfterVerify = null
                action?.invoke()
            },
            lockManager = lockManager
        )
    }

    // Dialog: AutoLockTimeout
    if (showTimeoutDialog) {
        AutoLockTimeoutSelectionDialog(
            currentTimeout = currentTimeout,
            onSelect = { timeout ->
                lockManager.setAutoLockTimeout(timeout)
                showTimeoutDialog = false
            },
            onDismiss = { showTimeoutDialog = false }
        )
    }
}

@Composable
private fun PasscodeEditDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var v1 by remember { mutableStateOf(false) }
    var v2 by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .background(colors.cardBackground)
                .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Passcode Input 1
                PasscodeInputBox(
                    value = p1,
                    onValueChange = { p1 = it; error = null },
                    placeholder = "كلمة المرور الجديدة (4 خانات على الأقل)",
                    isVisible = v1,
                    onToggleVisibility = { v1 = !v1 }
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Passcode Input 2
                PasscodeInputBox(
                    value = p2,
                    onValueChange = { p2 = it; error = null },
                    placeholder = "تأكيد كلمة المرور",
                    isVisible = v2,
                    onToggleVisibility = { v2 = !v2 }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", color = colors.textSecondary)
                    }

                    AppPrimaryButton(
                        text = "حفظ",
                        onClick = {
                            if (p1.length < 4) {
                                error = "كلمة المرور يجب أن تكون 4 خانات على الأقل"
                            } else if (p1 != p2) {
                                error = "كلمتا المرور غير متطابقتين"
                            } else {
                                onConfirm(p1)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = p1.isNotEmpty() && p2.isNotEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun PasscodeVerifyDialog(
    lockManager: AppLockManager,
    onDismiss: () -> Unit,
    onVerified: () -> Unit
) {
    val colors = LocalAppColors.current
    var passcode by remember { mutableStateOf("") }
    var isVisible by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .background(colors.cardBackground)
                .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "التحقق من كلمة المرور الحالية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasscodeInputBox(
                    value = passcode,
                    onValueChange = { passcode = it; error = null },
                    placeholder = "أدخل كلمة المرور الحالية",
                    isVisible = isVisible,
                    onToggleVisibility = { isVisible = !isVisible }
                )

                if (error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("إلغاء", color = colors.textSecondary)
                    }

                    AppPrimaryButton(
                        text = "تأكيد",
                        onClick = {
                            if (lockManager.verifyPasscode(passcode)) {
                                onVerified()
                            } else {
                                error = "كلمة المرور غير صحيحة"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = passcode.isNotEmpty()
                    )
                }
            }
        }
    }
}

@Composable
private fun PasscodeInputBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isVisible: Boolean,
    onToggleVisibility: () -> Unit
) {
    val colors = LocalAppColors.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
            .background(colors.subCardBackground)
            .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                textStyle = TextStyle(
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Start
                ),
                cursorBrush = SolidColor(colors.textPrimary),
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = colors.textSecondary.copy(alpha = 0.6f),
                            fontSize = 14.sp
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = onToggleVisibility,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun AutoLockTimeoutSelectionDialog(
    currentTimeout: AutoLockTimeout,
    onSelect: (AutoLockTimeout) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalAppColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .background(colors.cardBackground)
                .border(AppDesignTokens.DividerThickness, colors.divider, RoundedCornerShape(AppDesignTokens.MenuCornerRadius))
                .padding(vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "القفل التلقائي",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )

                HorizontalDivider(color = colors.divider, thickness = AppDesignTokens.DividerThickness)

                AutoLockTimeout.entries.forEach { timeout ->
                    AppSelectableRow(
                        title = timeout.titleArabic,
                        selected = currentTimeout == timeout,
                        onClick = { onSelect(timeout) }
                    )
                }
            }
        }
    }
}
