package com.purchasely.shaker.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavoritesRepositoryTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private var storedSet: MutableSet<String> = mutableSetOf()

    @Before
    fun setUp() {
        storedSet = mutableSetOf()
        editor = mockk(relaxed = true) {
            every { putStringSet(any(), any()) } answers {
                storedSet = (secondArg() as Set<String>).toMutableSet()
                this@mockk
            }
        }
        prefs = mockk {
            every { getStringSet(any(), any()) } answers { storedSet.toSet() }
            every { edit() } returns editor
        }
        context = mockk {
            every { getSharedPreferences(any(), any()) } returns prefs
        }
    }

    private fun createRepository(): FavoritesRepository = FavoritesRepository(context)

    @Test
    fun `initial state is empty when no stored favorites`() {
        val repo = createRepository()
        assertTrue(repo.favoriteIds.value.isEmpty())
    }

    @Test
    fun `initial state loads stored favorites`() {
        storedSet = mutableSetOf("cocktail1", "cocktail2")
        val repo = createRepository()
        assertEquals(setOf("cocktail1", "cocktail2"), repo.favoriteIds.value)
    }

    @Test
    fun `addFavorite adds cocktail id`() {
        val repo = createRepository()
        repo.addFavorite("cocktail1")
        assertTrue(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `addFavorite persists to SharedPreferences`() {
        val repo = createRepository()
        repo.addFavorite("cocktail1")
        verify { editor.putStringSet(any(), match { it.contains("cocktail1") }) }
    }

    @Test
    fun `removeFavorite removes cocktail id`() {
        storedSet = mutableSetOf("cocktail1")
        val repo = createRepository()
        repo.removeFavorite("cocktail1")
        assertFalse(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `removeFavorite persists to SharedPreferences`() {
        storedSet = mutableSetOf("cocktail1")
        val repo = createRepository()
        repo.removeFavorite("cocktail1")
        verify { editor.putStringSet(any(), match { !it.contains("cocktail1") }) }
    }

    @Test
    fun `toggleFavorite adds when not present`() {
        val repo = createRepository()
        repo.toggleFavorite("cocktail1")
        assertTrue(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `toggleFavorite removes when already present`() {
        storedSet = mutableSetOf("cocktail1")
        val repo = createRepository()
        repo.toggleFavorite("cocktail1")
        assertFalse(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `isFavorite returns true for existing favorite`() {
        storedSet = mutableSetOf("cocktail1")
        val repo = createRepository()
        assertTrue(repo.isFavorite("cocktail1"))
    }

    @Test
    fun `isFavorite returns false for non-favorite`() {
        val repo = createRepository()
        assertFalse(repo.isFavorite("cocktail1"))
    }

    @Test
    fun `multiple add and remove operations`() {
        val repo = createRepository()
        repo.addFavorite("a")
        repo.addFavorite("b")
        repo.addFavorite("c")
        assertEquals(setOf("a", "b", "c"), repo.favoriteIds.value)

        repo.removeFavorite("b")
        assertEquals(setOf("a", "c"), repo.favoriteIds.value)
    }

    @Test
    fun `addFavorite is idempotent`() {
        val repo = createRepository()
        repo.addFavorite("cocktail1")
        repo.addFavorite("cocktail1")
        assertEquals(1, repo.favoriteIds.value.size)
    }

    @Test
    fun `removeFavorite on non-existing id is no-op`() {
        val repo = createRepository()
        repo.removeFavorite("nonexistent")
        assertTrue(repo.favoriteIds.value.isEmpty())
    }
}
