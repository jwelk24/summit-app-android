package com.summit.android.ui.horizon

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.data.entity.ScheduledItemEntity
import com.summit.android.ui.horizon.viewmodel.HorizonUiState
import com.summit.android.ui.horizon.viewmodel.HorizonViewModel
import com.summit.android.ui.horizon.viewmodel.ProjectionPoint
import com.summit.android.ui.transactions.formatCurrency
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorizonScreen(viewModel: HorizonViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Horizon") },
                actions = {
                    IconButton(onClick = { /* TODO: Forecast */ }) {
                        Icon(Icons.Default.ShowChart, contentDescription = "Forecast")
                    }
                    IconButton(onClick = { /* TODO: Add Scheduled */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Scheduled")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                HorizonSummaryCard(uiState)
            }

            if (uiState.pendingItems.isNotEmpty()) {
                item {
                    SectionHeader("Pending (Past Due)")
                }
                items(uiState.pendingItems) { item ->
                    PendingItemRow(item)
                }
            }

            item {
                SectionHeader("Next 90 Days")
            }

            if (uiState.projectionPoints.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No scheduled items in the next 90 days.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
                    }
                }
            } else {
                items(uiState.projectionPoints) { point ->
                    ProjectionRow(point)
                }
            }
        }
    }
}

@Composable
fun HorizonSummaryCard(uiState: HorizonUiState) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow("Starting Balance", uiState.startingBalance)
            SummaryRow("Lowest Projected", uiState.lowestProjected, isBold = true)
            SummaryRow("90-Day Projected", uiState.projected90Day, isBold = true)
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: BigDecimal, isBold: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
        Text(
            text = formatCurrency(amount.toDouble()),
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (amount < BigDecimal.ZERO) Color.Red else Color.Unspecified
        )
    }
}

@Composable
fun PendingItemRow(item: ScheduledItemEntity) {
    ListItem(
        headlineContent = { Text(item.name) },
        supportingContent = {
            val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Text(df.format(item.nextDate), color = Color.Red)
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(item.amount.toDouble()), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { /* TODO: Post Item */ }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(32.dp)) {
                    Text("Post", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    )
}

@Composable
fun ProjectionRow(point: ProjectionPoint) {
    ListItem(
        headlineContent = { Text(point.label) },
        supportingContent = {
            val df = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            Text(df.format(point.date))
        },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                val color = if (point.delta < BigDecimal.ZERO) MaterialTheme.colorScheme.onSurface else Color.Green
                Text(formatCurrency(point.delta.toDouble()), color = color, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "Bal: ${formatCurrency(point.runningBalance.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (point.runningBalance < BigDecimal.ZERO) Color.Red else MaterialTheme.colorScheme.secondary
                )
            }
        }
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
    )
}
