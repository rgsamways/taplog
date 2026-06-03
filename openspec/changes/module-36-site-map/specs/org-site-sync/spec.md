## ADDED Requirements

### Requirement: Site sync payload includes latitude and longitude
`SiteSyncRequest` SHALL include `latitude: Double?` and `longitude: Double?` fields. `Site.toSyncRequest()` SHALL map these fields from the entity. The backend `SiteIn` Pydantic model SHALL accept these as optional nullable fields and store them on the site document.

#### Scenario: Site with coordinates syncs lat/lng to backend
- **WHEN** a site with non-null `latitude` and `longitude` is POSTed to `/api/v1/sites`
- **THEN** the backend stores `latitude` and `longitude` on the site document

#### Scenario: Site without coordinates syncs with null lat/lng
- **WHEN** a site with null `latitude` and `longitude` is POSTed to `/api/v1/sites`
- **THEN** the backend accepts the request and stores the site without coordinate fields (null/absent)
