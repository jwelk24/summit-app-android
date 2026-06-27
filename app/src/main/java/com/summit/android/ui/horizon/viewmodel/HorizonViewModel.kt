package com.summit.android.ui.horizon.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.summit.android.data.AppDatabase
import com.summit.android.data.entity.ScheduledItemEntity
import com.summit.android.data.model.AccountType
import com.summit.android.data.model.ScheduledKind
import kotlinx.coroutines.flow.*
import java.math.BigDecimal
import java.util.*

data class ProjectionPoint(
    val id: UUID = UUID.randomUUID(),
    val date: Date,
    val label: String,
    val kind: ScheduledKind,
    val delta: BigDecimal,
    val runningBalance: BigDecimal,
    val item: ScheduledItemEntity
)

data class HorizonUiState(
    val startingBalance: BigDecimal = BigDecimal.ZERO,
    val lowestProjected: BigDecimal = BigDecimal.ZERO,
    val projected90Day: BigDecimal = BigDecimal.ZERO,
    val pendingItems: List<ScheduledItemEntity> = emptyList(),
    val projectionPoints: List<ProjectionPoint> = emptyList()
)

class HorizonViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "summit-db"
    ).build()

    val uiState: StateFlow<HorizonUiState> = combine(
        db.accountDao().getAll(),
        db.scheduledItemDao().getAll()
    ) { accounts, scheduledItems ->
        val startingBalance = accounts
            .filter { it.type == AccountType.CHECKING || it.type == AccountType.SAVINGS }
            .fold(BigDecimal.ZERO) { acc, account -> acc.add(account.balance) }

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val pending = scheduledItems.filter { it.nextDate.before(today) }
        
        val points = calculateProjections(startingBalance, scheduledItems, today)
        
        val lowest = points.minOfOrNull { it.runningBalance } ?: startingBalance
        val last = points.lastOrNull()?.runningBalance ?: startingBalance

        HorizonUiState(
            startingBalance = startingBalance,
            lowestProjected = lowest,
            projected90Day = last,
            pendingItems = pending,
            projectionPoints = points
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HorizonUiState())

    private fun calculateProjections(
        startingBalance: BigDecimal,
        items: List<ScheduledItemEntity>,
        today: Date
    ): List<ProjectionPoint> {
        val calendar = Calendar.getInstance()
        val horizon = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 90) }.time
        
        val events = mutableListOf<Pair<Date, ScheduledItemEntity>>()
        
        for (item in items) {
            calendar.time = item.nextDate
            var safety = 0
            while (calendar.time.before(horizon) && safety < 365) {
                if (!calendar.time.before(today)) {
                    events.add(calendar.time to item)
                }
                if (item.intervalDays <= 0) break
                calendar.add(Calendar.DAY_OF_YEAR, item.intervalDays)
                safety++
            }
        }
        
        events.sortBy { it.first }
        
        var running = startingBalance
        return events.map { (date, item) ->
            running = running.add(item.amount)
            ProjectionPoint(
                date = date,
                label = item.name,
                kind = item.kind,
                delta = item.amount,
                runningBalance = running,
                item = item
            )
        }
    }
}
