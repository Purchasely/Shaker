---
title: "refactor: Stabiliser Shaker et aligner docs/code"
type: refactor
date: 2026-02-17
based_on:
  - docs/plans/2026-02-06-feat-shaker-sample-app-plan.md
---

# Shaker - Plan de stabilisation et d'amelioration

## Overview

Le projet Shaker est deja tres avance sur le scope fonctionnel (phases 1 a 5), mais il reste un ecart entre:
- le code reel
- la documentation
- les garde-fous qualite/securite

Objectif: rendre Shaker fiable et "demo-ready" sans ajouter de nouvelles features produit.

## Etat actuel (snapshot au 2026-02-17)

- Vision et scope du projet definis dans le plan initial: `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md:12`.
- Phases 1 a 5 majoritairement cochees: `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md:224`.
- CI Android + iOS deja en place: `Shaker/.github/workflows/ci.yml:1`.
- 25 cocktails et 25 images presents dans `Shaker/shared-assets/`.
- GDPR implemente dans le code Android/iOS:
  - `Shaker/android/app/src/main/java/com/purchasely/shaker/ui/screen/settings/SettingsViewModel.kt:136`
  - `Shaker/ios/Shaker/Screens/Settings/SettingsViewModel.swift:122`
- Ecart de documentation paywall:
  - README mentionne encore `presentationView/presentationController`: `Shaker/README.md:10`
  - La solution retenue impose `fetchPresentation + display()`: `Shaker/docs/solutions/integration-issues/purchasely-fetchpresentation-pattern.md:141`
- Risque principal securite/config:
  - API key hardcodee Android: `Shaker/android/app/build.gradle.kts:29`
  - API key hardcodee iOS: `Shaker/ios/Shaker/AppViewModel.swift:16`

## Problem Statement

1. Le depot contient des cles API hardcodees, incompatible avec une base de code partagee.
2. La doc publique ne reflete pas toujours le pattern reel de presentation paywall.
3. Le suivi d'avancement est partiellement obsolete (cases globales non alignees avec le code).
4. Les garanties de non-regression sont faibles (quasi pas de tests automatises).

## Proposed Solution

Mettre en place un hardening en 5 phases courtes:
- P0: corriger securite + coherence documentaire.
- P1: industrialiser la validation fonctionnelle et la non-regression.
- P2: finaliser la readiness de demo/release.

## Implementation Phases

### Phase 1 - P0 Security & Runtime Config (1-2 jours)

**But:** supprimer toute cle hardcodee et imposer une config locale explicite.

#### Taches

- Android:
  - remplacer la valeur hardcodee de `PURCHASELY_API_KEY` par `local.properties` dans `Shaker/android/app/build.gradle.kts`.
  - garder `Shaker/android/local.properties.example` comme template.
- iOS:
  - remplacer la cle hardcodee dans `Shaker/ios/Shaker/AppViewModel.swift`.
  - lire la cle depuis `Info.plist` (`$(PURCHASELY_API_KEY)`) et `Config.xcconfig`.
  - conserver `Shaker/ios/Config.xcconfig.example` comme template.
- Ajouter un garde-fou CI anti-secrets (script shell + job CI):
  - detecter motifs `PURCHASELY_API_KEY` hardcodees et UUID d'API key dans le code applicatif.

#### Acceptance Criteria

- [ ] Aucune cle Purchasely en dur dans le code versionne.
- [ ] Build Android et iOS passent avec config locale.
- [ ] CI echoue si une cle hardcodee est reintroduite.

### Phase 2 - P0 Documentation Parity (0.5-1 jour)

**But:** aligner README/plan/solutions avec le comportement reel.

#### Taches

- Mettre a jour `Shaker/README.md`:
  - remplacer `presentationView/presentationController` par `fetchPresentation + display()` dans le tableau SDK.
- Mettre a jour le plan initial `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md`:
  - corriger les cases globales qui ne refletent plus l'etat reel.
  - corriger la ligne GDPR en fonction de l'implementation effective.
- Ajouter une section "Known constraints" dans `Shaker/README.md` (ex: dependance config Console).

#### Acceptance Criteria

