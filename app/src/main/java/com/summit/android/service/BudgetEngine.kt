package com.summit.android.service

import android.content.Context
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.*
import com.summit.android.data.model.GoalType
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.util.*

class BudgetEngine(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "summit-db"
    ).build()

    suspend fun availableToBudget(transactions: List<TransactionEntity>, budgetMonth: BudgetMonthEntity?, year: Int, month: Int): BigDecimal {
        val calendar = Calendar.getInstance()
        val inflow = transactions.filter {
            calendar.time = it.date
            it.amount > BigDecimal.ZERO &&
            calendar.get(Calendar.YEAR) == year &&
            (calendar.get(Calendar.MONTH) + 1) == month
        }.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }

        val allocations = budgetMonth?.let { db.budgetDao().getAllocations(it.id).first() } ?: emptyList()
        val assigned = allocations.fold(BigDecimal.ZERO) { acc, alloc -> acc.add(alloc.amount) }
        val carry = budgetMonth?.carryover ?: BigDecimal.ZERO
        
        return inflow.add(carry).subtract(assigned)
    }

    suspend fun assigned(category: CategoryEntity, budgetMonth: BudgetMonthEntity?): BigDecimal {
        if (budgetMonth == null) return BigDecimal.ZERO
        return db.budgetDao().getAllocation(budgetMonth.id, category.id)?.amount ?: BigDecimal.ZERO
    }

    suspend fun activity(category: CategoryEntity, year: Int, month: Int): BigDecimal {
        val calendar = Calendar.getInstance()
        val transactions = db.transactionDao().getAll().first().filter { tx ->
            calendar.time = tx.date
            tx.categoryId == category.id &&
            calendar.get(Calendar.YEAR) == year &&
            (calendar.get(Calendar.MONTH) + 1) == month
        }
        return transactions.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
    }

    suspend fun available(category: CategoryEntity, budgetMonth: BudgetMonthEntity?, year: Int, month: Int): BigDecimal {
        return assigned(category, budgetMonth).add(activity(category, year, month))
    }

    suspend fun ensureMonth(year: Int, month: Int): BudgetMonthEntity {
        val existing = db.budgetDao().getMonth(year, month)
        if (existing != null) return existing
        
        val newMonth = BudgetMonthEntity(year = year, month = month, carryover = BigDecimal.ZERO)
        db.budgetDao().insertMonth(newMonth)
        return newMonth
    }

    suspend fun setAssigned(amount: BigDecimal, category: CategoryEntity, budgetMonth: BudgetMonthEntity) {
        val existing = db.budgetDao().getAllocation(budgetMonth.id, category.id)
        if (existing != null) {
            db.budgetDao().updateAllocation(existing.copy(amount = amount))
        } else {
            db.budgetDao().insertAllocation(
                BudgetAllocationEntity(
                    amount = amount,
                    categoryId = category.id,
                    monthId = budgetMonth.id
                )
            )
        }
    }

    suspend fun autoAssignAvailable(
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        budgetMonth: BudgetMonthEntity
    ) {
        var remaining = availableToBudget(transactions, budgetMonth, budgetMonth.year, budgetMonth.month)
        if (remaining <= BigDecimal.ZERO) return

        for (cat in categories) {
            if (remaining <= BigDecimal.ZERO) break
            
            val goal = db.goalDao().getGoalForCategory(cat.id) ?: continue
            val alreadyAssigned = assigned(cat, budgetMonth)
            val currentlyAvailable = available(cat, budgetMonth, budgetMonth.year, budgetMonth.month)
            
            val needed = when (goal.type) {
                GoalType.MONTHLY_AMOUNT -> BigDecimal.ZERO.max(goal.targetAmount.subtract(alreadyAssigned))
                GoalType.SAVINGS_TARGET, GoalType.BY_DATE_TARGET -> 
                    BigDecimal.ZERO.max(goal.targetAmount.subtract(BigDecimal.ZERO.max(currentlyAvailable)))
            }
            
            val toAssign = remaining.min(needed)
            if (toAssign > BigDecimal.ZERO) {
                setAssigned(alreadyAssigned.add(toAssign), cat, budgetMonth)
                remaining = remaining.subtract(toAssign)
            }
        }
    }

    suspend fun coverOverspending(
        source: CategoryEntity,
        target: CategoryEntity,
        amount: BigDecimal,
        budgetMonth: BudgetMonthEntity
    ) {
        val sourceAlloc = db.budgetDao().getAllocation(budgetMonth.id, source.id)
        val sourceAssigned = sourceAlloc?.amount ?: BigDecimal.ZERO
        val newSource = BigDecimal.ZERO.max(sourceAssigned.subtract(amount))
        val delta = sourceAssigned.subtract(newSource)
        
        if (sourceAlloc != null) {
            db.budgetDao().updateAllocation(sourceAlloc.copy(amount = newSource))
        }

        val targetAlloc = db.budgetDao().getAllocation(budgetMonth.id, target.id)
        if (targetAlloc != null) {
            db.budgetDao().updateAllocation(targetAlloc.copy(amount = targetAlloc.amount.add(delta)))
        } else {
            db.budgetDao().insertAllocation(
                BudgetAllocationEntity(
                    amount = delta,
                    categoryId = target.id,
                    monthId = budgetMonth.id
                )
            )
        }
    }

    suspend fun rollToNextMonth(current: BudgetMonthEntity, transactions: List<TransactionEntity>, categories: List<CategoryEntity>) {
        val unassigned = availableToBudget(transactions, current, current.year, current.month)
        var overspentTotal = BigDecimal.ZERO
        for (cat in categories) {
            val avail = available(cat, current, current.year, current.month)
            if (avail < BigDecimal.ZERO) {
                overspentTotal = overspentTotal.add(avail)
            }
        }
        
        val carry = BigDecimal.ZERO.max(unassigned).add(overspentTotal)
        
        var nextYear = current.year
        var nextMonth = current.month + 1
        if (nextMonth > 12) {
            nextMonth = 1
            nextYear++
        }
        
        val nextMonthEntity = ensureMonth(nextYear, nextMonth)
        db.budgetDao().updateMonth(nextMonthEntity.copy(carryover = carry))
    }

    suspend fun postAllDue() {
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
        
        val items = db.scheduledItemDao().getAll().first()
        for (item in items) {
            var currentNextDate = item.nextDate
            while (currentNextDate.before(today)) {
                val tx = TransactionEntity(
                    date = currentNextDate,
                    amount = item.amount,
                    merchant = item.name,
                    memo = null,
                    cleared = false,
                    flagColor = null,
                    accountId = item.accountId,
                    categoryId = item.categoryId
                )
                db.transactionDao().insert(tx)
                
                if (item.intervalDays > 0) {
                    val cal = Calendar.getInstance()
                    cal.time = currentNextDate
                    cal.add(Calendar.DAY_OF_YEAR, item.intervalDays)
                    currentNextDate = cal.time
                } else {
                    break
                }
            }
            if (currentNextDate != item.nextDate) {
                db.scheduledItemDao().update(item.copy(nextDate = currentNextDate))
            }
        }
    }
}
