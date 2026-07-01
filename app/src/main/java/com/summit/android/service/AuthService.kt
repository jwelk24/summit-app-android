package com.summit.android.service

import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import java.security.MessageDigest

object AuthService {
    private val auth = SupabaseService.client.auth

    suspend fun signUp(email: String, password: String) {
        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signIn(email: String, password: String) {
        println("AuthService: Starting signIn for $email")
        try {
            auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            println("AuthService: signIn successful")
        } catch (e: Exception) {
            println("AuthService: signIn failed: ${e.message}")
            throw e
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.resetPasswordForEmail(email)
    }

    suspend fun signInWithApple(idToken: String, nonce: String) {
        auth.signInWith(IDToken) {
            this.idToken = idToken
            this.nonce = nonce
            this.provider = Google // placeholder — swap for Apple provider when SDK exposes it
        }
    }

    suspend fun linkApple(idToken: String, nonce: String) {
        auth.linkIdentityWith(IDToken) {
            this.idToken = idToken
            this.nonce = nonce
            this.provider = Google // placeholder — swap for Apple provider when SDK exposes it
        }
    }

    fun randomNonceString(length: Int = 32): String {
        val charset = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._"
        return (1..length).map { charset.random() }.joinToString("")
    }

    fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
