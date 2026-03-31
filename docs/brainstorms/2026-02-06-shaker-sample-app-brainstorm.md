# Brainstorm: Shaker - Sample App for Purchasely SDK

**Date:** 2026-02-06
**Status:** Ready for planning
**Refined:** 2026-02-06 (collaborative dialogue)

---

## What We're Building

**Shaker** -- a cocktail discovery app that serves as a showcase sample for integrating the Purchasely SDK. The app lives in its own dedicated repository (`https://github.com/Purchasely/Shaker`) at `Shaker/` in the Purchasely workspace, and demonstrates a realistic, production-quality integration of the SDK in both Kotlin (Android) and SwiftUI (iOS).

### The App Concept

A cocktail catalog where users can browse all cocktails freely, but detailed recipes, favorites, and advanced filters are gated behind a "Shaker Premium" subscription. This feature-gated model demonstrates content teasing -- the most compelling paywall pattern for subscription apps.

### What It Demonstrates (SDK Features)

| Feature | How It's Shown in Shaker |
|---------|--------------------------|
| SDK initialization | App startup with API key, store config, running mode |
| Placements (multiple) | Onboarding, recipe detail, favorites, filters -- each a distinct placement |
| Paywall display | Full-screen paywall when tapping locked content |
| Content teasing | Blurred/locked recipe steps, ingredients quantities |
| Subscription status | Check entitlement to unlock premium content |
| User login/logout | Account management with user ID sync |
| User attributes | Track preferences (favorite spirit, cocktail count viewed) |
| Event listeners | Log SDK events (presentation viewed, purchase, etc.) |
| Restore purchases | Restore button in settings |
| Paywall action interceptor | Custom handling of login/close/purchase actions |
| Deep linking | Open specific cocktail or paywall via deep link |
| Theme mode | Light/dark mode support |
| Privacy/GDPR | Consent management in settings |

### Data

~20-30 cocktails hardcoded in a shared JSON file. Zero external dependencies. Each cocktail includes: name, image asset, category (classic, tropical, shots...), difficulty, ingredients (names visible to all, quantities gated), recipe steps (gated), tags.

---

## App Screens

7 screens total:

1. **Onboarding** (first launch only) -- Welcome carousel introducing the app, ends with a Purchasely paywall placement (`onboarding`). Shows the most common SDK integration pattern.

2. **Home** -- Grid/list of all cocktails with photo, name, and category. Search bar. All content visible (no gating here).

3. **Cocktail Detail** -- Full photo, description, ingredients list (quantities blurred if not premium), recipe steps (locked if not premium). Tapping locked content triggers the `recipe_detail` paywall placement.

4. **Favorites** -- List of saved cocktails. Adding to favorites requires premium. Triggers `favorites` paywall placement on first tap if not subscribed.

5. **Filters** -- Filter cocktails by spirit (vodka, gin, rum...), difficulty, category. Advanced filters gated behind premium. Triggers `filters` paywall placement.

6. **Profile / Settings** -- User login/logout, restore purchases, theme toggle (light/dark), GDPR consent management, app version, SDK debug info.

7. **Paywall** -- Rendered by Purchasely SDK via placements. Not a custom screen -- the Console controls the design.

---

## Why This Approach

### Theme: Cocktails
- Visually appealing (colorful drinks, elegant presentation)
- Feature-gating feels natural (recipes are premium content)
- Content is easy to curate without licensing issues
- Universally relatable and fun

### Feature-Gated Model (vs Freemium or Multi-Tier)
- Best demonstrates content teasing, the most common paywall pattern
- All cocktails browsable = app feels generous, not crippled
- Clear value proposition: "unlock the full recipe"
- Single paywall keeps the sample focused

### JSON Hardcoded Data (vs API)
- Zero network dependency = always works in demos
- No API key management or rate limiting
- Keeps focus on the Purchasely SDK integration, not networking code
- Shared data file between Kotlin and Swift projects

### Native Design (Material 3 + Human Interface Guidelines)
- Each platform follows its own design language -- most realistic for developer reference
- Material You (Android) and SF Symbols / native navigation (iOS)
- Avoids custom design system overhead

---

## Key Decisions

1. **App name:** Shaker
2. **Theme:** Cocktail discovery / recipe app
3. **Platforms:** Kotlin (Jetpack Compose) + SwiftUI -- native only for v1
4. **Paywall model:** Feature-gated (all content visible, details locked behind paywall)
5. **Data source:** Shared embedded JSON (~20-30 cocktails), no external API
6. **Assets:** Shared `shared-assets/` directory at repo root (single cocktails.json + images, copied into each platform at build/setup)
7. **Audience:** Dual-purpose -- sales demos AND developer reference
8. **Scope:** Showcase complet -- covers all major SDK features across 7 screens
9. **Design:** Native per platform (Material 3 on Android, HIG on iOS)
10. **Console:** Dedicated "Shaker" app in Purchasely Console with pre-configured placements and paywall
11. **API key:** Injected via environment variable / local.properties -- never committed to repo
12. **CI/CD:** GitHub Actions workflow to build both samples on SDK releases (catch breaking changes)
13. **Onboarding:** First-launch onboarding flow ending with paywall placement

---

## Purchasely Console Configuration (Shaker App)

### Placements to Create

| Placement ID | Trigger | Location in App |
|-------------|---------|-----------------|
| `onboarding` | First app launch | Onboarding screen (last step) |
| `recipe_detail` | Tap locked recipe content | Cocktail detail screen |
| `favorites` | First tap on "Add to favorites" | Favorites screen |
| `filters` | Tap advanced filter option | Filters screen |

### Suggested Plan Structure

- **Shaker Premium** -- single plan with weekly/monthly/annual options
- One paywall design in the Console, mapped to all placements (can be A/B tested later)

---

## Directory Structure

**Repository:** `https://github.com/Purchasely/Shaker`

```
Shaker/                              # Dedicated git repository
├── README.md                        # Overview: what is Shaker, how to set up, what SDK features are covered
├── shared-assets/
│   ├── cocktails.json               # Single source of truth for cocktail data
│   └── images/                      # Cocktail photos (royalty-free)
│       ├── mojito.webp
│       ├── old-fashioned.webp
│       └── ...
├── android/                         # Kotlin + Jetpack Compose
│   ├── README.md                    # Android-specific setup, architecture notes
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/purchasely/shaker/
│   │       ├── res/
│   │       └── assets/              # cocktails.json + images copied here
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
└── ios/                             # SwiftUI
    ├── README.md                    # iOS-specific setup, architecture notes
    ├── Shaker.xcodeproj
    └── Shaker/
        ├── App/
        ├── Features/
        ├── Models/
        ├── Resources/               # cocktails.json + images copied here
        └── Assets.xcassets
```

---

## Next Steps

Run `/workflows:plan` to create a detailed implementation plan covering:
- Cocktail data schema and content curation
- Android project setup (Compose, Purchasely SDK dependency, architecture)
- iOS project setup (SwiftUI, CocoaPods/SPM, architecture)
- Console configuration (placements, paywall design, plan setup)
- CI/CD workflow
- README documentation
