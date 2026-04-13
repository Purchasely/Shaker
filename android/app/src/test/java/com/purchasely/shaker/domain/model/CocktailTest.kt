package com.purchasely.shaker.domain.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CocktailTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize single cocktail from JSON`() {
        val jsonString = """
        {
            "id": "mojito",
            "name": "Mojito",
            "image": "mojito.jpg",
            "description": "A refreshing Cuban cocktail",
            "category": "Classic",
            "spirit": "Rum",
            "difficulty": "Easy",
            "tags": ["refreshing", "summer"],
            "ingredients": [
                {"name": "White Rum", "amount": "60ml"},
                {"name": "Lime Juice", "amount": "30ml"}
            ],
            "instructions": ["Muddle mint", "Add rum and lime", "Top with soda"]
        }
        """.trimIndent()

        val cocktail = json.decodeFromString<Cocktail>(jsonString)

        assertEquals("mojito", cocktail.id)
        assertEquals("Mojito", cocktail.name)
        assertEquals("mojito.jpg", cocktail.image)
        assertEquals("A refreshing Cuban cocktail", cocktail.description)
        assertEquals("Classic", cocktail.category)
        assertEquals("Rum", cocktail.spirit)
        assertEquals("Easy", cocktail.difficulty)
        assertEquals(listOf("refreshing", "summer"), cocktail.tags)
        assertEquals(2, cocktail.ingredients.size)
        assertEquals("White Rum", cocktail.ingredients[0].name)
        assertEquals("60ml", cocktail.ingredients[0].amount)
        assertEquals(3, cocktail.instructions.size)
    }

    @Test
    fun `deserialize CocktailsData with multiple cocktails`() {
        val jsonString = """
        {
            "cocktails": [
                {
                    "id": "1",
                    "name": "Mojito",
                    "image": "mojito.jpg",
                    "description": "Desc",
                    "category": "Classic",
                    "spirit": "Rum",
                    "difficulty": "Easy",
                    "tags": [],
                    "ingredients": [],
                    "instructions": []
                },
                {
                    "id": "2",
                    "name": "Margarita",
                    "image": "margarita.jpg",
                    "description": "Desc",
                    "category": "Classic",
                    "spirit": "Tequila",
                    "difficulty": "Medium",
                    "tags": [],
                    "ingredients": [],
                    "instructions": []
                }
            ]
        }
        """.trimIndent()

        val data = json.decodeFromString<CocktailsData>(jsonString)

        assertEquals(2, data.cocktails.size)
        assertEquals("Mojito", data.cocktails[0].name)
        assertEquals("Margarita", data.cocktails[1].name)
    }

    @Test
    fun `cocktail data class equality`() {
        val c1 = Cocktail("1", "Mojito", "img", "desc", "Classic", "Rum", "Easy", emptyList(), emptyList(), emptyList())
        val c2 = Cocktail("1", "Mojito", "img", "desc", "Classic", "Rum", "Easy", emptyList(), emptyList(), emptyList())
        assertEquals(c1, c2)
    }

    @Test
    fun `ingredient data class equality`() {
        val i1 = Ingredient("Rum", "60ml")
        val i2 = Ingredient("Rum", "60ml")
        assertEquals(i1, i2)
    }

    @Test
    fun `deserialize ignores unknown keys`() {
        val jsonString = """
        {
            "id": "1",
            "name": "Test",
            "image": "test.jpg",
            "description": "Desc",
            "category": "Cat",
            "spirit": "Gin",
            "difficulty": "Hard",
            "tags": [],
            "ingredients": [],
            "instructions": [],
            "unknownField": "should be ignored"
        }
        """.trimIndent()

        val cocktail = json.decodeFromString<Cocktail>(jsonString)
        assertEquals("Test", cocktail.name)
    }

    @Test
    fun `cocktail with empty collections`() {
        val cocktail = Cocktail("1", "Empty", "img", "desc", "Cat", "Gin", "Easy", emptyList(), emptyList(), emptyList())
        assertTrue(cocktail.tags.isEmpty())
        assertTrue(cocktail.ingredients.isEmpty())
        assertTrue(cocktail.instructions.isEmpty())
    }
}
