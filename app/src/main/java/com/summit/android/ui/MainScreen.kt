package com.summit.android.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.summit.android.service.SyncService
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.*
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.summit.android.ui.budget.BudgetScreen
import com.summit.android.ui.horizon.HorizonScreen
import com.summit.android.ui.insights.AIInsightsScreen
import com.summit.android.ui.navigation.Screen
import com.summit.android.ui.navigation.bottomNavItems
import com.summit.android.ui.networth.NetWorthScreen
import com.summit.android.ui.networth.PlaidConnectionsScreen
import com.summit.android.ui.reports.ReportsScreen
import com.summit.android.ui.transactions.TransactionsScreen
import com.summit.android.ui.transactions.ReceiptScannerScreen
import com.summit.android.ui.transactions.editor.TransactionEditorScreen
import java.util.UUID

import com.summit.android.ui.billing.PaywallScreen

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            // Hide bottom bar on editor and paywall
            val hideBottomBar = currentDestination?.route?.startsWith(Screen.TransactionEditor.route) == true || 
                               currentDestination?.route == Screen.Paywall.route
            if (!hideBottomBar) {
                Column {
                    val isSyncing by SyncService.isSyncing.collectAsStateWithLifecycle()
                    AnimatedVisibility(
                        visible = isSyncing,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
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
            composable(Screen.Budget.route) { BudgetScreen() }
            composable(Screen.Transactions.route) { 
                TransactionsScreen(
                    onAddTransaction = { navController.navigate(Screen.TransactionEditor.route) },
                    onEditTransaction = { txId -> navController.navigate("${Screen.TransactionEditor.route}/$txId") },
                    onScanReceipt = { navController.navigate(Screen.ReceiptScanner.route) }
                ) 
            }
            composable(Screen.ReceiptScanner.route) {
                ReceiptScannerScreen(onDismiss = { navController.popBackStack() })
            }
            composable(Screen.NetWorth.route) { 
                NetWorthScreen(onManageConnections = { navController.navigate(Screen.PlaidConnections.route) }) 
            }
            composable(Screen.PlaidConnections.route) {
                PlaidConnectionsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Horizon.route) { HorizonScreen() }
            composable(Screen.Reports.route) { ReportsScreen() }
            composable(Screen.Insights.route) { AIInsightsScreen() }
            composable(
                route = "${Screen.TransactionEditor.route}/{transactionId}",
                arguments = listOf(navArgument("transactionId") { type = NavType.StringType; nullable = true })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getString("transactionId")?.let { UUID.fromString(it) }
                TransactionEditorScreen(
                    transactionId = transactionId,
                    onDismiss = { navController.popBackStack() }
                ) 
            }
            composable(Screen.TransactionEditor.route) { 
                TransactionEditorScreen(onDismiss = { navController.popBackStack() }) 
            }
        }
    }
}
