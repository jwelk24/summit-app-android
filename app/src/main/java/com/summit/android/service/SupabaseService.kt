package com.summit.android.service

import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.createSupabaseClient
import io.github.jan_tennert.supabase.gotrue.GoTrue
import io.github.jan_tennert.supabase.gotrue.gotrue
import io.github.jan_tennert.supabase.postgrest.Postgrest
import io.github.jan_tennert.supabase.realtime.Realtime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

object SupabaseService {
    private const val PROJECT_URL = "https://eebpmgilbguussctttgl.supabase.co"
    private const val ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImVlYnBtZ2lsYmd1dXNzY3R0dGdsIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODE5MTY1MjEsImV4cCI6MjA5NzQ5MjUyMX0.D7agtESCefKpfAhuMq-x40Xlj7hWfp5oInE-ophrEWg"

    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = PROJECT_URL,
        supabaseKey = ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
        install(Realtime)
    }

    private val _currentUserID = MutableStateFlow<UUID?>(null)
    val currentUserID: StateFlow<UUID?> = _currentUserID

    private val _currentEmail = MutableStateFlow<String?>(null)
    val currentEmail: StateFlow<String?> = _currentEmail

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated

    // In a real app, you would observe auth state changes here
    // For now, this is a skeleton for the service
}
