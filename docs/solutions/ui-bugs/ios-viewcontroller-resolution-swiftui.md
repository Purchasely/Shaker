---
title: "iOS ViewControllerResolver fails in List/TabView contexts"
category: ui-bugs
tags: [ios, swiftui, uikit, viewcontroller, purchasely, presentation]
module: iOS UI
symptoms:
  - "hostViewController is nil"
  - "Button tap does nothing"
  - "Paywall doesn't present from Settings/List screens"
severity: medium
date_solved: 2026-02-06
---

# iOS ViewControllerResolver fails in List/TabView contexts

## Problem

`ViewControllerResolver` -- a `UIViewControllerRepresentable` that resolves the parent UIViewController via `didMove(toParent:)` -- fails to provide a valid UIViewController when placed as a `.background()` modifier on SwiftUI views inside `List`, `TabView`, or deep `NavigationStack` hierarchies.

This caused paywall presentation to silently fail in certain screens of the Shaker app.

## Symptoms

- "Show Onboarding" button in Settings does nothing when tapped.
- `hostViewController` is `nil`; the `guard let vc = hostViewController else { return }` statement exits silently with no visible error.
- Paywall presentation works correctly on some screens (e.g., full-screen OnboardingScreen) but fails on others (e.g., Settings inside a List/TabView).

## Root cause

`ViewControllerResolver` uses `didMove(toParent:)` to walk the UIKit responder chain and find the hosting UIViewController. This breaks in specific SwiftUI container views:

- **`List`** renders rows using `UITableView` internally. The resolved UIViewController may be a cell container view controller that is not suitable for modal presentation.
- **`TabView`** and **nested `NavigationStack`** introduce intermediate container VCs that may not be connected to the presentation hierarchy in the expected way.
- The `.background()` view may not have been laid out yet at the time of resolution, resulting in a `nil` VC.

Because the code used a `guard let` with an early return and no logging, the failure was completely silent.

## Solution

Replace `ViewControllerResolver` with a direct lookup of the root view controller from the key window scene.

### Before (fragile)

```swift
@State private var hostViewController: UIViewController?

var body: some View {
    SomeView()
        .background(
            ViewControllerResolver { vc in
                hostViewController = vc
            }
            .frame(width: 0, height: 0)
        )
}

// Usage:
func presentPaywall() {
    guard let vc = hostViewController else { return }
    // present using vc...
}
```

### After (reliable)

```swift
func presentPaywall() {
    let vc = UIApplication.shared.connectedScenes
        .compactMap { $0 as? UIWindowScene }
        .flatMap { $0.windows }
        .first { $0.isKeyWindow }?
        .rootViewController

    guard let vc else { return }
    // present using vc...
}
```

This approach retrieves the root view controller from the active key window, which is always valid regardless of the SwiftUI container hierarchy the calling view is embedded in.

## When ViewControllerResolver is still acceptable

The `OnboardingScreen` (splash/welcome screen) continues to use `ViewControllerResolver` because:

- It is displayed as a full-screen view **before** the `TabView`/`NavigationStack` hierarchy is established.
- The UIKit view hierarchy is simple and flat at that point, so `didMove(toParent:)` resolves reliably.

## Prevention

For any new SwiftUI screen that needs to present a UIKit view controller (Purchasely paywalls, alerts, share sheets, etc.):

1. **Default to the window scene approach.** Use the `UIApplication.shared.connectedScenes` lookup shown above.
2. **Reserve `ViewControllerResolver`** only for simple, full-screen contexts where the view is guaranteed not to be inside `List`, `TabView`, or nested navigation containers.
3. **Add logging on failure.** Never use a bare `guard let ... else { return }` when resolving a view controller. Log a warning so silent failures are caught during development.
