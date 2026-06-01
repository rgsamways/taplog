## Why

TapLog's NFC scan events and tag attachment history are currently ephemeral — there is no record of who scanned what, when, or what happened to a tag over its lifetime. Fire inspection compliance requires tamper-evident audit trails: regulators and insurance carriers need proof that a tag was physically read on a specific date by a specific inspector, and that tag replacements are tracked when tags are damaged, lost, or swapped. Without this, TapLog cannot support post-incident investigations or regulatory audits.

## What Changes

- New `ScanEvent` entity — insert-only record of every NFC tap (tag ID, inspector, timestamp, event type: INSPECTION or BROWSE)
- New `TagEvent` entity — insert-only record of every tag attachment/retirement (asset ID, tag ID, attached/retired timestamps, retire reason: DAMAGED/LOST/REPLACED/REMOVED)
- Room database bumped version 3 → 4 with `MIGRATION_3_4` for both new tables
- `AssetDetailScreen` gains a collapsible scan history section showing recent ScanEvents
- Tag replacement workflow on `AssetDetailScreen` — "Replace Tag" button triggers retire-old/attach-new flow
- `OFCAssetTypes.kt` updated — all 35 OFC asset types get `checklistItems: List<String>` populated against CAN/ULC-S536:19 standard
- `InspectionFormScreen` gains a pre-inspection checklist section (read-only, derived from asset type)
- Two new backend endpoints: `POST /api/v1/scan_events` and `POST /api/v1/tag_events`
- `SyncRepository` updated — sync order: assets → tag_events → inspections → deficiencies → scan_events
- `TapLogApiService` updated with two new endpoint calls
- `SyncModels.kt` updated with `ScanEventSyncRequest` and `TagEventSyncRequest` wire models

## Capabilities

### New Capabilities

- `scan-log`: Insert-only tamper-evident record of every NFC tap, synced to backend, displayed on AssetDetailScreen
- `tag-lifecycle`: Insert-only tag attachment/retirement history, retire reason enum, tag replacement workflow on AssetDetailScreen
- `ofc-checklists`: CAN/ULC-S536:19-aligned inspection checklists for all 35 OFC asset types, displayed pre-inspection offline

### Modified Capabilities

<!-- No existing spec-level capabilities are changing — this module adds new ones only -->

## Impact

**Android:**
- `data/EmberModels.kt` — add `ScanEvent`, `TagEvent` entities; add `RetireReason` enum
- `data/EmberDAO.kt` — add `ScanEventDao`, `TagEventDao`
- `data/AppDatabase.kt` — version 3 → 4, `MIGRATION_3_4`, new DAOs
- `data/Converters.kt` — add `RetireReason` converter
- `data/EmberRepository.kt` — expose scan event + tag event insert/query methods
- `data/OFCAssetTypes.kt` — populate `checklistItems` for all 35 types
- `data/SyncModels.kt` — add `ScanEventSyncRequest`, `TagEventSyncRequest`, wire extensions
- `data/TapLogApiService.kt` — add two new POST endpoints
- `data/SyncRepository.kt` — add `syncScanEvents()`, `syncTagEvents()`, updated `syncAll()` order
- `ui/ember/AssetDetailScreen.kt` — scan log section, replace-tag button + dialog
- `ui/ember/InspectionFormScreen.kt` — pre-inspection checklist display
- `ui/ember/EmberViewModel.kt` — `insertScanEvent()`, `insertTagEvent()`, `retireTag()`, `attachNewTag()`

**Backend (`C:\dev\taplog-api`):**
- `app/models.py` — `ScanEventIn/Out`, `TagEventIn/Out` Pydantic models
- `app/routers/scan_events.py` — new router, `POST /api/v1/scan_events`
- `app/routers/tag_events.py` — new router, `POST /api/v1/tag_events`
- `app/main.py` — register two new routers
