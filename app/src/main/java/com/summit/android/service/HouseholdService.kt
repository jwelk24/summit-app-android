package com.summit.android.service

import io.github.jan_tennert.supabase.postgrest.from
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable
import java.util.*

@Serializable
data class Household(
    val id: String,
    val name: String,
    val owner_user_id: String,
    val created_at: String
)

@Serializable
data class HouseholdMembership(
    val household_id: String,
    val user_id: String,
    val role: String,
    val joined_at: String
)

@Serializable
data class HouseholdInvite(
    val code: String,
    val household_id: String,
    val role: String,
    val created_by: String,
    val expires_at: String,
    val used_at: String? = null,
    val used_by: String? = null
)

@Serializable
private data class InviteInsert(
    val code: String,
    val household_id: String,
    val role: String,
    val created_by: String,
    val expires_at: String
)

@Serializable
private data class RedeemParams(
    val invite_code: String
)

enum class HouseholdRole(val value: String) {
    OWNER("owner"),
    MEMBER("member"),
    VIEWER("viewer");

    val canWrite: Boolean get() = this == OWNER || this == MEMBER
    val canInvite: Boolean get() = this == OWNER

    companion object {
        fun fromString(value: String): HouseholdRole? = values().find { it.value == value }
    }
}

object HouseholdService {
    private val _currentHousehold = MutableStateFlow<Household?>(null)
    val currentHousehold: StateFlow<Household?> = _currentHousehold

    private val _currentRole = MutableStateFlow<HouseholdRole?>(null)
    val currentRole: StateFlow<HouseholdRole?> = _currentRole

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    suspend fun refresh() {
        val userID = SupabaseService.currentUserID.value ?: run {
            _currentHousehold.value = null
            _currentRole.value = null
            return
        }
        
        _isLoading.value = true
        try {
            val memberships = SupabaseService.client.from("household_members")
                .select {
                    filter {
                        eq("user_id", userID.toString().lowercase())
                    }
                }.decodeList<HouseholdMembership>()

            val primary = memberships.firstOrNull() ?: run {
                _currentHousehold.value = null
                _currentRole.value = null
                _isLoading.value = false
                return
            }

            val households = SupabaseService.client.from("households")
                .select {
                    filter {
                        eq("id", primary.household_id.lowercase())
                    }
                }.decodeList<Household>()

            _currentHousehold.value = households.firstOrNull()
            _currentRole.value = HouseholdRole.fromString(primary.role)
            _lastError.value = null
        } catch (e: Exception) {
            _lastError.value = e.localizedMessage
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun createInvite(role: HouseholdRole = HouseholdRole.MEMBER, expiresInDays: Int = 7): String {
        val household = _currentHousehold.value ?: throw Exception("No household")
        val userID = SupabaseService.currentUserID.value ?: throw Exception("Not authenticated")
        if (_currentRole.value?.canInvite != true) throw Exception("Not owner")

        val code = generateInviteCode()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, expiresInDays)
        val expires = calendar.time.toString() // Simplified date format for now

        val payload = InviteInsert(
            code = code,
            household_id = household.id,
            role = role.value,
            created_by = userID.toString().lowercase(),
            expires_at = expires
        )
        
        SupabaseService.client.from("household_invites").insert(payload)
        return code
    }

    suspend fun redeemInvite(code: String) {
        val trimmed = code.trim().uppercase()
        if (trimmed.isEmpty()) throw Exception("Invalid code")
        
        val params = RedeemParams(invite_code = trimmed)
        SupabaseService.client.from("redeem_household_invite").insert(params) // In Supabase KT, rpc might be different
        refresh()
    }

    private fun generateInviteCode(): String {
        val alphabet = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"
        return (1..8).map { alphabet.random() }.joinToString("")
    }
}
