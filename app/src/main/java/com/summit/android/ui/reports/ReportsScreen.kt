package com.summit.android.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import com.summit.android.ui.reports.viewmodel.CategorySpending
import com.summit.android.ui.reports.viewmodel.MonthlyFlow
import com.summit.android.ui.reports.viewmodel.ReportsViewModel
import com.summit.android.ui.transactions.formatCurrency
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reports") }) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                SectionHeader("Spending This Month")
            }

            if (uiState.currentMonthSpending.isEmpty()) {
                item {
                    Text(
                        "No spending recorded this month.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            } else {
                item {
                    CategorySpendingChart(uiState.currentMonthSpending)
                }
            }

            item {
                SectionHeader("Income vs Spending (6 months)")
            }

            item {
                IncomeVsSpendingChart(uiState.sixMonthFlow)
            }
        }
    }
}

@Composable
fun CategorySpendingChart(spending: List<CategorySpending>) {
    val chartEntryModel = entryModelOf(spending.map { it.amount.toFloat() })

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> formatCurrency(value.toDouble()) }
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ -> spending.getOrNull(value.toInt())?.categoryName ?: "" }
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun IncomeVsSpendingChart(flow: List<MonthlyFlow>) {
    val chartEntryModel = entryModelOf(
        flow.map { it.income.toFloat() },
        flow.map { it.spending.toFloat() }
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            Chart(
                chart = columnChart(),
                model = chartEntryModel,
                startAxis = rememberStartAxis(
                    valueFormatter = { value, _ -> formatCurrency(value.toDouble()) }
                ),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = { value, _ -> flow.getOrNull(value.toInt())?.monthLabel ?: "" }
                ),
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}
