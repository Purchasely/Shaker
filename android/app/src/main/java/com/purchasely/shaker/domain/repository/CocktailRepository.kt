package com.purchasely.shaker.domain.repository

import com.purchasely.shaker.domain.model.Cocktail

interface CocktailRepository {
    fun loadCocktails(): List<Cocktail>
    fun getCocktail(id: String): Cocktail?
    fun getSpirits(): List<String>
    fun getCategories(): List<String>
    fun getDifficulties(): List<String>
}
