package com.purchasely.shaker.data

import com.purchasely.shaker.data.storage.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavoritesRepositoryTest {

    private lateinit var store: InMemoryKeyValueStore

    @Before
    fun setUp() {
        store = InMemoryKeyValueStore()
    }

    private fun createRepository(): FavoritesRepository = FavoritesRepository(store)

    @Test
    fun `initial state is empty when no stored favorites`() {
        val repo = createRepository()
        assertTrue(repo.favoriteIds.value.isEmpty())
    }

    @Test
    fun `initial state loads stored favorites`() {
        store.putStringSet("favorite_cocktail_ids", setOf("cocktail1", "cocktail2"))
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
    fun `addFavorite persists to store`() {
        val repo = createRepository()
        repo.addFavorite("cocktail1")
        assertTrue(store.getStringSet("favorite_cocktail_ids").contains("cocktail1"))
    }

    @Test
    fun `removeFavorite removes cocktail id`() {
        store.putStringSet("favorite_cocktail_ids", setOf("cocktail1"))
        val repo = createRepository()
        repo.removeFavorite("cocktail1")
        assertFalse(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `removeFavorite persists to store`() {
        store.putStringSet("favorite_cocktail_ids", setOf("cocktail1"))
        val repo = createRepository()
        repo.removeFavorite("cocktail1")
        assertFalse(store.getStringSet("favorite_cocktail_ids").contains("cocktail1"))
    }

    @Test
    fun `toggleFavorite adds when not present`() {
        val repo = createRepository()
        repo.toggleFavorite("cocktail1")
        assertTrue(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `toggleFavorite removes when already present`() {
        store.putStringSet("favorite_cocktail_ids", setOf("cocktail1"))
        val repo = createRepository()
        repo.toggleFavorite("cocktail1")
        assertFalse(repo.favoriteIds.value.contains("cocktail1"))
    }

    @Test
    fun `isFavorite returns true for existing favorite`() {
        store.putStringSet("favorite_cocktail_ids", setOf("cocktail1"))
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
