package com.summit.android.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "investment_holdings",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("accountId"), Index("plaidHoldingKey", unique = true)]
)
data class InvestmentHoldingEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val plaidHoldingKey: String,
    val plaidAccountId: String,
    val plaidSecurityId: String,
    val tickerSymbol: String?,
    val securityName: String?,
    val securityType: String?,
    val isCashEquivalent: Boolean,
    val quantity: BigDecimal,
    val institutionPrice: BigDecimal,
    val institutionValue: BigDecimal,
    val costBasis: BigDecimal?,
    val currencyCode: String,
    val asOfDate: Date,
    val accountId: UUID?
)
