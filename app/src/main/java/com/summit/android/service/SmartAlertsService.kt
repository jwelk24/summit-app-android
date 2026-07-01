package com.summit.android.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.summit.android.R
import com.summit.android.billing.PremiumManager
import com.summit.android.data.AppDatabase
import com.summit.android.data.model.AccountType
import com.summit.android.data.model.ScheduledKind
import com.summit.android.service.CashFlowForecasterUtils
import com.summit.android.ui.transactions.formatCurrency
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import java.util.concurrent.TimeUnit

object SmartAlertsService {
    private const val CHANNEL_ID = "smart_alerts"
    private const val PREFS_NAME = "smart_alerts_prefs"

    // Keys
    private const val BUDGET_ENABLED_KEY = "alerts.budget.enabled"
    private const val BUDGET_THRESHOLD_KEY = "alerts.budget.threshold"
    private const val UNUSUAL_ENABLED_KEY = "alerts.unusual.enabled"
    private const val UNUSUAL_AMOUNT_KEY = "alerts.unusual.amount"
    private const val BILL_ENABLED_KEY = "alerts.bill.enabled"
    private const val BILL_LEAD_DAYS_KEY = "alerts.bill.leadDays"
    private const val LOW_BALANCE_ENABLED_KEY = "alerts.lowbalance.enabled"
    private const val LOW_BALANCE_THRESHOLD_KEY = "alerts.lowbalance.threshold"
    private const val PRICE_CHANGE_ENABLED_KEY = "alerts.pricechange.enabled"

    fun isBudgetEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(BUDGET_ENABLED_KEY, false)

    fun setBudgetEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(BUDGET_ENABLED_KEY, enabled).apply()

