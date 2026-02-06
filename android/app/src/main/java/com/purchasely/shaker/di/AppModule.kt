package com.purchasely.shaker.di

import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.ui.screen.home.HomeViewModel
import com.purchasely.shaker.ui.screen.detail.DetailViewModel
import com.purchasely.shaker.ui.screen.favorites.FavoritesViewModel
import com.purchasely.shaker.ui.screen.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { CocktailRepository(androidContext()) }
    viewModel { HomeViewModel(get()) }
    viewModel { params -> DetailViewModel(get(), params.get()) }
    viewModel { FavoritesViewModel(get()) }
    viewModel { SettingsViewModel() }
}
