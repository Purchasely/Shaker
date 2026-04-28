package com.purchasely.shaker.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.purchasely.shaker.domain.repository.OnboardingRepository
import com.purchasely.shaker.ui.screen.detail.DetailScreen
import com.purchasely.shaker.ui.screen.favorites.FavoritesScreen
import com.purchasely.shaker.ui.screen.home.HomeScreen
import com.purchasely.shaker.ui.screen.mixing.MixingScreen
import com.purchasely.shaker.ui.screen.onboarding.OnboardingScreen
import com.purchasely.shaker.ui.screen.settings.SettingsScreen
import com.purchasely.shaker.ui.theme.Shaker
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable data object Home
@Serializable data object Favorites
@Serializable data object Settings
@Serializable data class Detail(val cocktailId: String)
@Serializable data class Mixing(val cocktailId: String)

private data class BottomNavItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val id: String,
)

private val bottomNavItems = listOf(
    BottomNavItem(Home, "Home", Icons.Filled.Home, Icons.Outlined.Home, "home"),
    BottomNavItem(Favorites, "Favorites", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, "favorites"),
    BottomNavItem(Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "settings"),
)

@Composable
fun ShakerNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val onboardingRepository: OnboardingRepository = koinInject()
    var showSplash by remember { mutableStateOf(true) }
    val isFirstLaunch = remember { !onboardingRepository.isOnboardingCompleted }

    if (showSplash) {
        OnboardingScreen(
            showOnboarding = isFirstLaunch,
            onComplete = {
                onboardingRepository.isOnboardingCompleted = true
                showSplash = false
            },
        )
        return
    }

    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ShakerBottomBar(
                    currentDestination = currentDestination,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
        containerColor = Shaker.tokens.bg,
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable<Home> {
                HomeScreen(onCocktailClick = { id -> navController.navigate(Detail(id)) })
            }
            composable<Favorites> {
                FavoritesScreen(onCocktailClick = { id -> navController.navigate(Detail(id)) })
            }
            composable<Settings> { SettingsScreen() }
            composable<Detail> { entry ->
                val d: Detail = entry.toRoute()
                DetailScreen(
                    cocktailId = d.cocktailId,
                    onBack = { navController.popBackStack() },
                    onStartMixing = { id -> navController.navigate(Mixing(id)) },
                )
            }
            composable<Mixing> { entry ->
                val m: Mixing = entry.toRoute()
                MixingScreen(
                    cocktailId = m.cocktailId,
                    onExit = { navController.popBackStack() },
                )
            }
        }
    }
}

@Composable
private fun ShakerBottomBar(
    currentDestination: androidx.navigation.NavDestination?,
    onNavigate: (Any) -> Unit,
) {
    val tokens = Shaker.tokens
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (tokens.dark) tokens.bgCard else tokens.bgSubtle)
            .navigationBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            bottomNavItems.forEach { item ->
                val selected = currentDestination?.hasRoute(item.route::class) == true
                val activeColor = if (tokens.dark) androidx.compose.ui.graphics.Color.White else tokens.indigo
                Column(
                    modifier = Modifier.clickable { if (!selected) onNavigate(item.route) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (selected) tokens.indigoSoft
                                else androidx.compose.ui.graphics.Color.Transparent
                            )
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.label,
                            tint = if (selected) activeColor else tokens.textSec,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.label,
                        color = if (selected) activeColor else tokens.textSec,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
        }
    }
}

