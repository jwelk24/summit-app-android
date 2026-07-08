package com.summit.android.ui.transactions.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.TransactionEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).addMigrations(AppDatabase.MIGRATION_1_2).build()

    val transactions: StateFlow<List<TransactionEntity>> = db.transactionDao().getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Trigger Supabase/Plaid sync
            _isRefreshing.value = false
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            db.transactionDao().delete(transaction)
        }
    }
}
