# Module 40 — Context File
> For: `/opsx:propose "module-40-dashboard-intelligence"`
> Written: 2026-05-31
> Depends on: Module 39 (DashboardScreen shell must exist)

---

## What this module is

Add the three intelligence sections to `DashboardScreen`: calendar, tasks, and contacts. These are the sections that make the dashboard genuinely useful as a professional tool rather than just a navigation portal.

This is Module 40 (39b) of the two-part dashboard build.

---

## Scope

**In scope:**
- `CalendarSection.kt` — full month calendar with inspection due date dots
- `TasksSection.kt` — unified task list (open deficiencies + overdue assets)
- `ContactsSection.kt` — searchable contacts list aggregated from all sites
- `EmberViewModel` additions: `calendarEvents`, `allContacts`, `upcomingInspections` StateFlows
- New DAO queries to support the above
- All sections embedded in `DashboardScreen` with "See all" links to full-screen versions
- Full-screen versions: `CalendarScreen.kt`, `TasksScreen.kt`, `ContactsScreen.kt`

**No Room migrations** — all data derived from existing entities.

---

## Calendar section

### Concept
Pull `nextInspectionDue` from all active assets across all sites. Plot them on a scrollable month calendar. Days with inspections due get a colored dot. Tap a day to see what's scheduled. Tap an asset to navigate to `AssetDetailScreen`.

### Calendar implementation
- Build with pure Compose — no external calendar library dependency
- Month grid: 7 columns, variable rows, standard calendar layout
- Swipe left/right between months (or arrow buttons)
- Day cell with dot indicator(s):
  - **Green dot** — upcoming (due more than 7 days away)
  - **Amber dot** — due soon (within 7 days)
  - **Red dot** — overdue (past `nextInspectionDue`)
- Multiple assets due same day: show up to 3 dots; if more, show count
- Tap a day → bottom sheet or inline expansion showing list of assets due that day: asset name, site name, asset type chip
- Each asset row tap → `AssetDetailScreen`

### ViewModel
```kotlin
data class CalendarEvent(
    val assetId: String,
    val assetName: String,
    val siteName: String,
    val assetTypeCode: String,
    val dueDate: LocalDate,
    val urgency: CalendarUrgency  // UPCOMING, DUE_SOON, OVERDUE
)

enum class CalendarUrgency { UPCOMING, DUE_SOON, OVERDUE }

// map of LocalDate → list of CalendarEvent
val calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>>
```

### Dashboard preview
In the dashboard, show just the current month with dots. "See all" → `CalendarScreen` (full screen with swipe navigation).

---

## Tasks section

### Concept
A unified task list that combines two sources of actionable work:
1. **Open deficiencies** — `Deficiency` where `resolvedAt == null`, any severity
2. **Overdue assets** — `Asset` where `nextInspectionDue < now` and `isActive == true`

This is the "what do I need to do" view. An inspector arriving on site should be able to pull this up and immediately know what's outstanding.

### Task model
```kotlin
data class Task(
    val id: String,
    val type: TaskType,
    val title: String,          // e.g. "Replace EXT at Main Lobby" or "ABC Dry Chemical overdue"
    val siteName: String,
    val severity: DeficiencySeverity?,   // null for overdue assets
    val daysOverdue: Int?,               // null for deficiencies
    val createdAt: Long
)

enum class TaskType { OPEN_DEFICIENCY, OVERDUE_ASSET }
```

### Sort order
- CRITICAL deficiencies first
- HIGH deficiencies second
- OVERDUE_ASSET items (sorted by most overdue)
- MEDIUM deficiencies
- LOW deficiencies

### Dashboard preview
Show top 5 tasks (after sort). "See all" → `TasksScreen` with full list + filter chips (All / Deficiencies / Overdue).

### `TasksScreen.kt`
- Filter chips: All · Deficiencies · Overdue
- Each task row: type icon, title, site name, severity chip or days-overdue chip, tap → relevant screen
  - Deficiency tap → `AssetDetailScreen` (scrolled to deficiency, ideally — or just the asset)
  - Overdue asset tap → `AssetDetailScreen`
- Mark-resolved stays on `AssetDetailScreen` / `OpenDeficienciesScreen` — don't duplicate it here

### ViewModel
```kotlin
val tasks: StateFlow<List<Task>>
```

---

## Contacts section

### Concept
Every `Site` record has up to four contact fields: `clientName`, `clientPhone`, `contactName`, `contactPhone`. Flatten all of these across all sites into a single searchable contacts list. An inspector doing their morning briefing should be able to find the building manager's number without navigating into each site record.

### Contact model
```kotlin
data class SiteContact(
    val name: String,
    val phone: String?,
    val role: String,           // "Client" or "Contact"
    val siteName: String,
    val siteId: String
)
```

### Query logic
For each site:
- If `clientName` is not null → emit `SiteContact(clientName, clientPhone, "Client", siteName, siteId)`
- If `contactName` is not null and differs from `clientName` → emit `SiteContact(contactName, contactPhone, "Contact", siteName, siteId)`
- Deduplicate by name+phone (same person may appear as contact on multiple sites — show once, list sites)

### Search
Client-side filter on the `StateFlow<List<SiteContact>>` — no DB query on keypress, just filter the in-memory list.

### Tap action
- Tap phone number → `Intent(Intent.ACTION_DIAL, "tel:$phone")` — opens dialer, doesn't auto-call
- Tap row → `SiteDetailScreen` for that site

### Dashboard preview
Show top 4–5 contacts (alphabetical). Search field visible even in preview. "See all" → `ContactsScreen`.

### ViewModel
```kotlin
val allContacts: StateFlow<List<SiteContact>>
```

---

## Full-screen screens

Three new screens, all simple:

- `CalendarScreen.kt` — full calendar with month swipe, day detail expansion
- `TasksScreen.kt` — full task list with filter chips
- `ContactsScreen.kt` — full contact list with search bar

Each is navigable from the dashboard "See all" links and also from a bottom nav or drawer (if one exists by this point — if not, just back-navigable from dashboard).

---

## DAO additions

New queries needed (all read-only, no migrations):

```kotlin
// All assets with nextInspectionDue set, across all sites — for calendar
fun getAssetsWithDueDates(): Flow<List<AssetWithSite>>

// All sites with their contact fields — for contacts
fun getAllSiteContacts(): Flow<List<Site>>

// Open deficiencies with asset + site info — for tasks
fun getOpenDeficienciesWithContext(): Flow<List<DeficiencyWithContext>>
```

`AssetWithSite` is a Room JOIN result — asset + its parent site name. Check if one already exists in `EmberDAO`; if so, reuse it.

---

## Design notes

- Calendar dots should be small (6–8dp) and positioned at the bottom of the day cell, not overlapping the date number
- Tasks section severity chips reuse the `deficiency severity` colors already defined in `Color.kt`
- Contacts section uses `Icons.Default.Phone` for the dial action — already available via `material-icons-extended`
- All three sections follow the same header pattern established in Module 39: label left, "See all" right
- Empty states matter: "No open tasks" with a checkmark is motivating; "No contacts" should suggest adding site contact info

---

## What does NOT change

- Room schema — no migrations
- Backend — no changes
- `DashboardScreen` shell from Module 39 — extended, not replaced; new sections are appended to the existing scroll
- `SiteListScreen`, `OpenDeficienciesScreen` — not replaced; remain full-screen destinations
- All existing vertical/inspection flow — untouched
