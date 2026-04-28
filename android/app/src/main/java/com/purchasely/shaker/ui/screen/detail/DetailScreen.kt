package com.purchasely.shaker.ui.screen.detail

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.purchasely.shaker.domain.model.Cocktail
import com.purchasely.shaker.domain.model.Ingredient
import com.purchasely.shaker.purchasely.DisplayResult
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.components.CocktailArt
import com.purchasely.shaker.ui.theme.Shaker
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun DetailScreen(
    cocktailId: String,
    onBack: () -> Unit,
    onStartMixing: (String) -> Unit,
    viewModel: DetailViewModel = koinViewModel { parametersOf(cocktailId) },
) {
    val tokens = Shaker.tokens
    val cocktail by viewModel.cocktail.collectAsStateWithLifecycle()
    val isPremium by viewModel.isPremium.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isFavorite = favoriteIds.contains(cocktailId)
    val context = LocalContext.current
    val purchaselyWrapper: PurchaselyWrapper = koinInject()

    val view = LocalView.current
    DisposableEffect(Unit) {
        val activity = context as? Activity ?: return@DisposableEffect onDispose {}
        val controller = WindowCompat.getInsetsController(activity.window, view)
        controller.isAppearanceLightStatusBars = false
        onDispose { controller.isAppearanceLightStatusBars = !tokens.dark }
    }

    LaunchedEffect(Unit) {
        viewModel.requestRecipePaywall.collect { handle ->
            val activity = context as? Activity ?: return@collect
            val result = purchaselyWrapper.display(handle, activity)
            when (result) {
                is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPaywallDismissed()
                else -> {}
            }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.requestFavoritesPaywall.collect { handle ->
            val activity = context as? Activity ?: return@collect
            val result = purchaselyWrapper.display(handle, activity)
            when (result) {
                is DisplayResult.Purchased, is DisplayResult.Restored -> viewModel.onPaywallDismissed()
                else -> {}
            }
        }
    }

    val c = cocktail ?: return
    val locked = !isPremium

    Box(modifier = Modifier.fillMaxSize().background(tokens.bg)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                CocktailArt(cocktail = c, modifier = Modifier.fillMaxSize())
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0x59000000), Color.Transparent)
                            )
                        )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RoundButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF1C1E2C))
                    }
                    Spacer(Modifier.weight(1f))
                    RoundButton(onClick = {
                        if (isPremium) viewModel.toggleFavorite() else viewModel.showFavoritesPaywall()
                    }) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isFavorite) tokens.danger else Color(0xFF1C1E2C),
                        )
                    }
                }
                if (locked) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 40.dp)
                            .clip(RoundedCornerShape(100.dp))
                            .background(Color(0xD11C1E2C))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Default.Lock, null, tint = Color(0xFFFFD572), modifier = Modifier.size(11.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("PRO RECIPE", color = Color(0xFFFFD572), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-28).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(tokens.bg)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(tokens.hairStrong)
                        .align(Alignment.CenterHorizontally),
                )
                Spacer(Modifier.height(16.dp))
                Text(c.name, color = tokens.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text(c.description, color = tokens.textSec, fontSize = 15.sp)
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(c.category.replaceFirstChar { it.uppercase() })
                    Chip(c.spirit.replaceFirstChar { it.uppercase() })
                    Chip(c.difficulty.replaceFirstChar { it.uppercase() })
                }

                Spacer(Modifier.height(24.dp))
                IngredientsSection(c.ingredients)

                Spacer(Modifier.height(24.dp))
                InstructionsSection(
                    steps = c.instructions,
                    locked = locked,
                    onUnlock = { viewModel.showRecipePaywall() },
                )

                if (!locked) {
                    Spacer(Modifier.height(20.dp))
                    ActionsRow(
                        isFavorite = isFavorite,
                        onStartMixing = { onStartMixing(c.id) },
                        onFav = { viewModel.toggleFavorite() },
                    )
                }
                Spacer(Modifier.height(60.dp))
            }
        }
    }
}

