package com.purchasely.shaker.ui.screen.home

import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
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
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val testCocktails = listOf(
        testCocktail("1", "Mojito", "Rum", "Classic", "Easy"),
        testCocktail("2", "Margarita", "Tequila", "Classic", "Medium"),
        testCocktail("3", "Negroni", "Gin", "Bitter", "Easy"),
        testCocktail("4", "Old Fashioned", "Whiskey", "Classic", "Hard"),
        testCocktail("5", "Daiquiri", "Rum", "Tropical", "Easy")
    )

    private lateinit var repository: CocktailRepository
    private lateinit var premiumRepository: PremiumRepository
    private lateinit var wrapper: PurchaselyWrapper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk {
            every { loadCocktails() } returns testCocktails
            every { getSpirits() } returns listOf("Gin", "Rum", "Tequila", "Whiskey")
            every { getCategories() } returns listOf("Bitter", "Classic", "Tropical")
            every { getDifficulties() } returns listOf("Easy", "Medium", "Hard")
        }
        premiumRepository = mockk {
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

    private fun createViewModel() = HomeViewModel(repository, premiumRepository, wrapper)

    @Test
    fun `initial cocktails are loaded from repository`() {
        val vm = createViewModel()
        assertEquals(testCocktails, vm.cocktails.value)
    }

    @Test
    fun `availableSpirits delegates to repository`() {
        val vm = createViewModel()
        assertEquals(listOf("Gin", "Rum", "Tequila", "Whiskey"), vm.availableSpirits)
    }

    @Test
    fun `availableCategories delegates to repository`() {
        val vm = createViewModel()
        assertEquals(listOf("Bitter", "Classic", "Tropical"), vm.availableCategories)
    }

    @Test
    fun `availableDifficulties delegates to repository`() {
        val vm = createViewModel()
        assertEquals(listOf("Easy", "Medium", "Hard"), vm.availableDifficulties)
    }

    @Test
    fun `search filters cocktails by name`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("Mojito")
        assertEquals(1, vm.cocktails.value.size)
        assertEquals("Mojito", vm.cocktails.value[0].name)
    }

    @Test
    fun `search is case insensitive`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("mojito")
        assertEquals(1, vm.cocktails.value.size)
    }

    @Test
    fun `empty search returns all cocktails`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("Mojito")
        vm.onSearchQueryChanged("")
        assertEquals(5, vm.cocktails.value.size)
    }

    @Test
    fun `search sets has_used_search user attribute`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("test")
        verify { wrapper.setUserAttribute("has_used_search", true) }
    }

    @Test
    fun `blank search does not set user attribute`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("   ")
        verify(exactly = 0) { wrapper.setUserAttribute("has_used_search", true) }
    }

    @Test
    fun `toggleSpirit filters by spirit`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        assertEquals(2, vm.cocktails.value.size)
        assertTrue(vm.cocktails.value.all { it.spirit == "Rum" })
    }

    @Test
    fun `toggleSpirit twice removes filter`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleSpirit("Rum")
        assertEquals(5, vm.cocktails.value.size)
    }

    @Test
    fun `multiple spirits filter with OR logic`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleSpirit("Gin")
        assertEquals(3, vm.cocktails.value.size)
    }

    @Test
    fun `toggleCategory filters by category`() {
        val vm = createViewModel()
        vm.toggleCategory("Classic")
        assertEquals(3, vm.cocktails.value.size)
        assertTrue(vm.cocktails.value.all { it.category == "Classic" })
    }

    @Test
    fun `toggleCategory twice removes filter`() {
        val vm = createViewModel()
        vm.toggleCategory("Classic")
        vm.toggleCategory("Classic")
        assertEquals(5, vm.cocktails.value.size)
    }

    @Test
    fun `selectDifficulty filters by difficulty`() {
        val vm = createViewModel()
        vm.selectDifficulty("Easy")
        assertEquals(3, vm.cocktails.value.size)
        assertTrue(vm.cocktails.value.all { it.difficulty == "Easy" })
    }

    @Test
    fun `selectDifficulty same value toggles off`() {
        val vm = createViewModel()
        vm.selectDifficulty("Easy")
        vm.selectDifficulty("Easy")
        assertEquals(5, vm.cocktails.value.size)
    }

    @Test
    fun `combined spirit and category filters`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleCategory("Classic")
        assertEquals(1, vm.cocktails.value.size)
        assertEquals("Mojito", vm.cocktails.value[0].name)
    }

    @Test
    fun `combined search and spirit filter`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.onSearchQueryChanged("Daiquiri")
        assertEquals(1, vm.cocktails.value.size)
        assertEquals("Daiquiri", vm.cocktails.value[0].name)
    }

    @Test
    fun `clearFilters resets all filters`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        vm.toggleCategory("Classic")
        vm.selectDifficulty("Easy")
        vm.clearFilters()
        assertEquals(5, vm.cocktails.value.size)
        assertTrue(vm.selectedSpirits.value.isEmpty())
        assertTrue(vm.selectedCategories.value.isEmpty())
        assertEquals(null, vm.selectedDifficulty.value)
    }

    @Test
    fun `hasActiveFilters is false initially`() {
        val vm = createViewModel()
        assertFalse(vm.hasActiveFilters.value)
    }

    @Test
    fun `hasActiveFilters is true with spirit filter`() {
        val vm = createViewModel()
        vm.toggleSpirit("Rum")
        assertTrue(vm.hasActiveFilters.value)
    }

    @Test
    fun `hasActiveFilters is true with category filter`() {
        val vm = createViewModel()
        vm.toggleCategory("Classic")
        assertTrue(vm.hasActiveFilters.value)
    }

    @Test
    fun `hasActiveFilters is true with difficulty filter`() {
        val vm = createViewModel()
        vm.selectDifficulty("Easy")
        assertTrue(vm.hasActiveFilters.value)
    }

    @Test
    fun `init prefetches presentations when not premium`() {
        createViewModel()
        coVerify { wrapper.loadPresentation("filters", null) }
        coVerify { wrapper.loadPresentation("inline", null) }
    }

    @Test
    fun `init does not prefetch presentations when premium`() {
        every { premiumRepository.isPremium } returns MutableStateFlow(true)
        createViewModel()
        coVerify(exactly = 0) { wrapper.loadPresentation(any(), any()) }
    }

    @Test
    fun `onPaywallDismissed refreshes premium status`() {
        val vm = createViewModel()
        vm.onPaywallDismissed()
        verify { premiumRepository.refreshPremiumStatus() }
    }

    @Test
    fun `onFilterClick does nothing when premium`() {
        every { premiumRepository.isPremium } returns MutableStateFlow(true)
        val vm = createViewModel()
        vm.onFilterClick()
        // No exception, no paywall display signal
    }

    @Test
    fun `filter returns no results when none match`() {
        val vm = createViewModel()
        vm.onSearchQueryChanged("NonExistentCocktail")
        assertTrue(vm.cocktails.value.isEmpty())
    }
}
