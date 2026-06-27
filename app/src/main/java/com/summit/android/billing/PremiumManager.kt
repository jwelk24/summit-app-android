package com.summit.android.billing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SubscriptionTier {
    NONE, // For during auth/onboarding
    PRO,
    PREMIUM
}

object PremiumManager {
    private val _currentTier = MutableStateFlow(SubscriptionTier.PRO) // Default to PRO for development
    val currentTier: StateFlow<SubscriptionTier> = _currentTier

    fun setTier(tier: SubscriptionTier) {
        _currentTier.value = tier
    }

    // Feature Gates
    fun canUseReceiptScanner(): Boolean = _currentTier.value == SubscriptionTier.PREMIUM
    fun canUseAIServices(): Boolean = _currentTier.value == SubscriptionTier.PREMIUM
    fun canShareHousehold(): Boolean = _currentTier.value == SubscriptionTier.PREMIUM
    fun getHorizonDays(): Int = if (_currentTier.value == SubscriptionTier.PREMIUM) 90 else 30
}
