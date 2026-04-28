package com.purchasely.shaker.ui.screen.favorites

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.components.CocktailArt
import com.purchasely.shaker.ui.theme.Shaker
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun FavoritesScreen(
    onCocktailClick: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel(),
) {
    val tokens = Shaker.tokens
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()

    LaunchedEffect(Unit) {
        viewModel.requestPaywallDisplay.collect { handle ->
            val activity = context as? Activity ?: return@collect
            val result = purchaselyWrapper.display(handle, activity)
            when (result) {
                is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPaywallDismissed()
                else -> {}
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(tokens.bg).statusBarsPadding(),
    ) {
        Text(
            "Favorites",
            color = tokens.text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        )
        if (favorites.isEmpty()) {
            EmptyState(isPremium = isPremium, onUnlock = { viewModel.showFavoritesPaywall() })
        } else {
            Text(
                "${favorites.size} saved",
                color = tokens.textSec,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 16.dp),
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(favorites, key = { it.id }) { cocktail ->
                    FavoriteCard(cocktail = cocktail, onClick = { onCocktailClick(cocktail.id) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isPremium: Boolean, onUnlock: () -> Unit) {
    val tokens = Shaker.tokens
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(tokens.indigoSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.FavoriteBorder,
                null,
                tint = tokens.indigoText,
                modifier = Modifier.size(52.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("No favorites yet", color = tokens.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap the heart on a card to save it here. Unlock Pro to save as many as you like.",
            color = tokens.textSec,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        if (!isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(52.dp)
                    .clip(RoundedCornerShape(100.dp))
                    .background(tokens.indigo)
                    .clickable(onClick = onUnlock),
                contentAlignment = Alignment.Center,
            ) {
                androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, null, tint = tokens.onIndigo, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("Unlock Favorites", color = tokens.onIndigo, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(cocktail: Cocktail, onClick: () -> Unit) {
    val tokens = Shaker.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.bgCard)
            .border(1.dp, tokens.hair, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.aspectRatio(1f)) {
            CocktailArt(cocktail = cocktail, modifier = Modifier.fillMaxSize())
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(cocktail.name, color = tokens.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                "${cocktail.category.replaceFirstChar { it.uppercase() }} · ${cocktail.difficulty.replaceFirstChar { it.uppercase() }}",
                color = tokens.textSec,
                fontSize = 12.sp,
            )
        }
    }
}
