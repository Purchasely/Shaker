package com.purchasely.shaker.ui.screen.favorites

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.ui.components.CocktailImage
import io.purchasely.ext.PLYPresentationType
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import org.koin.androidx.compose.koinViewModel

@Composable
fun FavoritesScreen(
    onCocktailClick: (String) -> Unit,
    viewModel: FavoritesViewModel = koinViewModel()
) {
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val favorites = viewModel.getFavoriteCocktails()
    val context = LocalContext.current

    if (favorites.isEmpty()) {
        // Empty state
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No favorites yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tap the heart icon on a cocktail to save it here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                if (!isPremium) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            val activity = context as? Activity ?: return@Button
                            Purchasely.fetchPresentation("favorites") { presentation, error ->
                                if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
                                    presentation.display(activity) { result, plan ->
                                        when (result) {
                                            PLYProductViewResult.PURCHASED,
                                            PLYProductViewResult.RESTORED -> {
                                                Log.d("FavoritesScreen", "[Shaker] Purchased/Restored: ${plan?.name}")
                                                viewModel.onPaywallDismissed()
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text("Unlock Favorites")
                    }
                }
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favorites, key = { it.id }) { cocktail ->
                FavoriteCard(
                    cocktail = cocktail,
                    onClick = { onCocktailClick(cocktail.id) }
                )
            }
        }
    }
}

@Composable
private fun FavoriteCard(cocktail: Cocktail, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CocktailImage(
                cocktail = cocktail,
                modifier = Modifier
                    .size(80.dp)
                    .clip(MaterialTheme.shapes.medium)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cocktail.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = cocktail.category.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
