---
title: "feat: Shaker Sample App for Purchasely SDK"
type: feat
date: 2026-02-06
brainstorm: docs/brainstorms/2026-02-06-shaker-sample-app-brainstorm.md
---

# Shaker -- Purchasely SDK Sample App

## Overview

Build **Shaker**, a cocktail discovery app in its own repository ([github.com/Purchasely/Shaker](https://github.com/Purchasely/Shaker)), demonstrating a production-quality Purchasely SDK integration. The repo contains both an Android app (Kotlin/Jetpack Compose) and an iOS app (SwiftUI), plus shared assets. Feature-gated model: all cocktails visible, recipes/favorites/filters behind a "Shaker Premium" paywall.

## Proposed Solution

A single git repository with three top-level directories: `shared-assets/`, `android/`, and `ios/`. Both apps consume the same `cocktails.json` and images. Both apps consume the **published** Purchasely SDK artifacts (Maven for Android, CocoaPods for iOS) -- not local source references. A dedicated "Shaker" app is configured in the Purchasely Console with 4 placements and a single "Shaker Premium" entitlement.

## Technical Approach

### Architecture

```
Shaker/                          # github.com/Purchasely/Shaker
├── README.md
├── CLAUDE.md
├── .github/workflows/
│   └── ci.yml                   # Build both apps on push/PR + SDK releases
├── shared-assets/
│   ├── cocktails.json
│   └── images/
├── android/                     # Standalone Gradle project
│   ├── app/
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── gradle.properties
│   ├── local.properties.example
│   └── README.md
└── ios/                         # Standalone Xcode project
    ├── Shaker.xcodeproj
    ├── Shaker/
    ├── Podfile
    └── README.md
```

### Data Schema (`shared-assets/cocktails.json`)

```json
{
  "cocktails": [
    {
      "id": "mojito",
      "name": "Mojito",
      "image": "mojito.webp",
      "description": "A refreshing Cuban classic with lime and mint.",
      "category": "classic",
      "spirit": "rum",
      "difficulty": "easy",
      "tags": ["refreshing", "summer", "citrus"],
      "ingredients": [
        { "name": "White rum", "amount": "60ml" },
        { "name": "Fresh lime juice", "amount": "30ml" },
        { "name": "Sugar syrup", "amount": "20ml" },
        { "name": "Soda water", "amount": "top" },
        { "name": "Mint leaves", "amount": "8-10" }
      ],
      "instructions": [
        "Muddle mint leaves gently with sugar syrup in a glass.",
        "Add lime juice and rum.",
        "Fill with crushed ice and top with soda water.",
        "Garnish with a sprig of mint."
      ]
    }
  ]
}
```

**Gating rules:**
- Always visible: `id`, `name`, `image`, `description`, `category`, `spirit`, `difficulty`, `tags`, ingredient `name` list
- Premium-only: ingredient `amount` values, `instructions` array content

### Navigation Architecture

Bottom tab bar with 3 tabs:

```
┌─────────────────────────────────────┐
│                                     │
│  [Screen Content Area]              │
│                                     │
├─────────┬───────────┬───────────────┤
│  Home   │ Favorites │   Settings    │
└─────────┴───────────┴───────────────┘
```

- **Home tab**: Cocktail grid + search bar. Tapping a cocktail pushes Detail. Filter icon in toolbar opens Filters as a bottom sheet/modal.
- **Favorites tab**: List of saved cocktails (gated for free users).
- **Settings tab**: Login, restore, theme, GDPR.
- **Onboarding**: Full-screen overlay on first launch, before tabs are visible.
- **Paywall**: Presented as a full-screen modal by the Purchasely SDK.

### Subscription & Entitlement Model

| Item | Value |
|------|-------|
| Entitlement ID | `SHAKER_PREMIUM` |
| Monthly plan | $4.99/month |
| Annual plan | $29.99/year (7-day free trial) |
| Running mode | `Full` (Purchasely handles purchases) |

**Premium check logic:**
```
// Pseudocode -- both platforms
isPremium = Purchasely.userSubscriptions().any { it.plan.hasEntitlement("SHAKER_PREMIUM") }
```

Cache the result in-memory on app launch and after paywall dismissal. Re-check on `purchaseListener` events and on `userLogin` refresh callback.

### Authentication

**Mock login** -- this is a demo app. The login screen has a single text field for "User ID" (any string). On submit:
1. Store the user ID locally
2. Call `Purchasely.userLogin(userId)`
3. Handle the `refresh` callback (re-fetch subscriptions if `true`)

Logout: clear local storage, call `Purchasely.userLogout()`.

### Placements & Paywall Triggers

| Placement ID | Trigger | Context |
|-------------|---------|---------|
| `onboarding` | First app launch (last onboarding slide) | Full-screen paywall |
| `recipe_detail` | Tap locked recipe content | Pass `contentId = cocktail.id` |
| `favorites` | Tap "Add to favorites" or open Favorites tab while free | -- |
| `filters` | Tap filter button/icon while free | All filters are premium-gated |

### Content Teasing

On the Cocktail Detail screen for free users:
- Full cocktail photo, name, description visible
- Ingredient names listed, but amounts replaced with "---" or blurred
- Instructions section shows a gradient blur overlay with a CTA button "Unlock Full Recipe"
- Tapping the CTA triggers the `recipe_detail` placement

### Expired Subscription Behavior

- Previously saved favorites remain visible (read-only)
- Adding new favorites triggers paywall
- Ingredient amounts and instructions re-lock
- No special "expired" messaging -- just normal gating

### Error States

| Scenario | Behavior |
|----------|----------|
| SDK init failure | Show Home with all content unlocked (fail open for demo) + toast/snackbar warning |
| Paywall load failure | SDK shows fallback paywall if configured; otherwise toast "Could not load offer" |
| Restore: no purchases found | Toast "No previous purchases found" |
| Restore: network error | Toast "Network error. Please try again." |
| Placement deactivated | Silently skip (don't show paywall) |

### Theme

Single toggle in Settings (Light / Dark / System). Updates both:
- App's own theme (Compose `darkTheme` / SwiftUI `colorScheme`)
- `Purchasely.setThemeMode(.light / .dark / .system)`

### GDPR

Simplified consent in Settings:
- Single toggle: "Allow analytics & personalization"
- ON = all purposes enabled (default)
- OFF = `Purchasely.revokeDataProcessingConsent([.analytics, .identifiedAnalytics, .campaigns, .personalization, .thirdPartyIntegrations])`
- No consent gate before SDK init (this is a demo app, not a production compliance implementation)

### User Attributes

Set these attributes to demonstrate the feature:

| Attribute Key | Type | When Set |
|--------------|------|----------|
| `cocktails_viewed` | Int | Incremented on each Cocktail Detail view |
| `favorite_spirit` | String | Updated when user views cocktails (most viewed spirit) |
| `app_theme` | String | Set when theme toggle changes |
| `has_used_search` | Boolean | Set to true on first search |

### Event Listener

Log all Purchasely events to the platform debug console (Logcat / Xcode console) with tag `[Shaker]`. Include event name and properties. This demonstrates the `eventListener` / `addEventListener` API without requiring external analytics setup.

### Deep Linking

Support the standard Purchasely deep link scheme:
- `shaker://ply/placements/PLACEMENT_ID` -- opens a specific paywall
- `shaker://cocktail/COCKTAIL_ID` -- opens a specific cocktail detail

Handle in `Application.onCreate` / `App.onOpenURL` → pass to `Purchasely.isDeeplinkHandled()` first, then route internally if not consumed.

---

## Implementation Phases

### Phase 1: Foundation (Shared + Project Scaffolding)

**Deliverables:** Repo structure, shared data, both projects compiling with SDK initialized.

1. Create `shared-assets/cocktails.json` with 25 cocktails across categories
2. Source 25 royalty-free cocktail images (webp, ~400x600px), place in `shared-assets/images/`
3. Scaffold Android project:
   - `android/` with Gradle 8.x, Kotlin 2.2, Compose, Material 3
   - Dependencies: `io.purchasely:core`, `io.purchasely:google-play` from Maven (`https://maven.purchasely.io`)
   - API key via `local.properties` (`purchasely.apiKey=...`)
   - Create `local.properties.example` with placeholder
   - Package: `com.purchasely.shaker`
   - Min SDK 26, Target/Compile SDK 35
4. Scaffold iOS project:
   - `ios/Shaker.xcodeproj` with SwiftUI, iOS 16+ deployment target
   - `Podfile` with `pod 'Purchasely'`
   - API key via Xcode scheme environment variable or `Config.xcconfig`
   - Create `Config.xcconfig.example` with placeholder
5. Both apps: implement `ShakerApp` entry point with Purchasely SDK init
6. Both apps: load and parse `cocktails.json` from bundled assets
7. Copy `shared-assets/` content into platform asset directories (document the copy step in READMEs)

**Acceptance criteria:**
- [x] Both apps compile and launch
- [x] SDK initializes successfully (log confirms "configured")
- [x] Cocktail data loads from JSON (verified via debug log)

### Phase 2: Core Screens (Home + Detail + Navigation)

**Deliverables:** Tab navigation, cocktail browsing, detail screen with content teasing.

1. Implement bottom tab bar navigation (Home, Favorites, Settings)
2. **Home screen:**
   - Cocktail grid (2 columns) with image, name, category badge
   - Search bar (filter by name, case-insensitive, real-time)
   - Filter icon in toolbar (tappable, will trigger paywall in Phase 3)
3. **Cocktail Detail screen (pushed from Home):**
   - Hero image
   - Name, description, category, spirit, difficulty badges
   - Ingredients list (names visible, amounts show "---" if not premium)
   - Instructions section with blur overlay + "Unlock Full Recipe" CTA if not premium
   - Full content visible if premium
4. Implement premium status check: `isPremium` computed from `Purchasely.userSubscriptions()`
5. Wire the `recipe_detail` placement: tapping "Unlock Full Recipe" calls `Purchasely.fetchPresentation(...)` then `presentation.display(...)` with `contentId = cocktail.id`

**Acceptance criteria:**
- [x] Tab navigation works (Home, Favorites placeholder, Settings placeholder)
- [x] Cocktail grid displays all 25 cocktails with images
- [x] Search filters cocktails by name in real-time
- [x] Detail screen shows gated content (blurred instructions, hidden amounts)
- [x] Tapping "Unlock" triggers Purchasely paywall for `recipe_detail` placement
- [x] After purchase, content unblurs immediately

### Phase 3: Premium Features (Favorites + Filters + Onboarding)

**Deliverables:** All gated features, onboarding flow.

1. **Favorites:**
   - Local persistence (SharedPreferences / UserDefaults) -- list of cocktail IDs
   - Heart icon on Detail screen: tap adds/removes from favorites
   - If free user taps heart → trigger `favorites` placement
   - Favorites tab: list of saved cocktails (or paywall if free user with no favorites)
   - Expired premium: show existing favorites read-only, block new additions
2. **Filters (bottom sheet from Home):**
   - Filter by spirit (multi-select), difficulty (single-select), category (multi-select)
   - If free user → trigger `filters` placement immediately when sheet opens
   - If premium → show all filter options, apply to cocktail grid
3. **Onboarding:**
   - 3-slide carousel (hardcoded content: "Discover cocktails", "Learn recipes", "Go Premium")
   - Last slide has a "Get Started" button that triggers `onboarding` placement
   - Skip button on all slides → go to Home
   - First launch only (boolean in local storage)
   - If user force-quits mid-carousel, onboarding restarts on next launch (mark complete only after the final slide or skip)

**Acceptance criteria:**
- [x] Favorites persist across app restarts
- [x] Free users see paywall when trying to favorite
- [x] Expired users see old favorites but can't add new ones
- [x] Filter sheet shows paywall for free users
- [x] Premium users can filter cocktails by spirit/difficulty/category
- [x] Onboarding shows on first launch only
- [x] Onboarding ends with Purchasely paywall

### Phase 4: Settings + SDK Features

**Deliverables:** Settings screen, all remaining SDK integrations.

1. **Settings screen:**
   - Mock login/logout (text field for user ID)
   - Login calls `Purchasely.userLogin(userId)`, handles refresh callback
   - Logout calls `Purchasely.userLogout()`, clears local user ID
   - "Restore Purchases" button → calls SDK restore, shows success/error toast
   - Theme toggle (Light / Dark / System) → updates app + SDK theme
   - GDPR toggle → calls `revokeDataProcessingConsent` or re-enables
   - App version display
   - "Powered by Purchasely" attribution
2. **User attributes:** set `cocktails_viewed`, `favorite_spirit`, `app_theme`, `has_used_search` at appropriate moments
3. **Event listener:** log all SDK events to console with `[Shaker]` tag
4. **Paywall action interceptor:** handle `.login` (show login sheet), `.navigate` (open URL), others delegated to SDK
5. **Deep linking:** register URL scheme `shaker://`, handle in app entry point

**Acceptance criteria:**
- [x] Login/logout calls Purchasely APIs correctly
- [x] Restore purchases works (success + "no purchases" + error states)
- [x] Theme toggle affects both app UI and Purchasely paywalls
- [x] GDPR toggle calls `revokeDataProcessingConsent` / re-enables
- [x] User attributes visible in Purchasely Console after use
- [x] SDK events logged to console
- [x] Login interceptor from paywall opens the login sheet
- [x] Deep links open correct content

### Phase 5: Polish + CI/CD + Documentation

**Deliverables:** README, CLAUDE.md, CI workflow, final polish.

1. **README.md** (repo root):
   - What is Shaker (with screenshots)
   - SDK features demonstrated (table)
   - Quick setup for each platform
   - Console configuration instructions
   - Link to Purchasely docs
2. **CLAUDE.md** (repo root):
   - Build commands for both platforms
   - Architecture overview
   - Data model reference
   - Conventions
3. **Platform READMEs** (`android/README.md`, `ios/README.md`):
   - Platform-specific setup (API key, build, run)
   - Architecture notes
4. **CI/CD** (`.github/workflows/ci.yml`):
   - Trigger: push, PR, manual
   - Jobs: build Android (`./gradlew :app:assembleDebug`), build iOS (`xcodebuild build -scheme Shaker -destination 'generic/platform=iOS Simulator'`)
   - Cache: Gradle build cache, CocoaPods cache
5. **Polish:**
   - App icon (cocktail shaker theme)
   - Splash screen
   - Empty states (no favorites, no search results)
   - Loading states (paywall loading spinner)

**Acceptance criteria:**
- [x] README has setup instructions that work from a fresh clone
- [x] CI builds both apps successfully
- [x] CLAUDE.md provides enough context for AI-assisted development
- [x] App looks polished with consistent styling per platform

---

## Acceptance Criteria (Overall)

### Functional Requirements

- [x] 25 cocktails browsable with images, search, and detail
- [x] Feature-gated content: recipes, favorites, filters locked for free users
- [x] 4 Purchasely placements triggering paywalls at the right moments
- [x] Onboarding flow on first launch ending with paywall
- [x] Mock login/logout wired to Purchasely user management
- [x] Restore purchases functional
- [x] Theme toggle (light/dark/system)
- [x] GDPR consent toggle
- [x] Deep link support
- [x] User attributes set and visible in Console

### Non-Functional Requirements

- [x] Both apps compile and run from a fresh clone with documented setup
- [ ] API key is never committed to the repository
- [x] CI builds both apps on every push
- [x] Native design: Material 3 on Android, HIG on iOS
- [x] Offline browsing works (JSON is embedded, only paywalls need network)

### Quality Gates

- [x] README setup instructions verified by following them from scratch
- [x] Both platforms demonstrate identical SDK features (parity)
- [ ] No hardcoded API keys in source code
- [ ] Each phase must compile perfectly on both iOS and Android before moving to the next phase

---

## Dependencies & Prerequisites

| Dependency | Owner | Status |
|-----------|-------|--------|
| Purchasely Console: create "Shaker" app | Kevin | Not started |
| Console: configure `SHAKER_PREMIUM` entitlement | Kevin | Not started |
| Console: create 4 placements | Kevin | Not started |
| Console: design paywall | Kevin | Not started |
| App Store: create sandbox products (monthly + annual) | Kevin | Not started |
| Play Store: create test products (monthly + annual) | Kevin | Not started |
| 25 royalty-free cocktail images | To source | Not started |
| Cocktail recipe content (25 cocktails) | To curate | Not started |

---

## Risk Analysis & Mitigation

| Risk | Impact | Mitigation |
|------|--------|------------|
| Purchasely SDK breaking change | Build failure | CI catches it; pin SDK version |
| Cocktail images too large | App binary bloated | Use webp, compress to ~50KB each |
| CocoaPods version conflict on iOS | Build failure | Pin pod version in Podfile |
| Google Play Billing version mismatch | Purchase timeout | Pin billing SDK version explicitly (learned from `docs/solutions/`) |
| Console setup delays | Blocks paywall testing | Use staging API key for development, configure Console in parallel |

---

## Platform-Specific Integration Notes

### Android (Kotlin/Jetpack Compose)

**SDK Reference**: `Documentation/platform/android.md` -- complete integration guide with all methods, code examples, and troubleshooting.

Based on patterns from `Android_SDK/samplev2/`:

- **SDK init** in `Application.onCreate()` via `Purchasely.Builder(context).apiKey(...).stores(listOf(GoogleStore())).build().start {...}`
- **Paywall display**: `Purchasely.fetchPresentation(...)` then `presentation.display(activity)` (required to support Purchasely flows)
- **Manual close handling** required after purchase (Android does not auto-close)
- **Dependencies**: `io.purchasely:core:5.6.0`, `io.purchasely:google-play:5.6.0` from `https://maven.purchasely.io`
- **Gradle config**: Compose enabled, Kotlin 2.2, Java target 11, min SDK 26, compile SDK 35
- **DI**: Koin (lightweight, matches existing sample pattern)
- **Serialization**: `kotlinx.serialization.json` for `cocktails.json` parsing
- **Navigation**: `NavHost` with sealed `Screen` routes, bottom `NavigationBar`

### iOS (SwiftUI)

**SDK Reference**: `Documentation/platform/ios.md` -- complete integration guide with all methods, code examples, and troubleshooting.

Based on patterns from `iOS_SDK/Example/PurchaselySampleV2/`:

- **SDK init** in root ViewModel's `onAppear`: `Purchasely.start(withAPIKey:runningMode:storekitSettings:logLevel:completion:)`
- **StoreKit 2** (`.storeKit2` setting) -- requires iOS 15+ (we target 16+)
- **Paywall display**: `Purchasely.fetchPresentation(for:, fetchCompletion:, completion:)` then `presentation.display(from:)` (required to support Purchasely flows)
- **iOS auto-closes** paywall after successful purchase (unlike Android)
- **Dependencies**: `pod 'Purchasely'` in Podfile
- **Architecture**: MVVM with `@Observable` (iOS 17) or `ObservableObject` (iOS 16), `NavigationStack`
- **Persistence**: `UserDefaults` for favorites and settings

---

## References & Research

### Internal References

- Brainstorm: `docs/brainstorms/2026-02-06-shaker-sample-app-brainstorm.md`
- Android sample patterns: `Android_SDK/samplev2/` (MVVM, Koin, Compose, NavHost)
- iOS sample patterns: `iOS_SDK/Example/PurchaselySampleV2/` (SwiftUI, MVVM, NavigationStack)
- Android SDK init: `Android_SDK/samplev2/src/main/java/com/purchasely/samplev2/SampleV2Application.kt`
- iOS SDK init: `iOS_SDK/Example/PurchaselySampleV2/PurchaselySampleV2/Screens/Main/MainViewModel.swift`
- Android CI patterns: `Android_SDK/.github/workflows/pull_request_build.yml`
- Billing version mismatch learning: `Android_SDK/docs/solutions/integration-issues/google-play-billing-version-mismatch-timeout.md`
- SDK public API: `Android_SDK/CLAUDE.md` (full API reference)
- Platform docs: `Documentation/platform/android.md`, `Documentation/platform/ios.md`

### External References

- Repository: [github.com/Purchasely/Shaker](https://github.com/Purchasely/Shaker)
- Purchasely Docs: [docs.purchasely.com](https://docs.purchasely.com)
- Purchasely Console: [console.purchasely.io](https://console.purchasely.io)
