package com.summit.android.ui.networth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.data.entity.AccountEntity
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Link
import com.summit.android.ui.transactions.EmptyStateView
import androidx.activity.compose.rememberLauncherForActivityResult
import com.plaid.link.OpenPlaidLink
import com.plaid.link.linkTokenConfiguration
import com.plaid.link.result.LinkSuccess
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.summit.android.ui.networth.viewmodel.ChartPoint
import com.summit.android.ui.networth.viewmodel.NetWorthTimeRange
import com.summit.android.ui.networth.viewmodel.NetWorthViewModel
import com.summit.android.ui.transactions.formatCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthScreen(
    onManageConnections: () -> Unit,
    viewModel: NetWorthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val linkToken by viewModel.linkToken.collectAsState()

    val linkLauncher = rememberLauncherForActivityResult(OpenPlaidLink()) { result ->
        if (result is LinkSuccess) {
            viewModel.exchangePublicToken(result.publicToken)
        }
    }

    LaunchedEffect(linkToken) {
        linkToken?.let {
            val configuration = linkTokenConfiguration {
                token = it
            }
            linkLauncher.launch(configuration)
            viewModel.onLinkTokenUsed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Net Worth") },
                actions = {
                    IconButton(onClick = onManageConnections) {
                        Icon(Icons.Default.Link, contentDescription = "Manage Connections")
                    }
                    IconButton(onClick = { viewModel.createLinkToken() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Account")
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
                NetWorthSummaryCard(
                    netWorth = uiState.netWorth,
                    totalAssets = uiState.totalAssets,
                    totalLiabilities = uiState.totalLiabilities
                )
            }

            item {
                TimeRangeSelector(
                    selectedRange = uiState.timeRange,
                    onRangeSelected = { viewModel.setTimeRange(it) }
                )
            }

            item {
                NetWorthChart(uiState.chartData)
            }

            if (uiState.accounts.isEmpty()) {
                item {
                    EmptyStateView(
                        icon = Icons.Default.AccountBalance,
                        message = "No accounts linked yet.",
                        actionLabel = "Link a Bank",
                        onAction = { viewModel.createLinkToken() }
                    )
                }
            } else {
                val assets = uiState.accounts.filter { it.type.isAsset }
                if (assets.isNotEmpty()) {
                    item {
                        SectionHeader("Assets")
                    }
                    items(assets) { account ->
                        AccountRow(account)
                    }
                }

                val liabilities = uiState.accounts.filter { !it.type.isAsset }
                if (liabilities.isNotEmpty()) {
                    item {
                        SectionHeader("Liabilities")
                    }
                    items(liabilities) { account ->
                        AccountRow(account)
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthSummaryCard(netWorth: BigDecimal, totalAssets: BigDecimal, totalLiabilities: BigDecimal) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Net Worth", style = MaterialTheme.typography.titleMedium)
                val color = if (netWorth >= BigDecimal.ZERO) Color.Green else Color.Red
                Text(
                    text = formatCurrency(netWorth.toDouble()),
                    style = MaterialTheme.typography.headlineMedium,
                    color = color
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Total Assets", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatCurrency(totalAssets.toDouble()), style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Total Liabilities", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("-${formatCurrency(totalLiabilities.toDouble())}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun TimeRangeSelector(selectedRange: NetWorthTimeRange, onRangeSelected: (NetWorthTimeRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        NetWorthTimeRange.values().forEachIndexed { index, range ->
            SegmentedButton(
                selected = selectedRange == range,
                onClick = { onRangeSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = NetWorthTimeRange.values().size)
            ) {
                Text(range.label)
            }
        }
    }
}

@Composable
fun NetWorthChart(chartData: List<ChartPoint>) {
    if (chartData.isEmpty()) {
        NetWorthChartPlaceholder()
        return
    }

    val chartEntryModel = entryModelOf(chartData.map { it.value })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Chart(
                chart = lineChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> formatCurrency(value.toDouble()) }
                ),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun NetWorthChartPlaceholder() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text("Trend Chart Coming Soon", style = MaterialTheme.typography.bodySmall)
        }
    }
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

@Composable
fun AccountRow(account: AccountEntity) {
    ListItem(
        headlineContent = { Text(account.name) },
        supportingContent = { Text(account.type.displayName) },
        trailingContent = {
            Text(
                text = formatCurrency(account.balance.toDouble()),
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )
        }
    )
}
