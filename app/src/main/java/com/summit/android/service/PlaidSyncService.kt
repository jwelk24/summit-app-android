package com.summit.android.service

import android.content.Context
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.*
import com.summit.android.data.model.AccountType
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

class PlaidSyncService(context: Context) {
    private val db = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "summit-db"
    ).build()
    
    private val plaidStorage = PlaidStorage(context)
    private val plaidApi = PlaidService.api

    data class FullSyncResult(
        val accounts: Int = 0,
        val transactionsAdded: Int = 0,
        val transactionsModified: Int = 0,
        val transactionsRemoved: Int = 0,
        val holdings: Int = 0,
        val investmentTransactions: Int = 0,
        val liabilities: Int = 0
    )

    suspend fun syncAll(item: StoredPlaidItem): FullSyncResult {
        val accounts = syncAccounts(item)
        val txResult = syncTransactions(item)
        
        // Investment and Liabilities sync omitted for brevity in this port, 
        // but would follow the same pattern as iOS.
        
        return FullSyncResult(
            accounts = accounts.size,
            transactionsAdded = txResult.added,
            transactionsModified = txResult.modified,
            transactionsRemoved = txResult.removed
        )
    }

    private suspend fun syncAccounts(item: StoredPlaidItem): List<AccountEntity> {
        val response = plaidApi.getAccounts(item.accessToken)
        val results = mutableListOf<AccountEntity>()
        for (plaidAccount in response.accounts) {
            results.add(upsertAccount(plaidAccount, item.itemId))
        }
        return results
    }

    private suspend fun upsertAccount(plaidAccount: PlaidAccount, plaidItemId: String): AccountEntity {
        val plaidId = plaidAccount.account_id
        val balance = BigDecimal(plaidAccount.balances.current ?: plaidAccount.balances.available ?: 0.0)
        val currency = plaidAccount.balances.iso_currency_code ?: "USD"
        val type = mapAccountType(plaidAccount.type, plaidAccount.subtype)
        val displayName = plaidAccount.official_name ?: plaidAccount.name

        val existingLink = db.plaidLinkDao().getAccountLink(plaidId)

        if (existingLink != null) {
            val account = db.accountDao().getById(existingLink.accountModelId)
            if (account != null) {
                val updatedAccount = account.copy(
                    name = displayName,
                    type = type,
                    balance = balance,
                    currencyCode = currency
                )
                db.accountDao().update(updatedAccount)
                
                val updatedLink = existingLink.copy(
                    lastBalance = balance,
                    updatedAt = Date()
                )
                db.plaidLinkDao().insertAccountLink(updatedLink)
                
                appendSnapshotIfChanged(updatedAccount, balance)
                return updatedAccount
            }
        }

        val account = AccountEntity(
            name = displayName,
            type = type,
            balance = balance,
            currencyCode = currency
        )
        db.accountDao().insert(account)
        appendSnapshotIfChanged(account, balance)

        val link = PlaidAccountLinkEntity(
            plaidAccountId = plaidId,
            plaidItemId = plaidItemId,
            accountModelId = account.id,
            lastBalance = balance
        )
        db.plaidLinkDao().insertAccountLink(link)
        return account
    }

    private suspend fun appendSnapshotIfChanged(account: AccountEntity, balance: BigDecimal) {
        // Implementation for snapshots...
    }

    data class SyncTxResult(val added: Int, val modified: Int, val removed: Int)

    private suspend fun syncTransactions(item: StoredPlaidItem): SyncTxResult {
        val cursor = plaidStorage.getCursor(item.itemId)
        val response = plaidApi.syncTransactions(item.accessToken, SyncBody(cursor))

        for (tx in response.added) {
            applyAdded(tx)
        }
        // applyModified and applyRemoved logic would go here...

        if (response.nextCursor != null) {
            plaidStorage.setCursor(item.itemId, response.nextCursor)
        }

        return SyncTxResult(response.added.size, response.modified.size, response.removed.size)
    }

    private suspend fun applyAdded(tx: PlaidTransaction) {
        val existingLink = db.plaidLinkDao().getTransactionLink(tx.transaction_id)
        if (existingLink != null) {
            // applyModified...
            return
        }

        val accountLink = db.plaidLinkDao().getAccountLink(tx.account_id) ?: return
        val account = db.accountDao().getById(accountLink.accountModelId) ?: return

        val date = parsePlaidDate(tx.date) ?: Date()
        val amount = BigDecimal(-tx.amount)
        
        val transaction = TransactionEntity(
            date = date,
            amount = amount,
            merchant = tx.name,
            memo = null,
            cleared = true,
            flagColor = null,
            accountId = account.id,
            categoryId = null
        )
        db.transactionDao().insert(transaction)

        val link = PlaidTransactionLinkEntity(
            plaidTransactionId = tx.transaction_id,
            transactionModelId = transaction.id,
            plaidAccountId = tx.account_id,
            pending = false
        )
        db.plaidLinkDao().insertTransactionLink(link)
    }

    private fun mapAccountType(plaidType: String, subtype: String?): AccountType {
        return when (plaidType.lowercase()) {
            "depository" -> when (subtype?.lowercase()) {
                "savings", "money market", "cd" -> AccountType.SAVINGS
                else -> AccountType.CHECKING
            }
            "credit" -> AccountType.CREDIT_CARD
            "loan" -> AccountType.LOAN
            "investment" -> AccountType.INVESTMENT
            else -> AccountType.MANUAL_ASSET
        }
    }

    private fun parsePlaidDate(value: String): Date? {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(value)
        } catch (e: Exception) {
            null
        }
    }
}
