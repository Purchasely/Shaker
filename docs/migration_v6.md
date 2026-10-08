# Migration SDK Purchasely v6

## Contexte

Cette branche migre Shaker vers les APIs Purchasely SDK v6. Android était déjà migré sur la branche ; ce document récapitule Android et les changements iOS ajoutés ensuite.

## Android

Changements présents sur la branche :

- Dépendance Purchasely Android montée en `6.0.0`.
- Mode `PLYRunningMode.PaywallObserver` remplacé par `PLYRunningMode.Observer`.
- Initialisation migrée du builder fluent v5 vers le DSL Kotlin v6 : `Purchasely { context(...); apiKey(...); onInitialized { ... } }`.
- Intercepteur global `setPaywallActionsInterceptor` remplacé par des intercepteurs typés `interceptAction<...>`.
- Ancien callback `processAction(Boolean)` remplacé par `PLYInterceptResult` (`SUCCESS`, `FAILED`, `NOT_HANDLED`).
- Fetch paywall migré vers `PLYPresentation { placementId(...); contentId(...) }.preload()`.
- Résultat d’affichage migré vers `PLYPresentationOutcome` + `PLYPurchaseResult`.
- Deeplinks migrés de `isDeeplinkHandled(...)` vers `handleDeeplink(...)`.
- Tests Android adaptés aux nouveaux types v6.

## iOS

### Dépendance

`Purchasely-iOS-Sources/develop` ne contient pas de `Package.swift` à la racine. Shaker utilise donc un package Swift local dans `ios/LocalPackages/Purchasely` qui pointe vers `../iOS/Purchasely` via symlink.

Le package local :

- expose un produit `Purchasely` compatible SPM ;
- inclut les sources `common`, `specific/ios`, `specific/uikit`, `specific/swiftUI` ;
- exclut tvOS et les tests SDK ;
- ajoute `Exports.swift` pour exporter les imports UIKit/Foundation requis par les sources ;
- remplace le petit fichier Objective-C `PLYLottieView` par un shim Swift, car SwiftPM ne supporte pas les sources mixtes Swift/ObjC dans un même target.

### APIs migrées

- `Purchasely.start(withAPIKey:...)` → `Purchasely.apiKey(...).appUserId(...).runningMode(...).storekitSettings(...).logLevel(...).start { error in ... }`.
- `.paywallObserver` → `.observer` pour `PLYRunningMode`.
- `readyToOpenDeeplink(true)` → `allowDeeplink(true)`.
- `isDeeplinkHandled(deeplink:)` → `handleDeeplink(_:)`.
- `setPaywallActionsInterceptor` → `interceptAction(.login/.navigate/.purchase/.restore)`.
- `processAction(Bool)` → `PLYInterceptResult`.
- `fetchPresentation(for:contentId:fetchCompletion:completion:)` → `PLYPresentationBuilder.from(placementId:).contentId(...).build().preload(...)`.
- Dismiss result v5 `(PLYProductViewControllerResult, PLYPlan?)` → `PLYPresentationOutcome`.
- Embedded SwiftUI rendering no longer uses `controller.PresentationView`; Shaker now wraps `PLYPresentationViewController` with `UIViewControllerRepresentable`.

### Notes d’intégration

- Observer-mode native purchases restent dans `PurchaseManager` (StoreKit 2 côté app).
- Le SDK est démarré avec `.storeKit1` côté Purchasely dans cette branche : le snapshot `develop` v6 scanne StoreKit 2 avant de déclencher le callback d’initialisation, ce qui bloque le flux démo sur simulateur.
- `OnboardingScreen` ne fetch plus le paywall si l’onboarding est déjà marqué terminé.
- Les callbacks `onClose` et `onDismissed` sont installés sur le builder puis réassignés sur la présentation chargée pour couvrir les snapshots develop où les callbacks seedés ne remontent pas toujours.

## Vérification

Commandes exécutées :

```bash
cd ios && xcodegen generate
cd ios && xcodebuild test -project Shaker.xcodeproj -scheme Shaker -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' -quiet
cd android && ./gradlew testDebugUnitTest --quiet
```

Résultat : tests iOS et Android terminés avec code 0.

Vérification simulateur :

- build iOS Debug installé et lancé sur `iPhone 17 (iOS 26.5)` ;
- le paywall/onboarding Purchasely se rend visuellement ;
- interaction automatisée via `cliclick` testée, mais la validation complète des parcours paywall reste à refaire manuellement dans Simulator/Xcode car les coordonnées ont fini par backgrounder l’app.
