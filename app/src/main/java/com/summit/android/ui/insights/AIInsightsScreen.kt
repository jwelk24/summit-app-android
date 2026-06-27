package com.summit.android.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Sparkles
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

import com.summit.android.billing.PremiumManager
import com.summit.android.ui.transactions.EmptyStateView
import androidx.compose.material.icons.filled.Lock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIInsightsScreen(
    onUpgrade: () -> Unit,
    viewModel: AIInsightsViewModel = viewModel()
) {
    val digest by viewModel.digest.collectAsState()
    val isGeneratingDigest by viewModel.isGeneratingDigest.collectAsState()
    val isCategorizing by viewModel.isCategorizing.collectAsState()
    
    val currentTier by PremiumManager.currentTier.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Insights") }) }
    ) { padding ->
        if (currentTier != com.summit.android.billing.SubscriptionTier.PREMIUM) {
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                EmptyStateView(
                    icon = Icons.Default.Lock,
                    message = "AI Insights require a Premium subscription.",
                    actionLabel = "View Plans",
                    onAction = onUpgrade
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WeeklyDigestCard(
                        digest = digest,
                        isLoading = isGeneratingDigest,
                        onGenerate = { viewModel.generateDigest() }
                    )
                }

                item {
                    SmartCategorizeCard(
                        isLoading = isCategorizing,
                        onRun = { viewModel.runSmartCategorize() }
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklyDigestCard(
    digest: com.summit.android.service.WeeklyDigest?,
    isLoading: Boolean,
    onGenerate: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weekly Digest", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Summarizing your week...", style = MaterialTheme.typography.bodyMedium)
                }
            } else if (digest != null) {
                Text(digest.headline, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                digest.bullets.forEach { bullet ->
                    Text("• $bullet", style = MaterialTheme.typography.bodyMedium)
                }
                if (digest.suggestion.isNotBlank()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Row {
                        Icon(Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(digest.suggestion, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                Text(
                    "Get a plain-English summary of your spending over the last 7 days.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onGenerate,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Sparkles, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (digest == null) "Generate Weekly Digest" else "Regenerate")
            }
        }
    }
}

@Composable
fun SmartCategorizeCard(
    isLoading: Boolean,
    onRun: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Smart Categorize", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Let AI assign a category to every transaction that's currently uncategorized.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRun,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Categorize Uncategorized")
                }
            }
        }
    }
}
