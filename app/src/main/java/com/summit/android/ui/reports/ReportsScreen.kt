package com.summit.android.ui.reports

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.summit.android.service.ReportCompareMode
import com.summit.android.service.ReportRange
import com.summit.android.ui.reports.viewmodel.CategorySpending
import com.summit.android.ui.reports.viewmodel.MonthlyFlow
import com.summit.android.ui.reports.viewmodel.ReportsViewModel
import com.summit.android.ui.transactions.formatCurrency
import java.io.File
import java.math.BigDecimal
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(viewModel: ReportsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showExportSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports") },
                actions = {
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
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
            // Compare mode picker (mirrors iOS "Compare to" picker in the range section)
            item {
                CompareModePicker(
                    selected = uiState.compareMode,
                    onSelect = { viewModel.setCompareMode(it) }
                )
            }

            // Comparison section — shown when a mode is active and we have data
            if (uiState.compareMode != ReportCompareMode.OFF &&
                uiState.currentSummary != null && uiState.compareSummary != null
            ) {
                item {
                    SectionHeader("vs ${uiState.compareSummary.period.label}")
                }
                item {
                    ReportComparisonSection(
                        current = uiState.currentSummary,
                        previous = uiState.compareSummary
                    )
                }
            }

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
                items(uiState.currentMonthSpending) { spending ->
                    SpendingBar(spending, uiState.currentMonthSpending.first().amount)
                }
            }

            item {
                SectionHeader("Income vs Spending (6 months)")
            }

            items(uiState.sixMonthFlow) { flow ->
                FlowRow(flow)
            }
        }
    }

    if (showExportSheet) {
        ModalBottomSheet(onDismissRequest = { showExportSheet = false }) {
            ReportsExportContent(
                onDismiss = { showExportSheet = false },
                onExport = { file ->
                    if (file != null) {
                        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = if (file.extension == "pdf") "application/pdf" else "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Report"))
                    }
                    showExportSheet = false
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun ReportsExportContent(
    onDismiss: () -> Unit,
    onExport: (File?) -> Unit,
    viewModel: ReportsViewModel
) {
    var range by remember { mutableStateOf(ReportRange.THIS_MONTH) }
    var customStart by remember { mutableStateOf(Date()) }
    var customEnd by remember { mutableStateOf(Date()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Export Data", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        // Range Picker
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Range: ${range.displayName}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReportRange.values().forEach { r ->
                    DropdownMenuItem(
                        text = { Text(r.displayName) },
                        onClick = {
                            range = r
                            expanded = false
                        }
                    )
                }
            }
        }

        if (range == ReportRange.CUSTOM) {
            // Simplified date pickers for brevity, ideally would use full DatePickerDialog
            Text("Custom dates enabled (Implementation pending full picker)", style = MaterialTheme.typography.bodySmall)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { viewModel.exportCSV(range, customStart, customEnd, onExport) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export as CSV")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { viewModel.exportPDF(range, customStart, customEnd, onExport) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Export as PDF")
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
            Text("Cancel")
        }
    }
}

@Composable
fun SpendingBar(spending: CategorySpending, maxAmount: BigDecimal) {
    val fraction = if (maxAmount > BigDecimal.ZERO) {
        spending.amount.toDouble() / maxAmount.toDouble()
    } else 0.0

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(spending.categoryName, style = MaterialTheme.typography.bodyMedium)
            Text(formatCurrency(spending.amount.toDouble()), style = MaterialTheme.typography.bodySmall)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.toFloat())
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
fun FlowRow(flow: MonthlyFlow) {
    ListItem(
        headlineContent = { Text(flow.monthLabel) },
        trailingContent = {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Income: ${formatCurrency(flow.income.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF10B981)
                )
                Text(
                    "Spending: ${formatCurrency(flow.spending.toDouble())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFEF4444)
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
        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun CompareModePicker(
    selected: ReportCompareMode,
    onSelect: (ReportCompareMode) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Compare to", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selected.displayName)
                Spacer(Modifier.width(4.dp))
                Icon(androidx.compose.material.icons.Icons.Default.ArrowDropDown,
                    contentDescription = null, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ReportCompareMode.values().forEach { mode ->
                    DropdownMenuItem(
                        text = { Text(mode.displayName) },
                        onClick = { onSelect(mode); expanded = false },
                        trailingIcon = {
                            if (mode == selected)
                                Icon(androidx.compose.material.icons.Icons.Default.Check,
                                    contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }
        }
    }
}
