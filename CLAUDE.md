# Shaker - Purchasely SDK Sample App

## About

Shaker is a **reference demo application** showcasing the best way to integrate the Purchasely SDK on iOS and Android. It serves as the canonical example for SDK integration patterns — all code must follow the best practices defined in `docs/purchasely-best-practices.md`. It contains both an Android app (Kotlin/Jetpack Compose) and an iOS app (SwiftUI), plus shared assets.

## Repository Structure

```
Shaker/
├── shared-assets/           # Shared cocktail data and images
│   ├── cocktails.json       # 25 cocktails with full data
│   └── images/              # Placeholder SVG images
├── android/                 # Android app (Kotlin/Jetpack Compose)
│   ├── app/src/main/java/com/purchasely/shaker/
│   │   ├── data/            # *Impl repositories, PremiumManagerImpl, storage/KeyValueStore
│   │   ├── di/              # Koin DI modules (AppModule)
│   │   ├── domain/          # model/ (Cocktail, Ingredient), repository/ (interfaces), usecase/
│   │   ├── purchasely/      # PurchaselyWrapper, PresentationHandle, EmbeddedScreenBanner, result types
│   │   └── ui/              # Compose UI (screens, navigation, theme, components)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── ios/                     # iOS app (SwiftUI)
    ├── Shaker/
    │   ├── Data/            # CocktailRepository, FavoritesRepository, PremiumManager, OnboardingRepository
    │   ├── Helpers/         # ViewControllerResolver, CocktailImage
    │   ├── Model/           # Cocktail, Ingredient
    │   ├── Purchasely/      # PurchaselyWrapper, EmbeddedScreenBanner, FetchResult, DisplayResult
    │   └── Screens/         # Home, Detail, Favorites, Settings, Onboarding
    └── project.yml          # XcodeGen spec (declares SPM dependency on Purchasely)
```

## Build Commands

### Android

```bash
cd android
./gradlew :app:assembleDebug
```

**API key setup**: Copy `local.properties.example` to `local.properties` and set `purchasely.apiKey=YOUR_KEY`.

### iOS

```bash
cd ios
xcodegen generate
open Shaker.xcodeproj
# Build with Xcode (Cmd+B)
```

**Dependencies**: Purchasely SDK is integrated via Swift Package Manager (declared in `project.yml`). Xcode resolves packages automatically on first open.

**API key setup**: Copy `Config.xcconfig.example` to `Config.xcconfig` and set `PURCHASELY_API_KEY=YOUR_KEY`.

**Important**: After adding/removing Swift files, run `xcodegen generate` to regenerate the Xcode project.

## Purchasely SDK Documentation

**When implementing Purchasely SDK features, always refer to the official platform docs:**

| Platform | Reference | Description |
|----------|-----------|-------------|
| Android/Kotlin | https://docs.purchasely.com/quick-start/android | Complete Android SDK integration guide |
| iOS/Swift | https://docs.purchasely.com/quick-start/ios | Complete iOS SDK integration guide |

In-repo, see also [`docs/INTEGRATION_GUIDE.md`](docs/INTEGRATION_GUIDE.md) and [`docs/purchasely-best-practices.md`](docs/purchasely-best-practices.md).

### Verified SDK APIs (v6, from actual SDK inspection)

**Android SDK 6.0.0-beta2 (mavenLocal, from ../Android fix/presentation_builder):**
- Init DSL: `Purchasely { context(...); apiKey(...); runningMode(...); stores(...); onInitialized { error -> } }`
- Presentation: `PLYPresentation { placementId("...") }` → `prepared.preload()` (suspend, throws) →
  `presentation.display(activity)` (non-suspend, returns `PLYPresentationSession`; `session.await()`
  suspends until dismissal and returns `PLYPresentationOutcome`)
- Interceptors per action: `Purchasely.interceptAction<PLYPresentationAction.Purchase> { info, purchase -> PLYInterceptResult }`
  (SUCCESS / FAILED / NOT_HANDLED)
- `userSubscriptions(invalidateCache: Boolean, listener: SubscriptionsListener)`;
  `subscriptionData.data.subscriptionStatus?.isExpired()` (nullable + function)
- `setUserAttribute(key, value)` / `incrementUserAttribute(key)`

**iOS SDK v6 (local SPM package → ../.worktrees/ios-develop):**
- Init builder: `Purchasely.apiKey("...").appUserId(...).runningMode(...).logLevel(...).start { error in }`
  — ⚠️ v6 default runningMode is `.observer` (was `.full` in v5): always set it explicitly
- Presentation: `PLYPresentationBuilder.from(placementId:).contentId(...).onClose{}.onDismissed{outcome}`
  → `.build().preload { presentation, error in }` → `presentation.display(from: viewController?)`
- `PLYPresentation` is a protocol (`any PLYPresentation`); lifecycle callbacks (`onPresented`,
  `onClose`, `onDismissed`) are mutable on the loaded presentation
