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
import java.util.Calendar

class BudgetViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).build()
    
    private val engine = BudgetEngine(application)

    val groups: StateFlow<List<CategoryGroupEntity>> = db.categoryDao().getGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = db.categoryDao().getCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val selectedYear: StateFlow<Int> = _selectedYear

    private val _selectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val selectedMonth: StateFlow<Int> = _selectedMonth

    private val _availableToBudget = MutableStateFlow(BigDecimal.ZERO)
    val availableToBudget: StateFlow<BigDecimal> = _availableToBudget

    init {
        updateAvailableToBudget()
    }

    private fun updateAvailableToBudget() {
        viewModelScope.launch {
            val txs = db.transactionDao().getAll().first()
            val month = engine.ensureMonth(_selectedYear.value, _selectedMonth.value)
            _availableToBudget.value = engine.availableToBudget(txs, month, _selectedYear.value, _selectedMonth.value)
        }
    }

    fun nextMonth() {
        if (_selectedMonth.value == 12) {
            _selectedMonth.value = 1
            _selectedYear.value++
        } else {
            _selectedMonth.value++
        }
        updateAvailableToBudget()
    }

    fun prevMonth() {
        if (_selectedMonth.value == 1) {
            _selectedMonth.value = 12
            _selectedYear.value--
        } else {
            _selectedMonth.value--
        }
        updateAvailableToBudget()
    }

    fun autoAssign() {
        viewModelScope.launch {
            val txs = db.transactionDao().getAll().first()
            val cats = categories.value
            val month = engine.ensureMonth(_selectedYear.value, _selectedMonth.value)
            engine.autoAssignAvailable(txs, cats, month)
            updateAvailableToBudget()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            // Trigger Sync
            updateAvailableToBudget()
        }
    }
}
