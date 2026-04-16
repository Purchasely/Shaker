package com.purchasely.shaker.domain.usecase

import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.testCocktail
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetFilteredCocktailsUseCaseTest {

    private val testCocktails = listOf(
        testCocktail("1", "Mojito", "Rum", "Classic", "Easy"),
        testCocktail("2", "Margarita", "Tequila", "Classic", "Medium"),
        testCocktail("3", "Negroni", "Gin", "Bitter", "Easy"),
        testCocktail("4", "Old Fashioned", "Whiskey", "Classic", "Hard"),
        testCocktail("5", "Daiquiri", "Rum", "Tropical", "Easy")
    )

    private lateinit var repository: CocktailRepository
    private lateinit var useCase: GetFilteredCocktailsUseCase

    @Before
    fun setUp() {
        repository = mockk {
            every { loadCocktails() } returns testCocktails
        }
        useCase = GetFilteredCocktailsUseCase(repository)
    }

    @Test
    fun `no filters returns all cocktails`() {
        val result = useCase()
        assertEquals(testCocktails, result)
    }

    @Test
    fun `filter by spirit`() {
        val result = useCase(spirits = setOf("Rum"))
        assertEquals(2, result.size)
        assertTrue(result.all { it.spirit == "Rum" })
    }

    @Test
    fun `filter by multiple spirits`() {
        val result = useCase(spirits = setOf("Rum", "Gin"))
        assertEquals(3, result.size)
        assertTrue(result.all { it.spirit in setOf("Rum", "Gin") })
    }

    @Test
    fun `filter by query case insensitive`() {
        val result = useCase(query = "mojito")
        assertEquals(1, result.size)
        assertEquals("Mojito", result[0].name)
    }

    @Test
    fun `filter by query partial match`() {
        val result = useCase(query = "Moj")
        assertEquals(1, result.size)
        assertEquals("Mojito", result[0].name)
    }

    @Test
    fun `filter by category`() {
        val result = useCase(categories = setOf("Classic"))
        assertEquals(3, result.size)
        assertTrue(result.all { it.category == "Classic" })
    }

    @Test
    fun `filter by difficulty`() {
        val result = useCase(difficulty = "Easy")
        assertEquals(3, result.size)
        assertTrue(result.all { it.difficulty == "Easy" })
    }

    @Test
    fun `combined filters`() {
        val result = useCase(
            spirits = setOf("Rum"),
            categories = setOf("Classic"),
            difficulty = "Easy"
        )
        assertEquals(1, result.size)
        assertEquals("Mojito", result[0].name)
    }

    @Test
    fun `combined query and spirit filter`() {
        val result = useCase(query = "Daiquiri", spirits = setOf("Rum"))
        assertEquals(1, result.size)
        assertEquals("Daiquiri", result[0].name)
    }

    @Test
    fun `empty result when no matches`() {
        val result = useCase(query = "NonExistent")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `empty result with conflicting filters`() {
        val result = useCase(spirits = setOf("Gin"), categories = setOf("Classic"))
        assertTrue(result.isEmpty())
    }

    @Test
    fun `blank query returns all cocktails`() {
        val result = useCase(query = "   ")
        assertEquals(testCocktails, result)
    }
}
