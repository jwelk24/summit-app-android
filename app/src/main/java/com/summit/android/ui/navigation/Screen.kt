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
    object CategoryRules : Screen("category_rules", "Rules", Icons.Default.AutoFixHigh)
    object SmartAlerts : Screen("smart_alerts", "Smart Alerts", Icons.Default.NotificationsActive)
    object Subscriptions : Screen("subscriptions", "Subscriptions", Icons.Default.Repeat)
    object RuleEditor : Screen("rule_editor", "Edit Rule", Icons.Default.Edit)
    object CustomizeAppearance : Screen("customize_appearance", "Appearance", Icons.Default.Palette)
    object PaycheckPlan : Screen("paycheck_plan", "Paycheck Plan", Icons.Default.Payments)
    object Challenges : Screen("challenges", "Challenges", Icons.Default.EmojiEvents)
    object WeeklyReview : Screen("weekly_review", "Weekly Review", Icons.Default.CheckCircle)
    object Wrapped : Screen("wrapped", "Wrapped", Icons.Default.AutoAwesome)
    object BillCalendar : Screen("bill_calendar", "Bill Calendar", Icons.Default.CalendarMonth)
    object WhatIf : Screen("what_if", "What If?", Icons.Default.TrendingUp)
    object RefundTracker : Screen("refund_tracker", "Refund Tracker", Icons.Default.AssignmentReturn)
}

val bottomNavItems = listOf(
    Screen.Budget,
    Screen.Transactions,
    Screen.NetWorth,
    Screen.Horizon,
    Screen.Reports,
    Screen.Insights
)
