package com.summit.android.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onSyncAccount: () -> Unit,
    onSettleUp: () -> Unit,
    onCategoryRules: () -> Unit,
    onSmartAlerts: () -> Unit,
    onSubscriptions: () -> Unit,
    onCustomizeAppearance: () -> Unit,
    onFeatureGuide: () -> Unit = {}
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Settings") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item { SettingsSectionHeader("Account") }
            item {
                SettingsRow("Sync & Account", Icons.Default.Cloud, onSyncAccount)
            }
            item {
                SettingsRow("Shared Expenses", Icons.Default.People, onSettleUp)
            }
            item { SettingsSectionHeader("Automation") }
            item {
                SettingsRow("Transaction Rules", Icons.Default.AutoFixHigh, onCategoryRules)
            }
            item {
                SettingsRow("Smart Alerts", Icons.Default.NotificationsActive, onSmartAlerts)
            }
            item {
                SettingsRow("Subscriptions", Icons.Default.Repeat, onSubscriptions)
            }
            item { SettingsSectionHeader("Appearance") }
            item {
                SettingsRow("Customize Tabs & Colors", Icons.Default.Palette, onCustomizeAppearance)
            }
            item { SettingsSectionHeader("Help") }
            item {
                SettingsRow("Feature Guide", Icons.Default.Map, onFeatureGuide)
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun SettingsRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null) },
        trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
        modifier = Modifier.clickable { onClick() }
    )
    HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
}
