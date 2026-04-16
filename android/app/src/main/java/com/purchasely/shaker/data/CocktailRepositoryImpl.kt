package com.purchasely.shaker.data

import android.content.Context
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.CocktailsData
import com.purchasely.shaker.domain.repository.CocktailRepository
import kotlinx.serialization.json.Json

class CocktailRepositoryImpl(private val context: Context) : CocktailRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private var cocktails: List<Cocktail> = emptyList()

    override fun loadCocktails(): List<Cocktail> {
        if (cocktails.isNotEmpty()) return cocktails

        val jsonString = context.assets.open("cocktails.json")
            .bufferedReader()
            .use { it.readText() }

        cocktails = json.decodeFromString<CocktailsData>(jsonString).cocktails
        return cocktails
    }

    override fun getCocktail(id: String): Cocktail? {
        return loadCocktails().find { it.id == id }
    }

    override fun getSpirits(): List<String> = loadCocktails().map { it.spirit }.distinct().sorted()
    override fun getCategories(): List<String> = loadCocktails().map { it.category }.distinct().sorted()
    override fun getDifficulties(): List<String> = loadCocktails().map { it.difficulty }.distinct()
}
