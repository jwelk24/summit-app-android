package com.summit.android.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.summit.android.service.SyncService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.summit.android.ui.budget.BudgetScreen
import com.summit.android.ui.horizon.CashFlowForecastScreen
import com.summit.android.ui.horizon.HorizonScreen
import com.summit.android.ui.insights.AIInsightsScreen
import com.summit.android.ui.navigation.Screen
import com.summit.android.ui.navigation.bottomNavItems
import com.summit.android.ui.networth.NetWorthScreen
import com.summit.android.ui.networth.PlaidConnectionsScreen
import com.summit.android.ui.reports.ReportsScreen
import com.summit.android.ui.settings.CustomizeAppearanceScreen
import com.summit.android.ui.transactions.TransactionsScreen
import com.summit.android.ui.transactions.ReceiptScannerScreen
import com.summit.android.ui.transactions.editor.TransactionEditorScreen
import java.util.UUID

import com.summit.android.ui.alerts.SmartAlertsScreen
import com.summit.android.ui.billing.PaywallScreen
import com.summit.android.ui.budget.PaycheckPlanScreen
import com.summit.android.ui.calendar.BillCalendarScreen
import com.summit.android.ui.challenges.ChallengesScreen
import com.summit.android.ui.review.WeeklyReviewScreen
import com.summit.android.ui.rules.CategoryRulesScreen
import com.summit.android.ui.rules.RuleEditorScreen
import com.summit.android.ui.subscriptions.SubscriptionsScreen
import com.summit.android.ui.transactions.RefundTrackerScreen
import com.summit.android.ui.whatif.WhatIfScreen
import com.summit.android.ui.wrapped.WrappedScreen
import com.summit.android.ui.coach.CoachScreen
import com.summit.android.ui.savetospend.SafeToSpendScreen
import com.summit.android.ui.health.FinancialHealthScreen
import com.summit.android.ui.budget.BudgetDraftScreen
import com.summit.android.ui.debt.DebtPayoffScreen
import com.summit.android.ui.settleup.SettleUpScreen
import com.summit.android.ui.tax.TaxPackScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val hideBottomBar = currentDestination?.route?.startsWith(Screen.TransactionEditor.route) == true || 
                               currentDestination?.route == Screen.Paywall.route ||
                               currentDestination?.route?.startsWith(Screen.CategoryRules.route) == true ||
                               currentDestination?.route?.startsWith(Screen.RuleEditor.route) == true
            if (!hideBottomBar) {
                Column {
                    SyncIndicator()
                    NavigationBar {
                        bottomNavItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = null) },
                                label = { Text(screen.title) },
                                selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Budget.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { it / 10 } },
            exitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { -it / 10 } },
            popEnterTransition = { fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { -it / 10 } },
            popExitTransition = { fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { it / 10 } }
        ) {
            composable(Screen.Budget.route) {
                BudgetScreen(
                    onManageRules = { navController.navigate(Screen.CategoryRules.route) },
                    onManageAlerts = { navController.navigate(Screen.SmartAlerts.route) },
                    onManageSubscriptions = { navController.navigate(Screen.Subscriptions.route) },
                    onCustomizeAppearance = { navController.navigate(Screen.CustomizeAppearance.route) },
                    onPaycheckPlan = { navController.navigate(Screen.PaycheckPlan.route) },
                    onBudgetDraft = { navController.navigate(Screen.BudgetDraft.route) },
                    onDebtPayoff = { navController.navigate(Screen.DebtPayoff.route) },
                    onSettleUp = { navController.navigate(Screen.SettleUp.route) },
                    onTaxPack = { navController.navigate(Screen.TaxPack.route) }
                )
            }
            composable(Screen.Transactions.route) {
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(Screen.TransactionEditor.route) },
                    onEditTransaction = { txId -> navController.navigate("${Screen.TransactionEditor.route}/$txId") },
                    onScanReceipt = { navController.navigate(Screen.ReceiptScanner.route) },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) },
                    onRefundTracker = { navController.navigate(Screen.RefundTracker.route) }
                )
            }
            composable(Screen.ReceiptScanner.route) {
                ReceiptScannerScreen(onDismiss = { navController.popBackStack() })
            }
            composable(Screen.NetWorth.route) { 
                NetWorthScreen(onManageConnections = { navController.navigate(Screen.PlaidConnections.route) }) 
            }
            composable(Screen.PlaidConnections.route) {
                PlaidConnectionsScreen(
                    onBack = { navController.popBackStack() },
                    onAddBank = { /* TODO */ },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.Horizon.route) {
                HorizonScreen(
                    onShowForecast = { navController.navigate(Screen.CashFlowForecast.route) },
                    onWhatIf = { navController.navigate(Screen.WhatIf.route) },
                    onBillCalendar = { navController.navigate(Screen.BillCalendar.route) }
                )
            }
            composable(Screen.CashFlowForecast.route) {
                CashFlowForecastScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.Insights.route) {
                AIInsightsScreen(
                    onUpgrade = { navController.navigate(Screen.Paywall.route) },
                    onWeeklyReview = { navController.navigate(Screen.WeeklyReview.route) },
                    onWrapped = { navController.navigate(Screen.Wrapped.route) },
                    onChallenges = { navController.navigate(Screen.Challenges.route) },
                    onCoach = { navController.navigate(Screen.Coach.route) },
                    onSafeToSpend = { navController.navigate(Screen.SafeToSpend.route) },
                    onFinancialHealth = { navController.navigate(Screen.FinancialHealth.route) }
                )
            }
            composable(Screen.CategoryRules.route) {
                CategoryRulesScreen(
                    onBack = { navController.popBackStack() },
                    onAddRule = { navController.navigate(Screen.RuleEditor.route) },
                    onEditRule = { ruleId -> navController.navigate("${Screen.RuleEditor.route}?ruleId=$ruleId") },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.SmartAlerts.route) {
                SmartAlertsScreen(
                    onBack = { navController.popBackStack() },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    onBack = { navController.popBackStack() },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(Screen.CustomizeAppearance.route) {
                CustomizeAppearanceScreen(onBack = { navController.popBackStack() })
            }
            composable(
                route = "${Screen.RuleEditor.route}?ruleId={ruleId}&seedMerchant={seedMerchant}&seedCategoryId={seedCategoryId}",
                arguments = listOf(
                    navArgument("ruleId") { type = NavType.StringType; nullable = true },
                    navArgument("seedMerchant") { type = NavType.StringType; nullable = true },
                    navArgument("seedCategoryId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val ruleId = backStackEntry.arguments?.getString("ruleId")?.let { UUID.fromString(it) }
                val seedMerchant = backStackEntry.arguments?.getString("seedMerchant")
                val seedCategoryId = backStackEntry.arguments?.getString("seedCategoryId")?.let { UUID.fromString(it) }
                RuleEditorScreen(
                    ruleId = ruleId,
                    seedMerchant = seedMerchant,
                    seedCategoryId = seedCategoryId,
                    onDismiss = { navController.popBackStack() }
                )
            }
            composable(Screen.Paywall.route) {
                PaywallScreen(onDismiss = { navController.popBackStack() })
            }
            composable(Screen.PaycheckPlan.route) {
                PaycheckPlanScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Challenges.route) {
                ChallengesScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.WeeklyReview.route) {
                WeeklyReviewScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Wrapped.route) {
                WrappedScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.BillCalendar.route) {
                BillCalendarScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.WhatIf.route) {
                WhatIfScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.RefundTracker.route) {
                RefundTrackerScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Coach.route) {
                CoachScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SafeToSpend.route) {
                SafeToSpendScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.FinancialHealth.route) {
                FinancialHealthScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.BudgetDraft.route) {
                BudgetDraftScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.DebtPayoff.route) {
                DebtPayoffScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.SettleUp.route) {
                SettleUpScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.TaxPack.route) {
                TaxPackScreen(
                    onBack = { navController.popBackStack() },
                    onUpgrade = { navController.navigate(Screen.Paywall.route) }
                )
            }
            composable(
                route = "${Screen.TransactionEditor.route}/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")?.let { UUID.fromString(it) }
                TransactionEditorScreen(
                    transactionId = transactionId,
                    onDismiss = { navController.popBackStack() },
                    onCreateRule = { merchant, categoryId ->
                        navController.navigate("${Screen.RuleEditor.route}?seedMerchant=$merchant&seedCategoryId=$categoryId")
                    }
                ) 
            }
            composable(Screen.TransactionEditor.route) { 
                TransactionEditorScreen(
                    onDismiss = { navController.popBackStack() },
                    onCreateRule = { merchant, categoryId ->
                        navController.navigate("${Screen.RuleEditor.route}?seedMerchant=$merchant&seedCategoryId=$categoryId")
                    }
                )
            }
        }
    }
}

@Composable
fun SyncIndicator() {
    val isSyncing by SyncService.isSyncing.collectAsStateWithLifecycle()
    AnimatedVisibility(
        visible = isSyncing,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
}
