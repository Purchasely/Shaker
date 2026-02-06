package com.purchasely.shaker.data

import android.content.Context
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.CocktailsData
import kotlinx.serialization.json.Json

class CocktailRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private var cocktails: List<Cocktail> = emptyList()

    fun loadCocktails(): List<Cocktail> {
        if (cocktails.isNotEmpty()) return cocktails

        val jsonString = context.assets.open("cocktails.json")
            .bufferedReader()
            .use { it.readText() }

        cocktails = json.decodeFromString<CocktailsData>(jsonString).cocktails
        return cocktails
    }

    fun getCocktail(id: String): Cocktail? {
        return loadCocktails().find { it.id == id }
    }

    fun getSpirits(): List<String> = loadCocktails().map { it.spirit }.distinct().sorted()
    fun getCategories(): List<String> = loadCocktails().map { it.category }.distinct().sorted()
    fun getDifficulties(): List<String> = loadCocktails().map { it.difficulty }.distinct()
}
