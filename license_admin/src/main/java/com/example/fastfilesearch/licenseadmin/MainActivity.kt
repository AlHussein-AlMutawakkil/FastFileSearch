package com.example.fastfilesearch.licenseadmin

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.fastfilesearch.licenseadmin.history.IssuedLicenseItem
import com.example.fastfilesearch.licenseadmin.history.LicenseRenewalDraft
import com.example.fastfilesearch.licenseadmin.security.AdminLockManager
import com.example.fastfilesearch.licenseadmin.sync.LicenseLogSyncManager
import com.example.fastfilesearch.licenseadmin.ui.components.AppPrimaryButton
import com.example.fastfilesearch.licenseadmin.ui.screens.AdminSettingsScreen
import com.example.fastfilesearch.licenseadmin.ui.screens.GeneratorScreen
import com.example.fastfilesearch.licenseadmin.ui.screens.HistoryDetailScreen
import com.example.fastfilesearch.licenseadmin.ui.screens.HistoryScreen
import com.example.fastfilesearch.licenseadmin.ui.theme.LocalAdminColors
import com.example.fastfilesearch.licenseadmin.ui.theme.ProvideAdminTheme

enum class AdminScreen {
    GENERATOR,
    HISTORY,
    HISTORY_DETAIL,
    SETTINGS
}

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LicenseLogSyncManager.scheduleIfPending(this)

        setContent {
            val isDark = isSystemInDarkTheme()

            ProvideAdminTheme(isDark = isDark) {
                var currentScreen by remember { mutableStateOf(AdminScreen.GENERATOR) }
                var selectedHistoryItem by remember { mutableStateOf<IssuedLicenseItem?>(null) }
                var renewalDraft by remember { mutableStateOf<LicenseRenewalDraft?>(null) }
                var isLocked by remember { mutableStateOf(AdminLockManager.isLockEnabled(this@MainActivity)) }

                fun requestUnlock() {
                    AdminLockManager.promptBiometric(
                        activity = this@MainActivity,
                        title = "تأكيد الهوية",
                        subtitle = "يرجى مسح البصمة أو استخدام قفل الهاتف للمتابعة",
                        onSuccess = {
                            isLocked = false
                        },
                        onError = {
                            // Remain locked
                        }
                    )
                }

                LaunchedEffect(isLocked) {
                    if (isLocked) {
                        if (!AdminLockManager.canAuthenticate(this@MainActivity)) {
                            isLocked = false
                        } else {
                            requestUnlock()
                        }
                    }
                }

                if (isLocked) {
                    AdminLockScreen(onUnlockClick = { requestUnlock() })
                } else {
                    BackHandler(enabled = currentScreen != AdminScreen.GENERATOR) {
                        currentScreen = when (currentScreen) {
                            AdminScreen.HISTORY_DETAIL -> AdminScreen.HISTORY
                            else -> AdminScreen.GENERATOR
                        }
                    }

                    when (currentScreen) {
                        AdminScreen.GENERATOR -> {
                            GeneratorScreen(
                                onNavigateToHistory = { currentScreen = AdminScreen.HISTORY },
                                onNavigateToSettings = { currentScreen = AdminScreen.SETTINGS },
                                renewalDraft = renewalDraft,
                                onConsumeRenewalDraft = { renewalDraft = null }
                            )
                        }
                        AdminScreen.HISTORY -> {
                            HistoryScreen(
                                onBack = { currentScreen = AdminScreen.GENERATOR },
                                onOpenDetail = { item ->
                                    selectedHistoryItem = item
                                    currentScreen = AdminScreen.HISTORY_DETAIL
                                },
                                onRenew = { item ->
                                    renewalDraft = LicenseRenewalDraft(item = item)
                                    currentScreen = AdminScreen.GENERATOR
                                }
                            )
                        }
                        AdminScreen.HISTORY_DETAIL -> {
                            val detailItem = selectedHistoryItem
                            if (detailItem != null) {
                                HistoryDetailScreen(
                                    item = detailItem,
                                    onBack = { currentScreen = AdminScreen.HISTORY },
                                    onRenew = { item ->
                                        renewalDraft = LicenseRenewalDraft(item = item)
                                        currentScreen = AdminScreen.GENERATOR
                                    },
                                    onDeleted = {
                                        selectedHistoryItem = null
                                        currentScreen = AdminScreen.HISTORY
                                    }
                                )
                            } else {
                                currentScreen = AdminScreen.HISTORY
                            }
                        }
                        AdminScreen.SETTINGS -> {
                            AdminSettingsScreen(
                                onBack = { currentScreen = AdminScreen.GENERATOR }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminLockScreen(onUnlockClick: () -> Unit) {
    val colors = LocalAdminColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(colors.cardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_encrypted_shield),
                    contentDescription = null,
                    tint = colors.textPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "التطبيق مقفل",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "يرجى تأكيد هويتك للوصول إلى مولّد التراخيص",
                fontSize = 14.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(28.dp))

            AppPrimaryButton(
                text = "إلغاء القفل بالبصمة",
                icon = Icons.Outlined.Fingerprint,
                onClick = onUnlockClick
            )
        }
    }
}