- Interceptors per action: `Purchasely.interceptAction(.purchase) { info, params in ... return .success }`
  (async handler variant available; `.success` / `.failed` / `.notHandled`)
- `userSubscriptions(success:, failure:)` — separate closures; `PLYSubscription.status` enum:
  `.autoRenewing`, `.onHold`, `.inGracePeriod`, `.autoRenewingCanceled`, `.deactivated`, …
- `setUserAttribute(withStringValue:, forKey:)` / `incrementUserAttribute(withKey:)`
- `restoreAllProducts(success:, failure:)`; `synchronize(success:, failure:)`
- `onChange(of:)` with `{ _, newValue }` requires iOS 17; use `{ newValue }` for iOS 16

## Architecture

- **Android**: Clean Architecture (domain/data/ui) + MVVM + Koin DI + Jetpack Compose + NavHost (@Serializable routes) + kotlinx.serialization
- **iOS**: MVVM + SwiftUI + NavigationStack + Codable

### Data Flow

```
cocktails.json → CocktailRepository → ViewModel (StateFlow/Published) → Composable/View
                                          ↕
                              Purchasely SDK (entitlements, paywalls)
```

### Key Components

- **PurchaselyWrapper**: singleton wrapping all Purchasely SDK calls. ViewModels use this exclusively — on BOTH platforms, only the `purchasely/` package may import the SDK. See `docs/purchasely-best-practices.md`.
- **PresentationHandle** (both platforms): opaque wrapper around the loaded presentation (`@JvmInline value class` on Android, struct on iOS). ViewModels hold this handle — SDK type never leaks to the UI layer.
- **SubscriptionInfo / ConsentPurpose / PurchaselySdkMode** (both platforms): SDK-free boundary types mapped inside the wrapper.
- **CocktailMood** (both platforms): mood-discovery enum (same keys/labels/emojis/tags on Android & iOS) reported as the `preferred_mood` user attribute; "Surprise me" increments `surprise_me_count`.
- **EmbeddedScreenBanner**: Reusable Composable for inline paywall display. Uses PurchaselyWrapper internally.
- **PremiumManager** / **PremiumManagerImpl** (Android): Implements `PremiumRepository` interface. Injects `PurchaselyWrapper` (no direct SDK calls). Wired to `onTransactionCompleted` callback in AppModule.
- **CocktailRepository** / **CocktailRepositoryImpl**: Loads `cocktails.json` from bundled assets. Single source of truth for cocktail data.
- **FavoritesRepository** / **FavoritesRepositoryImpl**: Backed by `KeyValueStore` (Android) / UserDefaults (iOS). Premium-gated feature.
- **OnboardingRepository** / **OnboardingRepositoryImpl**: Tracks whether onboarding has been shown. Backed by `KeyValueStore` / UserDefaults.
- **SettingsRepository** (Android): Extracts all persistent settings (userId, theme, displayMode, consent) from `SettingsViewModel` into a repository backed by `KeyValueStore`.
- **KeyValueStore** (Android): Interface abstracting SharedPreferences. `SharedPreferencesKeyValueStore` in production, `InMemoryKeyValueStore` in tests — no Android framework needed in unit tests.
- **OnboardingViewModel** (Android): Owns onboarding paywall logic (fetch + display). `OnboardingScreen` is a thin UI shell.
- **GetFilteredCocktailsUseCase / ToggleFavoriteUseCase** (Android): Domain-layer UseCases — filtering and favorites logic extracted from ViewModels, independently testable.
- **CocktailImage**: Native placeholder image component (spirit-based colors). No external image loading library needed.
- **RunningModeRepository**: Persists Full/Observer mode choice. Toggle in Settings re-initializes SDK.
- **PurchaseManager**: Native purchase handling (StoreKit 2 / Google Play Billing) used only in Observer mode. Calls `synchronize()` after every transaction.

## Conventions

- Package: `com.purchasely.shaker` (Android) / Shaker (iOS)
- Entitlement: `SHAKER_PREMIUM`
- Placements: `onboarding`, `recipe_detail`, `favorites`, `filters`
- Shared data in `shared-assets/` - copied to platform asset dirs
- API keys via local config files (never committed)
- **Purchasely best practices**: All SDK integration changes must follow `docs/purchasely-best-practices.md`. Update the doc when patterns change.

## Gotchas

- `settings.gradle.kts`: Use `dependencyResolutionManagement` (not `dependencyResolution`)
- XcodeGen + SPM: declare packages under top-level `packages:` and reference them in target `dependencies: - package: Purchasely` — Xcode resolves automatically
- iOS `onChange(of:)` with `{ _, newValue }` requires iOS 17 - use `{ newValue }` for iOS 16 compat
- SVGs in iOS asset catalog can fail - use native `CocktailImage` view instead
- iOS `Purchasely.synchronize()` requires `success:` and `failure:` closures — Android version is parameterless
- Android `PLYPlan` uses `store_product_id` (not `productId`) to get the Google Play product ID

## History

For past architecture decisions (Android refactor 2026-04), see [`android/docs/plans/`](android/docs/plans/).
