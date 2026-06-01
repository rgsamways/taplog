## 1. Data Layer — New Entities and Room Migration

- [x] 1.1 Add `RetireReason` enum to `EmberModels.kt` (DAMAGED, LOST, REPLACED, REMOVED)
- [x] 1.2 Add `ScanEventType` enum to `EmberModels.kt` (BROWSE, INSPECTION)
- [x] 1.3 Add `ScanEvent` entity to `EmberModels.kt` (id, tagId, assetId, inspectorId nullable, inspectorName, scannedAt, eventType, isSynced)
- [x] 1.4 Add `TagEvent` entity to `EmberModels.kt` (id, assetId, tagId, attachedAt, retiredAt nullable, retiredReason nullable, retiredByInspectorId nullable, isSynced)
- [x] 1.5 Add `RetireReason` and `ScanEventType` converters to `Converters.kt`
- [x] 1.6 Create `ScanEventDao` in `EmberDAO.kt` — insert + query methods only (no update/delete)
- [x] 1.7 Create `TagEventDao` in `EmberDAO.kt` — insert + query methods only
- [x] 1.8 Write `MIGRATION_3_4` in `AppDatabase.kt` — creates `scan_events` and `tag_events` tables
- [x] 1.9 Bump `@Database` version to 4 and add `ScanEvent`, `TagEvent` to entities list in `AppDatabase.kt`
- [x] 1.10 Add `ScanEventDao` and `TagEventDao` abstract methods to `AppDatabase`
- [x] 1.11 Wire new DAOs in `TapLogApplication.kt`

## 2. OFC Checklists

- [x] 2.1 Add `checklistItems: List<String>` field to `OFCAssetType` data class
- [x] 2.2 Populate `checklistItems` for all 8 Portable Extinguisher types against CAN/ULC-S536:19
- [x] 2.3 Populate `checklistItems` for all 8 Suppression System types
- [x] 2.4 Populate `checklistItems` for all 8 Detection & Alarm types
- [x] 2.5 Populate `checklistItems` for all 4 Emergency Lighting & Exit Sign types
- [x] 2.6 Populate `checklistItems` for all 5 Fire Doors & Closers types
- [x] 2.7 Populate `checklistItems` for all 4 Firefighting Equipment types

## 3. Repository Layer

- [x] 3.1 Add `insertScanEvent(event: ScanEvent)` to `EmberRepository`
- [x] 3.2 Add `getScanEventsForAsset(assetId: String): Flow<List<ScanEvent>>` to `EmberRepository`
- [x] 3.3 Add `insertTagEvent(event: TagEvent)` to `EmberRepository`
- [x] 3.4 Add `getTagEventsForAsset(assetId: String): Flow<List<TagEvent>>` to `EmberRepository`
- [x] 3.5 Add `replaceTag(asset: Asset, newTagId: String, reason: RetireReason)` to `EmberRepository` — atomic transaction: retire old TagEvent + update Asset.nfcTagId + insert new TagEvent

## 4. ViewModel

- [x] 4.1 Add `logScanEvent(asset: Asset, eventType: ScanEventType)` to `EmberViewModel`
- [x] 4.2 Add `scanEventsForCurrentAsset: StateFlow<List<ScanEvent>>` to `EmberViewModel`
- [x] 4.3 Add `tagEventsForCurrentAsset: StateFlow<List<TagEvent>>` to `EmberViewModel`
- [x] 4.4 Add `replaceTag(newTagId: String, reason: RetireReason)` to `EmberViewModel` — validates new tag not already registered, then delegates to repository
- [x] 4.5 Call `logScanEvent(asset, BROWSE)` in `EmberViewModel.onNfcTagScanned()` immediately when asset is found
- [x] 4.6 Call `logScanEvent(asset, INSPECTION)` in `EmberViewModel.saveInspection()` at submission time
- [x] 4.7 Call `insertTagEvent` in `EmberViewModel.saveAsset()` at asset creation

## 5. UI — AssetDetailScreen

- [x] 5.1 Add collapsible "Scan History" section to `AssetDetailScreen` showing recent `ScanEvent` list (timestamp, inspector name, event type)
- [x] 5.2 Add "Replace Tag" button to `AssetDetailScreen` (shown only when asset is active)
- [x] 5.3 Create `ReplaceTagDialog` composable — retire reason selector (4 options) + instruction to scan new tag
- [x] 5.4 Wire `ReplaceTagDialog` to `EmberViewModel.replaceTag()` — trigger NFC scan, pass new tag ID on receipt
- [x] 5.5 Show error snackbar if replacement fails (tag already registered)

## 6. UI — InspectionFormScreen

- [x] 6.1 Add read-only "Checklist" card at top of `InspectionFormScreen` — numbered list of `checklistItems` from the asset's `OFCAssetType`
- [x] 6.2 Ensure checklist renders correctly for asset types with 3–12 items (no overflow or truncation)

## 7. Sync — Android

- [x] 7.1 Add `ScanEventSyncRequest` and `TagEventSyncRequest` wire models to `SyncModels.kt`
- [x] 7.2 Add `toSyncRequest()` extension functions for `ScanEvent` and `TagEvent`
- [x] 7.3 Add `postScanEvent` and `postTagEvent` to `TapLogApiService.kt`
- [x] 7.4 Add `syncTagEvents()` to `SyncRepository` — batch POST, 409 = success, mark isSynced
- [x] 7.5 Add `syncScanEvents()` to `SyncRepository` — batch POST, 409 = success, mark isSynced
- [x] 7.6 Update `syncAll()` sync order: assets → tag_events → inspections → deficiencies → scan_events

## 8. Backend

- [x] 8.1 Add `ScanEventIn` and `ScanEventOut` Pydantic models to `app/models.py`
- [x] 8.2 Add `TagEventIn` and `TagEventOut` Pydantic models to `app/models.py`
- [x] 8.3 Create `app/routers/scan_events.py` with `POST /api/v1/scan_events` endpoint
- [x] 8.4 Create `app/routers/tag_events.py` with `POST /api/v1/tag_events` endpoint
- [x] 8.5 Register both new routers in `app/main.py`
- [x] 8.6 Push to GitHub to trigger Railway auto-deploy and verify endpoints live
