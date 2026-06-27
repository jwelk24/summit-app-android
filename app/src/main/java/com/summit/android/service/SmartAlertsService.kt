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
import com.summit.android.ui.transactions.formatCurrency
import kotlinx.coroutines.flow.first
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*

object SmartAlertsService {
    private const val CHANNEL_ID = "smart_alerts"
    private const val PREFS_NAME = "smart_alerts_prefs"

    // Keys
    private const val BUDGET_ENABLED_KEY = "alerts.budget.enabled"
    private const val BUDGET_THRESHOLD_KEY = "alerts.budget.threshold"
    private const val UNUSUAL_ENABLED_KEY = "alerts.unusual.enabled"
    private const val UNUSUAL_AMOUNT_KEY = "alerts.unusual.amount"

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

    suspend fun runChecks(context: Context, year: Int, month: Int): Int {
        if (!PremiumManager.canUseSmartAlerts()) return 0

        var sent = 0
        if (isBudgetEnabled(context)) {
            sent += runBudgetChecks(context, year, month)
        }
        if (isUnusualEnabled(context)) {
            sent += runUnusualChargeChecks(context)
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
