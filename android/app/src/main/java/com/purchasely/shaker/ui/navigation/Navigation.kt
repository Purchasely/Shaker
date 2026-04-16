package com.purchasely.shaker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
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
import com.purchasely.shaker.R
import com.purchasely.shaker.ui.screen.onboarding.OnboardingScreen
import com.purchasely.shaker.ui.screen.settings.SettingsScreen
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable data object Home
@Serializable data object Favorites
@Serializable data object Settings
@Serializable data class Detail(val cocktailId: String)

data class BottomNavItem(
    val route: Any,
    @StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Home, R.string.home, Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Favorites, R.string.favorites, Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Settings, R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings),
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
            }
        )
        return
    }

    val showBottomBar = currentDestination?.let { dest ->
        bottomNavItems.any { dest.hasRoute(it.route::class) }
    } ?: false

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hasRoute(item.route::class) == true
                        val label = stringResource(item.labelRes)
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = label
                                )
                            },
                            label = { Text(label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            composable<Home> {
                HomeScreen(
                    onCocktailClick = { cocktailId ->
                        navController.navigate(Detail(cocktailId))
                    }
                )
            }
            composable<Favorites> {
                FavoritesScreen(
                    onCocktailClick = { cocktailId ->
                        navController.navigate(Detail(cocktailId))
                    }
                )
            }
            composable<Settings> {
                SettingsScreen()
            }
            composable<Detail> { backStackEntry ->
                val detail: Detail = backStackEntry.toRoute()
                DetailScreen(
                    cocktailId = detail.cocktailId,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
