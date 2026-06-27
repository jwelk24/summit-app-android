package com.summit.android.service

import io.github.jan_tennert.supabase.realtime.Realtime
import io.github.jan_tennert.supabase.realtime.RealtimeChannel
import io.github.jan_tennert.supabase.realtime.postgresChangeFlow
import io.github.jan_tennert.supabase.realtime.realtime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.*

object RealtimeService {
    private var channel: RealtimeChannel? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var job: Job? = null

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected

    private val subscribedTables = listOf(
        "accounts", "category_groups", "categories",
        "goals", "budget_months", "budget_allocations",
        "scheduled_items", "transactions", "transaction_splits",
        "balance_snapshots"
    )

    fun start(householdID: UUID) {
        job?.cancel()
        job = scope.launch {
            val client = SupabaseService.client
            channel = client.realtime.createChannel("household_${householdID.toString().lowercase()}")
            
            subscribedTables.forEach { table ->
                val flow = channel!!.postgresChangeFlow<Any>(schema = "public") {
                    this.table = table
                    filter = "household_id=eq.${householdID.toString().lowercase()}"
                }
                
                launch {
                    flow.collect {
                        handleEvent()
                    }
                }
            }
            
            channel!!.subscribe()
            _isConnected.value = true
        }
    }

    private var debounceJob: Job? = null
    private fun handleEvent() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(1000)
            // Trigger sync
            // SyncService.syncAll(context) // We need a context here, might need to pass it or use a global one
        }
    }

    fun stop() {
        job?.cancel()
        debounceJob?.cancel()
        scope.launch {
            channel?.unsubscribe()
            channel = null
            _isConnected.value = false
        }
    }
}
