package com.purchasely.shaker.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.purchasely.shaker.domain.model.Cocktail

@Composable
fun CocktailImage(
    cocktail: Cocktail,
    modifier: Modifier = Modifier,
) {
    CocktailArt(cocktail = cocktail, modifier = modifier)
}
