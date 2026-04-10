package com.purchasely.shaker

import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.Ingredient

fun testCocktail(
    id: String = "1",
    name: String = "Mojito",
    spirit: String = "Rum",
    category: String = "Classic",
    difficulty: String = "Easy"
) = Cocktail(
    id = id,
    name = name,
    image = "$id.jpg",
    description = "A test cocktail",
    category = category,
    spirit = spirit,
    difficulty = difficulty,
    tags = listOf("test"),
    ingredients = listOf(Ingredient("Ingredient", "60ml")),
    instructions = listOf("Mix and serve")
)
