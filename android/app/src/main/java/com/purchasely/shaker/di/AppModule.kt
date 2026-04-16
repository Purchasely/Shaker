package com.purchasely.shaker.di

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PendingPurchasesParams
import com.purchasely.shaker.data.CocktailRepositoryImpl
import com.purchasely.shaker.data.FavoritesRepositoryImpl
import com.purchasely.shaker.data.OnboardingRepositoryImpl
import com.purchasely.shaker.data.PremiumManagerImpl
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.SettingsRepository
import com.purchasely.shaker.domain.repository.CocktailRepository
import com.purchasely.shaker.domain.repository.FavoritesRepository
import com.purchasely.shaker.domain.repository.OnboardingRepository
import com.purchasely.shaker.domain.repository.PremiumRepository
import com.purchasely.shaker.data.purchase.PurchaseManager
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.storage.KeyValueStore
import com.purchasely.shaker.data.storage.SharedPreferencesKeyValueStore
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.screen.home.HomeViewModel
import com.purchasely.shaker.ui.screen.detail.DetailViewModel
import com.purchasely.shaker.ui.screen.favorites.FavoritesViewModel
import com.purchasely.shaker.ui.screen.onboarding.OnboardingViewModel
import com.purchasely.shaker.ui.screen.settings.SettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module

val appModule = module {
    single<CocktailRepository> { CocktailRepositoryImpl(androidContext()) }
    single(named("favorites")) {
        SharedPreferencesKeyValueStore(
            androidContext().getSharedPreferences("shaker_favorites", Context.MODE_PRIVATE)
        ) as KeyValueStore
    }
    single(named("onboarding")) {
        SharedPreferencesKeyValueStore(
            androidContext().getSharedPreferences("shaker_onboarding", Context.MODE_PRIVATE)
        ) as KeyValueStore
    }
    single(named("settings")) {
        SharedPreferencesKeyValueStore(
            androidContext().getSharedPreferences("shaker_settings", Context.MODE_PRIVATE)
        ) as KeyValueStore
    }
    single<FavoritesRepository> { FavoritesRepositoryImpl(get(named("favorites"))) }
    single<OnboardingRepository> { OnboardingRepositoryImpl(get(named("onboarding"))) }
    single { RunningModeRepository(get(named("settings"))) }
    single { SettingsRepository(get(named("settings"))) }
    // Reactive flows for purchase orchestration
    single(named("purchaseRequests")) { MutableSharedFlow<PurchaseRequest>() }
    single(named("restoreRequests")) { MutableSharedFlow<RestoreRequest>() }
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    single {
        PurchaseManager(
            billingClientFactory = { listener ->
                BillingClient.newBuilder(androidContext())
                    .setListener(listener)
                    .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                            .enableOneTimeProducts()
                            .build()
                    )
                    .build()
            },
            purchaseRequests = get<MutableSharedFlow<PurchaseRequest>>(named("purchaseRequests")),
            restoreRequests = get<MutableSharedFlow<RestoreRequest>>(named("restoreRequests")),
            scope = get(named("appScope"))
        )
    }
    single {
        PurchaselyWrapper(
            runningModeRepo = get(),
            purchaseRequests = get(named("purchaseRequests")),
            restoreRequests = get(named("restoreRequests")),
            transactionResult = get<PurchaseManager>().transactionResult,
            scope = get(named("appScope"))
        )
    }
    single<PremiumRepository> {
        PremiumManagerImpl(wrapper = get()).also { pm ->
            get<PurchaselyWrapper>().onTransactionCompleted = { pm.refreshPremiumStatus() }
        }
    }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(get<SettingsRepository>(), get(), get(), get()) }
}
