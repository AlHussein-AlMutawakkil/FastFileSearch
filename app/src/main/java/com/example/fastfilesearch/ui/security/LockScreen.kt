package com.example.fastfilesearch.ui.security

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.fastfilesearch.R
import com.example.fastfilesearch.security.AppLockManager
import com.example.fastfilesearch.security.AppLockMode
import com.example.fastfilesearch.ui.designsystem.AppDesignTokens
import com.example.fastfilesearch.ui.designsystem.AppPrimaryButton
import com.example.fastfilesearch.ui.designsystem.LocalAppColors
import com.example.fastfilesearch.ui.designsystem.ProvideAppDesignTokens
import com.example.fastfilesearch.util.findActivity
import kotlinx.coroutines.delay

@Composable
fun LockScreen(
    lockManager: AppLockManager,
    isDark: Boolean = isSystemInDarkTheme(),
    onUnlock: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity<FragmentActivity>()

    BackHandler {
        activity?.moveTaskToBack(true)
    }

    ProvideAppDesignTokens(isDark = isDark) {
        val colors = LocalAppColors.current

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            val lockMode = lockManager.lockMode
            var passcode by remember { mutableStateOf("") }
            var passwordVisible by remember { mutableStateOf(false) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            var lockoutSeconds by remember { mutableIntStateOf(lockManager.getRemainingLockoutSeconds()) }

            LaunchedEffect(lockoutSeconds) {
                if (lockoutSeconds > 0) {
                    delay(1000L)
                    lockoutSeconds = lockManager.getRemainingLockoutSeconds()
                }
            }

            // Launch biometric automatically when screen appears
            LaunchedEffect(Unit) {
                if ((lockMode == AppLockMode.BIOMETRIC_ONLY || lockMode == AppLockMode.BOTH) &&
                    AppLockManager.canAuthenticateBiometric(context) &&
                    activity != null
                ) {
                    AppLockManager.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            lockManager.unlock()
                            onUnlock()
                        },
                        onError = { /* Keep screen and allow manual retry or passcode */ }
                    )
                }
            }

            val triggerBiometric = {
                if (activity != null && AppLockManager.canAuthenticateBiometric(context)) {
                    AppLockManager.showBiometricPrompt(
                        activity = activity,
                        onSuccess = {
                            lockManager.unlock()
                            onUnlock()
                        },
                        onError = { errorMessage = it }
                    )
                }
            }

            val attemptPasscodeUnlock = {
                if (lockoutSeconds == 0) {
                    if (lockManager.verifyPasscode(passcode)) {
                        errorMessage = null
                        onUnlock()
                    } else {
                        lockoutSeconds = lockManager.getRemainingLockoutSeconds()
                        errorMessage = if (lockoutSeconds > 0) {
                            "تم تجاوز عدد المحاولات. يرجى الانتظار $lockoutSeconds ثانية"
                        } else {
                            "كلمة المرور غير صحيحة"
                        }
                    }
                }
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
                // App Logo (no card/container background) and Title
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

                Spacer(modifier = Modifier.height(28.dp))

                // Passcode Section (if PASSCODE_ONLY or BOTH)
                if (lockMode == AppLockMode.PASSCODE_ONLY || lockMode == AppLockMode.BOTH) {
                    // Password Input Field
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(AppDesignTokens.SubCardCornerRadius))
                            .background(colors.cardBackground)
                            .border(
                                width = 1.dp,
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
                                value = passcode,
                                onValueChange = {
                                    passcode = it
                                    errorMessage = null
                                },
                                singleLine = true,
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = { attemptPasscodeUnlock() }),
                                textStyle = TextStyle(
                                    color = colors.textPrimary,
                                    fontSize = 16.sp,
                                    textAlign = TextAlign.Start
                                ),
                                cursorBrush = SolidColor(colors.textPrimary),
                                enabled = lockoutSeconds == 0,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (passcode.isEmpty()) {
                                        Text(
                                            text = "أدخل كلمة المرور",
                                            color = colors.textSecondary.copy(alpha = 0.6f),
                                            fontSize = 16.sp
                                        )
                                    }
                                    innerTextField()
                                }
                            )

                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (lockoutSeconds > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المحاولات مقيدة. يرجى الانتظار $lockoutSeconds ثانية",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    } else if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AppPrimaryButton(
                        text = "فتح التطبيق",
                        onClick = { attemptPasscodeUnlock() },
                        enabled = passcode.isNotBlank() && lockoutSeconds == 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Biometric Icon Only (no button frame, no text)
                if (lockMode == AppLockMode.BIOMETRIC_ONLY || lockMode == AppLockMode.BOTH) {
                    Spacer(modifier = Modifier.height(24.dp))

                    IconButton(
                        onClick = triggerBiometric,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Fingerprint,
                            contentDescription = "فتح بالبصمة",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }
        }
    }
}
