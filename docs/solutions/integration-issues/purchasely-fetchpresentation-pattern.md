---
title: "Use fetchPresentation + display() for Purchasely paywalls"
category: integration-issues
tags: [purchasely, android, ios, paywalls, flows, fetchPresentation]
module: SDK Integration
symptoms:
  - "Purchasely flows don't work"
  - "Paywall displays but flow sequence doesn't trigger"
  - "presentationView doesn't support flows"
severity: high
date_solved: 2026-02-06
---

# Use fetchPresentation + display() for Purchasely paywalls

## Problem

Purchasely flows (multi-step paywall sequences configured in the Purchasely console) do not work when using the convenience APIs `presentationView` (Android) or `presentationController` (iOS) to display paywalls.

## Symptoms

- Paywalls display correctly on their own, but flows and sequences configured in the Purchasely console do not trigger.
- Multi-step paywall sequences are silently skipped or ignored.

## Root cause

`presentationView` (Android) and `presentationController` (iOS) are convenience methods that skip the presentation fetching step. Purchasely flows require the full `fetchPresentation` -> check type -> `display()` lifecycle in order to resolve and execute flow sequences.

## Solution - Android (Kotlin/Compose)

### Before (broken for flows)

```kotlin
Purchasely.presentationView(
    context = context,
    properties = PLYPresentationProperties(placementId = "placement_id")
) { result, plan -> ... }
```

### After (correct)

```kotlin
val activity = context as? Activity ?: return
Purchasely.fetchPresentation("placement_id") { presentation, error ->
    if (presentation != null && presentation.type != PLYPresentationType.DEACTIVATED) {
        presentation.display(activity) { result, plan ->
            when (result) {
                PLYProductViewResult.PURCHASED,
                PLYProductViewResult.RESTORED -> { /* handle */ }
                else -> {}
            }
        }
    }
}
```

### With contentId

Use the named parameter `properties =` to avoid Kotlin overload ambiguity:

```kotlin
Purchasely.fetchPresentation(properties = PLYPresentationProperties(
    placementId = "recipe_detail", contentId = cocktailId
)) { presentation, error -> ... }
```

## Solution - iOS (SwiftUI)

### Before (broken for flows)

```swift
let ctrl = Purchasely.presentationController(for: "placement_id", completion: { result, plan in ... })
ctrl?.present(from: vc)
```

### After (correct)

```swift
let vc = UIApplication.shared.connectedScenes
    .compactMap { $0 as? UIWindowScene }
    .flatMap { $0.windows }
    .first { $0.isKeyWindow }?
    .rootViewController

Purchasely.fetchPresentation(
    for: "placement_id",
    fetchCompletion: { presentation, error in
        guard let presentation = presentation, presentation.type != .deactivated else { return }
        DispatchQueue.main.async {
            presentation.display(from: vc)
        }
    },
    completion: { result, plan in
        switch result {
        case .purchased, .restored:
            PremiumManager.shared.refreshPremiumStatus()
        case .cancelled: break
        @unknown default: break
        }
    }
)
```

### With contentId

```swift
Purchasely.fetchPresentation(
    for: "recipe_detail",
    contentId: cocktailId,
    fetchCompletion: { presentation, error in
        guard let presentation = presentation, presentation.type != .deactivated else { return }
        DispatchQueue.main.async {
            presentation.display(from: vc)
        }
    },
    completion: { result, plan in
        switch result {
        case .purchased, .restored:
            PremiumManager.shared.refreshPremiumStatus()
        case .cancelled: break
        @unknown default: break
        }
    }
)
```

## Gotchas

1. **Android: overload ambiguity with contentId** -- `fetchPresentation` with `contentId` requires using `PLYPresentationProperties` with the named parameter `properties =` to avoid Kotlin overload ambiguity.

2. **iOS: check for deactivated presentations** -- Always check `presentation.type != .deactivated` before calling `display(from:)`. A deactivated presentation should not be shown.

3. **iOS: main thread requirement** -- `display(from:)` must be called on the main thread via `DispatchQueue.main.async`. Calling it from a background thread will cause UI issues or crashes.

4. **iOS: avoid ViewControllerResolver in SwiftUI** -- Do not use a custom `ViewControllerResolver` (UIViewControllerRepresentable) to obtain the presenting view controller. It is fragile inside `List` and `TabView` contexts. Instead, use `UIApplication.shared.connectedScenes` to get the root view controller from the key window.

5. **iOS: two completion handlers** -- `fetchCompletion` is where you check the presentation type and call `display(from:)`. `completion` is where you handle the purchase result (purchased, restored, cancelled).

## Prevention

Always use `fetchPresentation` + `display()` for any new paywall integration. Never use `presentationView` (Android) or `presentationController` (iOS) -- these convenience methods do not support Purchasely flows.
