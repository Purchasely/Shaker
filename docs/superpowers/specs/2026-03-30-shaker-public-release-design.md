# Shaker Public Release — Design Spec

**Created:** 2026-03-30
**Status:** Draft

## Overview

Transform Shaker from an internal demo app into a **public-facing sample app** that serves as both a "wow demo" (clone, build, run in 5 minutes) and an exhaustive SDK integration reference for Purchasely clients. Ships with a public demo API key, real cocktail images, comprehensive inline documentation, and a companion integration guide designed to double as a Claude skill source.

## Goals

1. **Every Purchasely SDK feature** demonstrated in working code on both platforms
2. **Inline comments** on every SDK call (`// PURCHASELY:` prefix) with docs links
3. **Companion integration guide** walking through every feature, organized by topic
4. **Marketing-grade README** with screenshots, quick start, and architecture diagram
5. **Clone-and-run** experience with a public demo API key

## Non-Goals

- Production-grade cocktail content (it's a demo, not a recipe app)
- Localization beyond English
- Tablet/iPad-specific layouts
- Automated UI tests (covered by Mobile-UITests repo)

---

## Phase 1: PaywallObserver Mode

### 1.1 Running Mode Toggle

**Settings screen** on both platforms gains a **"Running Mode"** picker:
- Options: `Full` (default) / `PaywallObserver`
- Persisted in UserDefaults (iOS) / SharedPreferences (Android)
- Changing mode **re-initializes the Purchasely SDK** by calling `start()` again with the new `runningMode` parameter (no app restart needed — the SDK supports multiple `start()` calls)
- UI shows current mode with a brief explanation text

**SDK re-init flow:**
1. User toggles mode in Settings
2. App stores new mode preference
3. App calls `Purchasely.start()` again with updated `runningMode:` parameter (safe to call multiple times)
4. `PremiumManager` refreshes subscription state

### 1.2 PurchaseManager (New Class)

New class on each platform encapsulating native purchase logic, used only in Observer mode:

**iOS — `PurchaseManager.swift`:**
- Uses StoreKit 2 (`Product.purchase()`)
- `purchase(productId:) async throws -> Transaction` — initiates native purchase
- `purchaseWithPromoOffer(productId:, offerId:, signature:) async throws -> Transaction` — promo offer purchase with signature (note: signature generation typically requires a server; demo uses a hardcoded test signature or Purchasely's signing endpoint if available)
- `restoreAllTransactions() async throws` — iterates `Transaction.currentEntitlements`
- After every successful transaction: calls `Purchasely.synchronize()`

**Android — `PurchaseManager.kt`:**
- Uses Google Play Billing Library (`BillingClient`)
- `purchase(activity, productId, planId)` — launches billing flow
- `restore()` — queries existing purchases via `queryPurchasesAsync`
- `acknowledge(purchase)` — acknowledges after verification
- After every successful transaction: calls `Purchasely.synchronize()`

### 1.3 Interceptor Branching

`setPaywallActionsInterceptor` updated to branch on running mode:

**Full mode (existing behavior):**
- `.login` → prompt login
- `.navigate` → open URL
- All purchase actions → `proceed()` (SDK handles)

**Observer mode (new):**
- `.purchase` → call `PurchaseManager.purchase()`, then `synchronize()`, then `proceed()`
- `.restore` → call `PurchaseManager.restore()`, then `synchronize()`, then `proceed()`
- `.promoOffer` (iOS) → extract offer details from `parameters`, generate signature, call `PurchaseManager.purchaseWithPromoOffer()`, then `synchronize()`, then `proceed()`
- `.login` / `.navigate` → same as Full mode

### 1.4 Synchronize

- `Purchasely.synchronize()` called after every native purchase/restore in Observer mode
- Also called on app launch when in Observer mode (to catch transactions completed outside the app)

### 1.5 What This Demonstrates

- Toggling between Full and Observer running modes
- Complete native purchase flow when SDK doesn't own purchases
- Proper `synchronize()` usage and timing
- Promotional offer handling with StoreKit 2 (iOS)
- Google Play Billing integration (Android)

---

## Phase 2: Content & Visual Polish

### 2.1 Non-Alcoholic Cocktails

Add **8-10 non-alcoholic (mocktail) recipes** to `cocktails.json`:
- Virgin Mojito, Shirley Temple, Virgin Piña Colada, Roy Rogers, Arnold Palmer, Virgin Mary, Nojito, Cinderella, Safe Sex on the Beach, Italian Soda
- New `spirit` value: `"non-alcoholic"`
- Placed **first** in the list via JSON ordering + repository sort (non-alcoholic first, then alphabetical by spirit)
- Same data schema as existing cocktails: id, name, image, description, category, spirit, difficulty, tags, ingredients, instructions

### 2.2 Real Cocktail Images

- Source: royalty-free photos from Unsplash/Pexels (landscape or square, consistent aspect ratio)
- Resized to **600×400px** (sufficient for mobile, small file size)
- Format: JPEG (compressed) for both platforms
- Stored in:
  - Android: `app/src/main/assets/images/` (replacing SVGs)
  - iOS: asset catalog or bundle resources
- `CocktailImage` component updated to load real images with spirit-based color placeholder as fallback during loading
- Shared originals kept in `shared-assets/images/` for reference

### 2.3 Updated CocktailRepository

- Sort order: non-alcoholic cocktails first, then by spirit alphabetically
- Optional: add a `isAlcoholic: Boolean` computed property for filtering convenience

---

## Phase 3: Missing SDK Features

### 3.1 Embedded/Nested Paywall Views

**What:** Display a paywall inline within a screen (not fullscreen).

**Where:** New inline banner/card on the Home screen showing a compact paywall for non-premium users.

**Implementation:**
- iOS: `PLYPresentationView` wrapped in `UIViewControllerRepresentable`
- Android: `PLYPresentationView` in Jetpack Compose via `AndroidView`
- Uses a dedicated placement: `"home_banner"` (configure in Purchasely console)
- Handles presentation results inline

### 3.2 Custom Paywall (CLIENT Presentation Type)

**What:** When `fetchPresentation` returns a `CLIENT` type, the app builds its own native paywall UI from the plan data.

**Where:** Any placement can return CLIENT type based on console configuration.

**Implementation:**
- Check `presentation.type == .client` in fetchPresentation callback
- Extract plans/products from the presentation
- Render a simple native paywall (Card with plan name, price, CTA button)
- Call `Purchasely.plan.purchase()` or native purchase depending on mode
- Demonstrates the CLIENT flow without being overly complex

### 3.3 Display Modes (Android Only)

**Where:** New picker in Android Settings: "Paywall Display Mode"

**Options:** FULLSCREEN (default), MODAL, DRAWER, POPIN

**Implementation:**
- Store preference in SharedPreferences
- Pass `PLYPresentationDisplayMode` to `presentation.display(activity, mode)`
- Each mode shows different visual presentation of the same paywall

### 3.4 Anonymous User ID

**Where:** Settings screen, below the login/logout section.

**Implementation:**
- Display `Purchasely.anonymousUserId` in a read-only field
- Label: "Anonymous ID" with copy-to-clipboard action
- Useful for debugging and support

### 3.5 User Attribute Listener

**What:** Capture attribute changes triggered by in-paywall surveys.

**Implementation:**
- iOS: `Purchasely.setUserAttributeListener { key, value in }` in app init
- Android: `Purchasely.setUserAttributeListener { key, value -> }` in Application
- Log to console with `[Shaker]` prefix
- Show a brief toast/snackbar: "Attribute updated: {key}"

### 3.6 Default Presentation Result Handler

**What:** Handle results from paywall presentations triggered by deeplinks.

**Implementation:**
- Set `Purchasely.setDefaultPresentationResultHandler { result, plan in }` at init
- Log result and refresh premium state
- Ensures deeplink-triggered paywalls report their results

### 3.7 Promoted In-App Purchases (iOS Only)

**What:** Handle purchases initiated from the App Store product page.

**Implementation:**
- Implement `Purchasely.readyToOpenDeeplink = true` at init
- Handle in `AppDelegate` or via the default presentation result handler
- Log the promoted purchase event

### 3.8 PROMO_CODE Action (iOS Only)

**What:** Handle promo code redemption from paywall.

**Implementation:**
- Add `.promoCode` case in interceptor
- Open `SKPaymentQueue.presentCodeRedemptionSheet()` (or StoreKit 2 equivalent)
- Log the action

### 3.9 Additional User Attributes

**New attributes tracked:**
- `last_open_date` (Date) — set on each app launch
- `session_count` (Int) — incremented on each app launch
- Demonstrates Date and Int typed attributes beyond existing String/Bool

---

## Phase 4: Documentation

### 4.1 Inline Comments

Every Purchasely SDK call gets a comment with this format:
```
// PURCHASELY: [Brief description]
// [Why this call is needed / when to use it]
// Docs: https://docs.purchasely.com/[relevant-path]
```

Applied to all existing and new SDK calls across both platforms.

### 4.2 Companion Integration Guide (`docs/INTEGRATION_GUIDE.md`)

Organized by feature, not by screen:

1. **SDK Initialization** — start(), running modes, log levels
2. **Displaying Paywalls** — fetchPresentation + display, placements, content IDs
3. **Embedded Paywalls** — inline presentation views
4. **Custom Paywalls** — CLIENT type handling
5. **PaywallObserver Mode** — native purchase flow, synchronize
6. **Paywall Actions Interceptor** — all action types with examples
7. **User Authentication** — login, logout, anonymous ID
8. **Subscription Status** — userSubscriptions, entitlement checking
9. **User Attributes** — all types, increment, listener
10. **Events & Analytics** — event delegate/listener setup
11. **Deeplinks** — readyToOpenDeeplink, default result handler
12. **GDPR & Privacy** — consent management, data processing
13. **Restore Purchases** — restore flow in both modes
14. **Promotional Offers** — promo offer handling (iOS)
15. **Promoted IAP** — App Store initiated purchases (iOS)

Each section includes:
- What the feature does
- Code snippets from both platforms (copied from actual Shaker code)
- Console configuration required
- Common pitfalls

### 4.3 Marketing README

Structure:
```
# 🍸 Shaker — Purchasely SDK Sample App

[Hero banner image]

[Badges: platforms, SDK version, license, build status]

> A cocktail discovery app showcasing every Purchasely SDK feature.
> Clone it. Build it. See a full integration in 5 minutes.

## ✨ Features
[Checkmark list of all demonstrated SDK features]

## 📱 Screenshots
[Side-by-side iOS and Android screenshots]

## 🚀 Quick Start
[5 steps: clone, setup, build, run]

## 🏗️ Architecture
[Mermaid diagram: data flow, SDK integration points]

## 📖 Integration Guide
[Link to docs/INTEGRATION_GUIDE.md]

## 🤝 Contributing
[Link to CONTRIBUTING.md]

## 📄 License
MIT
```

### 4.4 MIT License & Contributing Guide

- `LICENSE` — standard MIT license, copyright Purchasely
- `CONTRIBUTING.md` — setup instructions, branch naming, PR guidelines, code style (follow existing patterns)

---

## Phase 5: Final Polish

- Capture screenshots from both platforms (Home, Detail, Settings, Paywall, Observer mode)
- Record a short GIF of the paywall flow
- Final code review pass for consistency
- CI validation on both platforms
- Update CLAUDE.md with new features and patterns

---

## Architecture Changes Summary

### New Files
| Platform | File | Purpose |
|----------|------|---------|
| Both | `PurchaseManager` | Native purchase handling for Observer mode |
| Both | Settings additions | Running mode toggle, display mode picker (Android), anonymous ID |
| Shared | `docs/INTEGRATION_GUIDE.md` | Companion guide |
| Shared | `CONTRIBUTING.md` | Contributor guidelines |
| Shared | `LICENSE` | MIT license |
| iOS | Updated `AppDelegate` or init | Promoted IAP, default result handler |

### Modified Files
| File | Changes |
|------|---------|
| `ShakerApplication` / `AppViewModel` | SDK restart on mode change, new listeners, additional attributes |
| `SettingsScreen` (both) | New toggles and displays |
| `HomeScreen` (both) | Embedded paywall banner |
| `cocktails.json` | Non-alcoholic cocktails added first |
| `CocktailImage` (both) | Real image loading |
| `CocktailRepository` (both) | Updated sort order |
| All files with SDK calls | Inline `// PURCHASELY:` comments |
| `README.md` | Complete rewrite for public release |
| `CLAUDE.md` | Updated with new features |

### Console Configuration Required
New placements to create in Purchasely demo app:
- `home_banner` — for embedded paywall view
- Existing: `onboarding`, `recipe_detail`, `favorites`, `filters`
