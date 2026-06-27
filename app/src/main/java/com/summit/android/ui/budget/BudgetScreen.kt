package com.summit.android.ui.budget

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.data.entity.CategoryEntity
import com.summit.android.data.entity.CategoryGroupEntity
import com.summit.android.ui.budget.viewmodel.BudgetViewModel
import com.summit.android.ui.transactions.formatCurrency
import java.text.DateFormatSymbols

import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.input.nestedscroll.nestedScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = viewModel()) {
    val groups by viewModel.groups.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val year by viewModel.selectedYear.collectAsState()
    val month by viewModel.selectedMonth.collectAsState()
    val availableToBudget by viewModel.availableToBudget.collectAsState()
    
    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Budget") },
                actions = {
                    IconButton(onClick = { viewModel.autoAssign() }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Actions")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            Column(modifier = Modifier.fillMaxSize()) {
                MonthNavigator(
                    year = year,
                    month = month,
                    onPrev = { viewModel.prevMonth() },
                    onNext = { viewModel.nextMonth() }
                )
                
                BudgetSummary(availableToBudget)

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    groups.forEach { group ->
                        item {
                            GroupHeader(group.name)
                        }
                        items(categories.filter { it.groupId == group.id }) { category ->
                            CategoryRow(category)
                        }
                    }
                }
            }
            
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun MonthNavigator(year: Int, month: Int, onPrev: () -> Unit, onNext: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Month")
        }
        Text(
            text = "${DateFormatSymbols().months[month - 1]} $year",
            style = MaterialTheme.typography.headlineSmall
        )
        IconButton(onClick = onNext) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next Month")
        }
    }
}

@Composable
fun BudgetSummary(amount: BigDecimal) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Available to Budget", style = MaterialTheme.typography.labelLarge)
            Text(formatCurrency(amount.toDouble()), style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
fun GroupHeader(name: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun CategoryRow(category: CategoryEntity) {
    ListItem(
        headlineContent = { Text(category.name) },
        supportingContent = { Text("Activity: ${formatCurrency(0.0)} · Available: ${formatCurrency(0.0)}") },
        trailingContent = {
            Text(formatCurrency(0.0), style = MaterialTheme.typography.bodyLarge)
        }
    )
}
