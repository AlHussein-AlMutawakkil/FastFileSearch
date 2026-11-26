package com.example.fastfilesearch.desktop.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
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
            Icon(
                imageVector = Icons.Outlined.Lock,
                contentDescription = "قفل التطبيق",
                tint = colors.textPrimary,
                modifier = Modifier.size(52.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "بحث آمن",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "التطبيق مقفل بكلمة مرور",
                    fontSize = 14.sp,
                    color = colors.textSecondary
                )
            }

            DesktopSectionCard {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.subCardBackground)
                            .border(
                                1.dp,
                                if (errorMessage != null) colors.dangerText else colors.divider,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BasicTextField(
                            value = passcode,
                            onValueChange = {
                                passcode = it
                                errorMessage = null
                            },
                            singleLine = true,
                            enabled = lockoutSeconds == 0,
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
                            cursorBrush = SolidColor(colors.textPrimary),
                            modifier = Modifier
                                .weight(1f)
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
                            decorationBox = { inner ->
                                if (passcode.isEmpty()) {
                                    Text(
                                        text = "أدخل كلمة المرور",
                                        color = colors.textSecondary.copy(alpha = 0.6f),
                                        fontSize = 14.sp
                                    )
                                }
                                inner()
                            }
                        )

                        IconButton(
                            onClick = { passwordVisible = !passwordVisible },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (passwordVisible) "إخفاء" else "إظهار",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

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
