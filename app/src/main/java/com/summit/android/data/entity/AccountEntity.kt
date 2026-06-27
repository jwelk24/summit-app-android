package com.summit.android.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.summit.android.data.model.AccountType
import java.math.BigDecimal
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: UUID = UUID.randomUUID(),
    val name: String,
    val type: AccountType,
    val balance: BigDecimal,
    val currencyCode: String = "USD"
)
