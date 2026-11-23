package com.example.fastfilesearch.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fastfilesearch.ui.designsystem.AppSectionCard
import com.example.fastfilesearch.ui.designsystem.AppSelectableRow
import com.example.fastfilesearch.ui.designsystem.AppTopBar
import com.example.fastfilesearch.ui.designsystem.LocalAppColors

@Composable
fun ThemeSettingsScreen(
    currentTheme: String,
    onSetTheme: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    BackHandler(onBack = onBack)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.screenBackground)
            .navigationBarsPadding()
    ) {
        AppTopBar(
            title = "المظهر",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AppSectionCard {
                item {
                    AppSelectableRow(
                        title = "النظام",
                        selected = currentTheme == "system",
                        onClick = { onSetTheme("system") }
                    )
                }
                item {
                    AppSelectableRow(
                        title = "فاتح",
                        selected = currentTheme == "light",
                        onClick = { onSetTheme("light") }
                    )
                }
                item {
                    AppSelectableRow(
                        title = "مظلم",
                        selected = currentTheme == "dark",
                        onClick = { onSetTheme("dark") }
                    )
                }
            }
        }
    }
}
