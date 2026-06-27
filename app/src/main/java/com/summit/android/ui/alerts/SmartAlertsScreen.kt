package com.summit.android.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summit.android.billing.PremiumFeature
import com.summit.android.billing.PremiumManager
import com.summit.android.billing.SubscriptionTier
import com.summit.android.service.SmartAlertsService
import com.summit.android.ui.auth.LockedFeatureCard
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAlertsScreen(
    onBack: () -> Unit,
    onUpgrade: () -> Unit
) {
    val context = LocalContext.current
    val currentTier by PremiumManager.currentTier.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Alerts") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (currentTier != SubscriptionTier.PREMIUM) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                LockedFeatureCard(
                    feature = PremiumFeature.SMART_ALERTS,
                    onUpgrade = onUpgrade
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                BudgetThresholdSection(context)
                Spacer(modifier = Modifier.height(24.dp))
                UnusualChargesSection(context)
                Spacer(modifier = Modifier.height(24.dp))
                TestSection(context)
            }
        }
    }
}

@Composable
fun BudgetThresholdSection(context: android.content.Context) {
    var enabled by remember { mutableStateOf(SmartAlertsService.isBudgetEnabled(context)) }
    var threshold by remember { mutableStateOf(SmartAlertsService.getBudgetThreshold(context)) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Budget Thresholds", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        SmartAlertsService.setBudgetEnabled(context, it)
                    }
                )
            }
            Text(
                "Get notified when spending in a category crosses your chosen percentage of assigned budget.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )
            
            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Alert near category limit at:", style = MaterialTheme.typography.labelMedium)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    listOf(50, 80, 100).forEachIndexed { index, value ->
                        SegmentedButton(
                            selected = threshold == value,
                            onClick = {
                                threshold = value
                                SmartAlertsService.setBudgetThreshold(context, value)
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = 3)
                        ) {
                            Text("$value%")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UnusualChargesSection(context: android.content.Context) {
    var enabled by remember { mutableStateOf(SmartAlertsService.isUnusualEnabled(context)) }
    var amount by remember { mutableStateOf(SmartAlertsService.getUnusualAmountThreshold(context).toString()) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unusual Charges", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = enabled,
                    onCheckedChange = {
                        enabled = it
                        SmartAlertsService.setUnusualEnabled(context, it)
                    }
                )
            }
            Text(
                "Get notified when an outflow exceeds the threshold, or is from a merchant you've never seen before.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary
            )

            if (enabled) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        amount = it
                        val decimal = it.toBigDecimalOrNull()
                        if (decimal != null) {
                            SmartAlertsService.setUnusualAmountThreshold(context, decimal)
                        }
                    },
                    label = { Text("Amount Threshold") },
                    prefix = { Text("$") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TestSection(context: android.content.Context) {
    Button(
        onClick = { SmartAlertsService.sendTestNotification(context) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("Send Test Notification")
    }
    Text(
        "Alerts run automatically after each sync. Use this to confirm notifications are working.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.secondary,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = Modifier.padding(top = 8.dp)
    )
}
