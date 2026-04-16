package com.purchasely.shaker.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface PremiumRepository {
    val isPremium: StateFlow<Boolean>
    fun refreshPremiumStatus()
}
