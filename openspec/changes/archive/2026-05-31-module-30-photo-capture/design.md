## Context

`Deficiency.photoPath: String?` is already in Room, in `DeficiencySyncRequest`, and in the backend `DeficiencyIn` model. The field has been waiting since the original data model. The `FileProvider` registered in Module 29 (`ca.taplog.app.fileprovider`, path `TapLog/`) already covers a `TapLog/photos/` subdirectory — no manifest or file_paths.xml changes needed beyond the `CAMERA` permission.

`ActivityResultContracts.TakePicture()` from `activity-ktx` (already transitively included via `activity-compose`) handles the full camera intent: create a content URI, launch the system camera app, receive success/fail callback. No CameraX dependency required for v1.

`DeficiencyChip` and `DeficiencyWithAssetCard` need conditional thumbnail rendering. Camera photos are large (2–8 MB JPEG); displaying them as 48dp thumbnails requires downsampling. `BitmapFactory.Options.inSampleSize` achieves this without any library.

## Goals / Non-Goals

**Goals:**
- Inspector can take a photo at the moment of discovering a deficiency, during the inspection form flow
- Photo persisted to app-private external storage, path stored in `Deficiency.photoPath`
- 48dp thumbnail displayed inline in deficiency chip and open deficiencies card
- Zero new Gradle dependencies

**Non-Goals:**
- Binary photo upload to backend (local path string already syncs — binary upload is a future enhancement)
- Gallery picker (camera-only for v1 — takes a fresh photo at the asset)
- Photo editing or annotation
- Multiple photos per deficiency (single photo per deficiency for v1)
- Photo deletion or retake within the same dialog session (cancel-and-reopen is sufficient for v1)

## Decisions

### Decision 1: `rememberLauncherForActivityResult` inside `AddDeficiencyDialog`

The launcher is registered inside the dialog composable, not in `InspectionFormScreen` or `MainActivity`. This keeps the photo capture logic co-located with the deficiency creation flow.

**Alternative considered:** Hoist the launcher to `InspectionFormScreen`. Rejected — adds callback indirection without benefit; composable launchers work correctly inside `AlertDialog`.

### Decision 2: File stored in `getExternalFilesDir(null)/TapLog/photos/`

This directory is already covered by the existing `FileProvider` path configuration (`external-files-path path="TapLog/"`). No changes to `file_paths.xml` or `AndroidManifest.xml` beyond the `CAMERA` permission. No storage permission needed (`getExternalFilesDir` is app-private).

File naming: `def_<timestamp>.jpg` — deterministic within a session, not globally unique (acceptable for v1 since path is stored on the deficiency record).

### Decision 3: `BitmapFactory.decodeFile()` with `inSampleSize` in a `LaunchedEffect`

Thumbnail loading runs in a coroutine via `LaunchedEffect(photoPath)` and `withContext(Dispatchers.IO)`. `inSampleSize = 8` produces a ~96×128 px bitmap from a typical 4032×3024 camera photo — well below 50KB, negligible memory footprint. No Coil or Glide dependency.

**Alternative considered:** Coil image loading library (~1 MB). Rejected — overkill for a single fixed-size thumbnail from a local file. `BitmapFactory` with downsampling is 20 lines and handles the one use case.

### Decision 4: `CAMERA` permission only — no storage permission

`ActivityResultContracts.TakePicture()` with a `FileProvider` URI requires `CAMERA` permission. Writing to `getExternalFilesDir()` requires no storage permission (app-private directory). The `CAMERA` permission is a normal permission on API 29+ — no runtime request dialog is required; it's granted at install.

Wait — `CAMERA` IS a dangerous permission on Android (requires runtime request on API 23+). The correct approach: request the permission via `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` before launching the camera, then launch the camera on grant.

**Implementation note:** Add a permission launcher alongside the camera launcher in `AddDeficiencyDialog`. If permission is already granted, skip the request. If denied, show a snackbar/toast explaining why.

### Decision 5: No photo shown on `DeficiencyWithAsset` JOIN result

`DeficiencyWithAsset` (the JOIN result used by `OpenDeficienciesScreen`) does not include `photoPath`. The fix: add `photoPath` to `DeficiencyWithAsset` and update the JOIN query in `DeficiencyDao.getOpenWithAsset()`.

## Risks / Trade-offs

- **Large photo files in external storage** — camera photos are 2–8 MB. Over time, `TapLog/photos/` could accumulate significant storage. Mitigation for v1: none (acceptable for pilot). v2 can add a JPEG compress-before-save step or a periodic cleanup.
- **`inSampleSize` is power-of-2 only** — `BitmapFactory` rounds `inSampleSize` to the nearest power of 2. For a 48dp thumbnail this is fine (any inSampleSize ≥ 4 produces a small enough bitmap).
- **Photo not synced to backend** — `photoPath` is a device-local path string. The backend stores it but it is meaningless remotely. This is a known v1 limitation; binary upload is Module 30b.
