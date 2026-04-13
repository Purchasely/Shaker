# Purchasely Wrapper + Observer Purchase Refactor — Android

**Date:** 2026-04-10
**Status:** Approved
**Scope:** Android only (iOS follows after validation)

---

## Problem

Responsibilities are scattered across `ShakerApp`, `PurchaselyWrapper`, and `PurchaseManager`:

- `ShakerApp.initPurchasely()` handles SDK initialization, event listener setup, AND the entire paywall actions interceptor (LOGIN, NAVIGATE, PURCHASE, RESTORE)
- `PurchaselyWrapper` wraps SDK calls but does not manage init or the interceptor
- `PurchaseManager` handles Google Play Billing but is coupled to `PurchaselyWrapper` (imports it, calls `synchronize()` directly)
- `PremiumManager` is called ad-hoc from multiple places after purchase/restore

This makes the code hard to test, tightly coupled, and difficult to evolve (e.g., removing Purchasely would require touching PurchaseManager).

## Goals

1. `PurchaselyWrapper` becomes the single orchestrator — owns init, interceptor, and purchase flow coordination
2. `PurchaseManager` becomes a pure billing service — zero Purchasely imports, communicates via reactive flows
3. `ShakerApp` becomes trivial — just Koin init + `wrapper.initialize()`
4. Full decoupling: PurchaseManager can work without Purchasely (future-proof for SDK removal)

## Design Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Wrapper ↔ PurchaseManager coupling | Reactive flows (SharedFlow) | Total decoupling, PurchaseManager has zero Purchasely knowledge |
| Actions via flow mechanism | PURCHASE and RESTORE only | LOGIN/NAVIGATE are simple UI actions, no billing needed |
| synchronize() caller | PurchaselyWrapper observes TransactionResult | PurchaseManager stays pure billing |
| processAction callback storage | Single pending property | Only one purchase at a time from a paywall |
| Application reference | Passed via `initialize(application, config)` | Explicit, no DI magic |
| PremiumManager access | Injected into PurchaselyWrapper | Wrapper is the central point, avoids callback boilerplate |

---

## Architecture

### Reactive Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    PurchaselyWrapper                         │
│                                                             │
│  Interceptor receives PURCHASE (Observer mode)              │
│    → stores processAction in pendingProcessAction           │
│    → emits PurchaseRequest into _purchaseRequests flow      │
│                                                             │
│  Interceptor receives RESTORE (Observer mode)               │
│    → stores processAction in pendingProcessAction           │
│    → emits RestoreRequest into _restoreRequests flow        │
│                                                             │
│  Collects transactionResult:                                │
│    Success → synchronize() → pendingProcessAction(false)    │
│              → premiumManager.refreshPremiumStatus()         │
│    Cancelled/Error → pendingProcessAction(false)            │
│    Idle → ignore                                            │
└──────────┬──────────────────────────────────┬───────────────┘
           │ PurchaseRequest                  │ RestoreRequest
           ▼                                  ▼
┌─────────────────────────────────────────────────────────────┐
│                    PurchaseManager                           │
│              (zero Purchasely imports)                       │
│                                                             │
│  Collects purchaseRequests:                                 │
│    → queryProductDetails → launchBillingFlow                │
│                                                             │
│  Collects restoreRequests:                                  │
│    → queryPurchases → acknowledgePurchase                   │
│                                                             │
│  onPurchasesUpdated / query results:                        │
│    → emits TransactionResult into _transactionResult flow   │
└─────────────────────────────────────────────────────────────┘
```

### New Types

All in `data/purchase/`:

```kotlin
// PurchaseRequest.kt
data class PurchaseRequest(
    val activity: Activity,
    val productId: String,
    val offerToken: String
)

// RestoreRequest.kt
data object RestoreRequest

