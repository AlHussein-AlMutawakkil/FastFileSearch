package com.example.fastfilesearch.desktop.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fastfilesearch.desktop.licensing.DesktopAppLockManager
import kotlinx.coroutines.delay

@Composable
fun DesktopLockScreen(
    lockManager: DesktopAppLockManager,
    onUnlocked: () -> Unit
) {
    val colors = LocalDesktopColors.current
    var passcode by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lockoutSeconds by remember { mutableIntStateOf(lockManager.getRemainingLockoutSeconds()) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(lockoutSeconds) {
        if (lockoutSeconds > 0) {
            delay(1000L)
            lockoutSeconds = lockManager.getRemainingLockoutSeconds()
        }
    }

    fun attemptUnlock() {
        if (lockoutSeconds > 0 || passcode.isBlank()) return
        if (lockManager.verifyPasscode(passcode)) {
            passcode = ""
            errorMessage = null
            onUnlocked()
        } else {
            lockoutSeconds = lockManager.getRemainingLockoutSeconds()
            errorMessage = if (lockoutSeconds > 0) {
                "تم تجاوز عدد المحاولات. يرجى الانتظار $lockoutSeconds ثانية."
            } else {
                "كلمة المرور غير صحيحة"
            }
        }
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
                .widthIn(max = 420.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = DesktopDesignTokens.AppLogo,
                    contentDescription = "شعار البرنامج",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(68.dp)
                )
                Text(
                    text = "بحث آمن",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            }

            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FluentTextBox(
                        value = passcode,
                        onValueChange = {
                            passcode = it
                            errorMessage = null
                        },
                        placeholder = "أدخل كلمة المرور",
                        enabled = lockoutSeconds == 0,
                        isError = errorMessage != null,
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        textStyle = TextStyle(
                            color = colors.textPrimary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Start
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        fieldModifier = Modifier
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                ) {
                                    attemptUnlock()
                                    true
                                } else {
                                    false
                                }
                            },
                        trailingContent = {
                            FluentTooltip(if (passwordVisible) "إخفاء كلمة المرور" else "إظهار كلمة المرور") {
                                IconButton(
                                    onClick = { passwordVisible = !passwordVisible },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                        contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )

                    if (lockoutSeconds > 0) {
                        Text(
                            text = "المحاولات مقيدة. يرجى الانتظار $lockoutSeconds ثانية",
                            fontSize = 13.sp,
                            color = colors.dangerText
                        )
                    } else if (errorMessage != null) {
                        Text(
                            text = errorMessage.orEmpty(),
                            fontSize = 13.sp,
                            color = colors.dangerText
                        )
                    }

                    DesktopPrimaryButton(
                        text = "فتح التطبيق",
                        enabled = passcode.isNotBlank() && lockoutSeconds == 0,
                        onClick = { attemptUnlock() },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
