package com.summit.android.ui.networth.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.billing.PremiumManager
import com.summit.android.billing.SubscriptionTier
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.AccountEntity
import com.summit.android.data.entity.BalanceSnapshotEntity
import com.summit.android.data.entity.InvestmentHoldingEntity
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.util.*


enum class NetWorthTimeRange(val label: String, val days: Int?) {
    MONTH_1("1M", 30),
    MONTH_3("3M", 90),
    MONTH_6("6M", 180),
    YEAR_1("1Y", 365),
    ALL("ALL", null)
}

data class NetWorthUiState(
    val netWorth: BigDecimal = BigDecimal.ZERO,
    val assets: List<AccountEntity> = emptyList(),
    val liabilities: List<AccountEntity> = emptyList(),
    val holdings: List<InvestmentHoldingEntity> = emptyList(),
    val timeRange: NetWorthTimeRange = NetWorthTimeRange.MONTH_3,
    val chartPoints: List<BigDecimal> = emptyList(),
    val currentTier: SubscriptionTier = SubscriptionTier.NONE
)

class NetWorthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).build()

    private val _timeRange = MutableStateFlow(NetWorthTimeRange.MONTH_3)

    val uiState: StateFlow<NetWorthUiState> = combine(
        db.accountDao().getAll(),
        db.investmentDao().getAllHoldings(),
        db.netWorthDao().getAllSnapshots(),
        combine(PremiumManager.currentTier, _timeRange) { tier, range -> tier to range }
    ) { accounts, holdings, snapshots, (tier, range) ->
        val assets = accounts.filter { it.type.isAsset }
        val liabilities = accounts.filter { !it.type.isAsset }

        val totalAssets = assets.fold(BigDecimal.ZERO) { acc, a -> acc.add(a.balance) }
        val totalLiabs = liabilities.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.balance.abs()) }

        // Build net-worth history from balance snapshots, bucketed by day.
        val cutoff: Date? = range.days?.let { days ->
            Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }.time
        }
        val filtered = if (cutoff != null) snapshots.filter { it.date.after(cutoff) } else snapshots
        val assetIds = assets.map { it.id }.toSet()
        val liabIds = liabilities.map { it.id }.toSet()

        val byDay = filtered.groupBy { snap ->
            val cal = Calendar.getInstance().apply { time = snap.date }
            Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        }
        val chartPoints = byDay.keys.sorted().map { key ->
            val daySnaps = byDay[key] ?: emptyList()
            val latestPerAccount = daySnaps.groupBy { it.accountId }
                .mapValues { (_, v) -> v.maxByOrNull { it.date }!! }
            val dayAssets = latestPerAccount.entries.filter { it.key in assetIds }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.value.balance) }
            val dayLiabs = latestPerAccount.entries.filter { it.key in liabIds }
                .fold(BigDecimal.ZERO) { acc, e -> acc.add(e.value.balance.abs()) }
            dayAssets.subtract(dayLiabs)
        }

        NetWorthUiState(
            netWorth = totalAssets.subtract(totalLiabs),
            assets = assets,
            liabilities = liabilities,
            holdings = holdings,
            timeRange = range,
            chartPoints = chartPoints,
            currentTier = tier
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), NetWorthUiState())

    fun setTimeRange(range: NetWorthTimeRange) {
        _timeRange.value = range
    }
}
