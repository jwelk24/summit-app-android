package com.summit.android.ui.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.summit.android.billing.PremiumManager
import com.summit.android.billing.SubscriptionTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallScreen(onDismiss: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Plan") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Elevate Your Finances",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                "Choose the Summit plan that fits your life.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            TierCard(
                name = "Summit Pro",
                price = "$9.99/mo",
                features = listOf("Plaid Bank Sync", "30-Day Forecasting", "Basic Insights", "Solo Use"),
                buttonText = "Start Pro Trial",
                onSelect = { 
                    PremiumManager.setTier(SubscriptionTier.PRO)
                    onDismiss()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            TierCard(
                name = "Summit Premium",
                price = "$19.99/mo",
                features = listOf(
                    "Everything in Pro",
                    "Household Sharing",
                    "AI Smart Categorization",
                    "AI Weekly Summaries",
                    "AI Receipt Scanning",
                    "90-Day Full Horizon"
                ),
                buttonText = "Go Premium",
                isHighlighted = true,
                onSelect = {
                    PremiumManager.setTier(SubscriptionTier.PREMIUM)
                    onDismiss()
                }
            )
        }
    }
}

@Composable
fun TierCard(
    name: String,
    price: String,
    features: List<String>,
    buttonText: String,
    isHighlighted: Boolean = false,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        border = if (isHighlighted) CardDefaults.outlinedCardBorder() else null
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            if (isHighlighted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(" RECOMMENDED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Text(name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(price, style = MaterialTheme.typography.titleLarge)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            features.forEach { feature ->
                Row(modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text(feature, modifier = Modifier.padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onSelect,
                modifier = Modifier.fillMaxWidth(),
                colors = if (isHighlighted) ButtonDefaults.buttonColors() else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text(buttonText)
            }
        }
    }
}
