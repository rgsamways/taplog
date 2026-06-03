## ADDED Requirements

### Requirement: Site entity carries latitude and longitude
The `Site` Room entity SHALL include `latitude: Double? = null` and `longitude: Double? = null` fields. Room database SHALL be version 9. `MIGRATION_8_9` SHALL add both columns to the `sites` table as nullable REAL columns without modifying any existing data.

#### Scenario: Migration is additive
- **WHEN** the app is updated on a device running Room version 8
- **THEN** the database migrates to version 9 with `latitude` and `longitude` columns added to `sites`; all existing site records are preserved with null values for the new columns

#### Scenario: New site has null coords initially
- **WHEN** a site is saved to Room before geocoding completes
- **THEN** `latitude` and `longitude` are null

### Requirement: Address is geocoded at site save time
After a site is saved to Room, `EmberViewModel.saveSite()` SHALL launch a background coroutine that calls `GeocodingRepository.geocode(address, city, province)` using the Mapbox Geocoding REST API. On success, the site SHALL be updated in Room with the returned coordinates. Geocoding failure SHALL be silent and non-blocking — the site saves and the UI proceeds regardless.

#### Scenario: Successful geocode updates site with coordinates
- **WHEN** a site is saved and geocoding returns valid coordinates
- **THEN** the site's Room record is updated with the latitude and longitude; the UI proceeds to `ScanState.SiteList` without waiting for geocoding

#### Scenario: Failed geocode does not block save
- **WHEN** geocoding fails (network error, no results, or API error)
- **THEN** the site is saved with null coordinates; no error is shown to the inspector; the UI proceeds normally

#### Scenario: Geocoding targets Canada
- **WHEN** `GeocodingRepository.geocode()` is called
- **THEN** the Mapbox API request includes `country=CA` to restrict results to Canada

### Requirement: GeocodingRepository uses Mapbox Geocoding REST API
`GeocodingRepository` SHALL call `https://api.mapbox.com/geocoding/v5/mapbox.places/{query}.json` with `country=CA&limit=1&access_token={token}`. The query SHALL be the concatenation of `address`, `city`, `province`, and `"Canada"`. The call SHALL run on `Dispatchers.IO`. The repository SHALL return `Pair<Double, Double>?` (lat, lng) or null on any failure.

#### Scenario: Coordinates extracted from API response
- **WHEN** the Mapbox API returns a valid feature with geometry coordinates
- **THEN** `GeocodingRepository` returns `Pair(latitude, longitude)` where latitude is `coordinates[1]` and longitude is `coordinates[0]`

#### Scenario: Empty feature list returns null
- **WHEN** the Mapbox API returns a response with an empty features array
- **THEN** `GeocodingRepository` returns null
