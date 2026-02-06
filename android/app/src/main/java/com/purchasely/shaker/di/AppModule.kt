package com.purchasely.shaker.di

import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.OnboardingRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.ui.screen.home.HomeViewModel
import com.purchasely.shaker.ui.screen.detail.DetailViewModel
import com.purchasely.shaker.ui.screen.favorites.FavoritesViewModel
import com.purchasely.shaker.ui.screen.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { PremiumManager() }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get()) }
}
