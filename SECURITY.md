# Security Policy

## Reporting a Vulnerability

If you discover a security vulnerability in this sample app, please **do not open a public issue**.

Instead, email **security@purchasely.com** with:

- A description of the issue
- Steps to reproduce
- The affected platform(s) and SDK version(s)
- Any proof-of-concept code or screenshots

We will acknowledge receipt within 3 business days and aim to provide an initial assessment within 7 business days.

## Scope

This repository is a **demo/reference application** showcasing Purchasely SDK integration patterns. It is not intended for production use. The shipped demo Purchasely API key is intentionally public — please do not report its presence as a vulnerability.

For vulnerabilities in the **Purchasely SDK itself**, please refer to the dedicated SDK repositories:

- iOS SDK: https://github.com/Purchasely/Purchasely-iOS
- Android SDK: https://github.com/Purchasely/Purchasely-Android

## Out of Scope

- The hardcoded demo Purchasely API key in `android/app/build.gradle.kts` and `ios/Shaker/AppViewModel.swift`
- The fallback debug keystore password `shaker2026` in `android/app/build.gradle.kts` (used only for local debug signing)
- Cosmetic, dependency-version-only, or denial-of-service reports without a working PoC
