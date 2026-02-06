package com.purchasely.shaker.ui.screen.detail

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    cocktailId: String,
    onBack: () -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(cocktailId) }
) {
    val cocktail by viewModel.cocktail.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current
    var showPaywall by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(cocktail?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        cocktail?.let { c ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Hero image
                AsyncImage(
                    model = "file:///android_asset/images/${c.image.replace(".webp", ".svg")}",
                    contentDescription = c.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )

                Column(modifier = Modifier.padding(16.dp)) {
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
                                    onClick = {
                                        val view = Purchasely.presentationView(
                                            context = context,
                                            properties = PLYPresentationProperties(
                                                placementId = "recipe_detail",
                                                contentId = c.id,
                                                onClose = {
                                                    viewModel.onPaywallDismissed()
                                                }
                                            )
                                        ) { result, plan ->
                                            when (result) {
                                                PLYProductViewResult.PURCHASED -> {
                                                    Log.d("DetailScreen", "[Shaker] Purchased: ${plan?.name}")
                                                    viewModel.onPaywallDismissed()
                                                }
                                                PLYProductViewResult.RESTORED -> {
                                                    Log.d("DetailScreen", "[Shaker] Restored: ${plan?.name}")
                                                    viewModel.onPaywallDismissed()
                                                }
                                                PLYProductViewResult.CANCELLED -> {
                                                    Log.d("DetailScreen", "[Shaker] Cancelled")
                                                }
                                                else -> {}
                                            }
                                        }
                                        // The SDK displays the paywall automatically
                                    },
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
    }
}
