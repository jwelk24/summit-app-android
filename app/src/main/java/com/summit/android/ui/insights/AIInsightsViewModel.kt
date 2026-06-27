package com.summit.android.ui.insights

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.service.AIInsightsService
import com.summit.android.service.WeeklyDigest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AIInsightsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).build()
    
    private val aiService = AIInsightsService(application)

    private val _digest = MutableStateFlow<WeeklyDigest?>(null)
    val digest: StateFlow<WeeklyDigest?> = _digest

    private val _isGeneratingDigest = MutableStateFlow(false)
    val isGeneratingDigest: StateFlow<Boolean> = _isGeneratingDigest

    private val _isCategorizing = MutableStateFlow(false)
    val isCategorizing: StateFlow<Boolean> = _isCategorizing

    fun generateDigest() {
        viewModelScope.launch {
            _isGeneratingDigest.value = true
            try {
                val transactions = db.transactionDao().getAll().first()
                val result = aiService.generateWeeklySummary(transactions)
                _digest.value = result
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isGeneratingDigest.value = false
            }
        }
    }

    fun runSmartCategorize() {
        viewModelScope.launch {
            _isCategorizing.value = true
            try {
                val transactions = db.transactionDao().getAll().first().filter { it.categoryId == null }
                val categories = db.categoryDao().getCategories().first()
                
                transactions.forEach { tx ->
                    val suggestion = aiService.suggestCategory(tx, categories)
                    if (suggestion != null && suggestion.confidence > 0.5) {
                        val category = categories.find { it.id.toString() == suggestion.categoryId }
                        if (category != null) {
                            db.transactionDao().update(tx.copy(categoryId = category.id))
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            } finally {
                _isCategorizing.value = false
            }
        }
    }
}
