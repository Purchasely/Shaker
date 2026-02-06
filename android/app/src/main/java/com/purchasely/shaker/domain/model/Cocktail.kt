package com.purchasely.shaker.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CocktailsData(
    val cocktails: List<Cocktail>
)

@Serializable
data class Cocktail(
    val id: String,
    val name: String,
    val image: String,
    val description: String,
    val category: String,
    val spirit: String,
    val difficulty: String,
    val tags: List<String>,
    val ingredients: List<Ingredient>,
    val instructions: List<String>
)

@Serializable
data class Ingredient(
    val name: String,
    val amount: String
)
