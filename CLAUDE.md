# Shaker - Purchasely SDK Sample App

## About

Shaker is a cocktail discovery app demonstrating a production-quality Purchasely SDK integration. It contains both an Android app (Kotlin/Jetpack Compose) and an iOS app (SwiftUI), plus shared assets.

## Repository Structure

```
Shaker/
├── shared-assets/           # Shared cocktail data and images
│   ├── cocktails.json       # 25 cocktails with full data
│   └── images/              # Placeholder SVG images
├── android/                 # Android app (Kotlin/Jetpack Compose)
│   ├── app/src/main/java/com/purchasely/shaker/
│   │   ├── data/            # CocktailRepository, FavoritesRepository, PremiumManager, OnboardingRepository
│   │   ├── di/              # Koin DI modules
│   │   ├── domain/model/    # Data models (Cocktail, Ingredient)
│   │   └── ui/              # Compose UI (screens, navigation, theme, components)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── ios/                     # iOS app (SwiftUI)
    ├── Shaker/
    │   ├── Data/            # CocktailRepository, FavoritesRepository, PremiumManager, OnboardingRepository
    │   ├── Helpers/         # ViewControllerResolver, CocktailImage
    │   ├── Model/           # Cocktail, Ingredient
    │   └── Screens/         # Home, Detail, Favorites, Settings, Onboarding
    ├── project.yml          # XcodeGen spec
    └── Podfile
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
pod install
xcodegen generate
pod install
open Shaker.xcworkspace
# Build with Xcode (Cmd+B)
```

**API key setup**: Copy `Config.xcconfig.example` to `Config.xcconfig` and set `PURCHASELY_API_KEY=YOUR_KEY`.

**Important**: After adding/removing Swift files, run `xcodegen generate && pod install` to regenerate the Xcode project.

## Purchasely SDK Documentation

**When implementing Purchasely SDK features, always refer to these complete platform guides:**

| Platform | Reference File | Description |
|----------|---------------|-------------|
| Android/Kotlin | `Documentation/platform/android.md` | Complete Android SDK integration guide |
| iOS/Swift | `Documentation/platform/ios.md` | Complete iOS SDK integration guide |

### Verified SDK APIs (from actual SDK inspection)

**Android SDK 5.6.0:**
- `userSubscriptions(invalidateCache: Boolean, listener: SubscriptionsListener)` - first param is boolean
- `subscriptionStatus?.isExpired()` - isExpired is a function, subscriptionStatus is nullable
- `setUserAttribute(key: String, value: Any)` / `incrementUserAttribute(key: String)`
- `setPaywallActionsInterceptor { info, action, parameters, proceed -> }`
- `fetchPresentation(placementId) { presentation, error -> }` + `presentation.display(activity) { result, plan -> }`
- No `hasEntitlement()` on PLYPlan

**iOS SDK 5.6.4:**
- `userSubscriptions(success:, failure:)` - separate closures (not a single completion handler)
- `PLYSubscriptionStatus` is an enum: `.autoRenewing`, `.onHold`, `.inGracePeriod`, `.autoRenewingCanceled`, `.deactivated`, `.revoked`, `.paused`, `.unpaid`, `.unknown`
- `setUserAttribute(withStringValue:, forKey:)` / `setUserAttribute(withBoolValue:, forKey:)` / `incrementUserAttribute(withKey:)`
- `setPaywallActionsInterceptor { action, parameters, info, proceed in }`
- `fetchPresentation(for:, fetchCompletion:, completion:)` + `presentation.display(from: viewController)`
- `PLYEventDelegate.eventTriggered(_ event: PLYEvent, properties: [String: Any]?)` - properties is OPTIONAL
- `restoreAllProducts(success:, failure:)` - separate closures
- `onChange(of:)` with `{ _, newValue }` requires iOS 17; use `{ newValue }` for iOS 16

## Architecture

- **Android**: MVVM + Koin DI + Jetpack Compose + NavHost + kotlinx.serialization
- **iOS**: MVVM + SwiftUI + NavigationStack + Codable

### Data Flow

```
cocktails.json → CocktailRepository → ViewModel (StateFlow/Published) → Composable/View
                                          ↕
                              Purchasely SDK (entitlements, paywalls)
```

### Key Components

- **PremiumManager**: Singleton checking subscription status via `userSubscriptions`. Used by ViewModels to gate features.
- **CocktailRepository**: Loads `cocktails.json` from bundled assets. Single source of truth for cocktail data.
- **FavoritesRepository**: UserDefaults/SharedPreferences backed. Premium-gated feature.
- **OnboardingRepository**: Tracks whether onboarding has been shown (UserDefaults/SharedPreferences).
- **CocktailImage**: Native placeholder image component (spirit-based colors). No external image loading library needed.

## Conventions

- Package: `com.purchasely.shaker` (Android) / Shaker (iOS)
- Entitlement: `SHAKER_PREMIUM`
- Placements: `onboarding`, `recipe_detail`, `favorites`, `filters`
- Shared data in `shared-assets/` - copied to platform asset dirs
- API keys via local config files (never committed)
- Purchasely Maven repo: `https://maven.purchasely.io`

## Gotchas

- `settings.gradle.kts`: Use `dependencyResolutionManagement` (not `dependencyResolution`)
- XcodeGen + CocoaPods: Don't use `configFiles` at target level - causes "no parent for object" error with `pod install`
- iOS `onChange(of:)` with `{ _, newValue }` requires iOS 17 - use `{ newValue }` for iOS 16 compat
- SVGs in iOS asset catalog can fail - use native `CocktailImage` view instead

## Plan

See `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md` for the full implementation plan.