    fun getBudgetThreshold(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(BUDGET_THRESHOLD_KEY, 80)

    fun setBudgetThreshold(context: Context, threshold: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(BUDGET_THRESHOLD_KEY, threshold).apply()

    fun isUnusualEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(UNUSUAL_ENABLED_KEY, false)

    fun setUnusualEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(UNUSUAL_ENABLED_KEY, enabled).apply()

    fun getUnusualAmountThreshold(context: Context): BigDecimal =
        BigDecimal(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(UNUSUAL_AMOUNT_KEY, "200") ?: "200")

    fun setUnusualAmountThreshold(context: Context, threshold: BigDecimal) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(UNUSUAL_AMOUNT_KEY, threshold.toString()).apply()

    fun isBillRemindersEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(BILL_ENABLED_KEY, false)

    fun setBillRemindersEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(BILL_ENABLED_KEY, enabled).apply()

    fun getBillReminderLeadDays(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(BILL_LEAD_DAYS_KEY, 3)

    fun setBillReminderLeadDays(context: Context, days: Int) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putInt(BILL_LEAD_DAYS_KEY, days).apply()

    fun isLowBalanceEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(LOW_BALANCE_ENABLED_KEY, false)

    fun setLowBalanceEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(LOW_BALANCE_ENABLED_KEY, enabled).apply()

    fun getLowBalanceThreshold(context: Context): BigDecimal =
        BigDecimal(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(LOW_BALANCE_THRESHOLD_KEY, "100") ?: "100")

    fun setLowBalanceThreshold(context: Context, threshold: BigDecimal) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putString(LOW_BALANCE_THRESHOLD_KEY, threshold.toString()).apply()

    fun isPriceChangeEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(PRICE_CHANGE_ENABLED_KEY, false)

    fun setPriceChangeEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(PRICE_CHANGE_ENABLED_KEY, enabled).apply()

    suspend fun runChecks(context: Context, year: Int, month: Int): Int {
        var sent = 0
        // Bill reminders and low-balance warning available on all tiers
        if (isBillRemindersEnabled(context)) {
            sent += runBillReminderChecks(context)
        }
        if (isLowBalanceEnabled(context)) {
            sent += runLowBalanceCheck(context)
        }
        // Budget threshold, unusual charges, price change are Premium
        if (PremiumManager.canUseSmartAlerts()) {
            if (isBudgetEnabled(context)) {
                sent += runBudgetChecks(context, year, month)
            }
            if (isUnusualEnabled(context)) {
                sent += runUnusualChargeChecks(context)
            }
            if (isPriceChangeEnabled(context)) {
                sent += runPriceChangeChecks(context)
            }
        }
        return sent
    }

    private suspend fun runBudgetChecks(context: Context, year: Int, month: Int): Int {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val budgetMonth = db.budgetDao().getMonth(year, month) ?: return 0
        val categories = db.categoryDao().getCategories().first()
        val threshold = getBudgetThreshold(context)
        val thresholdDecimal = BigDecimal(threshold).divide(BigDecimal(100))
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sent = 0

        for (category in categories) {
            val assigned = db.budgetDao().getAllocation(budgetMonth.id, category.id)?.amount ?: BigDecimal.ZERO
            if (assigned <= BigDecimal.ZERO) continue

            // Logic to calculate spent in current month for this category
            val engine = BudgetEngine(context)
            val activity = engine.activity(category, year, month)
            val spent = activity.abs()
            if (activity >= BigDecimal.ZERO) continue

            val pct = spent.divide(assigned, 4, RoundingMode.HALF_UP)
            if (pct >= thresholdDecimal) {
                val dedupKey = "alert.budget.${category.id}.${year}.${month}.${threshold}"
                if (prefs.getBoolean(dedupKey, false)) continue

                val title = "${category.name} budget"
                val body = "You've spent ${formatCurrency(spent.toDouble())} of ${formatCurrency(assigned.toDouble())} — ${(pct.multiply(BigDecimal(100)).toInt())}%."
                sendNotification(context, title, body, dedupKey.hashCode())
                prefs.edit().putBoolean(dedupKey, true).apply()
                sent++
            }
        }
        return sent
    }

    private suspend fun runUnusualChargeChecks(context: Context): Int {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val cutoff = calendar.time

        val recent = db.transactionDao().getAll().first().filter { it.date.after(cutoff) && it.amount < BigDecimal.ZERO }
        val threshold = getUnusualAmountThreshold(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sent = 0

        for (tx in recent) {
            val absAmount = tx.amount.abs()
            if (absAmount < threshold) continue

            val dedupKey = "alert.tx.${tx.id}"
            if (prefs.getBoolean(dedupKey, false)) continue

            // Check if new merchant
            val allTransactions = db.transactionDao().getAll().first()
            val isNewMerchant = allTransactions.none { it.merchant == tx.merchant && it.date.before(tx.date) }

            val title = if (isNewMerchant) "New-merchant charge" else "Unusual charge"
            val suffix = if (isNewMerchant) " — first time at this merchant." else "."
            val body = "${tx.merchant}: ${formatCurrency(absAmount.toDouble())}$suffix"
            
            sendNotification(context, title, body, tx.id.hashCode())
            prefs.edit().putBoolean(dedupKey, true).apply()
            sent++
        }
        return sent
    }

    private suspend fun runBillReminderChecks(context: Context): Int {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val cal = Calendar.getInstance()
        val today = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
        val horizon = Calendar.getInstance().apply { time = today; add(Calendar.DAY_OF_YEAR, 45) }.time
        val items = db.scheduledItemDao().getAll().first()
        val leadDays = getBillReminderLeadDays(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sent = 0

        for (item in items) {
            if (item.kind != ScheduledKind.BILL && item.kind != ScheduledKind.SUBSCRIPTION) continue
            val due = item.nextDate
            val dueCal = Calendar.getInstance().apply { time = due; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            val dueStart = dueCal.time
            if (dueStart.before(today) || dueStart.after(horizon)) continue

            val daysUntilDue = TimeUnit.MILLISECONDS.toDays(dueStart.time - today.time).toInt()
            if (daysUntilDue > leadDays) continue

            val dedupKey = "alert.bill.${item.id}.${dueStart.time}"
            if (prefs.getBoolean(dedupKey, false)) continue

            val amount = item.amount.abs()
            val duePhrase = when (daysUntilDue) {
                0 -> "today"
                1 -> "tomorrow"
                else -> "in $daysUntilDue days"
            }
            val title = "Upcoming bill"
            val body = "${item.name} — ${formatCurrency(amount.toDouble())} due $duePhrase."
            sendNotification(context, title, body, dedupKey.hashCode())
            prefs.edit().putBoolean(dedupKey, true).apply()
            sent++
        }
        return sent
    }

    private suspend fun runPriceChangeChecks(context: Context): Int {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val transactions = db.transactionDao().getAll().first()
        val changes = SubscriptionTracker.detectPriceChanges(transactions)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var sent = 0

        for (change in changes) {
            val canonical = SubscriptionTracker.canonicalMerchant(change.merchant)
            val newKey = change.newAmount.toPlainString()
            val dedupKey = "alert.pricechange.$canonical.$newKey"
            if (prefs.getBoolean(dedupKey, false)) continue

            val title = if (change.isIncrease) "Subscription price increase" else "Subscription price drop"
            val verb = if (change.isIncrease) "went up" else "dropped"
            val body = "${change.merchant} $verb from ${formatCurrency(change.oldAmount.toDouble())} to ${formatCurrency(change.newAmount.toDouble())}."
            sendNotification(context, title, body, dedupKey.hashCode())
            prefs.edit().putBoolean(dedupKey, true).apply()
            sent++
        }
        return sent
    }

    private suspend fun runLowBalanceCheck(context: Context): Int {
        val db = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "summit-db").build()
        val accounts = db.accountDao().getAll().first()
        if (accounts.none { it.type == AccountType.CHECKING || it.type == AccountType.SAVINGS }) return 0

        val scheduled = db.scheduledItemDao().getAll().first()
        val startingBalance = CashFlowForecasterUtils.spendableBalance(accounts)
        val forecaster = com.summit.android.service.CashFlowForecaster(startingBalance, scheduled, 30)
        val result = forecaster.project()

        val threshold = getLowBalanceThreshold(context)
        val dip = result.points.firstOrNull { it.balance < threshold } ?: return 0

        val cal = Calendar.getInstance()
        val today = cal.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.time
        val todayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
        val dedupKey = "alert.lowbalance.$todayKey"

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.getBoolean(dedupKey, false)) return 0

        val daysOut = TimeUnit.MILLISECONDS.toDays(dip.date.time - today.time).toInt()
        val whenPhrase = when (daysOut) {
            0 -> "right now"
            1 -> "tomorrow"
            else -> "in $daysOut days"
        }
        val title = "Low balance ahead"
        val body = "Your spendable balance is projected to reach ${formatCurrency(dip.balance.toDouble())} $whenPhrase, below your ${formatCurrency(threshold.toDouble())} cushion."
        sendNotification(context, title, body, dedupKey.hashCode())
        prefs.edit().putBoolean(dedupKey, true).apply()
        return 1
    }

    private fun sendNotification(context: Context, title: String, body: String, id: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Smart Alerts", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Budget alerts and unusual charge notifications"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(id, notification)
    }

    fun sendTestNotification(context: Context) {
        sendNotification(context, "Summit Alerts", "This is what a Summit alert looks like.", 9999)
    }
}
