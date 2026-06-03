## MODIFIED Requirements

### Requirement: Backend exposes verticals endpoints gated by licensedVerticals
The backend SHALL expose `GET /api/v1/verticals` returning the list of `VerticalConfig` documents for the authenticated org's `licensedVerticals`, and `GET /api/v1/verticals/{code}` returning a single config. The `verticals` collection SHALL contain documents for all seeded verticals; the endpoint filters by the requesting org's `licensedVerticals`.

#### Scenario: Org with default license gets Ember config
- **WHEN** an authenticated request is made to `GET /api/v1/verticals` for an org with `licensedVerticals = ["EMBER"]`
- **THEN** the response contains exactly one VerticalConfig with `vertical = "EMBER"`

#### Scenario: Org with Anchor licensed gets both configs
- **WHEN** an authenticated request is made to `GET /api/v1/verticals` for an org with `licensedVerticals = ["EMBER", "ANCHOR"]`
- **THEN** the response contains exactly two VerticalConfig documents, one with `vertical = "EMBER"` and one with `vertical = "ANCHOR"`

#### Scenario: Anchor config is retrievable by code
- **WHEN** a request is made to `GET /api/v1/verticals/ANCHOR`
- **THEN** the backend returns HTTP 200 with the full Anchor VerticalConfig document

#### Scenario: Unknown vertical code returns 404
- **WHEN** a request is made to `GET /api/v1/verticals/UNKNOWN`
- **THEN** the backend returns HTTP 404
