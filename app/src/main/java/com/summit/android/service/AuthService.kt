package com.summit.android.service

import io.github.jan_tennert.supabase.gotrue.gotrue
import io.github.jan_tennert.supabase.gotrue.providers.builtin.Email

object AuthService {
    private val auth = SupabaseService.client.gotrue

    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.sendForgotPasswordEmail(email)
    }
}
