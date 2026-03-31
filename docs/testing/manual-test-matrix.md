---
title: "Shaker manual validation matrix"
category: testing
tags: [shaker, manual-tests, android, ios, purchasely, paywall]
date: 2026-02-17
---

# Shaker manual validation matrix

## Purpose

Validate Purchasely integration parity between Android and iOS for:
- user states: free, premium, expired
- placements: `onboarding`, `recipe_detail`, `favorites`, `filters`
- supporting flows: login/logout, restore, deep links, GDPR toggles

## Preconditions

### Console setup

- Shaker app exists in Purchasely Console.
- Entitlement `SHAKER_PREMIUM` exists.
- Placements exist and are active:
  - `onboarding`
  - `recipe_detail`
  - `favorites`
  - `filters`
- At least one subscription plan grants `SHAKER_PREMIUM`.
- Sandbox/test products are configured for iOS and Android stores.

### App setup

- Android build installs and launches.
- iOS build installs and launches.
- Device/emulator has network access for paywall fetch.
- Use clean app state when test case requires first launch.

### Test users

- `free_user`: no active subscription
- `premium_user`: active subscription with `SHAKER_PREMIUM`
- `expired_user`: previously subscribed, now expired

## Result legend

- `PASS`: expected behavior observed
- `FAIL`: behavior deviates from expected
- `BLOCKED`: missing precondition/environment issue
- `N/A`: not applicable for platform/state

## Test matrix