// TransactionResult.kt
sealed class TransactionResult {
    data object Success : TransactionResult()
    data object Cancelled : TransactionResult()
    data class Error(val message: String?) : TransactionResult()
    data object Idle : TransactionResult()  // initial state / reset after consumption
}
```

`Idle` resets the state after consumption to prevent stale results being re-read by new collectors.

### PurchaseManager (refactored)

```kotlin
class PurchaseManager(
    context: Context,
    purchaseRequests: SharedFlow<PurchaseRequest>,
    restoreRequests: SharedFlow<RestoreRequest>,
    scope: CoroutineScope
) : PurchasesUpdatedListener {

    private val _transactionResult = MutableSharedFlow<TransactionResult>(replay = 1)
    val transactionResult: SharedFlow<TransactionResult> = _transactionResult.asSharedFlow()

    // BillingClient setup (unchanged)
    // init { connectBillingClient(); collectFlows(scope) }

    // Collects purchaseRequests → queryProductDetails → launchBillingFlow
    // Collects restoreRequests → queryPurchases → acknowledge

    // onPurchasesUpdated → emits TransactionResult (no synchronize, no wrapper call)
}
```

**Removed:** `purchaselyWrapper` dependency, `onPurchaseResult` callback, `synchronize()` call.

### PurchaselyWrapper (refactored)

```kotlin
class PurchaselyWrapper(
    private val premiumManager: PremiumManager,
    private val runningModeRepo: RunningModeRepository,
    private val purchaseRequests: MutableSharedFlow<PurchaseRequest>,
    private val restoreRequests: MutableSharedFlow<RestoreRequest>,
    private val transactionResult: SharedFlow<TransactionResult>,
    private val scope: CoroutineScope
) {
    private var application: Application? = null
    private var pendingProcessAction: ((Boolean) -> Unit)? = null

    fun initialize(
        application: Application,
        apiKey: String,
        logLevel: LogLevel = LogLevel.DEBUG
    ) {
        this.application = application
        val mode = runningModeRepo.runningMode
        // Build and start SDK
        // Configure event listener
        // Configure paywall actions interceptor (internal)
        // Start collecting transactionResult
    }

    fun restart() {
        close()
        application?.let { initialize(it, ...) }
    }

    // All existing methods remain: loadPresentation, display, getView,
    // userLogin, userLogout, setUserAttribute, incrementUserAttribute,
    // restoreAllProducts, synchronize, revokeDataProcessingConsent, etc.
}
```

**Internal interceptor logic:**

| Action | Observer mode | Full mode |
|--------|--------------|-----------|
| PURCHASE | Store processAction, emit PurchaseRequest | proceed(true) |
| RESTORE | Store processAction, emit RestoreRequest | proceed(true) |
| LOGIN | proceed(false) | proceed(false) |
| NAVIGATE | Open URL via application.startActivity(), proceed(false) | Same |
| Other | proceed(true) | proceed(true) |

**TransactionResult observation:**

| Result | Actions |
|--------|---------|
| Success | synchronize() → pendingProcessAction(false) → premiumManager.refreshPremiumStatus() |
| Cancelled | pendingProcessAction(false) |
| Error | pendingProcessAction(false) |
| Idle | ignore |

### ShakerApp (simplified)

```kotlin
class ShakerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@ShakerApp)
            modules(appModule)
        }

        val wrapper: PurchaselyWrapper by inject()
        wrapper.initialize(
            application = this,
            apiKey = BuildConfig.PURCHASELY_API_KEY,
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN
        )
    }
}
```

**Removed:** `initPurchasely()`, `restartPurchaselySdk()`, `getSdkModeFromStorage()`, event listener, interceptor.

### DI Module (Koin)

```kotlin
val appModule = module {
    // Shared flows
    single { MutableSharedFlow<PurchaseRequest>() }
    single { MutableSharedFlow<RestoreRequest>() }

    // Repositories
    single { CocktailRepository(androidContext()) }
    single { FavoritesRepository(androidContext()) }
    single { OnboardingRepository(androidContext()) }
    single { RunningModeRepository(androidContext()) }

    // Managers
    single { PremiumManager() }
    single {
        PurchaseManager(
            context = androidContext(),
            purchaseRequests = get(),
            restoreRequests = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }
    single {
        PurchaselyWrapper(
            premiumManager = get(),
            runningModeRepo = get(),
            purchaseRequests = get(),
            restoreRequests = get(),
            transactionResult = get<PurchaseManager>().transactionResult,
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        )
    }

    // ViewModels
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { params -> DetailViewModel(get(), get(), get(), get(), params.get()) }
    viewModel { FavoritesViewModel(get(), get(), get(), get()) }
    viewModel { SettingsViewModel(androidContext(), get(), get(), get()) }
}
```

### SettingsViewModel Change

`restartPurchaselySdk` currently casts context to ShakerApp:
```kotlin
val app = context.applicationContext as? ShakerApp
app?.restartPurchaselySdk()
```

Becomes:
```kotlin
purchaselyWrapper.restart()
```

The ViewModel already has a reference to `purchaselyWrapper`.

---

## Files Impacted

| File | Action |
|------|--------|
| `data/purchase/PurchaseRequest.kt` | **New** |
| `data/purchase/RestoreRequest.kt` | **New** |
| `data/purchase/TransactionResult.kt` | **New** |
| `data/purchase/PurchaseManager.kt` | **Refactor** — remove wrapper dependency, observe flows, emit TransactionResult |
| `purchasely/PurchaselyWrapper.kt` | **Refactor** — DI constructor, absorb init + interceptor, emit requests, observe results |
| `ShakerApp.kt` | **Simplify** — Koin init + wrapper.initialize() only |
| `di/AppModule.kt` | **Modify** — new DI graph with shared flows |
| `ui/screen/settings/SettingsViewModel.kt` | **Minor** — restartPurchaselySdk → wrapper.restart() |

### Test Files

| File | Action |
|------|--------|
| `purchasely/PurchaselyWrapperTest.kt` | **Refactor** — test orchestration (interceptor → flow → synchronize → processAction) |
| `ui/screen/settings/SettingsViewModelTest.kt` | **Minor** — adapt restart test |
| `data/purchase/PurchaseManagerTest.kt` | **New** — test flow collection, billing, TransactionResult emission |

### No Impact

Screens (Compose), HomeViewModel, DetailViewModel, FavoritesViewModel, all repositories, models, FetchResult, DisplayResult.

---

## Out of Scope

- iOS implementation (follows after Android validation)
- Updating `docs/purchasely-best-practices.md` (after implementation)
- PremiumManager refactoring (stays as-is, called by wrapper)
