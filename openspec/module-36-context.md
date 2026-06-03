# Module 36 — Site Map (Mapbox)
> Generated from strategy session 2026-05-31
> Save to `openspec/module-36-context.md` in the repo.
> Fun module — no compliance logic, no migrations (other than lat/lng on Site).

---

## What this module is

Adds a list/map toggle to `SiteListScreen`. Map view shows all sites as pins on a Mapbox
map. Tapping a pin navigates to the existing `SiteDetailScreen`. Sites are geocoded at
save time; existing sites without coords are geocoded lazily on first map open.

---

## Scope

**In scope:**
- `Site` entity gains `latitude: Double?` and `longitude: Double?` — Room v8 → v9 migration
- Mapbox SDK added to dependencies
- `SiteRegistrationScreen` geocodes the address on save via Mapbox Geocoding REST API
- `SiteListScreen` gains a list/map toggle in the top bar
- Map view renders a `MapboxMap` composable with a pin per site
- Tapping a pin calls `onSiteSelected(site)` — same callback as the list rows
- Sites with null coords are geocoded lazily when map is first opened
- Sites that fail geocoding show in the list view only, not on the map

**Out of scope:**
- User location / GPS tracking
- Routing or directions between sites
- Clustering of pins (can add later if many sites)
- Offline map tiles

---

## Dependencies to add

```kotlin
// app/build.gradle.kts

// Mapbox Maps SDK for Android
implementation("com.mapbox.maps:android:11.9.0")

// Mapbox Compose extension
implementation("com.mapbox.extension:maps-compose:11.9.0")
```

Also add to `gradle/libs.versions.toml`:
```toml
[versions]
mapbox = "11.9.0"

[libraries]
mapbox-maps = { group = "com.mapbox.maps", name = "android", version.ref = "mapbox" }
mapbox-compose = { group = "com.mapbox.extension", name = "maps-compose", version.ref = "mapbox" }
```

Mapbox API key goes in `local.properties` (already present per project setup):
```
MAPBOX_PUBLIC_TOKEN=pk.eyJ1...
```

And in `app/build.gradle.kts` BuildConfig:
```kotlin
buildConfigField("String", "MAPBOX_PUBLIC_TOKEN", "\"${localProperties["MAPBOX_PUBLIC_TOKEN"]}\"")
```

And in `AndroidManifest.xml` inside `<application>`:
```xml
<meta-data
    android:name="com.mapbox.token"
    android:value="${MAPBOX_PUBLIC_TOKEN}" />
```

---

## Data model changes

### Site entity (EmberModels.kt)

Add two nullable fields:

```kotlin
@Entity(tableName = "sites")
data class Site(
    @PrimaryKey val id: String,
    val organisationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val postalCode: String,
    val clientName: String,
    val clientPhone: String,
    val contactName: String,
    val contactPhone: String,
    val notes: String,
    val isSynced: Boolean = false,
    val latitude: Double? = null,    // ADD
    val longitude: Double? = null    // ADD
)
```

### Room migration v8 → v9

```kotlin
val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE sites ADD COLUMN latitude REAL")
        database.execSQL("ALTER TABLE sites ADD COLUMN longitude REAL")
    }
}
```

Register in `AppDatabase`:
```kotlin
.addMigrations(MIGRATION_8_9)
```

Update `AppDatabase` version to 9.

---

## Geocoding

Use the Mapbox Geocoding REST API directly via `httpx` on the backend, or via a simple
`HttpURLConnection` / OkHttp call on Android. Android-side geocoding is simpler for this
module since we already have OkHttp in the project (via Retrofit).

### Geocoding function (new file: `GeocodingRepository.kt`)

```kotlin
class GeocodingRepository(private val mapboxToken: String) {

    suspend fun geocode(address: String, city: String, province: String): Pair<Double, Double>? {
        val query = "$address, $city, $province, Canada"
        val encoded = URLEncoder.encode(query, "UTF-8")
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json" +
                  "?country=CA&limit=1&access_token=$mapboxToken"

        return withContext(Dispatchers.IO) {
            try {
                val response = URL(url).readText()
                val json = JSONObject(response)
                val features = json.getJSONArray("features")
                if (features.length() == 0) return@withContext null
                val coords = features.getJSONObject(0)
                    .getJSONObject("geometry")
                    .getJSONArray("coordinates")
                val lng = coords.getDouble(0)
                val lat = coords.getDouble(1)
                Pair(lat, lng)
            } catch (e: Exception) {
                null  // Geocoding failure is non-fatal — site saves without coords
            }
        }
    }
}
```

Register `GeocodingRepository` in `TapLogApplication`:
```kotlin
val geocodingRepository by lazy {
    GeocodingRepository(BuildConfig.MAPBOX_PUBLIC_TOKEN)
}
```

### Geocoding on save (SiteRegistrationScreen / EmberViewModel)

In `EmberViewModel.saveSite()`, after saving to Room, attempt geocoding:

```kotlin
fun saveSite(...) {
    viewModelScope.launch {
        val site = Site(id = UUID.randomUUID().toString(), ...)
        repository.insertSite(site)

        // Geocode in background — non-blocking, non-fatal
        val coords = geocodingRepository.geocode(address, city, province)
        if (coords != null) {
            val updated = site.copy(latitude = coords.first, longitude = coords.second)
            repository.insertSite(updated)   // upsert by primary key
        }

        _scanState.value = ScanState.SiteList
    }
}
```

### Lazy geocoding for existing sites (on map open)

When map view is first opened, iterate sites with null coords and geocode them:

```kotlin
fun geocodeUnresolvedSites(sites: List<Site>) {
    viewModelScope.launch {
        sites.filter { it.latitude == null }.forEach { site ->
            val coords = geocodingRepository.geocode(site.address, site.city, site.province)
            if (coords != null) {
                repository.insertSite(
                    site.copy(latitude = coords.first, longitude = coords.second)
                )
            }
        }
    }
}
```

---

## SiteListScreen changes

### Toggle state

Add a `viewMode` toggle — `LIST` or `MAP`:

```kotlin
var viewMode by remember { mutableStateOf(ViewMode.LIST) }

enum class ViewMode { LIST, MAP }
```

### Top bar toggle buttons

Add two icon buttons to the existing `TopAppBar` trailing content:

```kotlin
IconButton(onClick = { viewMode = ViewMode.LIST }) {
    Icon(
        imageVector = if (viewMode == ViewMode.LIST)
            Icons.Default.ViewList else Icons.AutoMirrored.Outlined.ViewList,
        contentDescription = "List view",
        tint = if (viewMode == ViewMode.LIST)
            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
IconButton(onClick = { viewMode = ViewMode.MAP }) {
    Icon(
        imageVector = Icons.Default.Map,
        contentDescription = "Map view",
        tint = if (viewMode == ViewMode.MAP)
            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}
```

### Map view composable (new: `SiteMapView.kt`)

```kotlin
@Composable
fun SiteMapView(
    sites: List<Site>,
    onSiteSelected: (Site) -> Unit
) {
    val sitesWithCoords = sites.filter { it.latitude != null && it.longitude != null }

    // Centre on the centroid of all sites, fallback to Ontario
    val initialCamera = if (sitesWithCoords.isNotEmpty()) {
        val avgLat = sitesWithCoords.map { it.latitude!! }.average()
        val avgLng = sitesWithCoords.map { it.longitude!! }.average()
        CameraOptions.Builder().center(Point.fromLngLat(avgLng, avgLat)).zoom(10.0).build()
    } else {
        CameraOptions.Builder().center(Point.fromLngLat(-79.3832, 43.6532)).zoom(7.0).build()
    }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapInitOptionsFactory = { context ->
            MapInitOptions(
                context = context,
                styleUri = Style.DARK,   // Dark style matches TapLog navy theme
                cameraOptions = initialCamera
            )
        }
    ) {
        sitesWithCoords.forEach { site ->
            ViewAnnotation(
                options = viewAnnotationOptions {
                    geometry(Point.fromLngLat(site.longitude!!, site.latitude!!))
                    allowOverlap(false)
                }
            ) {
                SitePin(
                    site = site,
                    onClick = { onSiteSelected(site) }
                )
            }
        }
    }

    // Warn if any sites couldn't be mapped
    val unmapped = sites.size - sitesWithCoords.size
    if (unmapped > 0) {
        Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
            Text(
                text = "$unmapped site${if (unmapped > 1) "s" else ""} not yet geocoded",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### SitePin composable

```kotlin
@Composable
fun SitePin(site: Site, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            Text(
                text = site.name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        // Pin triangle
        Canvas(modifier = Modifier.size(width = 12.dp, height = 6.dp)) {
            val path = Path()
            path.moveTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.lineTo(size.width / 2, size.height)
            path.close()
            drawPath(path, color = Color(0xFF0B1F3A))
        }
    }
}
```

---

## Map style

Use `Style.DARK` — matches the TapLog navy brand. The dark Mapbox style with navy/teal
pins looks sharp. If Mapbox token gives access to custom styles, a custom style URL can
be substituted later.

---

## SyncModels update

Add `latitude` and `longitude` to `SiteSyncRequest` and `toSyncRequest()` so coords sync
to the backend:

```kotlin
data class SiteSyncRequest(
    ...
    val latitude: Double?,
    val longitude: Double?
)

fun Site.toSyncRequest() = SiteSyncRequest(
    ...
    latitude = latitude,
    longitude = longitude
)
```

Also update the backend `Site` Pydantic model to accept these fields (optional, nullable).

---

## File summary

| File | Change |
|---|---|
| `EmberModels.kt` | Add `latitude`, `longitude` to `Site` |
| `AppDatabase.kt` | Version 8 → 9, add `MIGRATION_8_9` |
| `GeocodingRepository.kt` | New — Mapbox geocoding REST call |
| `TapLogApplication.kt` | Register `geocodingRepository` |
| `EmberViewModel.kt` | Geocode on save, `geocodeUnresolvedSites()` |
| `SiteListScreen.kt` | Add `ViewMode` toggle, render `SiteMapView` when MAP |
| `SiteMapView.kt` | New — Mapbox composable + site pins |
| `SyncModels.kt` | Add lat/lng to `SiteSyncRequest` |
| `app/build.gradle.kts` | Add Mapbox dependencies + BuildConfig field |
| `libs.versions.toml` | Add Mapbox version catalog entries |
| `AndroidManifest.xml` | Add Mapbox token meta-data |
| `app/routers/sites.py` | Accept optional lat/lng on site upsert |

---

## Validation checklist

- [ ] Existing sites without coords show in list view, absent from map
- [ ] New site saved → geocoded → pin appears on map immediately
- [ ] Tapping a pin navigates to SiteDetailScreen
- [ ] Map opens centred on the cluster of sites
- [ ] Dark map style matches app theme
- [ ] `Style.DARK` renders correctly — no token errors
- [ ] Lazy geocoding runs on map open for sites missing coords
- [ ] Unmapped count shown at bottom of map view
- [ ] Toggle persists within the session (not across sessions — that's fine)
