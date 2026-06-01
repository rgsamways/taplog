## Why

`Deficiency.photoPath: String?` has been in the Room entity since the initial data model. Module 30 activates it. A photo of a deficiency — a corroded extinguisher, a blocked exit sign, a damaged fusible link — makes the inspection record more useful to the client, more defensible for the inspector, and adds a visual layer to the tamper-evident audit trail. It is the last piece of proof-of-presence infrastructure before Visual Asset Identification (Module 31).

## What Changes

- `CAMERA` permission added to `AndroidManifest.xml`
- `AddDeficiencyDialog` — camera button launches `ActivityResultContracts.TakePicture()`; photo stored in `getExternalFilesDir(null)/TapLog/photos/`; thumbnail preview shown in dialog before saving
- `DeficiencyChip` (in `InspectionFormScreen`) — if `photoPath != null`, shows a 48dp thumbnail alongside code and description
- `OpenDeficienciesScreen` `DeficiencyWithAssetCard` — same thumbnail display for open deficiencies
- Photo file creation uses `FileProvider` — same authority (`ca.taplog.app.fileprovider`) and path config (`res/xml/file_paths.xml`) already registered in Module 29; `TapLog/photos/` subdirectory is covered by existing `TapLog/` path entry
- **No new dependencies** — `ActivityResultContracts.TakePicture()` is in `activity-ktx` (already a dep); bitmap loading via `BitmapFactory.decodeFile()` with `inSampleSize` downsampling (API built-in, async via `LaunchedEffect`)
- **No backend changes** — `photoPath` (local file path string) is already in `DeficiencySyncRequest` and `DeficiencyIn`. Binary photo upload is a future enhancement.

## Capabilities

### New Capabilities

- `deficiency-photo-capture`: Inspector can photograph a deficiency during inspection; photo stored locally, thumbnail shown in form and deficiency list; path persisted in Room and passed through sync

### Modified Capabilities

<!-- No existing spec-level capability requirements change -->

## Impact

**Android:**
- MODIFIED `AndroidManifest.xml` — `<uses-permission android:name="android.permission.CAMERA" />`
- MODIFIED `ui/ember/InspectionFormScreen.kt` — `AddDeficiencyDialog` gets camera launcher + thumbnail preview; `DeficiencyChip` shows thumbnail
- MODIFIED `ui/ember/OpenDeficienciesScreen.kt` — `DeficiencyWithAssetCard` shows thumbnail

**No backend changes.** `photoPath` string already flows through sync.

**No new Gradle dependencies.** `activity-ktx` (already present via `activity-compose`); bitmap via `BitmapFactory`.
