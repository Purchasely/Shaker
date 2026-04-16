package com.purchasely.shaker.ui.screen.settings

import com.purchasely.shaker.data.PurchaselySdkMode
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.SettingsRepository
import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.purchasely.ext.PLYDataProcessingPurpose
import io.purchasely.models.PLYError
import io.purchasely.models.PLYPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var store: InMemoryKeyValueStore
    private lateinit var settingsRepo: SettingsRepository
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var runningModeRepo: RunningModeRepository
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        store = InMemoryKeyValueStore()
        settingsRepo = SettingsRepository(store)
        premiumRepository = mockk {
            every { isPremium } returns MutableStateFlow(false)
            every { refreshPremiumStatus() } returns Unit
        }
        runningModeRepo = mockk(relaxed = true) {
            every { isObserverMode } returns false
        }
        wrapper = mockk(relaxed = true) {
            every { anonymousUserId } returns "anon-123"
            every { sdkVersion } returns "5.7.3"
            coEvery { loadPresentation(any(), any()) } returns FetchResult.Deactivated
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = SettingsViewModel(settingsRepo, premiumRepository, runningModeRepo, wrapper)

    @Test
    fun `initial userId is null when not stored`() {
        val vm = createViewModel()
        assertNull(vm.userId.value)
    }

    @Test
    fun `initial userId reads from repository`() {
        settingsRepo.userId = "kevin"
        val vm = createViewModel()
        assertEquals("kevin", vm.userId.value)
    }

    @Test
    fun `login sets userId and calls wrapper`() {
        val vm = createViewModel()
        vm.login("kevin")
        assertEquals("kevin", vm.userId.value)
        verify { wrapper.userLogin("kevin", any()) }
        verify { wrapper.setUserAttribute("user_id", "kevin") }
    }

    @Test
    fun `login persists userId to repository`() {
        val vm = createViewModel()
        vm.login("kevin")
        assertEquals("kevin", settingsRepo.userId)
    }

    @Test
    fun `login with blank userId does nothing`() {
        val vm = createViewModel()
        vm.login("")
        assertNull(vm.userId.value)
        verify(exactly = 0) { wrapper.userLogin(any(), any()) }
    }

    @Test
    fun `login with whitespace-only userId does nothing`() {
        val vm = createViewModel()
        vm.login("   ")
        assertNull(vm.userId.value)
        verify(exactly = 0) { wrapper.userLogin(any(), any()) }
    }

    @Test
    fun `login refresh callback triggers premium refresh`() {
        val refreshSlot = slot<(Boolean) -> Unit>()
        every { wrapper.userLogin(any(), capture(refreshSlot)) } answers {
            refreshSlot.captured(true)
        }
        val vm = createViewModel()
        vm.login("kevin")
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `logout clears userId and calls wrapper`() {
        settingsRepo.userId = "kevin"
        val vm = createViewModel()
        vm.logout()
        assertNull(vm.userId.value)
        verify { wrapper.userLogout() }
        assertNull(settingsRepo.userId)
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `restorePurchases success updates message`() {
        val successSlot = slot<(PLYPlan?) -> Unit>()
        every { wrapper.restoreAllProducts(capture(successSlot), any()) } answers {
            successSlot.captured(null)
        }
        val vm = createViewModel()
        vm.restorePurchases()
        assertEquals("Purchases restored successfully!", vm.restoreMessage.value)
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `restorePurchases error updates message`() {
        val errorSlot = slot<(PLYError?) -> Unit>()
        every { wrapper.restoreAllProducts(any(), capture(errorSlot)) } answers {
            errorSlot.captured(mockk { every { message } returns "No purchases found" })
        }
        val vm = createViewModel()
        vm.restorePurchases()
        assertEquals("No purchases found", vm.restoreMessage.value)
    }

    @Test
    fun `clearRestoreMessage resets message`() {
        val vm = createViewModel()
        vm.clearRestoreMessage()
        assertNull(vm.restoreMessage.value)
    }

    @Test
    fun `setThemeMode persists and sets user attribute`() {
        val vm = createViewModel()
        vm.setThemeMode("dark")
        assertEquals("dark", vm.themeMode.value)
        assertEquals("dark", settingsRepo.themeMode)
        verify { wrapper.setUserAttribute("app_theme", "dark") }
    }

    @Test
    fun `initial themeMode defaults to system`() {
        val vm = createViewModel()
        assertEquals("system", vm.themeMode.value)
    }

    @Test
    fun `setDisplayMode persists value`() {
        val vm = createViewModel()
        vm.setDisplayMode("embedded")
        assertEquals("embedded", vm.displayMode.value)
        assertEquals("embedded", settingsRepo.displayMode)
    }

    @Test
    fun `sdkVersion delegates to wrapper`() {
        val vm = createViewModel()
        assertEquals("5.7.3", vm.sdkVersion)
    }

    @Test
    fun `refreshAnonymousId updates from wrapper`() {
        val vm = createViewModel()
        every { wrapper.anonymousUserId } returns "new-anon-456"
        vm.refreshAnonymousId()
        assertEquals("new-anon-456", vm.anonymousId.value)
    }

    @Test
    fun `initial anonymousId reads from wrapper`() {
        val vm = createViewModel()
        assertEquals("anon-123", vm.anonymousId.value)
    }

    @Test
    fun `setAnalyticsConsent revokes when false`() {
        val vm = createViewModel()
        vm.setAnalyticsConsent(false)
        assertFalse(vm.analyticsConsent.value)
        verify { wrapper.revokeDataProcessingConsent(match { it.contains(PLYDataProcessingPurpose.Analytics) }) }
    }

    @Test
    fun `setIdentifiedAnalyticsConsent revokes when false`() {
        val vm = createViewModel()
        vm.setIdentifiedAnalyticsConsent(false)
        assertFalse(vm.identifiedAnalyticsConsent.value)
        verify { wrapper.revokeDataProcessingConsent(match { it.contains(PLYDataProcessingPurpose.IdentifiedAnalytics) }) }
    }

    @Test
    fun `setPersonalizationConsent revokes when false`() {
        val vm = createViewModel()
        vm.setPersonalizationConsent(false)
        assertFalse(vm.personalizationConsent.value)
        verify { wrapper.revokeDataProcessingConsent(match { it.contains(PLYDataProcessingPurpose.Personalization) }) }
    }

    @Test
    fun `setCampaignsConsent revokes when false`() {
        val vm = createViewModel()
        vm.setCampaignsConsent(false)
        assertFalse(vm.campaignsConsent.value)
        verify { wrapper.revokeDataProcessingConsent(match { it.contains(PLYDataProcessingPurpose.Campaigns) }) }
    }

    @Test
    fun `setThirdPartyConsent revokes when false`() {
        val vm = createViewModel()
        vm.setThirdPartyConsent(false)
        assertFalse(vm.thirdPartyConsent.value)
        verify { wrapper.revokeDataProcessingConsent(match { it.contains(PLYDataProcessingPurpose.ThirdPartyIntegrations) }) }
    }

    @Test
    fun `all consents true revokes empty set`() {
        val vm = createViewModel()
        // All consents default to true, verify empty set is passed
        verify { wrapper.revokeDataProcessingConsent(match { it.isEmpty() }) }
    }

    @Test
    fun `multiple consents revoked together`() {
        val vm = createViewModel()
        vm.setAnalyticsConsent(false)
        vm.setPersonalizationConsent(false)
        verify {
            wrapper.revokeDataProcessingConsent(match {
                it.contains(PLYDataProcessingPurpose.Analytics) &&
                it.contains(PLYDataProcessingPurpose.Personalization)
            })
        }
    }

    @Test
    fun `showOnboardingPaywall calls loadPresentation`() = runTest {
        val vm = createViewModel()
        vm.showOnboardingPaywall()
        coVerify { wrapper.loadPresentation("onboarding", null) }
    }

    @Test
    fun `onPurchaseCompleted refreshes premium status`() {
        val vm = createViewModel()
        vm.onPurchaseCompleted()
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `isPremium exposes premiumManager state`() {
        val vm = createViewModel()
        assertFalse(vm.isPremium.value)
    }

    @Test
    fun `initial runningMode reads from repository`() {
        val vm = createViewModel()
        assertEquals("full", vm.runningMode.value)
    }

    @Test
    fun `initial runningMode is observer when repo says so`() {
        every { runningModeRepo.isObserverMode } returns true
        val vm = createViewModel()
        assertEquals("observer", vm.runningMode.value)
    }

    @Test
    fun `setSdkMode calls wrapper restart`() {
        settingsRepo.sdkModeStorage = PurchaselySdkMode.FULL.storageValue
        val vm = createViewModel()
        vm.setSdkMode(PurchaselySdkMode.PAYWALL_OBSERVER)
        verify { wrapper.restart() }
    }

    @Test
    fun `initSdkModeIfNeeded sets default when not present`() {
        // Store starts empty, creating VM triggers initSdkModeIfNeeded
        val vm = createViewModel()
        assertEquals(PurchaselySdkMode.DEFAULT.storageValue, settingsRepo.sdkModeStorage)
    }

    @Test
    fun `initSdkModeIfNeeded does not overwrite existing value`() {
        settingsRepo.sdkModeStorage = PurchaselySdkMode.FULL.storageValue
        val vm = createViewModel()
        assertEquals(PurchaselySdkMode.FULL.storageValue, settingsRepo.sdkModeStorage)
    }
}
