## 1. Dependencies and config

- [x] 1.1 Add Mapbox version to `gradle/libs.versions.toml`: `mapbox = "11.9.0"` plus library entries for `mapbox-maps` and `mapbox-compose`
- [x] 1.2 Add `implementation(libs.mapbox.maps)` and `implementation(libs.mapbox.compose)` to `app/build.gradle.kts`
- [x] 1.3 Add `buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", ...)` to `app/build.gradle.kts` reading from `local.properties`
- [x] 1.4 Add `MAPBOX_PUBLIC_TOKEN=pk.eyJ1...` to `local.properties` (user supplies their own token)
- [x] 1.5 Add Mapbox token `<meta-data>` to `AndroidManifest.xml` inside `<application>`

## 2. Data model — Site lat/lng + Room v9

- [x] 2.1 Add `latitude: Double? = null` and `longitude: Double? = null` to the `Site` entity in `EmberModels.kt`
- [x] 2.2 Add `MIGRATION_8_9` to `AppDatabase.kt`: `ALTER TABLE sites ADD COLUMN latitude REAL` and `ALTER TABLE sites ADD COLUMN longitude REAL`
- [x] 2.3 Bump `AppDatabase` version to 9 and register `MIGRATION_8_9` in the builder

## 3. Geocoding

- [x] 3.1 Create `GeocodingRepository.kt` in `ca.taplog.app.data` — `geocode(address, city, province)` calls Mapbox REST API with `country=CA&limit=1`, runs on `Dispatchers.IO`, returns `Pair<Double, Double>?`
- [x] 3.2 Register `val geocodingRepository by lazy { GeocodingRepository(BuildConfig.MAPBOX_PUBLIC_TOKEN) }` in `TapLogApplication`
- [x] 3.3 Update `EmberViewModel.saveSite()` — after `repository.insertSite(site)`, launch a background coroutine to geocode and upsert with coords if successful
- [x] 3.4 Add `geocodeUnresolvedSites(sites: List<Site>)` to `EmberViewModel` — filters sites with null coords, geocodes sequentially, upserts each on success

## 4. Sync model update

- [x] 4.1 Add `latitude: Double?` and `longitude: Double?` to `SiteSyncRequest` in `SyncModels.kt`
- [x] 4.2 Update `Site.toSyncRequest()` to include `latitude = latitude, longitude = longitude`

## 5. Map composable

- [x] 5.1 Create `SiteMapView.kt` in `ca.taplog.app.ui.ember` — `MapboxMap` composable with `Style.DARK`, camera initialised on site centroid (zoom 10) or Ontario fallback (zoom 7)
- [x] 5.2 Add `ViewAnnotation` per geocoded site rendering a `SitePin` composable (labelled card + triangle indicator)
- [x] 5.3 `SitePin` composable — `Card` with site name, `Canvas` triangle below; `Modifier.clickable { onClick() }`
- [x] 5.4 Show unmapped site count at bottom of map view when any sites have null coords

## 6. SiteListScreen toggle

- [x] 6.1 Add `ViewMode` enum (`LIST`, `MAP`) and `var viewMode by remember { mutableStateOf(ViewMode.LIST) }` to `SiteListScreen`
- [x] 6.2 Add list/map icon toggle buttons to the `TopAppBar` trailing content — active button primary-coloured
- [x] 6.3 When `viewMode == ViewMode.MAP`, call `viewModel.geocodeUnresolvedSites(sites)` once on first switch, then render `SiteMapView`
- [x] 6.4 Confirm tapping a pin calls `onSiteSelected(site)` — same navigation path as list row tap

## 7. Backend — accept lat/lng on site upsert

- [x] 7.1 Add `latitude: Optional[float] = None` and `longitude: Optional[float] = None` to `SiteIn` in `app/models.py`
- [x] 7.2 Confirm `sites.py` router passes these through via `model_dump()` — no router code change needed if Pydantic model is updated

## 8. Validation

- [x] 8.1 Add `MAPBOX_PUBLIC_TOKEN` to `local.properties` and confirm the app builds without token errors
- [ ] 8.2 Save a new site — confirm geocoding runs in background and lat/lng appear in Room (check with App Inspection)
- [ ] 8.3 Open map view — confirm existing sites without coords are absent; confirm lazy geocoding runs and pins appear
- [ ] 8.4 Tap a site pin — confirm navigation to `SiteDetailScreen` for correct site
- [ ] 8.5 Confirm dark map style renders correctly and matches app theme
- [ ] 8.6 Confirm unmapped count message appears when any sites lack coords
- [ ] 8.7 Confirm Room migration 8→9 runs cleanly (clear app storage if testing on dirty DB)
- [x] 8.8 Build project with no compilation errors