- [ ] Plus de contradiction entre README et docs de solution.
- [ ] Le plan principal reflete correctement l'etat du code au 2026-02-17.

### Phase 3 - P1 Validation Matrix Purchasely (1-2 jours)

**But:** rendre la validation fonctionnelle reproductible sur Android et iOS.

#### Taches

- [x] Creer `Shaker/docs/testing/manual-test-matrix.md` avec scenarios:
  - user free / premium / expired
  - placements `onboarding`, `recipe_detail`, `favorites`, `filters`
  - login/logout, restore, deep links, GDPR toggles
- [x] Ajouter preconditions de test:
  - entitlement, placements, plans, produits sandbox.
- [x] Tracer un premier run de validation avec date/resultat par plateforme.

#### Acceptance Criteria

- [x] Matrice complete et executable sans connaissance implicite.
- [x] 1 passage complet Android + iOS documente.
- [x] Chaque echec est lie a une issue/action claire.

### Phase 4 - P1 Tests Automatises Minimals (3-5 jours)

**But:** couvrir les chemins critiques de gating et settings.

#### Taches

- Android (unit tests):
  - `CocktailRepository` parsing/erreurs.
  - `FavoritesRepository` persistence.
  - `SettingsViewModel` consent revocation mapping.
- iOS (unit tests):
  - `SettingsViewModel` consent revocation mapping.
  - logique onboarding/favorites persistence.
- CI:
  - ajouter jobs de tests unitaires Android/iOS en plus des builds.

#### Acceptance Criteria

- [ ] Suite de tests executee en CI sur PR.
- [ ] Couverture des cas critiques de gating/settings validee.
- [ ] Pas de regression fonctionnelle sur flux d'achat/restauration.

### Phase 5 - P2 Demo/Release Readiness (1-2 jours)

**But:** rendre le projet utilisable immediatement par equipe interne/partenaires.

#### Taches

- Ajouter assets de presentation dans `Shaker/docs/`:
  - screenshots Android/iOS
  - mini walkthrough video ou GIF
- Ajouter un guide "from clone to run" ultra court:
  - prerequis
  - setup config
  - commandes build/run
  - checklist de smoke test
- Ajouter une section "Troubleshooting rapide" (paywall non charge, key manquante, deep link).

#### Acceptance Criteria

- [ ] Un nouveau dev peut lancer Android+iOS en suivant uniquement la doc Shaker.
- [ ] Les points de friction connus sont documentes avec resolution.

## Overall Acceptance Criteria

### Functional

- [ ] Aucun secret sensible hardcode dans le repo.
- [ ] Tous les paywalls documentes utilisent le pattern `fetchPresentation + display`.
- [ ] Validation manuelle completee sur les 2 plateformes.

### Non-Functional

- [ ] CI couvre build + tests unitaires + garde-fou anti-secrets.
- [ ] Documentation coherent entre plan, README, solutions.

### Quality Gates

- [ ] Verification de setup depuis clone propre (Android+iOS).
- [ ] Relecture finale docs pour coherence technique.

## Dependencies & Risks

| Dependency / Risk | Impact | Mitigation |
|---|---|---|
| Configuration Console incomplete | bloque tests paywall | checklist pre-test + verification preconditions |
| Reintroduction de secrets | risque securite | job CI anti-secrets + review checklist |
| Divergence Android/iOS | incoherence demo | matrice de validation commune par scenario |
| Temps de CI iOS | feedback plus lent | separer build et tests, paralleliser jobs |

## Deliverables (fichiers cibles)

- `Shaker/android/app/build.gradle.kts`
- `Shaker/ios/Shaker/AppViewModel.swift`
- `Shaker/README.md`
- `docs/plans/2026-02-06-feat-shaker-sample-app-plan.md`
- `Shaker/docs/testing/manual-test-matrix.md` (nouveau)
- `Shaker/.github/workflows/ci.yml`

## Suggested Execution Order

1. Phase 1 (P0) - securite/config
2. Phase 2 (P0) - doc parity
3. Phase 3 (P1) - validation matrix
4. Phase 4 (P1) - tests automatises
5. Phase 5 (P2) - demo readiness
