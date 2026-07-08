package com.summit.android.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.summit.android.data.converter.Converters
import com.summit.android.data.dao.*
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
        PlaidTransactionLinkEntity::class,
        CategoryRuleEntity::class
    ],
    version = 2,
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
    abstract fun categoryRuleDao(): CategoryRuleDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun liabilityDao(): LiabilityDao
    abstract fun softDeleteTombstoneDao(): SoftDeleteTombstoneDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE transactions ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE category_rules ADD COLUMN renameTo TEXT")
                database.execSQL("ALTER TABLE category_rules ADD COLUMN addTags TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