| ID | Area | User state | Test steps | Expected result | Android | iOS | Evidence | Issue |
|---|---|---|---|---|---|---|---|---|
| T01 | Onboarding paywall | free_user | Fresh install -> launch app -> reach last onboarding slide -> tap Get Started | Placement `onboarding` is fetched and displayed | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/onboarding/OnboardingScreen.kt:41`, `Shaker/ios/Shaker/Screens/Onboarding/OnboardingScreen.swift:25` | - |
| T02 | Recipe gating | free_user | Open cocktail detail -> tap Unlock Full Recipe | Placement `recipe_detail` displayed with `contentId` | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/detail/DetailScreen.kt:232`, `Shaker/ios/Shaker/Screens/Detail/DetailViewModel.swift:34` | - |
| T03 | Recipe unlock refresh | premium_user | Complete purchase from recipe paywall | Instructions/amounts unblur without restart | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/detail/DetailScreen.kt:236`, `Shaker/ios/Shaker/Screens/Detail/DetailViewModel.swift:50` | - |
| T04 | Favorites gate | free_user | From detail, tap favorite icon OR open favorites tab with empty list | Placement `favorites` displayed | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/detail/DetailScreen.kt:82`, `Shaker/ios/Shaker/Screens/Favorites/FavoritesScreen.swift:75` | - |
| T05 | Favorites premium | premium_user | Add/remove favorites, restart app | Favorites persist after restart | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/data/FavoritesRepository.kt:17`, `Shaker/ios/Shaker/Data/FavoritesRepository.swift:13` | - |
| T06 | Expired behavior | expired_user | Open existing favorites, try adding a new favorite | Existing favorites visible; add new triggers paywall | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/favorites/FavoritesScreen.kt:81`, `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/detail/DetailScreen.kt:77`, `Shaker/ios/Shaker/Screens/Favorites/FavoritesScreen.swift:51`, `Shaker/ios/Shaker/Screens/Detail/DetailScreen.swift:113` | - |
| T07 | Filters gate | free_user | Home -> tap filter icon | Placement `filters` displayed | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeScreen.kt:83`, `Shaker/ios/Shaker/Screens/Home/HomeScreen.swift:80` | - |
| T08 | Filters premium | premium_user | Home -> open filters -> set spirit/category/difficulty | Filters apply correctly to cocktail grid | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/home/HomeViewModel.kt:87`, `Shaker/ios/Shaker/Screens/Home/HomeViewModel.swift:39` | - |
| T09 | Login/logout | free_user | Settings -> login with mock id -> logout | `userLogin` then `userLogout` called; state updated | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:56`, `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:70`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:42`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:56` | - |
| T10 | Restore success | premium_user | Settings -> Restore Purchases | Restore success UI message and premium state refreshed | BLOCKED (needs premium sandbox account) | BLOCKED (needs premium sandbox account) | Restore APIs wired: `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:79`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:65` | ACTION-P3-04 |
| T11 | Restore no purchase | free_user | Settings -> Restore Purchases | "no purchases" or equivalent error message shown | BLOCKED (needs free sandbox account scenario) | BLOCKED (needs free sandbox account scenario) | Error handling wired: `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:85`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:73` | ACTION-P3-04 |
| T12 | Theme mode parity | any | Settings -> switch Light/Dark/System | App theme and Purchasely paywall theme both update | FAIL | FAIL | Theme mode is stored but not applied to app root theme and no SDK theme API call. `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:100`, `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/theme/Theme.kt:38`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:86`, `Shaker/ios/Shaker/ContentView.swift:22` | ACTION-P3-01 |
| T13 | GDPR toggles | any | Settings -> toggle consent options on/off | `revokeDataProcessingConsent` reflects selected revoked purposes | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:136`, `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:122` | - |
| T14 | Deep link placement | any | Open `shaker://ply/placements/onboarding` | Purchasely handles deeplink and opens placement | PASS (code-path) | PASS (runtime + code-path) | Android handler: `Shaker/android/app/src/main/java/com/purchasely/shaker/MainActivity.kt:34`; iOS handler: `Shaker/ios/Shaker/ShakerApp.swift:15`; iOS runtime command succeeded: `xcrun simctl openurl ... 'shaker://ply/placements/onboarding'` | - |
| T15 | Deep link cocktail | any | Open `shaker://cocktail/mojito` | App navigates to cocktail detail for `mojito` | FAIL | FAIL | No fallback internal route for unhandled cocktail deep links. `Shaker/android/app/src/main/java/com/purchasely/shaker/MainActivity.kt:37`, `Shaker/ios/Shaker/ShakerApp.swift:16` | ACTION-P3-02 |
| T16 | Event logging | any | Trigger paywall, purchase/restore, login/logout | Console logs events with `[Shaker]` prefix | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt:50`, `Shaker/ios/Shaker/AppViewModel.swift:63` | - |
| T17 | Paywall action interceptor login | free_user | Trigger paywall action `.login` | Login flow/sheet opens instead of default action | FAIL | FAIL | Interceptor blocks default action but does not route to login UI. `Shaker/android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt:60`, `Shaker/ios/Shaker/AppViewModel.swift:44` | ACTION-P3-03 |
| T18 | Paywall action interceptor navigate | any | Trigger paywall action `.navigate` with URL | URL opened by app; no crash | PASS (code-path) | PASS (code-path) | `Shaker/android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt:65`, `Shaker/ios/Shaker/AppViewModel.swift:47` | - |

## Platform-specific notes to capture during execution

- Android:
  - verify manual close handling behavior after purchase/restore
  - verify activity context availability for paywall display
- iOS:
  - verify presentation works inside List/TabView contexts
  - verify paywall display runs on main thread

## Execution log

| Date | Tester | Platform | Scope | Result | Notes |
|---|---|---|---|---|---|
| 2026-02-17 | Codex | Android | Build precondition | PASS | `./gradlew :app:assembleDebug` -> `BUILD SUCCESSFUL` |
| 2026-02-17 | Codex | iOS | Build precondition | PASS | `xcodebuild build -workspace Shaker.xcworkspace -scheme Shaker -destination 'generic/platform=iOS Simulator' -quiet` |
| 2026-02-17 | Codex | iOS | Runtime smoke | PASS | Simulator boot + app install/launch + openURL commands succeeded (`simctl boot/install/launch/openurl`) |
| 2026-02-17 | Codex | Android + iOS | Full matrix pass | COMPLETE | Cases filled with PASS/FAIL/BLOCKED and linked actions |

## Action register

| Action ID | Type | Description | Owner | Status |
|---|---|---|---|---|
| ACTION-P3-01 | Fix | Implement theme sync: app theme application + Purchasely theme mode update on both platforms | TBD | OPEN |
| ACTION-P3-02 | Fix | Implement internal fallback routing for `shaker://cocktail/:id` deep links when not handled by Purchasely | TBD | OPEN |
| ACTION-P3-03 | Fix | Route paywall `.login` interceptor to in-app login screen/sheet | TBD | OPEN |
| ACTION-P3-04 | Validation | Run restore scenarios with real sandbox accounts (premium + free) and attach evidence | TBD | OPEN |

## Defect tracking template

When a case fails, log:
- test case ID
- platform and OS version
- exact repro steps
- expected vs actual
- logs/screenshots/video
- issue URL
