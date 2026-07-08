package com.summit.android.ui.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.data.entity.TransactionEntity
import com.summit.android.ui.transactions.viewmodel.TransactionsViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

import com.summit.android.billing.PremiumManager

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    onAddTransaction: () -> Unit,
    onEditTransaction: (UUID) -> Unit,
    onScanReceipt: () -> Unit,
    onUpgrade: () -> Unit,
    onRefundTracker: () -> Unit,
    viewModel: TransactionsViewModel = viewModel()
) {
    val transactions by viewModel.transactions.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    
    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.refresh()
            pullToRefreshState.endRefresh()
        }
    }

    var showAddMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transactions") },
                actions = {
                    IconButton(onClick = { showAddMenu = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Actions")
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("New Transaction") },
                            onClick = { onAddTransaction(); showAddMenu = false },
                            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (PremiumManager.canScanReceipts()) "Scan Receipt…" else "Scan Receipt (Premium)…") },
                            onClick = {
                                showAddMenu = false
                                if (PremiumManager.canScanReceipts()) onScanReceipt() else onUpgrade()
                            },
                            leadingIcon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Refund Tracker") },
                            onClick = { onRefundTracker(); showAddMenu = false },
                            leadingIcon = { Icon(Icons.Default.AssignmentReturn, contentDescription = null) }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize().nestedScroll(pullToRefreshState.nestedScrollConnection)) {
            if (transactions.isEmpty() && !isRefreshing) {
                EmptyStateView(
                    icon = Icons.Default.CreditCard,
                    message = "No transactions yet.",
                    actionLabel = "Add Manually",
                    onAction = onAddTransaction
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(transactions, key = { it.id }) { transaction ->
                        SwipeToDeleteRow(
                            onDelete = { viewModel.deleteTransaction(transaction) },
                            modifier = Modifier.animateItemPlacement()
                        ) {
                            TransactionRow(
                                transaction = transaction,
                                modifier = Modifier.clickable { onEditTransaction(transaction.id) }
                            )
                        }
                        HorizontalDivider()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteRow(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        backgroundContent = {
            val color = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.EndToStart -> Color.Red
                else -> Color.Transparent
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                }
            }
        },
        enableDismissFromStartToEnd = false,
        content = {
            content()
        }
    )
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.secondary)
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun TransactionRow(transaction: TransactionEntity, modifier: Modifier = Modifier) {
    ListItem(
        headlineContent = { Text(transaction.merchant) },
        supportingContent = {
            Text("${SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(transaction.date)} · ${transaction.memo ?: "No memo"}")
        },
        trailingContent = {
            val amountColor = if (transaction.amount.toDouble() < 0) MaterialTheme.colorScheme.onSurface else Color(0xFF10B981)
            Text(
                text = formatCurrency(transaction.amount.toDouble()),
                color = amountColor,
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
            )
        },
        modifier = modifier
    )
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale.US)
    return format.format(amount)
}
