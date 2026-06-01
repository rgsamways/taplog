## Context

TapLog's NFC scan pipeline currently writes nothing when a tag is tapped — the inspector opens an asset, sees its history, and optionally creates an inspection. There is no tamper-evident proof of the tap itself. Similarly, `Asset.nfcTagId` is overwritten when a tag is replaced, destroying the chain of custody. Fire inspection regulations (and insurance carriers) require both: a timestamped log of every physical scan, and a permanent record of every tag that was ever attached to an asset.

Room is currently at version 3 (`taplog_ember.db`). Two new entities (`ScanEvent`, `TagEvent`) require a migration to version 4. The sync pipeline (WorkManager → Retrofit → Railway FastAPI → MongoDB Atlas) is fully operational for assets, inspections, and deficiencies.

The dev device is a Pixel 10 Pro XL (API 36). Room migration crashes on dirty data are resolved by clearing app storage, not by writing destructive migration helpers.

## Goals / Non-Goals

**Goals:**
- Every NFC tap creates a `ScanEvent` record regardless of whether an inspection is submitted
- Every tag attachment or retirement creates a `TagEvent` record
- Tag replacement workflow on `AssetDetailScreen` — retire old tag, attach new one, all in a single flow
- All 35 OFC asset types have `checklistItems` populated against CAN/ULC-S536:19
- Checklists displayed on `InspectionFormScreen` before the form, no connectivity required
- Both new entities sync to backend via two new `POST` endpoints
- Room bumped to version 4 with a clean `MIGRATION_3_4`

**Non-Goals:**
- ScanEvent editing or deletion (insert-only by design)
- TagEvent editing or deletion (insert-only by design)
- Scan log filtering/searching (display is a flat recency list)
- Checklist items are reference only — no per-item checkbox state is persisted this module
- Backend deduplication logic (handled by idempotency key in Module 28 with auth)

## Decisions

### Decision 1: Insert-only enforcement in DAO, not DB constraint

Room does not support `WITHOUT ROWID` or trigger-based enforcement in Kotlin DSL. The insert-only invariant is enforced by having no `update` or `delete` methods on `ScanEventDao` and `TagEventDao`, and by the DAO being the only write path. Background sync only calls `insert` — it never reads back and mutates.

**Alternative considered:** A DB trigger via `execSQL` in migration. Rejected — harder to test, fragile across schema versions, and the risk of accidental mutation from outside the DAO layer is zero in a single-process Android app.

### Decision 2: ScanEvent written before inspection form opens

When the NFC tag is read in `MainActivity`, a `ScanEvent` with `eventType = BROWSE` is inserted immediately. If the inspector proceeds to submit an inspection, a second `ScanEvent` with `eventType = INSPECTION` is inserted at submission time. This ensures every physical tap is logged even if the inspector backs out without submitting.

**Alternative considered:** Only log on inspection submit. Rejected — this loses evidence of inspectors who scanned but didn't complete, which is a compliance gap.

### Decision 3: Tag replacement as a two-step atomic ViewModel operation

`retireTag(asset, retireReason)` + `attachNewTag(asset, newTagId)` are exposed as a single `replaceTag(asset, newTagId, retireReason)` function on `EmberViewModel`. Internally it:
1. Inserts a `TagEvent` with `retiredAt` and `retiredReason` for the old tag
2. Updates `Asset.nfcTagId` to the new tag ID
3. Inserts a new `TagEvent` with only `attachedAt` set (no `retiredAt`) for the new tag

All three writes happen in a single `withTransaction` block in the DAO layer. If any step fails the whole operation rolls back.

**Alternative considered:** Separate ViewModel calls from the UI. Rejected — the caller can't reason about partial state (asset pointing at a new tag but no TagEvent written, or vice versa).

### Decision 4: checklistItems as List<String> on OFCAssetType, not a separate entity

Checklist items are static, standards-derived reference data. They don't change per-asset or per-inspection. Embedding them in the enum is the simplest approach: no DB table, no sync, no migration risk, and they're available offline by definition.

**Alternative considered:** A separate `checklistItems` table in Room. Rejected — this is reference data, not user data. It belongs in code alongside the OFC type definitions.

### Decision 5: Sync order: assets → tag_events → inspections → deficiencies → scan_events

TagEvents reference asset IDs. They must be synced after assets exist on the backend. ScanEvents are audit/telemetry — they reference assets but are not required by the backend to accept inspections. Syncing them last ensures the critical records (assets, inspections, deficiencies) land first in low-bandwidth scenarios.

## Risks / Trade-offs

- **Migration on dirty dev data** → Mitigation: clear app storage (Settings → Apps → TapLog → Storage → Clear storage) before first install with v4 schema. `MIGRATION_3_4` only adds two tables — no column changes on existing tables — so it is safe on production data.

- **ScanEvent volume** → Every NFC tap creates a row. A busy inspector doing 50 assets/day creates 50–100 rows/day. At ~200 bytes/row, that's ~7 MB/year per inspector — acceptable for Room on device and MongoDB Atlas M0.

- **checklistItems list length** → Some asset types (e.g., FACP, sprinkler systems) may have 8–12 checklist items. The UI must handle variable-length lists without overflow. The `InspectionFormScreen` uses a `LazyColumn` wrapping a card per item — no truncation.

- **No auth in Module 27** → `inspectorId` on `ScanEvent` is nullable until Module 28. The `inspectorName` string (from DataStore inspector profile) is the only identifier until then. This is a known gap, documented here to prevent confusion during review.

## Migration Plan

1. Add `MIGRATION_3_4` to `AppDatabase.kt` — creates `scan_events` and `tag_events` tables
2. Bump `@Database(version = 4)`
3. Register new DAOs in `AppDatabase` and `TapLogApplication`
4. Deploy backend routers before Android release (Railway auto-deploys from GitHub push)
5. On first launch after update, Room runs migration automatically — no data loss on existing tables

**Rollback:** If migration fails on a device, clear app storage. All data will re-sync from backend on next connectivity. (This is the standard dev workflow already documented in session notes.)
