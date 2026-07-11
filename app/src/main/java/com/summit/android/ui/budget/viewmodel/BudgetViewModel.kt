package com.summit.android.ui.budget.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.CategoryEntity
import com.summit.android.data.entity.CategoryGroupEntity
import com.summit.android.service.BudgetEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*

data class BudgetUiState(
    val groups: List<CategoryGroupEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val availableToBudget: BigDecimal = BigDecimal.ZERO,
    val selectedDate: Date = Date(),
    val allocations: Map<UUID, BigDecimal> = emptyMap(),
    val activity: Map<UUID, BigDecimal> = emptyMap(),
    val ageOfMoney: Int? = null
)

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()
    
    private val engine = BudgetEngine(application)

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)

    val uiState: StateFlow<BudgetUiState> = combine(
        db.categoryDao().getGroups(),
        db.categoryDao().getCategories(),
        db.transactionDao().getAll(),
        _selectedYear,
        _selectedMonth
    ) { groups, categories, transactions, year, month ->
        val budgetMonth = engine.ensureMonth(year, month)
        val allocs = db.budgetDao().getAllocationsForMonth(budgetMonth.id).first()
        val allocationMap = mutableMapOf<UUID, BigDecimal>()
        allocs.forEach { alloc ->
            alloc.categoryId?.let { allocationMap[it] = alloc.amount }
        }
        
        val activityMap = categories.associate { cat ->
            cat.id to engine.activity(cat, year, month)
        }

        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        BudgetUiState(
            groups = groups,
            categories = categories,
            availableToBudget = engine.availableToBudget(transactions, budgetMonth, year, month),
            selectedDate = cal.time,
            allocations = allocationMap,
            activity = activityMap,
            ageOfMoney = BudgetEngine.ageOfMoneyDays(transactions)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BudgetUiState())

    fun changeMonth(delta: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value - 1)
            add(Calendar.MONTH, delta)
        }
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH) + 1
    }

    fun setAssigned(categoryId: UUID, amount: BigDecimal) {
        viewModelScope.launch {
            val month = engine.ensureMonth(_selectedYear.value, _selectedMonth.value)
            val category = db.categoryDao().getCategories().first().find { it.id == categoryId }
            if (category != null) {
                engine.setAssigned(amount, category, month)
            }
        }
    }

    fun autoAssign() {
        viewModelScope.launch {
            val txs = db.transactionDao().getAll().first()
            val cats = db.categoryDao().getCategories().first()
            val month = engine.ensureMonth(_selectedYear.value, _selectedMonth.value)
            engine.autoAssignAvailable(txs, cats, month)
        }
    }
}
