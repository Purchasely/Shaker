package com.purchasely.shaker.domain.model

import com.purchasely.shaker.testCocktail
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CocktailMoodTest {

    @Test
    fun `REFRESHING matches cocktails tagged refreshing or minty`() {
        assertTrue(CocktailMood.REFRESHING.matches(testCocktail(tags = listOf("refreshing", "summer"))))
        assertTrue(CocktailMood.REFRESHING.matches(testCocktail(tags = listOf("minty"))))
        assertFalse(CocktailMood.REFRESHING.matches(testCocktail(tags = listOf("sweet"))))
    }

    @Test
    fun `STRONG matches bold and smoky cocktails`() {
        assertTrue(CocktailMood.STRONG.matches(testCocktail(tags = listOf("bold"))))
        assertTrue(CocktailMood.STRONG.matches(testCocktail(tags = listOf("smoky", "complex"))))
        assertFalse(CocktailMood.STRONG.matches(testCocktail(tags = listOf("light"))))
    }

    @Test
    fun `SWEET matches sweet fruity creamy and tropical tags`() {
        assertTrue(CocktailMood.SWEET.matches(testCocktail(tags = listOf("fruity"))))
        assertTrue(CocktailMood.SWEET.matches(testCocktail(tags = listOf("creamy"))))
        assertFalse(CocktailMood.SWEET.matches(testCocktail(tags = listOf("bitter"))))
    }

    @Test
    fun `ZERO_PROOF matches on spirit not tags`() {
        assertTrue(CocktailMood.ZERO_PROOF.matches(testCocktail(spirit = "non-alcoholic", tags = listOf("bold"))))
        assertFalse(CocktailMood.ZERO_PROOF.matches(testCocktail(spirit = "rum", tags = listOf("refreshing"))))
    }

    @Test
    fun `PARTY matches celebratory and bubbly tags`() {
        assertTrue(CocktailMood.PARTY.matches(testCocktail(tags = listOf("bubbly"))))
        assertTrue(CocktailMood.PARTY.matches(testCocktail(tags = listOf("celebratory", "elegant"))))
        assertFalse(CocktailMood.PARTY.matches(testCocktail(tags = listOf("rustic"))))
    }

    @Test
    fun `every mood exposes a stable key for the Purchasely user attribute`() {
        // Keys feed the `preferred_mood` user attribute used by console audiences —
        // renaming one silently breaks targeting, so pin them.
        val keys = CocktailMood.entries.map { it.key }
        org.junit.Assert.assertEquals(
            listOf("refreshing", "strong", "sweet", "zero_proof", "party"),
            keys,
        )
    }
}
