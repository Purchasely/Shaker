# Purchasely Wrapper + Observer Purchase Refactor — iOS

**Date:** 2026-04-10
**Status:** Approved
**Scope:** iOS only (mirrors Android refactoring)

---

## Design

Same architecture as Android spec (`2026-04-10-purchasely-wrapper-observer-purchase-refactor.md`), adapted for Swift/iOS:

- **Combine** replaces Kotlin SharedFlow for reactive communication
- **StoreKit 2** replaces Google Play Billing (already in place)
- **Singletons** replace Koin DI (existing iOS pattern)
- `PurchaseRequest` only needs `productId` (no Activity/offerToken — StoreKit 2 API)

### Files

| File | Action |
|------|--------|
| `Data/purchase/PurchaseRequest.swift` | **New** — struct with productId |
| `Data/purchase/TransactionResult.swift` | **New** — enum Success/Cancelled/Error/Idle |
| `Data/PurchaselySDKMode.swift` | **New** — extracted from AppViewModel |
| `Data/PurchaseManager.swift` | **Refactor** — zero PurchaselyWrapper imports, observe Combine subjects |
| `Purchasely/PurchaselyWrapper.swift` | **Refactor** — absorb init + interceptor, emit requests, observe results |
| `Purchasely/PurchaselyWrapping.swift` | **Update** — add initialize, restart, closeDisplayedPresentation |
| `AppViewModel.swift` | **Simplify** — just wrapper.initialize() + isSDKReady |
| `Screens/Settings/SettingsViewModel.swift` | **Minor** — setSdkMode posts notification, wrapper handles restart |

### Reactive Flow

```
PurchaselyWrapper                          PurchaseManager
    │                                           │
    │ PURCHASE (observer) ──────────────────►   │
    │   purchaseSubject.send(PurchaseRequest)    │
    │                                           │ Product.purchase()
    │                                           │ transaction.finish()
    │   ◄────────────────────────────────────   │
    │   resultSubject.send(.success)             │
    │                                           │
    │ synchronize()                              │
    │ pendingProcessAction(false)                │
    │ premiumManager.refreshPremiumStatus()      │
```
