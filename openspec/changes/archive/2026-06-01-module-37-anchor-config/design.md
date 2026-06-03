## Context

The vertical engine (Module 34) serves `VerticalConfig` documents from a MongoDB `verticals` collection via `GET /api/v1/verticals`. Only the Ember document exists today. The Android app's `initVerticalRegistry()` already handles multi-vertical responses — it upserts every returned config to Room and registers each in `VerticalRegistry`. No Android changes are needed; the backend just needs a second document.

## Goals / Non-Goals

**Goals:**
- Anchor `VerticalConfig` document seeded to MongoDB and served by the existing verticals endpoint
- Idempotent seed script — safe to re-run in any environment
- App fetches and caches Anchor config automatically on next startup for any org with ANCHOR licensed

**Non-Goals:**
- Adding ANCHOR to any org's `licensedVerticals` — that is a billing/onboarding concern
- Anchor-specific UI on Android
- Batch pre-use inspection flows (blocked by inspection-cardinality.md open design doc)

## Decisions

**Decision: Seed script over manual Atlas insert**
A Python script at `scripts/seed_anchor_vertical.py` is reproducible across environments (dev, staging, prod) and version-controlled. Manual Atlas UI inserts are not auditable.

**Decision: PRE_USE as top-level triggerModel, CALENDAR at asset type level**
Anchor's primary and most frequent trigger is PRE_USE (before each use). Annual certified inspection is a secondary trigger for specific asset types (ANC, HLL, RKT). The `VerticalConfig.triggerModel` field carries the dominant trigger; `VerticalAssetType.triggerConfig` carries the per-type override. This matches the existing data model.

**Decision: No middle result option**
Anchor result options are PASS and REMOVE_FROM_SERVICE only — no "Requires Attention." CSA Z259 and O. Reg. 213/91 are unambiguous: any defect means immediate removal from service. Adding a soft middle state would be a compliance liability.

**Decision: snake_case field names in MongoDB document**
Consistent with the existing Ember seed document. The backend `verticals.py` router returns documents as-is; Gson `@SerializedName` annotations on the Kotlin models handle camelCase mapping on the Android side.

## Risks / Trade-offs

[Risk] Anchor asset types have dual trigger models (PRE_USE + CALENDAR) which the current `TriggerConfig` model represents at the asset-type level but the `InspectionFormScreen` only shows a single interval hint → Mitigation: The form screen uses the asset type's `triggerConfig` for display; dual-trigger guidance is a Module 40 concern (calendar section will surface annual cert dates). No functional gap in this module.

[Risk] Seed script requires MONGODB_URL environment variable → Mitigation: Script reads from `.env` via python-dotenv if available, falls back to os.environ; documented in script header.

## Migration Plan

1. Run `python scripts/seed_anchor_vertical.py` in the taplog-api environment
2. Confirm via `GET /api/v1/verticals/ANCHOR` → 200 with full document
3. On next Android app launch with connectivity, Anchor config is fetched, upserted to Room `vertical_configs`, and registered in `VerticalRegistry`
4. Rollback: `db.verticals.deleteOne({ vertical: "ANCHOR" })` — no other state changes
