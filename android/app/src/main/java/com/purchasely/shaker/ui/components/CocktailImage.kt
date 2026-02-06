package com.purchasely.shaker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.purchasely.shaker.domain.model.Cocktail

@Composable
fun CocktailImage(
    cocktail: Cocktail,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.background(spiritBackgroundColor(cocktail.spirit))) {
        AsyncImage(
            model = "file:///android_asset/images/${cocktail.image}",
            contentDescription = cocktail.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

private fun spiritBackgroundColor(spirit: String): Color = when (spirit.lowercase()) {
    "rum" -> Color(0xFFD4A574)
    "whiskey" -> Color(0xFFC68E17)
    "gin" -> Color(0xFF87CEEB)
    "tequila" -> Color(0xFF98FB98)
    "vodka" -> Color(0xFFE8E8E8)
    else -> Color(0xFFFFB6C1)
}
