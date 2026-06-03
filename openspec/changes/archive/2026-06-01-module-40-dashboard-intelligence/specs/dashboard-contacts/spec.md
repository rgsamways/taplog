## ADDED Requirements

### Requirement: ContactsSection displays searchable site contacts aggregated from all sites
The system SHALL display a `ContactsSection` composable in `DashboardScreen` showing a search field and up to 4–5 contacts drawn from all `Site` records in the organisation. Each site contributes 0–2 contacts: one from `clientName/clientPhone` (role "Client") and one from `contactName/contactPhone` (role "Contact") when those fields are non-null and differ. Contacts with identical name+phone SHALL be deduplicated — shown once. Contacts SHALL be sorted alphabetically by name. The search field SHALL filter the list client-side as the user types.

#### Scenario: Client contact from site appears in list
- **WHEN** a site has a non-null `clientName`
- **THEN** a contact row appears with the client name, "Client" role label, and site name

#### Scenario: Contact with same name+phone on two sites is shown once
- **WHEN** two sites have the same `clientName` and `clientPhone`
- **THEN** only one contact row appears for that person

#### Scenario: Search filters contacts by name
- **WHEN** the user types a name fragment in the search field
- **THEN** only contacts whose names contain that fragment (case-insensitive) are displayed

#### Scenario: Empty state prompts adding contact info
- **WHEN** no sites have any contact fields populated
- **THEN** `ContactsSection` displays a message suggesting to add contact info when registering sites

### Requirement: Tapping a contact phone number opens the device dialer
The system SHALL respond to a tap on a phone number in a contact row by launching `Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))`. This SHALL open the device dialer pre-filled with the number but SHALL NOT auto-initiate the call.

#### Scenario: Tap phone number opens dialer
- **WHEN** the user taps a phone number in a contact row
- **THEN** the device dialer opens with the number pre-filled and no call is placed automatically

#### Scenario: Contact with null phone has no dial action
- **WHEN** a contact has a null or blank phone number
- **THEN** no phone tappable element is rendered for that contact

### Requirement: Tapping a contact row navigates to the associated SiteDetailScreen
The system SHALL navigate to `SiteDetailScreen` when the user taps anywhere on a contact row (outside the phone number). `ScanState.SiteSelected` SHALL be emitted for the site associated with the contact, with `fromSiteList = false`.

#### Scenario: Tap contact row navigates to site detail
- **WHEN** the user taps a contact row (not the phone number)
- **THEN** `ScanState.SiteSelected` is emitted for the associated site and `SiteDetailScreen` is displayed

### Requirement: ContactsScreen provides full-screen searchable contact list
The system SHALL provide a `ContactsScreen` reachable via `ScanState.Contacts`. It SHALL display all contacts (no preview cap) with a prominent search bar. All tap behaviours from the section (dial, site navigation) SHALL apply. Back navigation SHALL return to `ScanState.Dashboard`.

#### Scenario: "See all" from dashboard opens ContactsScreen
- **WHEN** the user taps the "See all" action in the ContactsSection header
- **THEN** `ScanState.Contacts` is emitted and `ContactsScreen` is displayed

#### Scenario: Back from ContactsScreen returns to dashboard
- **WHEN** the user taps the back arrow in `ContactsScreen`
- **THEN** `ScanState.Dashboard` is emitted and `DashboardScreen` is displayed

### Requirement: allContacts StateFlow provides deduplicated contact list derived from sites
`EmberViewModel` SHALL expose `allContacts: StateFlow<List<SiteContact>>` derived from `sitesForOrg` flow. `SiteContact` SHALL carry `name`, `phone: String?`, `role: String`, `siteName`, and `siteId`. Derivation and deduplication SHALL occur in the ViewModel; no new DAO query is required. The list SHALL update reactively when site data changes.

#### Scenario: allContacts initialises to empty list
- **WHEN** `EmberViewModel` is created before org data is loaded
- **THEN** `allContacts` emits an empty list without throwing
