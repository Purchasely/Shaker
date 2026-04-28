package com.purchasely.shaker.ui.screen.home

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.EmbeddedScreenBanner
import com.purchasely.shaker.purchasely.FetchResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.components.CocktailArt
import com.purchasely.shaker.ui.components.ShakerLogo
import com.purchasely.shaker.ui.theme.Shaker
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

private val categoryChips = listOf("All", "classic", "non-alcoholic", "tropical", "easy")
private val categoryLabels = mapOf(
    "All" to "All",
    "classic" to "Classic",
    "non-alcoholic" to "Non-alcoholic",
    "tropical" to "Tropical",
    "easy" to "Easy",
)

@Composable
fun HomeScreen(
    onCocktailClick: (String) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val tokens = Shaker.tokens
    val cocktails by viewModel.cocktails.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val inlinePresentation by viewModel.inlinePresentation.collectAsStateWithLifecycle()
    val selectedCategories by viewModel.selectedCategories.collectAsStateWithLifecycle()
    val selectedSpirits by viewModel.selectedSpirits.collectAsStateWithLifecycle()
    val selectedDifficulty by viewModel.selectedDifficulty.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()
    var showFilterSheet by remember { mutableStateOf(false) }

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

    val activeCat = when {
        selectedDifficulty == "easy" -> "easy"
        selectedSpirits.contains("non-alcoholic") -> "non-alcoholic"
        selectedCategories.isNotEmpty() -> selectedCategories.first()
        else -> "All"
    }

    val onCatSelect: (String) -> Unit = { cat ->
        when (cat) {
            "All" -> viewModel.clearFilters()
            "easy" -> {
                viewModel.clearFilters()
                viewModel.selectDifficulty("easy")
            }
            "non-alcoholic" -> {
                viewModel.clearFilters()
                viewModel.toggleSpirit("non-alcoholic")
            }
            else -> {
                viewModel.clearFilters()
                viewModel.toggleCategory(cat)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(tokens.bg)
            .statusBarsPadding(),
    ) {
        HomeHeader(isPremium = isPremium)
        SearchBar(onFilters = {
            if (isPremium) showFilterSheet = true else viewModel.onFilterClick()
        })
        Spacer(Modifier.height(12.dp))

        val gridState = rememberLazyGridState()
        LaunchedEffect(inlinePresentation) {
            if (inlinePresentation is FetchResult.Success) gridState.animateScrollToItem(0)
        }

        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            val inlineResult = inlinePresentation
            if (!isPremium && inlineResult is FetchResult.Success) {
                item(span = { GridItemSpan(2) }) {
                    val mod = if (inlineResult.height > 0) {
                        Modifier.height(inlineResult.height.dp)
                    } else {
                        Modifier.heightIn(max = 200.dp)
                    }
                    EmbeddedScreenBanner(
                        fetchResult = inlineResult,
                        onResult = { viewModel.onPaywallDismissed() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(mod)
                            .padding(bottom = 4.dp),
                    )
                }
            }

            item(span = { GridItemSpan(2) }) {
                CategoryChipsRow(active = activeCat, onSelect = onCatSelect)
            }

            val showHero = activeCat == "All" && cocktails.isNotEmpty()
            if (showHero) {
                val hero = cocktails.firstOrNull { it.id == "manhattan" } ?: cocktails.first()
                item(span = { GridItemSpan(2) }) {
                    TonightsPickCard(cocktail = hero, onClick = { onCocktailClick(hero.id) })
                }
            }

            items(cocktails, key = { it.id }) { cocktail ->
                CocktailCard(
                    cocktail = cocktail,
                    onClick = { onCocktailClick(cocktail.id) },
                )
            }

            item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(12.dp)) }
        }
    }

    if (showFilterSheet) {
        FilterSheet(viewModel = viewModel, onDismiss = { showFilterSheet = false })
    }
}

@Composable
private fun HomeHeader(isPremium: Boolean) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShakerLogo(size = 28.dp, color = tokens.indigoText)
        Spacer(Modifier.size(10.dp))
        Text(
            "Shaker",
            color = tokens.indigoText,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(if (isPremium) tokens.goldSoft else tokens.indigoSoft)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                if (isPremium) "PRO" else "FREE",
                color = tokens.indigoText,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun SearchBar(onFilters: () -> Unit) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(44.dp)
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.inputBg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = tokens.textSec, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text("Search cocktails…", color = tokens.textSec, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Icon(
            Icons.Default.Tune,
            contentDescription = null,
            tint = tokens.textSec,
            modifier = Modifier
                .size(18.dp)
                .clickable { onFilters() },
        )
    }
}

@Composable
private fun CategoryChipsRow(active: String, onSelect: (String) -> Unit) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categoryChips.forEach { c ->
            val selected = c == active
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .then(
                        if (selected) Modifier.background(tokens.indigo)
                        else Modifier
                            .background(tokens.bgCard)
                            .border(1.dp, tokens.hair, RoundedCornerShape(100.dp))
                    )
                    .clickable { onSelect(c) }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(
                    categoryLabels[c] ?: c.replaceFirstChar { it.uppercase() },
                    color = if (selected) tokens.onIndigo else tokens.text,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun TonightsPickCard(cocktail: Cocktail, onClick: () -> Unit) {
    val tokens = Shaker.tokens
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        CocktailArt(cocktail = cocktail, modifier = Modifier.fillMaxSize())
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xBF0F1020)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY,
                    )
                ),
        )
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(Color.White.copy(alpha = 0.9f))
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .align(Alignment.TopStart),
        ) {
            Text(
                "✦ TONIGHT'S PICK",
                color = tokens.indigoText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(cocktail.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(
                cocktail.description,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CocktailCard(
    cocktail: Cocktail,
    onClick: () -> Unit,
) {
    val tokens = Shaker.tokens
    val premiumBadge = cocktail.tags.contains("premium")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(tokens.bgCard)
            .border(1.dp, tokens.hair, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
    ) {
        Column {
            Box(modifier = Modifier.aspectRatio(1f)) {
                CocktailArt(cocktail = cocktail, modifier = Modifier.fillMaxSize())
                if (premiumBadge) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xB80F1020))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFFD572), modifier = Modifier.size(10.dp))
                        Spacer(Modifier.size(4.dp))
                        Text("PRO", color = Color(0xFFFFD572), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Text(cocktail.name, color = tokens.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(
                    "${cocktail.category.replaceFirstChar { it.uppercase() }} · ${cocktail.difficulty.replaceFirstChar { it.uppercase() }}",
                    color = tokens.textSec,
                    fontSize = 12.sp,
                )
            }
        }
    }
}
