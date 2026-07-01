package com.summit.android.data.dao

import androidx.room.*
import com.summit.android.data.entity.InvestmentHoldingEntity
import com.summit.android.data.entity.InvestmentTransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investment_holdings WHERE accountId = :accountId")
    fun getHoldingsForAccount(accountId: UUID): Flow<List<InvestmentHoldingEntity>>

    @Query("SELECT * FROM investment_holdings")
    fun getAllHoldings(): Flow<List<InvestmentHoldingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolding(holding: InvestmentHoldingEntity)

    @Query("SELECT * FROM investment_transactions WHERE accountId = :accountId ORDER BY date DESC")
    fun getTransactionsForAccount(accountId: UUID): Flow<List<InvestmentTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: InvestmentTransactionEntity)

    @Query("SELECT * FROM investment_holdings")
    suspend fun getAllHoldingsList(): List<InvestmentHoldingEntity>

    @Query("SELECT * FROM investment_transactions ORDER BY date DESC")
    suspend fun getAllTransactionsList(): List<InvestmentTransactionEntity>
}
