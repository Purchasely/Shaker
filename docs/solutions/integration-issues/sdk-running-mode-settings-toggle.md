---
title: "Toggle Purchasely SDK running mode from Settings"
category: integration-issues
tags: [purchasely, android, ios, settings, running-mode, paywallObserver, full]
module: SDK Integration
symptoms:
  - "Need to switch SDK between full and paywallObserver at runtime"
  - "Mode choice should persist across app relaunch"
  - "Need restart behavior and user guidance after mode change"
severity: medium
date_solved: 2026-02-17
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

- New enum and storage contract:
  - `android/app/src/main/java/com/purchasely/shaker/data/PurchaselySdkMode.kt`
- SDK init now resolves running mode from storage:
  - `android/app/src/main/java/com/purchasely/shaker/ShakerApp.kt`
- Added explicit restart path:
  - `ShakerApp.restartPurchaselySdk()` -> `Purchasely.close()` then `initPurchasely()`
- Settings ViewModel:
  - reads/writes `purchasely_sdk_mode`
  - calls app restart when mode changes
  - exposes restart-required alert state
  - file: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt`
- Settings UI:
  - new "Purchasely SDK" segmented control
  - restart-required `AlertDialog`
  - file: `android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsScreen.kt`

## iOS implementation

- Added mode enum + storage helpers:
  - `ios/Shaker/AppViewModel.swift`
  - key: `purchasely_sdk_mode`
  - default: `.paywallObserver`
- SDK initialization now uses stored mode:
  - `Purchasely.start(... runningMode: selectedMode.runningMode, ...)`
- Restart behavior:
  - Settings posts `.purchaselySdkModeDidChange`
  - `AppViewModel` observes notification and restarts SDK (`closeDisplayedPresentation()` + `start(...)`)
- Settings ViewModel:
  - persists mode and emits restart message
  - file: `ios/Shaker/Screens/Settings/SettingsViewModel.swift`
- Settings UI:
  - new "Purchasely SDK" segmented picker
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
