package com.summit.android.ui.reports.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.service.PDFExporter
import com.summit.android.service.ReportRange
import com.summit.android.service.ReportsService
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*
import java.io.File

data class CategorySpending(
    val categoryName: String,
    val amount: BigDecimal
)

data class MonthlyFlow(
    val monthLabel: String,
    val income: BigDecimal,
    val spending: BigDecimal
)

data class ReportsUiState(
    val currentMonthSpending: List<CategorySpending> = emptyList(),
    val sixMonthFlow: List<MonthlyFlow> = emptyList()
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    val uiState: StateFlow<ReportsUiState> = combine(
        db.transactionDao().getAll(),
        db.categoryDao().getCategories()
    ) { transactions, categories ->
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1

        // Current Month Spending
        val categoryMap = categories.associateBy { it.id }
        val currentMonthTxs = transactions.filter {
            calendar.time = it.date
            calendar.get(Calendar.YEAR) == currentYear && (calendar.get(Calendar.MONTH) + 1) == currentMonth && it.amount < BigDecimal.ZERO
        }
        
        val spendingByCat = currentMonthTxs.groupBy { it.categoryId }
            .map { (catId, txs) ->
                val name = categoryMap[catId]?.name ?: "Uncategorized"
                CategorySpending(name, txs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount.abs()) })
            }.sortedByDescending { it.amount }

        // 6-Month Flow
        val sixMonthFlow = mutableListOf<MonthlyFlow>()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1
            
            val monthTxs = transactions.filter {
                calendar.time = it.date
                calendar.get(Calendar.YEAR) == year && (calendar.get(Calendar.MONTH) + 1) == month
            }
            
            val income = monthTxs.filter { it.amount > BigDecimal.ZERO }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
            val spending = monthTxs.filter { it.amount < BigDecimal.ZERO }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount.abs()) }
            
            val label = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) ?: ""
            sixMonthFlow.add(MonthlyFlow(label, income, spending))
        }

        ReportsUiState(
            currentMonthSpending = spendingByCat,
            sixMonthFlow = sixMonthFlow
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportsUiState())

    fun exportCSV(range: ReportRange, customStart: Date?, customEnd: Date?, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val period = ReportsService.resolvePeriod(range, customStart, customEnd)
            val txs = db.transactionDao().getAll().first()
            val file = ReportsService.exportToCSV(getApplication(), txs, period)
            onResult(file)
        }
    }

    fun exportPDF(range: ReportRange, customStart: Date?, customEnd: Date?, onResult: (File?) -> Unit) {
        viewModelScope.launch {
            val period = ReportsService.resolvePeriod(range, customStart, customEnd)
            val txs = db.transactionDao().getAll().first()
            val categories = db.categoryDao().getCategories().first().associate { it.id to it.name }
            val summary = ReportsService.buildSummary(txs, period, categories)
            val file = PDFExporter.exportReportToPDF(getApplication(), summary)
            onResult(file)
        }
    }
}
