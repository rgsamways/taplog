## ADDED Requirements

### Requirement: SiteListScreen provides a list/map view toggle
`SiteListScreen` SHALL display two toggle icon buttons in the `TopAppBar` trailing slot — one for list view and one for map view. The active view SHALL be visually distinguished by tinting the active icon with `MaterialTheme.colorScheme.primary`. The toggle state SHALL be session-local (not persisted across app restarts).

#### Scenario: Default view is list
- **WHEN** the inspector navigates to `SiteListScreen`
- **THEN** the list view is shown and the list icon button is primary-coloured

#### Scenario: Tapping map toggle switches to map view
- **WHEN** the inspector taps the map icon button
- **THEN** `SiteMapView` replaces the site list and the map icon button becomes primary-coloured

#### Scenario: Tapping list toggle returns to list view
- **WHEN** the inspector is in map view and taps the list icon button
- **THEN** the `LazyColumn` site list is shown again

### Requirement: Map view renders one pin per geocoded site
`SiteMapView` SHALL render a `MapboxMap` composable using `Style.DARK`. For each `Site` with non-null `latitude` and `longitude`, a `ViewAnnotation` SHALL be placed at the site's coordinates displaying the site name in a labelled card with a downward-pointing triangle indicator. Sites with null coordinates SHALL be omitted from the map.

#### Scenario: Sites with coords appear as pins
- **WHEN** the map view is rendered with sites that have non-null latitude and longitude
- **THEN** each such site has a visible labelled pin at its location on the dark Mapbox map

#### Scenario: Sites without coords are absent from map
- **WHEN** a site has `latitude == null` or `longitude == null`
- **THEN** no pin is shown for that site on the map

#### Scenario: Unmapped site count is shown
- **WHEN** at least one site in the list has null coordinates
- **THEN** a message at the bottom of the map view indicates the number of sites not yet geocoded (e.g. "2 sites not yet geocoded")

### Requirement: Map camera centres on site cluster
`SiteMapView` SHALL initialise the camera centred on the geographic centroid of all geocoded sites at zoom level 10. If no sites have coordinates, the camera SHALL default to Ontario (approx. lat 43.65, lng -79.38) at zoom level 7.

#### Scenario: Camera centres on geocoded sites
- **WHEN** the map opens with multiple geocoded sites
- **THEN** the initial camera position is centred on the average lat/lng of those sites at zoom 10

#### Scenario: Camera defaults to Ontario when no sites are geocoded
- **WHEN** the map opens and no sites have coordinates
- **THEN** the camera is centred on Ontario at zoom 7

### Requirement: Tapping a site pin navigates to SiteDetailScreen
Tapping a `SitePin` composable SHALL invoke the `onSiteSelected(site)` callback — the same callback used by list rows — routing the inspector to `SiteDetailScreen` for that site.

#### Scenario: Pin tap navigates to site detail
- **WHEN** the inspector taps a site pin on the map
- **THEN** `SiteDetailScreen` is shown for that site, identical to tapping a row in list view

### Requirement: Lazy geocoding runs on first map open
When map view is first opened, `SiteListScreen` SHALL trigger geocoding for any sites with null coordinates. Geocoding SHALL run sequentially in the background and update each site's Room record on success. The map SHALL update reactively as coords become available.

#### Scenario: Existing sites geocoded on map open
- **WHEN** the inspector opens map view and some sites have null coordinates
- **THEN** those sites are geocoded in the background and their pins appear on the map once coordinates are resolved
