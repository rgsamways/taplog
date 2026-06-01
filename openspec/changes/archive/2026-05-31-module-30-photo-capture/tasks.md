## 1. Permissions and Manifest

- [x] 1.1 Add `<uses-permission android:name="android.permission.CAMERA" />` to `AndroidManifest.xml`

## 2. AddDeficiencyDialog — Camera Capture

- [x] 2.1 Add `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` for CAMERA permission in `AddDeficiencyDialog`
- [x] 2.2 Add `rememberLauncherForActivityResult(ActivityResultContracts.TakePicture())` in `AddDeficiencyDialog` — on success: keep photoPath; on failure: clear photoPath and photoUri state
- [x] 2.3 Add helper function `createPhotoFile(context): Pair<File, Uri>` — creates `TapLog/photos/def_<timestamp>.jpg`, returns file + FileProvider URI
- [x] 2.4 Add camera icon button to `AddDeficiencyDialog` — taps check permission: if granted launch camera, if not request permission first
- [x] 2.5 Add thumbnail preview in `AddDeficiencyDialog` — if `photoUri != null`, show 64dp × 64dp image loaded with `BitmapFactory` + `LaunchedEffect`
- [x] 2.6 Pass `photoPath` through to `Deficiency` when dialog confirms — `onAdd` callback already takes a `Deficiency`; set `photoPath = capturedPhotoPath`

## 3. DeficiencyChip — Thumbnail Display

- [x] 3.1 Add `photoPath: String?` parameter read from `deficiency.photoPath` in `DeficiencyChip`
- [x] 3.2 Add async bitmap loading in `DeficiencyChip` — `LaunchedEffect(deficiency.photoPath)` + `withContext(Dispatchers.IO)` + `BitmapFactory.decodeFile()` with `inSampleSize = 8`
- [x] 3.3 If bitmap is non-null, render 48dp × 48dp thumbnail at the trailing end of the chip card

## 4. OpenDeficienciesScreen — Thumbnail Display

- [x] 4.1 Add `photoPath` to `DeficiencyWithAsset` data class in `EmberModels.kt` — already present
- [x] 4.2 Update `DeficiencyDao.getOpenWithAsset()` SQL query to include `d.photoPath as photoPath` — `d.*` already covers it
- [x] 4.3 Add thumbnail to `DeficiencyWithAssetCard` in `OpenDeficienciesScreen` using the same async `BitmapFactory` pattern as task 3.2–3.3
