# Phase 1: PaywallObserver Mode -- Code Review

**Reviewed:** 2026-03-30
**Branch:** `feat/paywall-observer-mode` (10 commits, bc6b4ff..0c4e466)
**Reviewer:** Code Review Agent

---

## Summary

Phase 1 is well-implemented and closely follows the plan and spec. All 9 tasks were completed, both platforms have equivalent functionality, and the code follows existing project patterns. The implementation is clean, well-documented with log statements, and correctly handles the core PaywallObserver flow (native purchase, synchronize, proceed).

---

## 1. Spec Compliance

**Status: PASS** -- All Phase 1 requirements are met.

| Spec Requirement | Status | Notes |
|---|---|---|
| Running mode toggle (Full/Observer) | Done | Both platforms |
| Persisted in UserDefaults/SharedPrefs | Done | `RunningModeRepository` on both |
| Re-initializes SDK on toggle | Done | `initPurchasely()` re-called |
| PurchaseManager (StoreKit 2) | Done | iOS `PurchaseManager.swift` |
| PurchaseManager (Google Play Billing) | Done | Android `PurchaseManager.kt` |
| Interceptor branches on mode | Done | Full -> proceed(true), Observer -> native + synchronize + proceed(false) |
| Promotional offer support (iOS) | Done | `purchaseWithPromoOffer` with `signPromotionalOffer()` |
| synchronize() after transactions | Done | Both platforms, all code paths |
| synchronize() on app launch (Observer) | Done | Both platforms |
| CLAUDE.md updated | Done | New components + gotchas documented |

---

## 2. Code Quality Assessment

### What was done well

- Clean separation of concerns: `RunningModeRepository` (persistence), `PurchaseManager` (native purchases), and interceptor logic (branching) are properly separated
- Consistent logging with `[Shaker]` prefix across all new code
- iOS `synchronize()` correctly uses `success:/failure:` closures (commit 19a8592 fixed this)
- Android `store_product_id` usage is correct per SDK API (documented in CLAUDE.md gotchas)
- Proper `@available(iOS 15.0, *)` guards on `PurchaseManager` and all call sites
- `weak self` used correctly in the SDK start callback on iOS
- BillingClient reconnection logic on Android is present

### Issues Found

#### Important (should fix)

**I-1: Missing divider between Purchases and SDK Mode sections (Android)**

File: `/Users/kevin/Purchasely/Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsScreen.kt` (line 186-188)

The plan explicitly specified a `Spacer + HorizontalDivider + Spacer` between the "Show Onboarding" button and the "SDK Mode" title, matching the pattern used between every other section. The implementation jumps directly from the button to the section title.

Fix: Insert between lines 186 and 188:
```kotlin
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(24.dp))
```

**I-2: iOS `onChange(of:)` uses `{ newValue in }` single-closure form**

File: `/Users/kevin/Purchasely/Shaker/ios/Shaker/Screens/Settings/SettingsScreen.swift` (lines 69, 156)

The CLAUDE.md gotchas state: "iOS `onChange(of:)` with `{ _, newValue }` requires iOS 17 -- use `{ newValue }` for iOS 16 compat." The implementation uses `{ newValue in }` which is correct for iOS 14-16 but will produce a deprecation warning on iOS 17+. This is fine per the project conventions and is consistent with existing code. **No action needed** -- documenting that this was checked.

**I-3: iOS SettingsScreen double-fires setRunningMode on initial load**

File: `/Users/kevin/Purchasely/Shaker/ios/Shaker/Screens/Settings/SettingsScreen.swift` (line 69-72)

The `onChange(of: viewModel.runningMode)` modifier fires when the Picker selection changes. Since `runningMode` is a `@Published` property and the Picker is bound to it with `$viewModel.runningMode`, the Picker write triggers `onChange`, which calls `viewModel.setRunningMode(newValue)` (writes `runningMode` again) and `appViewModel.initPurchasely()`. The `setRunningMode` call is redundant because the Picker binding already wrote the value. This means:
1. `RunningModeRepository.shared.runningMode` is set twice (harmless but wasteful)
2. `initPurchasely()` is called correctly

