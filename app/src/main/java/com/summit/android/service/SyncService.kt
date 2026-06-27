package com.summit.android.service

import android.content.Context
import androidx.room.Room
import com.summit.android.billing.PremiumManager
import com.summit.android.billing.SubscriptionTier
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.*
import com.summit.android.data.model.AccountType
import com.summit.android.data.model.GoalType
import com.summit.android.data.model.LiabilityKind
import com.summit.android.data.model.ScheduledKind
import io.github.jan_tennert.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

@Serializable
private data class AccountRow(
    val id: String,
    val household_id: String,
    val name: String,
    val type: String,
    val balance: Double,
    val currency_code: String,
    val deleted_at: String? = null
)

@Serializable
private data class CategoryGroupRow(
    val id: String,
    val household_id: String,
    val name: String,
    val sort: Int,
    val deleted_at: String? = null
)

@Serializable
private data class CategoryRow(
    val id: String,
    val household_id: String,
    val group_id: String?,
    val linked_account_id: String?,
    val name: String,
    val sort: Int,
    val deleted_at: String? = null
)

@Serializable
private data class TransactionRow(
    val id: String,
    val household_id: String,
    val account_id: String?,
    val category_id: String?,
    val date: String,
    val amount: Double,
    val merchant: String,
    val memo: String?,
    val cleared: Boolean,
    val flag_color: String?,
    val deleted_at: String? = null
)

@Serializable
private data class TransactionSplitRow(
    val id: String,
    val household_id: String,
    val transaction_id: String,
    val category_id: String?,
    val amount: Double,
    val memo: String?,
    val deleted_at: String? = null
)

@Serializable
private data class GoalRow(
    val id: String,
    val household_id: String,
    val category_id: String?,
    val type: String,
    val target_amount: Double,
    val target_date: String?,
    val deleted_at: String? = null
)

@Serializable
private data class BudgetMonthRow(
    val id: String,
    val household_id: String,
    val year: Int,
    val month: Int,
    val carryover: Double,
    val deleted_at: String? = null
)

@Serializable
private data class BudgetAllocationRow(
    val id: String,
    val household_id: String,
    val month_id: String,
    val category_id: String,
    val amount: Double,
    val deleted_at: String? = null
)

@Serializable
private data class ScheduledItemRow(
    val id: String,
    val household_id: String,
    val account_id: String?,
    val category_id: String?,
    val kind: String,
    val name: String,
    val amount: Double,
    val next_date: String,
    val interval_days: Int,
    val deleted_at: String? = null
)

@Serializable
private data class BalanceSnapshotRow(
    val id: String,
    val household_id: String,
    val account_id: String,
    val date: String,
    val balance: Double,
    val deleted_at: String? = null
)

@Serializable
private data class SoftDeleteUpdate(val deleted_at: String)

object SyncService {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    private val _lastSyncedAt = MutableStateFlow<Date?>(null)
    val lastSyncedAt: StateFlow<Date?> = _lastSyncedAt

