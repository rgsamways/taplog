## Context

`SiteListScreen` currently shows all sites in a `LazyColumn`. Inspectors have no spatial view of their portfolio — site priority and geographic clustering are invisible. The Mapbox Maps SDK for Android provides a production-quality map composable with minimal integration complexity. OkHttp is already in the project (via Retrofit), so geocoding via a simple REST call requires no new network dependency.

## Goals / Non-Goals

**Goals:**
- Add a list/map toggle to `SiteListScreen`; map shows one labelled pin per geocoded site
- Geocode site addresses at save time; store lat/lng on the `Site` Room entity
- Lazy-geocode existing sites without coords when map is first opened
- Sync lat/lng to the backend as optional fields on `SiteSyncRequest`
- Room v8 → v9 migration (additive; two nullable REAL columns on `sites`)

**Non-Goals:**
- User location / GPS tracking
- Routing or directions
- Pin clustering
- Offline map tiles
- Geocoding on the backend (Android-side only; backend stores whatever Android sends)

## Decisions

**Mapbox SDK v11.9.0 with Compose extension**
The Compose extension (`com.mapbox.extension:maps-compose`) provides a `MapboxMap` composable that integrates directly in the existing Compose UI without a Fragment or custom View. v11.x is the current stable release with native Compose support. No alternatives considered — this is the standard path for Mapbox + Compose.

**Geocoding via direct OkHttp call, not Mapbox Android SDK geocoding**
The Mapbox Android SDK includes a geocoding module, but it adds significant APK weight and requires the full SDK to be initialized. The Mapbox Geocoding REST API (`/geocoding/v5/mapbox.places/`) is a simple JSON endpoint that can be called with `URL.readText()` or OkHttp — zero additional dependencies, no SDK ceremony. The context file uses `URL.readText()` for simplicity; this is acceptable for a non-critical background operation.

**Geocoding on save is fire-and-forget, non-blocking**
`saveSite()` inserts the site immediately, then launches a background coroutine to geocode. The site appears in the list instantly; the map pin appears after geocoding completes (typically < 500ms on connectivity). If geocoding fails, the site exists in the list but not on the map — acceptable and communicated to the user via an unmapped count indicator.

**`Style.DARK` matches TapLog navy brand**
The dark Mapbox style (navy/dark grey base) pairs naturally with the `NavyBg = Color(0xFF0B1F3A)` splash and TapLog's overall dark brand. No custom style required at pilot scale.

**ViewAnnotation for site pins, not PointAnnotation**
`ViewAnnotation` allows arbitrary Compose composables as map markers, enabling the labelled card + triangle pin design from the context doc. `PointAnnotation` only supports bitmaps. The tradeoff is slightly higher rendering overhead per pin — irrelevant at pilot scale (< 50 sites).

**Toggle state is session-local (not persisted)**
The list/map preference resets to LIST on app restart. Persisting it to DataStore adds complexity for marginal gain at pilot scale. A returning user clicks one button to restore map view.

## Risks / Trade-offs

- [Risk] Mapbox public token exposed in APK via `BuildConfig` → Mitigation: Mapbox public tokens are designed to be embedded in apps (they're scoped to specific operations and restricted by URL/bundle ID in the Mapbox dashboard). This is standard practice, not a security risk.
- [Risk] Geocoding API rate limits during lazy geocoding of many existing sites → Mitigation: lazy geocoding is sequential (not parallel) per the context doc, with each site geocoded one at a time. At pilot scale (< 20 sites), this is never an issue.
- [Risk] Sites in rural Ontario return imprecise or null geocoding results → Mitigation: geocoding failure is explicitly non-fatal; the site appears in list view only with no error surfaced to the user.
- [Risk] Mapbox SDK increases APK size significantly → Mitigation: Mapbox Maps SDK adds ~8–12 MB to the APK. Acceptable for a field app; can be addressed with ProGuard/R8 shrinking if needed.
- [Risk] Room v8 → v9 migration on existing user devices → Mitigation: migration is purely additive (two nullable REAL columns, no data changes). No existing data at risk.

## Migration Plan

1. Add Mapbox token to `local.properties` (user step — token not in repo)
2. Ship Android update with Room v9 migration — migration runs automatically on first launch
3. Existing sites without coords appear in list view only until geocoded lazily on first map open
4. No backend migration required — `latitude`/`longitude` are optional fields; existing site records without them are unaffected
