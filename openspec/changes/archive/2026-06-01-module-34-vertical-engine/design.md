## Context

TapLog's inspection flow, asset type catalog, and form layout are all hardcoded to Ember (fire safety). `OFCAssetTypes.kt` is a static Kotlin enum embedded in the binary; `InspectionFormScreen` has a hardcoded layout for Ember's result/notes/deficiency model. To add any new vertical (e.g. Anchor for fall protection, Hatch for confined space), the team would need new screens, new ViewModels, new Room tables, and a Play Store release. This module replaces that hardcoded surface with a data-driven engine backed by MongoDB.

## Goals / Non-Goals

**Goals:**
- Introduce `VerticalConfig` / `VerticalRegistry` as the single source of truth for per-vertical behaviour on Android
- Migrate Ember's asset type catalog to the backend as the first `VerticalConfig` document
- Refactor `InspectionFormScreen` to render `FormField` definitions from `VerticalConfig.formProfile.fields`; all existing Ember behaviour is preserved
- Add Room v7 `vertical_configs` cache table for offline-first operation
- Add `GET /api/v1/verticals` and `GET /api/v1/verticals/{code}` backend endpoints gated by `licensedVerticals`
- Add `licensedVerticals` to the Organisation model as a billing hook

**Non-Goals:**
- Adding any vertical other than Ember in this module (Module 35 adds Anchor, Module 36 adds Hatch)
- Implementing actual MULTI_ROLE form logic — `EntryEventScreen` is a shell stub only
- Removing `OFCAssetTypes.kt` — it remains as a local fallback until the cache is proven in production
- Any changes to sync pipeline, auth, PDF report generator, or existing Room migrations 1–6

## Decisions

**Backend-driven config fetched at startup, cached in Room**
The whole point of the vertical engine is that new verticals require no Play Store update. Config must live on the server. However, TapLog is offline-first, so the config must be cached locally. Room is the right store — it already holds all other app state, and TypeConverters handle JSON serialization. The app fetches on every startup with network and falls back to the cached row when offline.

*Alternative considered*: Bundle a default Ember config as a JSON asset in the APK as the fallback. Rejected — it creates a second source of truth alongside `OFCAssetTypes.kt` during the transition, and the Room cache achieves the same offline guarantee once populated after first login.

**`OFCAssetTypes.kt` retained as a local fallback, not deleted**
Deleting it in this module would require proving the backend fetch-and-cache path works in production before shipping. Retaining it as a fallback means the Ember path can never break even if the backend migration has a data error. It will be removed in a follow-on cleanup module once the cache is validated.

*Alternative considered*: Delete immediately and rely solely on the backend. Rejected — too risky for a module that also refactors InspectionFormScreen.

**`InspectionFormScreen` reads `VerticalConfig.formProfile.fields` directly from `VerticalRegistry`**
The screen receives `asset.vertical` (a `TapLogVertical` enum value). It calls `VerticalRegistry.get(asset.vertical)` to get the config, then renders each `FormField` in `formProfile.fields`. This is the minimal change: no new ViewModel methods, no new navigation arguments. The Ember config's `fields` list is authored to exactly reproduce the current Ember hardcoded layout.

*Alternative considered*: Pass the `VerticalConfig` as a nav argument. Rejected — `VerticalConfig` is a large object; `TapLogVertical` is an enum and already available on the Asset.

**`VerticalRegistry` as a companion-object singleton populated at startup**
`TapLogApplication.onCreate()` launches a coroutine to fetch configs from the backend and call `VerticalRegistry.register()`. The registry throws if `get()` is called before the vertical is registered, so any screen attempting to open before startup fetch completes will crash visibly rather than silently degrading. This is acceptable — startup already shows a SplashScreen.

**`licensedVerticals` defaults to `["EMBER"]` on the backend; Android reads it from the Organisation sync response**
The field is a billing hook only. For all existing orgs it defaults to EMBER so no migration is needed. Android stores it on the `Organisation` Room entity; the verticals API uses it server-side to filter the response.

## Risks / Trade-offs

- [Risk] Backend `VerticalConfig` document for Ember is incorrect on first deploy → Mitigation: `OFCAssetTypes.kt` is the fallback; app will use static data if the Room cache is empty or the fetch fails
- [Risk] Room migration 6→7 has a bug on existing user devices → Mitigation: migration is additive (new table only, no column changes to existing tables); rollback is to ship v6 schema with a destructive migration if needed
- [Risk] `VerticalRegistry.get()` called before startup fetch completes on a slow network → Mitigation: SplashScreen waits for VerticalRegistry to have at least one entry before navigating; if fetch fails, cached Room data is loaded synchronously
- [Risk] `InspectionFormScreen` refactor regresses existing Ember inspection flow → Mitigation: Ember's `VerticalConfig` fields list is designed to be a 1:1 reproduction of the current hardcoded layout; existing behaviour is the acceptance criterion

## Migration Plan

1. Deploy backend changes first: new `verticals` collection seeded with Ember config, new endpoints, `licensed_verticals` added to Organisation model (defaults to `["EMBER"]`)
2. Ship Android update with Room v7 migration, `VerticalRegistry`, and refactored `InspectionFormScreen`
3. On first app launch post-update: startup fetch populates Room cache; `OFCAssetTypes.kt` fallback is available if fetch fails
4. Monitor: if backend Ember config is wrong, hotfix the MongoDB document — no Play Store update needed
5. Rollback: if Room migration fails, revert Android to v6 schema via a forced migration; backend changes are backward-compatible

## Open Questions

- Should the startup fetch block the SplashScreen entirely, or show a progress indicator and allow offline-only mode with cached config? (Current decision: block on SplashScreen until registry is populated or cache is loaded)
- What is the exact Room TypeConverter strategy for `VerticalConfig`? (Recommended: serialize the entire `VerticalConfig` as a JSON string in a single `TEXT` column — keeps the migration trivial and avoids a complex nested-table schema for config data)