This is not a bug but a code smell. Consider either removing `setRunningMode` from `onChange` and only calling `appViewModel.initPurchasely()`, or removing the `$viewModel.runningMode` binding and using a manual selection handler.

#### Suggestions (nice to have)

**S-1: Android PurchaseManager only handles SUBS, not INAPP products**

File: `/Users/kevin/Purchasely/Shaker/android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt` (line 66)

The `purchase()` function hardcodes `BillingClient.ProductType.SUBS`. If the demo ever includes one-time purchases, this would fail silently. The `restore()` function also only queries SUBS. Consider adding an `INAPP` query for completeness, or adding a TODO comment.

**S-2: Android PurchaseManager thread safety**

File: `/Users/kevin/Purchasely/Shaker/android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt` (line 19)

The `onPurchaseResult` callback field is not synchronized. If `purchase()` is called rapidly twice, the second call overwrites the first callback before `onPurchasesUpdated` fires. For a demo app this is acceptable, but a comment noting the limitation would be helpful.

**S-3: iOS PurchaseManager appAccountToken may silently fail**

File: `/Users/kevin/Purchasely/Shaker/ios/Shaker/Data/PurchaseManager.swift` (lines 23-26)

The `UUID(uuidString:)` will return nil if `anonymousUserId` is not a valid UUID, silently skipping the app account token. This is handled gracefully (purchase proceeds without the token) but a log message when the UUID conversion fails would help debugging.

**S-4: Android `enablePendingPurchases()` without a `PendingPurchaseParams` argument**

File: `/Users/kevin/Purchasely/Shaker/android/app/src/main/java/com/purchasely/shaker/data/purchase/PurchaseManager.kt` (line 23)

Google Play Billing Library 7.x has `enablePendingPurchases(PendingPurchaseParams)`. The parameterless overload still works but is deprecated. Consider using the explicit variant.

---

## 3. Cross-Platform Consistency

**Status: GOOD** -- Platforms are functionally equivalent with appropriate platform-specific differences.

| Feature | Android | iOS | Consistent? |
|---|---|---|---|
| RunningModeRepository | SharedPreferences, Koin singleton | UserDefaults, static shared | Yes (idiomatic per platform) |
| PurchaseManager | BillingClient, callback-based | StoreKit 2, async/await | Yes (idiomatic per platform) |
| Interceptor purchase | offerToken from parameters | appleProductId from plan | Yes (different SDK APIs) |
| Interceptor restore | queryPurchasesAsync + acknowledge | Transaction.currentEntitlements | Yes |
| Promo offers | N/A (Android uses offer tokens) | signPromotionalOffer + StoreKit 2 | Yes (iOS-only per spec) |
| synchronize() on launch | Parameterless call | success/failure closures | Yes (different SDK signatures) |
| Settings UI | SegmentedButtonRow | Picker .segmented | Yes (native patterns) |
| SDK re-init trigger | ShakerApp.initPurchasely() via cast | appViewModel.initPurchasely() via EnvironmentObject | Yes |

---

## 4. Architecture Assessment

- SOLID compliance is good: `PurchaseManager` has a single responsibility (native purchases), `RunningModeRepository` has a single responsibility (persistence), and the interceptor orchestrates them
- The `ShakerApp` cast (`context.applicationContext as ShakerApp`) on Android is a common pattern for accessing Application-level methods from ViewModels
- The iOS `@EnvironmentObject private var appViewModel` approach in SettingsScreen is clean and follows SwiftUI conventions
- No circular dependencies introduced
- DI registration in Koin is properly ordered

---

## 5. Verdict

**APPROVED** with one minor fix recommended (I-1: missing divider on Android Settings).

The implementation faithfully delivers every Phase 1 requirement. Code quality is high, cross-platform consistency is strong, and the new components integrate cleanly with the existing architecture. The CLAUDE.md updates with SDK API gotchas (`synchronize()` signatures, `store_product_id` naming) are a valuable addition for future development.
