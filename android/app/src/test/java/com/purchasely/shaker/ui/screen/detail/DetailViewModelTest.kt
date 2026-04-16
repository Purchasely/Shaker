package com.purchasely.shaker.ui.screen.detail

import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.FavoritesRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.domain.usecase.ToggleFavoriteUseCase
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val mojito = testCocktail("mojito", "Mojito", "Rum", "Classic", "Easy")

    private lateinit var repository: CocktailRepository
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var wrapper: PurchaselyWrapper
    private lateinit var toggleFavoriteUseCase: ToggleFavoriteUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            every { loadCocktails() } returns listOf(mojito)
            every { getCocktail("mojito") } returns mojito
            every { getCocktail("nonexistent") } returns null
        }
        premiumRepository = mockk {
            every { isPremium } returns MutableStateFlow(false)
            every { refreshPremiumStatus() } returns Unit
        }
        favoritesRepository = mockk(relaxed = true) {
            every { favoriteIds } returns MutableStateFlow(emptySet())
            every { isFavorite(any()) } returns false
        }
        wrapper = mockk(relaxed = true) {
            coEvery { loadPresentation(any(), any()) } returns FetchResult.Deactivated
        }
        toggleFavoriteUseCase = ToggleFavoriteUseCase(favoritesRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(cocktailId: String = "mojito") =
        DetailViewModel(repository, premiumRepository, favoritesRepository, wrapper, toggleFavoriteUseCase, cocktailId)

    @Test
    fun `loads cocktail by id on init`() {
        val vm = createViewModel()
        assertNotNull(vm.cocktail.value)
        assertEquals("Mojito", vm.cocktail.value?.name)
    }

    @Test
    fun `null cocktail when id not found`() {
        val vm = createViewModel("nonexistent")
        assertNull(vm.cocktail.value)
    }

    @Test
    fun `init tracks cocktails_viewed attribute`() {
        createViewModel()
        verify { wrapper.incrementUserAttribute("cocktails_viewed") }
    }

    @Test
    fun `init tracks favorite_spirit attribute`() {
        createViewModel()
        verify { wrapper.setUserAttribute("favorite_spirit", "Rum") }
    }

    @Test
    fun `init does not track spirit when cocktail not found`() {
        createViewModel("nonexistent")
        verify(exactly = 0) { wrapper.setUserAttribute("favorite_spirit", any<String>()) }
    }

    @Test
    fun `isFavorite delegates to favoritesRepository`() {
        every { favoritesRepository.isFavorite("mojito") } returns true
        val vm = createViewModel()
        assertTrue(vm.isFavorite())
    }

    @Test
    fun `isFavorite returns false when not favorited`() {
        val vm = createViewModel()
        assertFalse(vm.isFavorite())
    }

    @Test
    fun `toggleFavorite delegates to favoritesRepository`() {
        val vm = createViewModel()
        vm.toggleFavorite()
        verify { favoritesRepository.toggleFavorite("mojito") }
    }

    @Test
    fun `showRecipePaywall calls loadPresentation with recipe_detail placement`() = runTest {
        val vm = createViewModel()
        vm.showRecipePaywall()
        coVerify { wrapper.loadPresentation("recipe_detail", "mojito") }
    }

    @Test
    fun `showFavoritesPaywall calls loadPresentation with favorites placement`() = runTest {
        val vm = createViewModel()
        vm.showFavoritesPaywall()
        coVerify { wrapper.loadPresentation("favorites", null) }
    }

    @Test
    fun `onPaywallDismissed refreshes premium status`() {
        val vm = createViewModel()
        vm.onPaywallDismissed()
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `isPremium exposes premiumRepository state`() {
        val premiumFlow = MutableStateFlow(false)
        every { premiumRepository.isPremium } returns premiumFlow
        val vm = createViewModel()
        assertFalse(vm.isPremium.value)
        premiumFlow.value = true
        assertTrue(vm.isPremium.value)
    }

    @Test
    fun `favoriteIds exposes favoritesRepository state`() {
        val favFlow = MutableStateFlow(setOf("mojito"))
        every { favoritesRepository.favoriteIds } returns favFlow
        val vm = createViewModel()
        assertTrue(vm.favoriteIds.value.contains("mojito"))
    }
}
