package com.summit.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Budget : Screen("budget", "Budget", Icons.Default.TableChart)
    object Transactions : Screen("transactions", "Transactions", Icons.Default.CreditCard)
    object NetWorth : Screen("networth", "Net Worth", Icons.Default.ShowChart)
    object Horizon : Screen("horizon", "Horizon", Icons.Default.Terrain)
    object Reports : Screen("reports", "Reports", Icons.Default.PieChart)
    object Insights : Screen("insights", "Insights", Icons.Default.AutoAwesome)
    object Auth : Screen("auth", "Auth", Icons.Default.Person)
    object TransactionEditor : Screen("transaction_editor", "New Transaction", Icons.Default.Add)
    object PlaidConnections : Screen("plaid_connections", "Plaid Connections", Icons.Default.Link)
    object ReceiptScanner : Screen("receipt_scanner", "Scan Receipt", Icons.Default.DocumentScanner)
    object Paywall : Screen("paywall", "Upgrade", Icons.Default.Star)
}

val bottomNavItems = listOf(
    Screen.Budget,
    Screen.Transactions,
    Screen.NetWorth,
    Screen.Horizon,
    Screen.Reports,
    Screen.Insights
)
