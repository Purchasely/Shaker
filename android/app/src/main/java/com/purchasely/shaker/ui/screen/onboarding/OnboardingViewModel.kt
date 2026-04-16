package com.purchasely.shaker.ui.screen.onboarding

import androidx.lifecycle.ViewModel
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PresentationHandle
import com.purchasely.shaker.purchasely.PurchaselyWrapper

class OnboardingViewModel(
    private val purchaselyWrapper: PurchaselyWrapper,
    private val premiumRepository: PremiumRepository
) : ViewModel() {

    suspend fun loadOnboarding(): FetchResult {
        return purchaselyWrapper.loadPresentation("onboarding")
    }

    fun onPurchaseCompleted() {
        premiumRepository.refreshPremiumStatus()
    }
}
