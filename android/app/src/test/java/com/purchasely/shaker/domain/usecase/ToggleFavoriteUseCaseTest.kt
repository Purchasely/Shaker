package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.repository.FavoritesRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class ToggleFavoriteUseCaseTest {

    private lateinit var favoritesRepository: FavoritesRepository
    private lateinit var useCase: ToggleFavoriteUseCase

    @Before
    fun setUp() {
        favoritesRepository = mockk(relaxed = true)
        useCase = ToggleFavoriteUseCase(favoritesRepository)
    }

    @Test
    fun `invoke delegates to favoritesRepository toggleFavorite`() {
        useCase("cocktail-1")
        verify { favoritesRepository.toggleFavorite("cocktail-1") }
    }

    @Test
    fun `invoke passes correct cocktail id`() {
        useCase("mojito")
        verify { favoritesRepository.toggleFavorite("mojito") }
    }
}