@Composable
private fun RoundButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color(0xD9FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun Chip(text: String) {
    val tokens = Shaker.tokens
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(tokens.bgCard)
            .border(1.dp, tokens.hair, RoundedCornerShape(100.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(text, color = tokens.text, fontSize = 13.sp)
    }
}

@Composable
private fun IngredientsSection(ingredients: List<Ingredient>) {
    val tokens = Shaker.tokens
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.fillMaxWidth()) {
        Text("Ingredients", color = tokens.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Text("${ingredients.size} items", color = tokens.textSec, fontSize = 13.sp)
    }
    Spacer(Modifier.height(12.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.bgCard)
            .border(1.dp, tokens.hair, RoundedCornerShape(16.dp)),
    ) {
        ingredients.forEachIndexed { i, ingr ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(tokens.indigoSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", color = tokens.indigoText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.size(12.dp))
                Text(ingr.name, color = tokens.text, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text(ingr.amount, color = tokens.textTer, fontSize = 13.sp)
            }
            if (i < ingredients.size - 1) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.hair))
            }
        }
    }
}

@Composable
private fun InstructionsSection(steps: List<String>, locked: Boolean, onUnlock: () -> Unit) {
    val tokens = Shaker.tokens
    Text("Instructions", color = tokens.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(12.dp))
    if (locked) {
        LockedInstructionsCard(stepsCount = steps.size, onUnlock = onUnlock)
    } else {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(tokens.bgCard)
                .border(1.dp, tokens.hair, RoundedCornerShape(16.dp)),
        ) {
            steps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(tokens.indigo),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${i + 1}", color = tokens.onIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.size(12.dp))
                    Text(step, color = tokens.text, fontSize = 15.sp)
                }
                if (i < steps.size - 1) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(tokens.hair))
                }
            }
        }
    }
}

@Composable
private fun LockedInstructionsCard(stepsCount: Int, onUnlock: () -> Unit) {
    val tokens = Shaker.tokens
    val bgA = if (tokens.dark) Color(0xFF1D2040) else Color(0xFF2D345F)
    val bgB = if (tokens.dark) Color(0xFF2A1D3F) else Color(0xFF3F2A55)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(bgA, bgB),
                    start = androidx.compose.ui.geometry.Offset.Zero,
                    end = androidx.compose.ui.geometry.Offset(1f, 1f),
                )
            )
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(Color(0x2EF5B93A))
                .border(1.5.dp, Color(0x66F5B93A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, null, tint = Color(0xFFF5B93A), modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text("Pro recipe", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Unlock Shaker Pro to see the $stepsCount-step guided recipe, plus 35+ more recipes.",
            color = Color(0xBFFFFFFF),
            fontSize = 14.sp,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("✦ 35+ recipes", color = Color(0x99FFFFFF), fontSize = 12.sp)
            Text("✦ Save unlimited", color = Color(0x99FFFFFF), fontSize = 12.sp)
        }
        Spacer(Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(Color(0xFFF5B93A))
                .clickable(onClick = onUnlock)
                .padding(horizontal = 28.dp, vertical = 14.dp),
        ) {
            Text(
                "Unlock Shaker Pro",
                color = Color(0xFF2A1D05),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun ActionsRow(isFavorite: Boolean, onStartMixing: () -> Unit, onFav: () -> Unit) {
    val tokens = Shaker.tokens
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(100.dp))
                .background(tokens.indigo)
                .clickable(onClick = onStartMixing),
            contentAlignment = Alignment.Center,
        ) {
            Text("Start mixing", color = tokens.onIndigo, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(100.dp))
                .border(1.5.dp, tokens.indigoText, RoundedCornerShape(100.dp))
                .clickable(onClick = onFav),
            contentAlignment = Alignment.Center,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = if (isFavorite) tokens.danger else tokens.indigoText,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.size(8.dp))
                Text(if (isFavorite) "Saved" else "Save", color = tokens.indigoText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// Used only for typing — keeps the file compilable when referenced elsewhere
@Suppress("UnusedPrivateProperty")
private val _unused: Cocktail? = null
