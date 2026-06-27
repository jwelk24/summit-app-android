package com.summit.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "soft_delete_tombstones")
data class SoftDeleteTombstoneEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val table: String,
    val recordID: UUID,
    val createdAt: Date = Date()
)

@Entity(tableName = "plaid_account_links")
data class PlaidAccountLinkEntity(
    @PrimaryKey val plaidAccountId: String,
    val plaidItemId: String,
    val accountModelId: UUID,
    var lastBalance: Double,
    var updatedAt: Date = Date()
)

@Entity(tableName = "plaid_transaction_links")
data class PlaidTransactionLinkEntity(
    @PrimaryKey val plaidTransactionId: String,
    val transactionModelId: UUID,
    val plaidAccountId: String,
    var pending: Boolean
)