    private val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    suspend fun syncAll(context: Context) {
        val tier = PremiumManager.currentTier.value
        if (tier == SubscriptionTier.NONE) return
        
        val household = HouseholdService.currentHousehold.value ?: return
        
        _isSyncing.value = true
        try {
            val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
            val householdIdStr = household.id.lowercase()

            // 1. Push Deletions
            pushDeletions(db, householdIdStr)

            // 2. Sync Core Tables
            syncAccounts(db, householdIdStr)
            syncCategoryGroups(db, householdIdStr)
            syncCategories(db, householdIdStr)
            syncGoals(db, householdIdStr)
            syncBudgetMonths(db, householdIdStr)
            syncBudgetAllocations(db, householdIdStr)
            syncScheduledItems(db, householdIdStr)
            syncTransactions(db, householdIdStr)
            syncTransactionSplits(db, householdIdStr)
            syncBalanceSnapshots(db, householdIdStr)

            _lastSyncedAt.value = Date()
        } catch (e: Exception) {
            // Handle sync error
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun pushDeletions(db: AppDatabase, householdId: String) {
        // Implementation for soft deletions using tombstones
    }

    private suspend fun syncAccounts(db: AppDatabase, householdId: String) {
        val local = db.accountDao().getAll().first()
        val pushRows = local.map { a ->
            AccountRow(a.id.toString(), householdId, a.name, a.type.name.lowercase(), a.balance.toDouble(), a.currencyCode)
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("accounts").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("accounts").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<AccountRow>()

        remote.forEach { row ->
            val type = try { AccountType.valueOf(row.type.uppercase()) } catch(e: Exception) { AccountType.CHECKING }
            val localAcc = db.accountDao().getById(UUID.fromString(row.id))
            if (localAcc != null) {
                db.accountDao().update(localAcc.copy(name = row.name, type = type, balance = BigDecimal(row.balance), currencyCode = row.currency_code))
            } else {
                db.accountDao().insert(AccountEntity(UUID.fromString(row.id), row.name, type, BigDecimal(row.balance), row.currency_code))
            }
        }
    }

    private suspend fun syncCategoryGroups(db: AppDatabase, householdId: String) {
        val local = db.categoryDao().getGroups().first()
        val pushRows = local.map { g ->
            CategoryGroupRow(g.id.toString(), householdId, g.name, g.sort)
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("category_groups").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("category_groups").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<CategoryGroupRow>()

        remote.forEach { row ->
            // Insert/Update groups
            // (Similar logic to accounts)
        }
    }

    private suspend fun syncCategories(db: AppDatabase, householdId: String) {
        val local = db.categoryDao().getCategories().first()
        val pushRows = local.map { c ->
            CategoryRow(c.id.toString(), householdId, c.groupId?.toString(), c.linkedAccountId?.toString(), c.name, c.sort)
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("categories").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("categories").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<CategoryRow>()

        remote.forEach { row ->
            // Update/Insert categories
        }
    }

    private suspend fun syncGoals(db: AppDatabase, householdId: String) {
        val local = db.goalDao().getAllGoals().first()
        val pushRows = local.map { g ->
            GoalRow(g.id.toString(), householdId, g.categoryId.toString(), g.type.name.lowercase(), g.targetAmount.toDouble(), g.targetDate?.let { df.format(it) })
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("goals").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("goals").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<GoalRow>()

        remote.forEach { row ->
            // Update/Insert goals
        }
    }

    private suspend fun syncBudgetMonths(db: AppDatabase, householdId: String) {
        // Need to add getAllMonths to budgetDao
    }

    private suspend fun syncBudgetAllocations(db: AppDatabase, householdId: String) {
        val local = db.budgetDao().getAllAllocations()
        val pushRows = local.map { a ->
            BudgetAllocationRow(a.id.toString(), householdId, a.monthId.toString(), a.categoryId.toString(), a.amount.toDouble())
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("budget_allocations").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("budget_allocations").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<BudgetAllocationRow>()

        remote.forEach { row ->
            // Update/Insert allocations
        }
    }

    private suspend fun syncScheduledItems(db: AppDatabase, householdId: String) {
        val local = db.scheduledItemDao().getAll().first()
        val pushRows = local.map { s ->
            ScheduledItemRow(s.id.toString(), householdId, s.accountId?.toString(), s.categoryId?.toString(), s.kind.name.lowercase(), s.name, s.amount.toDouble(), df.format(s.nextDate), s.intervalDays)
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("scheduled_items").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("scheduled_items").select {
            filter { eq("household_id", householdId); is_("deleted_at", null) }
        }.decodeList<ScheduledItemRow>()

        remote.forEach { row ->
            // Update/Insert scheduled items
        }
    }

    private suspend fun syncTransactions(db: AppDatabase, householdId: String) {
        val local = db.transactionDao().getAll().first()
        val pushRows = local.map { t ->
            TransactionRow(t.id.toString(), householdId, t.accountId?.toString(), t.categoryId?.toString(), df.format(t.date), t.amount.toDouble(), t.merchant, t.memo, t.cleared, t.flagColor)
        }
        if (pushRows.isNotEmpty()) {
            SupabaseService.client.from("transactions").upsert(pushRows)
        }

        val remote = SupabaseService.client.from("transactions").select {
            filter { eq("household_id", householdId) } // pull all to handle remote deletions
        }.decodeList<TransactionRow>()

        remote.forEach { row ->
            val localTx = db.transactionDao().getById(UUID.fromString(row.id))
            if (row.deleted_at != null) {
                if (localTx != null) db.transactionDao().delete(localTx)
                return@forEach
            }
            // Update/Insert logic...
        }
    }

    private suspend fun syncTransactionSplits(db: AppDatabase, householdId: String) {
        // Implementation for splits sync
    }

    private suspend fun syncBalanceSnapshots(db: AppDatabase, householdId: String) {
        // Implementation for snapshots sync
    }
}
