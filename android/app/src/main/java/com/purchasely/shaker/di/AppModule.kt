package com.purchasely.shaker.di

import com.purchasely.shaker.data.CocktailRepository
import com.purchasely.shaker.data.FavoritesRepository
import com.purchasely.shaker.data.OnboardingRepository
import com.purchasely.shaker.data.PremiumManager
import com.purchasely.shaker.data.RunningModeRepository
import com.purchasely.shaker.data.purchase.PurchaseRequest
import com.purchasely.shaker.data.purchase.RestoreRequest
import com.purchasely.shaker.data.purchase.TransactionResult
// import com.purchasely.shaker.data.purchase.PurchaseManager // TODO: Task 4 will re-enable
import com.purchasely.shaker.purchasely.PurchaselyWrapper
import com.purchasely.shaker.ui.screen.home.HomeViewModel
import com.purchasely.shaker.ui.screen.detail.DetailViewModel
import com.purchasely.shaker.ui.screen.favorites.FavoritesViewModel
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
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { RunningModeRepository(androidContext()) }
    single { PremiumManager() }
    // Reactive flows for purchase orchestration
    single(named("purchaseRequests")) { MutableSharedFlow<PurchaseRequest>() }
    single(named("restoreRequests")) { MutableSharedFlow<RestoreRequest>() }
    single(named("transactionResult")) { MutableSharedFlow<TransactionResult>() }
    single(named("appScope")) { CoroutineScope(SupervisorJob() + Dispatchers.Main) }
    // TODO: Task 4 will update PurchaseManager wiring with reactive flows
    // single { PurchaseManager(androidContext(), get()) }
    single {
        PurchaselyWrapper(
            premiumManager = get(),
            runningModeRepo = get(),
            purchaseRequests = get(named("purchaseRequests")),
            restoreRequests = get(named("restoreRequests")),
            transactionResult = get(named("transactionResult")),
            scope = get(named("appScope"))
        )
    }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
}
