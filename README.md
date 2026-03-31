# Shaker — Purchasely SDK Sample App

**A cocktail discovery app showcasing every Purchasely SDK feature.**
Clone it. Build it. See a full integration in 5 minutes.

[![Platforms](https://img.shields.io/badge/platforms-iOS%20%7C%20Android-blue)]()
[![Purchasely SDK](https://img.shields.io/badge/Purchasely%20SDK-5.6-orange)]()
[![License](https://img.shields.io/badge/license-MIT-green)]()

## Features

### SDK Integration
- [x] SDK initialization (Full & PaywallObserver modes)
- [x] Paywall display via `fetchPresentation()` + `display()`
- [x] 4 placements: onboarding, recipe detail, favorites, filters
- [x] Paywall action interceptor (login, navigate, purchase, restore)
- [x] User authentication (`userLogin` / `userLogout`)
- [x] Subscription status checking
- [x] User attributes (string, bool, date, increment)
- [x] Event tracking
- [x] Deeplink handling
- [x] GDPR consent management (5 purposes)
- [x] Purchase restoration
- [x] Promo code redemption (iOS)
- [x] Paywall display modes (Android)

### App Features
- 35 cocktail recipes (10 non-alcoholic + 25 classic)
- Premium-gated features: filters, favorites, full recipes
- Light/dark/system theme support
- Search and filter by spirit, category, difficulty

## Quick Start

### Prerequisites
- **Android**: Android Studio, JDK 11+
- **iOS**: Xcode 15+, CocoaPods

### Android

```bash
git clone https://github.com/Purchasely/Shaker.git
cd Shaker/android
./gradlew :app:assembleDebug
```

The app ships with a demo API key — no setup needed.

### iOS

```bash
git clone https://github.com/Purchasely/Shaker.git
cd Shaker/ios
pod install && xcodegen generate && pod install
open Shaker.xcworkspace
```

Build and run on a simulator (Cmd+R). The app ships with a demo API key.

## Architecture

```
MVVM + Repository Pattern

cocktails.json → CocktailRepository → ViewModel → UI (Compose / SwiftUI)
                                          |
                              Purchasely SDK (paywalls, subscriptions, attributes)
```

| Component | Android | iOS |
|-----------|---------|-----|
| UI | Jetpack Compose | SwiftUI |
| DI | Koin | Singletons |
| Navigation | NavHost | NavigationStack |
| Data | kotlinx.serialization | Codable |
| Images | Coil | Bundle loading |

### Key Components

| Class | Purpose |
|-------|---------|
| `ShakerApp` / `AppViewModel` | SDK initialization, interceptor, event listener |
| `PremiumManager` | Subscription status via `userSubscriptions()` |
| `CocktailRepository` | Loads cocktail data from bundled JSON |
| `FavoritesRepository` | Premium-gated favorites (UserDefaults / SharedPreferences) |
| `SettingsViewModel` | Login, restore, GDPR consent, user attributes |

## Known Constraints

- Purchasely placements and plans must be configured in Console before paywalls can be displayed.
- A public demo Purchasely API key is hardcoded by default for quick startup.
- You can override it with local config files:
  - Android: `android/local.properties` (`purchasely.apiKey=...`)
  - iOS: `ios/Config.xcconfig` (`PURCHASELY_API_KEY = ...`)
- Paywall flows require `fetchPresentation + display()`. Do not use convenience APIs like `presentationView` or `presentationController`.
- Cocktail catalog browsing works offline from bundled JSON, but paywall fetch requires network access.

## Placements

| Placement ID | Where | Purpose |
|-------------|-------|---------|
| `onboarding` | First launch + Settings | Welcome paywall |
| `recipe_detail` | Detail screen | Unlock full recipe (with `contentId`) |
| `favorites` | Favorites tab + Detail | Unlock favorites feature |
| `filters` | Home toolbar | Unlock filter feature |

## Integration Guide

See [`docs/INTEGRATION_GUIDE.md`](docs/INTEGRATION_GUIDE.md) for a complete walkthrough of every SDK feature with code snippets from both platforms.

## Console Configuration

To use your own Purchasely account:

1. Create an app in the [Purchasely Console](https://console.purchasely.io)
2. Create the 4 placements listed above
3. Configure your store products and plans
4. **Android**: Replace the API key in `app/build.gradle.kts`
5. **iOS**: Replace the API key in `AppViewModel.swift`

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for setup instructions and guidelines.

## License

This project is licensed under the MIT License — see [LICENSE](LICENSE) for details.

---

Built with [Purchasely](https://www.purchasely.com)
