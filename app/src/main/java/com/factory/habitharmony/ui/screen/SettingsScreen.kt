package com.factory.habitharmony.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.factory.habitharmony.billing.PremiumManager
import com.factory.habitharmony.ui.components.PremiumUpgradeCard
import com.factory.habitharmony.ui.components.ProBadge
import com.factory.habitharmony.ui.theme.TextMuted
import com.factory.habitharmony.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    premiumManager: PremiumManager,
    onNavigateToPaywall: () -> Unit,
    onDarkModeChanged: (Boolean) -> Unit = {}
) {
    val premiumState by premiumManager.premiumState.collectAsState()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    val hapticFeedback = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("settings_prefs", android.content.Context.MODE_PRIVATE) }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications_enabled", true)) }
    var darkModeEnabled by remember { mutableStateOf(prefs.getBoolean("dark_mode_enabled", true)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 16.dp)
        )

        // Premium upgrade card
        PremiumUpgradeCard(
            isPremium = premiumState.isPremium,
            onClick = onNavigateToPaywall
        )

        Spacer(modifier = Modifier.height(20.dp))

        // General section
        SectionHeader("General")

        SettingsCard {
            SettingsToggle(
                icon = Icons.Filled.Notifications,
                title = "Notifications",
                subtitle = "Daily habit reminders",
                checked = notificationsEnabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    notificationsEnabled = it
                    prefs.edit().putBoolean("notifications_enabled", it).apply()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsToggle(
                icon = Icons.Filled.DarkMode,
                title = "Dark Mode",
                subtitle = "Use dark theme",
                checked = darkModeEnabled,
                onCheckedChange = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    darkModeEnabled = it
                    prefs.edit().putBoolean("dark_mode_enabled", it).apply()
                    onDarkModeChanged(it)
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Premium settings section
        SectionHeader("Premium Features")

        SettingsCard {
            // Cloud backup - premium
            SettingsRow(
                icon = Icons.Filled.CloudSync,
                title = "Cloud Backup & Sync",
                subtitle = "Sync your data across devices",
                isPremium = true,
                isUserPremium = premiumState.isPremium,
                onClick = {
                    if (!premiumState.isPremium) onNavigateToPaywall()
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // Custom theme - premium
            SettingsRow(
                icon = Icons.Filled.Palette,
                title = "Custom Theme",
                subtitle = "Personalize app colors",
                isPremium = true,
                isUserPremium = premiumState.isPremium,
                onClick = {
                    if (!premiumState.isPremium) onNavigateToPaywall()
                }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // About section
        SectionHeader("About")

        SettingsCard {
            SettingsRow(
                title = "Version",
                subtitle = versionName
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                title = "Terms of Service",
                onClick = { uriHandler.openUri("https://habitharmony.app/terms") }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsRow(
                title = "Privacy Policy",
                onClick = { uriHandler.openUri("https://habitharmony.app/privacy") }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        content()
    }
}

@Composable
private fun SettingsToggle(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icon != null) 12.dp else 0.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector? = null,
    title: String,
    subtitle: String? = null,
    isPremium: Boolean = false,
    isUserPremium: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val hapticFeedback = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable(role = Role.Button) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                } else Modifier
            )
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = if (icon != null) 12.dp else 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge)
                if (isPremium && !isUserPremium) {
                    ProBadge()
                }
            }
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}
