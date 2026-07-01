package com.summit.android.data.dao

import androidx.room.*
import com.summit.android.data.entity.AccountEntity
import com.summit.android.data.entity.BalanceSnapshotEntity
import com.summit.android.data.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
interface NetWorthDao {
    @Query("SELECT * FROM accounts")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM balance_snapshots WHERE accountId = :accountId ORDER BY date ASC")
    fun getSnapshotsForAccount(accountId: UUID): Flow<List<BalanceSnapshotEntity>>

    @Query("SELECT * FROM transactions WHERE accountId = :accountId ORDER BY date ASC")
    fun getTransactionsForAccount(accountId: UUID): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM balance_snapshots ORDER BY date ASC")
    fun getAllSnapshots(): Flow<List<BalanceSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: BalanceSnapshotEntity)

    @Query("SELECT * FROM balance_snapshots")
    suspend fun getAllSnapshotsList(): List<BalanceSnapshotEntity>
}
