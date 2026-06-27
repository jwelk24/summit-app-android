package com.summit.android.ui.networth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.AccountEntity
import com.summit.android.data.model.AccountType
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

enum class NetWorthTimeRange(val label: String, val days: Int?) {
    ONE_MONTH("1M", 30),
    THREE_MONTHS("3M", 90),
    SIX_MONTHS("6M", 180),
    ONE_YEAR("1Y", 365),
    ALL("All", null)
}

data class ChartPoint(val date: Long, val value: Float)

data class NetWorthUiState(
    val accounts: List<AccountEntity> = emptyList(),
    val totalAssets: BigDecimal = BigDecimal.ZERO,
    val totalLiabilities: BigDecimal = BigDecimal.ZERO,
    val netWorth: BigDecimal = BigDecimal.ZERO,
    val timeRange: NetWorthTimeRange = NetWorthTimeRange.THREE_MONTHS,
    val chartData: List<ChartPoint> = emptyList()
)

class NetWorthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).build()

    private val _timeRange = MutableStateFlow(NetWorthTimeRange.THREE_MONTHS)
    val timeRange: StateFlow<NetWorthTimeRange> = _timeRange

    val uiState: StateFlow<NetWorthUiState> = combine(
        db.netWorthDao().getAllAccounts(),
        db.netWorthDao().getAllSnapshots(),
        db.transactionDao().getAll(),
        _timeRange
    ) { accounts, snapshots, transactions, range ->
        val assets = accounts.filter { it.type.isAsset }
            .fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance) }
        val liabilities = accounts.filter { !it.type.isAsset }
            .fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance.abs()) }
        
        val netWorth = assets.subtract(liabilities)

        // Mock chart data for now, real calculation would involve iterating through dates
        val chartData = calculateChartData(accounts, snapshots, transactions, range)

        NetWorthUiState(
            accounts = accounts,
            totalAssets = assets,
            totalLiabilities = liabilities,
            netWorth = netWorth,
            timeRange = range,
            chartData = chartData
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthUiState())

    private fun calculateChartData(
        accounts: List<AccountEntity>,
        snapshots: List<com.summit.android.data.entity.BalanceSnapshotEntity>,
        transactions: List<com.summit.android.data.entity.TransactionEntity>,
        range: NetWorthTimeRange
    ): List<ChartPoint> {
        val calendar = Calendar.getInstance()
        val points = mutableListOf<ChartPoint>()
        val days = range.days ?: 365
        
        for (i in days downTo 0 step (days / 10).coerceAtLeast(1)) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -i)
            val date = cal.time
            
            // This is a simplified calculation: starting from current balance and subtracting transactions backwards
            var total = BigDecimal.ZERO
            for (account in accounts) {
                var bal = account.balance
                val afterTxs = transactions.filter { it.accountId == account.id && it.date.after(date) }
                val txSum = afterTxs.fold(BigDecimal.ZERO) { acc, tx -> acc.add(tx.amount) }
                bal = bal.subtract(txSum)
                total = if (account.type.isAsset) total.add(bal) else total.subtract(bal.abs())
            }
            points.add(ChartPoint(date.time, total.toFloat()))
        }
        return points
    }

    fun setTimeRange(range: NetWorthTimeRange) {
        _timeRange.value = range
    }

    private val _linkToken = MutableStateFlow<String?>(null)
    val linkToken: StateFlow<String?> = _linkToken

    fun createLinkToken() {
        viewModelScope.launch {
            try {
                val response = PlaidService.api.createLinkToken(emptyMap())
                _linkToken.value = response.linkToken
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun onLinkTokenUsed() {
        _linkToken.value = null
    }

    fun exchangePublicToken(publicToken: String) {
        viewModelScope.launch {
            try {
                val response = PlaidService.api.exchangePublicToken(mapOf("publicToken" to publicToken))
                val item = StoredPlaidItem(
                    itemId = response.itemId,
                    accessToken = response.accessToken,
                    institutionName = "Linked Bank", // We could fetch this from Plaid
                    linkedAt = System.currentTimeMillis()
                )
                PlaidStorage(getApplication()).saveItem(item)
                // Trigger a sync
                PlaidSyncService(getApplication()).syncAll(item)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
