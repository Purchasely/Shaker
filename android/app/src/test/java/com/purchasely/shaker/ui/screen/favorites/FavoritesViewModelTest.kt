package com.purchasely.shaker.ui.screen.favorites

import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.testCocktail
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testCocktails = listOf(
        testCocktail("1", "Mojito", "Rum"),
        testCocktail("2", "Margarita", "Tequila"),
        testCocktail("3", "Negroni", "Gin")
    )

    private lateinit var cocktailRepository: CocktailRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var premiumManager: PremiumManager
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        cocktailRepository = mockk {
            every { loadCocktails() } returns testCocktails
        }
        favoritesRepository = mockk(relaxed = true) {
            every { favoriteIds } returns MutableStateFlow(setOf("1", "3"))
        }
        premiumManager = mockk {
            every { isPremium } returns MutableStateFlow(false)
            every { refreshPremiumStatus() } returns Unit
        }
        wrapper = mockk(relaxed = true) {
            coEvery { loadPresentation(any(), any()) } returns FetchResult.Deactivated
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        FavoritesViewModel(cocktailRepository, favoritesRepository, premiumManager, wrapper)

    @Test
    fun `favorites returns matching cocktails reactively`() {
        val vm = createViewModel()
        val favorites = vm.favorites.value
        assertEquals(2, favorites.size)
        assertEquals("Mojito", favorites[0].name)
        assertEquals("Negroni", favorites[1].name)
    }

    @Test
    fun `favorites returns empty when no favorites`() {
        every { favoritesRepository.favoriteIds } returns MutableStateFlow(emptySet())
        val vm = createViewModel()
        assertTrue(vm.favorites.value.isEmpty())
    }

    @Test
    fun `removeFavorite delegates to repository`() {
        val vm = createViewModel()
        vm.removeFavorite("1")
        verify { favoritesRepository.removeFavorite("1") }
    }

    @Test
    fun `showFavoritesPaywall calls loadPresentation`() = runTest {
        val vm = createViewModel()
        vm.showFavoritesPaywall()
        coVerify { wrapper.loadPresentation("favorites", null) }
    }

    @Test
    fun `onPaywallDismissed refreshes premium status`() {
        val vm = createViewModel()
        vm.onPaywallDismissed()
        verify { premiumManager.refreshPremiumStatus() }
    }

    @Test
    fun `isPremium exposes premiumManager state`() {
        val vm = createViewModel()
        assertEquals(false, vm.isPremium.value)
    }

    @Test
    fun `favoriteIds exposes favoritesRepository state`() {
        val vm = createViewModel()
        assertEquals(setOf("1", "3"), vm.favoriteIds.value)
    }
}
