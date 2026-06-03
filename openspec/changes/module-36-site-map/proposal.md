## Why

Inspectors and company owners think in geography — which sites are overdue in the eastern corridor, which buildings cluster around a commercial district. The current list view gives no spatial context, making portfolio review and route planning mental work instead of visual work. Mapbox gives TapLog a map view that requires no backend changes and costs nothing at pilot scale.

## What Changes

- `Site` entity gains `latitude: Double?` and `longitude: Double?` — Room v8 → v9 migration
- `GeocodingRepository.kt` added — Mapbox Geocoding REST API via OkHttp; geocodes on site save; non-fatal on failure
- `SiteListScreen` gains a list/map toggle in the `TopAppBar`; map view renders a `MapboxMap` composable with a labelled pin per site; tapping a pin calls the existing `onSiteSelected` callback
- `SiteMapView.kt` added — new composable; sites without coords are absent from the map; lazy geocoding runs on first map open for existing sites
- `SyncModels.kt` updated — `latitude`/`longitude` added to `SiteSyncRequest` and `toSyncRequest()`
- Mapbox SDK added to dependencies (`com.mapbox.maps:android:11.9.0` + compose extension)
- Backend `sites.py` Pydantic model updated to accept optional lat/lng

## Capabilities

### New Capabilities
- `site-map-view`: List/map toggle on SiteListScreen; Mapbox dark-style map with one pin per geocoded site; pin tap navigates to SiteDetailScreen; lazy geocoding for existing sites without coords
- `site-geocoding`: Address geocoded to lat/lng at save time via Mapbox Geocoding REST API; coords stored on Site entity and synced to backend; failure is silent and non-blocking

### Modified Capabilities
- `org-site-sync`: `SiteSyncRequest` gains optional `latitude` and `longitude` fields

## Impact

- `EmberModels.kt` — `Site` gains two nullable fields
- `AppDatabase.kt` — version 9, `MIGRATION_8_9`
- `SyncModels.kt` — `SiteSyncRequest` + `toSyncRequest()` updated
- `EmberViewModel.kt` — `saveSite()` triggers geocoding post-save; new `geocodeUnresolvedSites()` function
- `TapLogApplication.kt` — registers `GeocodingRepository`
- `SiteListScreen.kt` — toggle added, `SiteMapView` rendered in map mode
- New files: `GeocodingRepository.kt`, `SiteMapView.kt`
- `app/build.gradle.kts`, `libs.versions.toml` — Mapbox dependencies
- `AndroidManifest.xml` — Mapbox token meta-data
- `local.properties` — `MAPBOX_PUBLIC_TOKEN` (user must add their own token)
- Backend `app/routers/sites.py` / `app/models.py` — optional lat/lng on `SiteIn`
