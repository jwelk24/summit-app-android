package com.summit.android.service

import android.content.Context
import androidx.room.Room
import com.summit.android.billing.PremiumManager
import com.summit.android.billing.SubscriptionTier
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.CategoryRuleEntity
import com.summit.android.data.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import java.util.*

object RuleEngine {

    private fun rulesEnabled(): Boolean {
        return PremiumManager.currentTier.value == SubscriptionTier.PREMIUM
    }

    fun matches(rule: CategoryRuleEntity, transaction: TransactionEntity): Boolean {
        if (!rule.enabled) return false
        if (rule.pattern.isEmpty()) return false

        val fieldValue = when (rule.matchField) {
            "merchant" -> transaction.merchant
            "memo" -> transaction.memo ?: ""
            else -> transaction.merchant
        }

        val pattern = if (rule.caseSensitive) rule.pattern else rule.pattern.lowercase()
        val target = if (rule.caseSensitive) fieldValue else fieldValue.lowercase()

        return when (rule.matchKind) {
            "contains" -> target.contains(pattern)
            "equals" -> target == pattern
            "startsWith" -> target.startsWith(pattern)
            "endsWith" -> target.endsWith(pattern)
            else -> target.contains(pattern)
        }
    }

    suspend fun applyRules(rules: List<CategoryRuleEntity>, tx: TransactionEntity, db: AppDatabase): UUID? {
        if (!rulesEnabled()) return null
        
        val sortedRules = rules.filter { it.enabled }.sortedBy { it.priority }
        for (rule in sortedRules) {
            if (matches(rule, tx)) {
                // Update hit stats
                db.categoryRuleDao().update(
                    rule.copy(
                        timesApplied = rule.timesApplied + 1,
                        lastAppliedAt = Date()
                    )
                )
                return rule.categoryId
            }
        }
        return null
    }

    suspend fun categorizeIfPossible(tx: TransactionEntity, db: AppDatabase) {
        if (!rulesEnabled()) return
        if (tx.categoryId != null) return

        val rules = db.categoryRuleDao().getEnabledRules()
        val matchedCategoryId = applyRules(rules, tx, db)
        if (matchedCategoryId != null) {
            db.transactionDao().update(tx.copy(categoryId = matchedCategoryId))
        }
    }

    suspend fun backfill(context: Context): Int {
        if (!rulesEnabled()) return 0
        
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val rules = db.categoryRuleDao().getEnabledRules()
        val transactions = db.transactionDao().getAll().first()
        
        var hits = 0
        for (tx in transactions) {
            if (tx.categoryId == null) {
                val matchedCategoryId = applyRules(rules, tx, db)
                if (matchedCategoryId != null) {
                    db.transactionDao().update(tx.copy(categoryId = matchedCategoryId))
                    hits++
                }
            }
        }
        return hits
    }
}
