# Contributing to Shaker

Thanks for your interest in contributing!

## Setup

### Android
1. Open `android/` in Android Studio
2. Sync Gradle
3. Run on emulator or device

### iOS
1. Run `cd ios && pod install && xcodegen generate && pod install`
2. Open `Shaker.xcworkspace` in Xcode
3. Build and run on simulator

## Guidelines

- Follow existing code patterns and naming conventions
- Use Conventional Commits: `feat(scope): description`, `fix(scope): description`
- Test on both platforms when possible
- Keep the demo API key for public builds

## Adding a New SDK Feature

1. Implement on both Android and iOS
2. Add `// PURCHASELY:` inline comments on SDK calls
3. Update `docs/INTEGRATION_GUIDE.md` with the new feature
4. Update this README's feature checklist

## Reporting Issues

Open an issue on GitHub with:
- Platform (Android/iOS)
- SDK version
- Steps to reproduce
- Expected vs actual behavior
