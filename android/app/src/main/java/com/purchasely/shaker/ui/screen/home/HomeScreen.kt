package com.purchasely.shaker.ui.screen.home

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.ui.components.CocktailImage
import io.purchasely.ext.PLYPresentationProperties
import io.purchasely.ext.PLYProductViewResult
import io.purchasely.ext.Purchasely
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCocktailClick: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel()
) {
    val cocktails by viewModel.cocktails.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isPremium by viewModel.isPremium.collectAsState()
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = { Text("Search cocktails...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    trailingIcon = {
                        IconButton(onClick = {
                            if (isPremium) {
                                showFilterSheet = true
                            } else {
                                // Free user: show filters paywall
                                Purchasely.presentationView(
                                    context = context,
                                    properties = PLYPresentationProperties(
                                        placementId = "filters",
                                        onClose = { viewModel.onPaywallDismissed() }
                                    )
                                ) { result, plan ->
                                    when (result) {
                                        PLYProductViewResult.PURCHASED,
                                        PLYProductViewResult.RESTORED -> {
                                            Log.d("HomeScreen", "[Shaker] Purchased/Restored from filters: ${plan?.name}")
                                            viewModel.onPaywallDismissed()
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }) {
                            if (viewModel.hasActiveFilters) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(Icons.Default.Tune, contentDescription = "Filters")
                                }
                            } else {
                                Icon(Icons.Default.Tune, contentDescription = "Filters")
                            }
                        }
                    }
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {}

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(cocktails, key = { it.id }) { cocktail ->
                CocktailCard(cocktail = cocktail, onClick = { onCocktailClick(cocktail.id) })
            }
        }
    }

    if (showFilterSheet) {
        FilterSheet(
            viewModel = viewModel,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun CocktailCard(cocktail: Cocktail, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            CocktailImage(
                cocktail = cocktail,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.medium)
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = cocktail.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
