---
title: "Toggle Purchasely SDK running mode from Settings"
category: integration-issues
tags: [purchasely, android, ios, settings, running-mode, paywallObserver, full]
module: SDK Integration
sdk: "Purchasely 5.7"
symptoms:
  - "Need to switch SDK between full and paywallObserver at runtime"
  - "Mode choice should persist across app relaunch"
  - "Need restart behavior and user guidance after mode change"
severity: medium
date_solved: 2026-02-17
last_reviewed: 2026-05-13
---

# Toggle Purchasely SDK running mode from Settings

## Problem

Shaker needed a cross-platform Settings control to switch Purchasely SDK running mode between:

- `paywallObserver`
- `full`

The selected mode had to be persisted, applied by default on next app launch, and trigger an SDK restart when changed.  
Default mode must be `paywallObserver`.

## Solution

Implemented a shared behavior on Android and iOS:

1. Add a Settings UI section to choose SDK mode (`Paywall Observer` / `Full`).
2. Persist selected mode in app storage under `purchasely_sdk_mode`.
3. Default to `paywallObserver` when no value exists.
4. Restart the SDK immediately when mode changes.
5. Show an alert telling the user to kill and relaunch the app.

## Android implementation

- Storage layer is now a dedicated repository (`RunningModeRepository`):
  - `android/app/src/main/java/com/purchasely/shaker/data/RunningModeRepository.kt`
  - `android/app/src/main/java/com/purchasely/shaker/data/PurchaselySdkMode.kt`
- `PurchaselyWrapper` reads the mode from the repository inside `initialize()` and `restart()`:
  - `android/app/src/main/java/com/purchasely/shaker/purchasely/PurchaselyWrapper.kt`
- Restart path is the wrapper's own `restart()` method (closes the SDK, then re-builds and re-starts it).
- Settings ViewModel:
  - reads/writes via `RunningModeRepository`
  - calls `purchaselyWrapper.restart()` when the mode changes
  - exposes restart-required alert state
  - file: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`
- Settings UI:
  - "Purchasely SDK" segmented control
  - restart-required `AlertDialog`
  - file: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsScreen.kt`

## iOS implementation

- Storage layer is now a dedicated `RunningModeRepository` (UserDefaults-backed):
  - `ios/Shaker/Data/RunningModeRepository.swift`
- `PurchaselyWrapper.initialize()` reads `PurchaselySDKMode.current()` (which consults the repository) before calling `Purchasely.start(...)`.
- Restart behavior:
  - Settings calls `wrapper.restart()`
  - `restart()` invalidates `PresentationCache` (mode change → previous fetches are stale), closes any displayed presentation, then re-starts the SDK with the new running mode.
- Settings ViewModel:
  - persists mode through the repository and emits restart message
  - file: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`
- Settings UI:
  - "Purchasely SDK" segmented picker
  - restart-required alert
  - file: `ios/Shaker/Screens/Settings/SettingsScreen.swift`

## Persistence details

- Storage key: `purchasely_sdk_mode`
- Values:
  - `paywallObserver`
  - `full`
- Default value when missing/invalid: `paywallObserver`

## Validation

- Android build:
  - `./gradlew :app:assembleDebug` -> success
- iOS build:
  - `xcodebuild build -workspace Shaker.xcworkspace -scheme Shaker CODE_SIGNING_ALLOWED=NO` -> success

## Notes

- iOS build without `CODE_SIGNING_ALLOWED=NO` still requires team signing configuration, unrelated to this feature.
