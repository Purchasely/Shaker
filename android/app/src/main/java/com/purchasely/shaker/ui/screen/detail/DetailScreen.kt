package com.purchasely.shaker.ui.screen.detail

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.ui.components.CocktailImage
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    cocktailId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(cocktailId) }
) {
    val cocktail by viewModel.cocktail.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isFavorite = favoriteIds.contains(cocktailId)
    val context = LocalContext.current

    // Force light (white) status bar icons over the hero image
    val view = LocalView.current
    DisposableEffect(Unit) {
        val window = (context as Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = false // white icons
        onDispose {
            controller.isAppearanceLightStatusBars = true // restore dark icons
        }
    }

    // Collect recipe paywall display requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestRecipePaywall.collect {
            val activity = context as? Activity ?: return@collect
            viewModel.displayPendingRecipePaywall(activity)
        }
    }

    // Collect favorites paywall display requests from ViewModel
    LaunchedEffect(Unit) {
        viewModel.requestFavoritesPaywall.collect {
            val activity = context as? Activity ?: return@collect
            viewModel.displayPendingFavoritesPaywall(activity)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        cocktail?.let { c ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero image — full bleed behind status bar, with top scrim
                Box {
                    CocktailImage(
                        cocktail = c,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(420.dp)
                    )
                    // Dark gradient scrim for status bar / nav bar readability
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                    // Name and description
                    Text(
                        text = c.name,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = c.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Badges
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(
                            onClick = {},
                            label = { Text(c.category.replaceFirstChar { it.uppercase() }) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(c.spirit.replaceFirstChar { it.uppercase() }) }
                        )
                        AssistChip(
                            onClick = {},
                            label = { Text(c.difficulty.replaceFirstChar { it.uppercase() }) }
                        )
                    }

                    // Ingredients
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Ingredients",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    c.ingredients.forEach { ingredient ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = ingredient.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = if (isPremium) ingredient.amount else "---",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isPremium) {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }
                    }

                    // Instructions
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Instructions",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (isPremium) {
                        c.instructions.forEachIndexed { index, instruction ->
                            Text(
                                text = "${index + 1}. $instruction",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    } else {
                        // Blurred instructions with unlock CTA
                        Box {
                            Column(
                                modifier = Modifier.blur(8.dp)
                            ) {
                                c.instructions.forEachIndexed { index, instruction ->
                                    Text(
                                        text = "${index + 1}. $instruction",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                }
                            }

                            // Gradient overlay + CTA
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                                                MaterialTheme.colorScheme.surface
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { viewModel.showRecipePaywall() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        Icons.Default.Lock,
                                        contentDescription = null,
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    Text("Unlock Full Recipe")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Transparent TopAppBar overlay — behind status bar
        TopAppBar(
            title = { },
            windowInsets = WindowInsets(0, 0, 0, 0),
            modifier = Modifier.statusBarsPadding(),
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = {
                    if (isPremium) {
                        viewModel.toggleFavorite()
                    } else {
                        viewModel.showFavoritesPaywall()
                    }
                }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                        tint = if (isFavorite) Color.Red else Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
    }
}
