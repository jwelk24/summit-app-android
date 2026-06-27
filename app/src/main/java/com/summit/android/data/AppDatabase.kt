package com.summit.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.summit.android.data.converter.Converters
import com.summit.android.data.entity.*

@Database(
    entities = [
        AccountEntity::class,
        CategoryGroupEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransactionSplitEntity::class,
        GoalEntity::class,
        ScheduledItemEntity::class,
        BudgetMonthEntity::class,
        BudgetAllocationEntity::class,
        BalanceSnapshotEntity::class,
        InvestmentHoldingEntity::class,
        InvestmentTransactionEntity::class,
        LiabilityEntity::class,
        SoftDeleteTombstoneEntity::class,
        PlaidAccountLinkEntity::class,
        PlaidTransactionLinkEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun netWorthDao(): NetWorthDao
    abstract fun scheduledItemDao(): ScheduledItemDao
    abstract fun plaidLinkDao(): PlaidLinkDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao
}
