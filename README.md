# Shaker

A cocktail discovery app demonstrating a production-quality [Purchasely](https://www.purchasely.com/) SDK integration. Built with native technologies on both platforms: **Kotlin/Jetpack Compose** on Android and **SwiftUI** on iOS.

## SDK Features Demonstrated

| Feature | Android | iOS | Placement |
|---------|---------|-----|-----------|
| Onboarding paywall | `fetchPresentation` + `display` | `fetchPresentation` + `display` | `onboarding` |
| Recipe detail paywall | `presentationView` | `presentationController` | `recipe_detail` |
| Favorites paywall | `presentationView` | `presentationController` | `favorites` |
| Filters paywall | `presentationView` | `presentationController` | `filters` |
| User login/logout | `userLogin` / `userLogout` | `userLogin` / `userLogout` | - |
| Restore purchases | `restoreAllProducts` | `restoreAllProducts` | - |
| User attributes | `setUserAttribute` / `incrementUserAttribute` | `setUserAttribute` / `incrementUserAttribute` | - |
| Event listener | `EventListener` | `PLYEventDelegate` | - |
| Paywall interceptor | `setPaywallActionsInterceptor` | `setPaywallActionsInterceptor` | - |
| Deep linking | `isDeeplinkHandled` | `isDeeplinkHandled` | - |
| Premium gating | `userSubscriptions` | `userSubscriptions` | - |

## Quick Setup

### Prerequisites

- A [Purchasely Console](https://console.purchasely.io/) account with an API key
- Android: Android Studio + JDK 11+
- iOS: Xcode 15+ with CocoaPods

### Android

```bash
cd android
```

1. Copy `local.properties.example` to `local.properties`
2. Set `purchasely.apiKey=YOUR_API_KEY`
3. Build and run:

```bash
./gradlew :app:assembleDebug
```

### iOS

```bash
cd ios
```

1. Copy `Config.xcconfig.example` to `Config.xcconfig`
2. Set `PURCHASELY_API_KEY = YOUR_API_KEY`
3. Install dependencies and build:

```bash
pod install
xcodegen generate
pod install
open Shaker.xcworkspace
```

Build with Cmd+B in Xcode.

## Console Configuration

Create the following in your Purchasely Console:

1. **Entitlement:** `SHAKER_PREMIUM`
2. **Placements:**
   - `onboarding` - shown on first launch
   - `recipe_detail` - shown when viewing locked recipe content
   - `favorites` - shown when accessing favorites feature
   - `filters` - shown when accessing filters feature
3. **Plans:** At least one subscription plan granting the `SHAKER_PREMIUM` entitlement

## Architecture

Both platforms follow MVVM with a shared data model:

```
cocktails.json -> CocktailRepository -> ViewModel -> UI
                                           |
                                    Purchasely SDK
                                    (paywalls, entitlements, user attributes)
```

- **Android:** MVVM + Koin DI + Jetpack Compose + NavHost + kotlinx.serialization
- **iOS:** MVVM + SwiftUI + NavigationStack + Codable

## Project Structure

```
Shaker/
├── shared-assets/          # Shared cocktail data and images
│   ├── cocktails.json      # 25 cocktails with full data
│   └── images/             # Placeholder SVG images
├── android/                # Android app (Kotlin/Jetpack Compose)
│   ├── app/src/main/java/com/purchasely/shaker/
│   │   ├── data/           # Repositories + PremiumManager
│   │   ├── di/             # Koin DI modules
│   │   ├── domain/model/   # Data models
│   │   ├── ui/components/  # Shared composables
│   │   └── ui/screen/      # Screens (home, detail, favorites, settings, onboarding)
│   └── build.gradle.kts
└── ios/                    # iOS app (SwiftUI)
    ├── Shaker/
    │   ├── Data/           # Repositories + PremiumManager
    │   ├── Helpers/        # ViewControllerResolver, CocktailImage
    │   ├── Model/          # Data models
    │   └── Screens/        # Screens (Home, Detail, Favorites, Settings, Onboarding)
    ├── project.yml         # XcodeGen spec
    └── Podfile
```

## Links

- [Purchasely Documentation](https://docs.purchasely.com/)
- [Android SDK Guide](https://docs.purchasely.com/quick-start/sdk-installation/quick-start-1)
- [iOS SDK Guide](https://docs.purchasely.com/quick-start/sdk-installation/quick-start)
