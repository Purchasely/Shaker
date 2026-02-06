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
│   │   ├── data/            # CocktailRepository
│   │   ├── di/              # Koin DI modules
│   │   ├── domain/model/    # Data models (Cocktail, Ingredient)
│   │   └── ui/              # Compose UI (screens, navigation, theme)
│   ├── build.gradle.kts
│   └── settings.gradle.kts
└── ios/                     # iOS app (SwiftUI) -- coming soon
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
open Shaker.xcworkspace
# Build with Xcode (Cmd+B)
```

**API key setup**: Copy `Config.xcconfig.example` to `Config.xcconfig` and set `PURCHASELY_API_KEY=YOUR_KEY`.

## Purchasely SDK Documentation

**When implementing Purchasely SDK features, always refer to these complete platform guides:**

| Platform | Reference File | Description |
|----------|---------------|-------------|
| Android/Kotlin | `Documentation/platform/android.md` | Complete Android SDK integration guide (init, paywalls, entitlements, user attributes, events, deeplinks) |
| iOS/Swift | `Documentation/platform/ios.md` | Complete iOS SDK integration guide (init, paywalls, entitlements, user attributes, events, deeplinks) |

These files contain all SDK methods, code examples, and troubleshooting steps for each platform.

### Key SDK Patterns

- **Init**: `Purchasely.Builder(context).apiKey(...).stores(listOf(GoogleStore())).build().start{...}` (Android) / `Purchasely.start(withAPIKey:runningMode:storekitSettings:logLevel:completion:)` (iOS)
- **Paywalls**: Display via placements -- `Purchasely.presentationView(context, placementId, ...)` (Android) / `Purchasely.presentationController(for:contentId:completion:)` (iOS)
- **Entitlements**: `Purchasely.userSubscriptions { subs -> subs.any { it.plan.hasEntitlement("SHAKER_PREMIUM") } }`
- **User login**: `Purchasely.userLogin(userId)` -- handles subscription transfer via refresh callback
- **Running mode**: `PLYRunningMode.Full` -- Purchasely handles entire purchase flow

## Architecture

- **Android**: MVVM + Koin DI + Jetpack Compose + NavHost + kotlinx.serialization
- **iOS**: MVVM + SwiftUI + NavigationStack + Codable

### Data Flow

```
cocktails.json → CocktailRepository → ViewModel (StateFlow/Published) → Composable/View
                                          ↕
                              Purchasely SDK (entitlements, paywalls)
```

## Conventions

- Package: `com.purchasely.shaker` (Android) / Shaker (iOS)
- Entitlement: `SHAKER_PREMIUM`
- Placements: `onboarding`, `recipe_detail`, `favorites`, `filters`
- Shared data in `shared-assets/` -- copy to platform asset dirs at build time
- API keys via local config files (never committed)
- Purchasely Maven repo: `https://maven.purchasely.io`

## Plan

See `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md` for the full implementation plan.
