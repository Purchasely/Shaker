package com.purchasely.shaker.ui.screen.home

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.purchasely.shaker.domain.model.Cocktail
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCocktailClick: (String) -> Unit,
    onFilterClick: () -> Unit = {},
    viewModel: HomeViewModel = koinViewModel()
) {
    val cocktails by viewModel.cocktails.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

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
                        IconButton(onClick = onFilterClick) {
                            Icon(Icons.Default.Tune, contentDescription = "Filters")
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
            AsyncImage(
                model = "file:///android_asset/images/${cocktail.image.replace(".webp", ".svg")}",
                contentDescription = cocktail.name,
                contentScale = ContentScale.Crop,
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
