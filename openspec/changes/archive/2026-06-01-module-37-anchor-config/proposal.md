## Why

The vertical engine (Module 34) is designed to serve any vertical via a MongoDB config document — but only Ember is seeded. Anchor (fall protection) is the next vertical in the life safety cluster and the first proof that the engine generalises beyond Ember. Adding the Anchor config requires no Android code changes.

## What Changes

- New `VerticalConfig` JSON document for vertical code `ANCHOR` seeded into the MongoDB `verticals` collection
- New Python seed script `scripts/seed_anchor_vertical.py` — idempotent upsert, runnable standalone
- `GET /api/v1/verticals` now returns Anchor config for any org with `ANCHOR` in `licensed_verticals`
- `GET /api/v1/verticals/{code}` returns the Anchor config on request

No Android files are modified. No Room migrations. No changes to existing backend routers or models.

## Capabilities

### New Capabilities
- `anchor-vertical-config`: Complete `VerticalConfig` document for the Anchor (fall protection) vertical — 6 asset types (HAR, LAN, SRL, ANC, HLL, RKT), PRE_USE + CALENDAR trigger model, PASS / REMOVE_FROM_SERVICE result options, CSA Z259 / O. Reg. 213/91 regulatory framework

### Modified Capabilities
- `vertical-config`: Backend `verticals` collection now holds a second document; `GET /api/v1/verticals` returns multi-vertical results

## Impact

- **Backend:** `scripts/seed_anchor_vertical.py` (new file); `verticals` MongoDB collection gains one document
- **Android:** No changes — app fetches and caches Anchor config automatically on next startup via the existing `initVerticalRegistry()` fallback chain
- **MongoDB:** `verticals` collection: one new document, `vertical: "ANCHOR"`
- **Dependencies:** Requires Mapbox and MONGODB_URL env var in scope where seed script runs
