package com.summit.android.service

import android.content.Context
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.AccountEntity
import com.summit.android.data.model.AccountType
import io.github.jan_tennert.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import java.math.BigDecimal
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

object SyncService {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    suspend fun syncAll(context: Context) {
        val household = HouseholdService.currentHousehold.value ?: return
        val userID = SupabaseService.currentUserID.value ?: return

        _isSyncing.value = true
        try {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "summit-db").build()
            
            // Push Accounts
            val localAccounts = db.accountDao().getAll().first()
            val pushRows = localAccounts.map { a ->
                AccountRow(
                    id = a.id.toString(),
                    household_id = household.id,
                    name = a.name,
                    type = a.type.name.lowercase(),
                    balance = a.balance.toDouble(),
                    currency_code = a.currencyCode
                )
            }
            if (pushRows.isNotEmpty()) {
                SupabaseService.client.from("accounts").upsert(pushRows)
            }

            // Pull Accounts
            val remoteAccounts = SupabaseService.client.from("accounts")
                .select {
                    filter {
                        eq("household_id", household.id)
                        is_("deleted_at", null)
                    }
                }.decodeList<AccountRow>()

            for (row in remoteAccounts) {
                val type = try { AccountType.valueOf(row.type.uppercase()) } catch(e: Exception) { AccountType.CHECKING }
                val local = db.accountDao().getById(UUID.fromString(row.id))
                if (local != null) {
                    db.accountDao().update(local.copy(
                        name = row.name,
                        type = type,
                        balance = BigDecimal(row.balance),
                        currencyCode = row.currency_code
                    ))
                } else {
                    db.accountDao().insert(AccountEntity(
                        id = UUID.fromString(row.id),
                        name = row.name,
                        type = type,
                        balance = BigDecimal(row.balance),
                        currencyCode = row.currency_code
                    ))
                }
            }

            // ... Port other entities (transactions, categories, etc.) similarly ...

        } catch (e: Exception) {
            // Handle error
        } finally {
            _isSyncing.value = false
        }
    }
}
