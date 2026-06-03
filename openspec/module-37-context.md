# Module 37 — Context File
> For: `/opsx:propose "module-37-anchor-config"`
> Written: 2026-05-31

---

## What this module is

Add the **Anchor** vertical config to the MongoDB `verticals` collection.

This is a **backend-only module**. Zero Android code changes. The vertical engine (Module 34) was designed to receive any vertical config from the backend and route inspection flow automatically. Anchor is the first proof that the engine works for a second vertical.

---

## Scope boundary

**In scope:**
- Write and seed a complete `VerticalConfig` JSON document for vertical code `ANCHOR` into the MongoDB `verticals` collection
- Write a Python seed script (`scripts/seed_anchor_vertical.py`) that can be re-run idempotently (upsert by `vertical` field)
- The app will fetch and cache Anchor config on next startup once it's in MongoDB — no Android changes needed

**Out of scope:**
- No Android code changes of any kind
- No new Kotlin files
- No Room migrations
- No changes to `VerticalRegistry`, `TapLogApplication`, or any screen
- No changes to `Organisation.licensedVerticals` — that's a billing/onboarding concern handled separately
- Anchor multi-asset batch pre-use (blocked by `inspection-cardinality.md` open design doc — do not address here)

---

## Regulatory framework

- **Legislation:** Ontario *Occupational Health and Safety Act* (OHSA), O. Reg. 213/91 (Construction Projects) — fall protection requirements
- **Standards:** CSA Z259 series (fall protection equipment)
  - Z259.1 — Body belts and saddles
  - Z259.10 — Full-body harnesses
  - Z259.11 — Energy absorbers and lanyards
  - Z259.12 — Connecting components
  - Z259.13 — Flexible horizontal lifeline systems
  - Z259.14 — Self-retracting devices (SRLs)
  - Z259.16 — Design of active fall protection systems
- **Key compliance rule:** Equipment must be inspected before each use (pre-use check by the worker) and annually by a Competent Person

---

## Trigger model

Anchor has **two trigger types** — both must be represented:

### 1. PRE_USE — Daily / before-each-use check
- Worker inspects their own equipment before use
- Quick visual + functional check
- Trigger: `TriggerModel.PRE_USE`
- No fixed calendar interval — triggered on every use
- `TriggerConfig(type = PRE_USE, intervalMonths = null)`

### 2. CALENDAR — Annual certified inspection
- Must be performed by a Competent Person (certified inspector)
- Interval: 12 months (some harnesses require 6 months in high-use environments — use 12 as default)
- Trigger: `TriggerModel.CALENDAR`
- `TriggerConfig(type = CALENDAR, intervalMonths = 12)`

**For the VerticalConfig document:** the top-level `triggerModel` field should be `PRE_USE` (the primary / most frequent trigger). Annual inspection is represented at the asset type level via `triggerConfig`.

---

## Role model

`RoleModel.SINGLE_INSPECTOR` — one person performs the check. No multi-role entry event. Routes to `InspectionFormScreen`, not `EntryEventScreen`.

---

## Result options

| code | label | action |
|---|---|---|
| `PASS` | Pass | `ResultAction.NONE` |
| `REMOVE_FROM_SERVICE` | Remove from service | `ResultAction.REMOVE_FROM_SERVICE` |

**Important:** Anchor has no "Requires Attention" middle state. A harness either passes or comes off. If any defect is found, the equipment must be immediately removed from service and quarantined. This is a hard regulatory requirement — do not add a soft middle option.

---

## Asset type registry

Six asset types. Each has:
- `code` — short identifier
- `label` — display name
- `description` — plain language description
- `triggerConfig` — PRE_USE for daily check, CALENDAR (12mo) for annual
- `checklistItems` — what to look for (pre-use check items from CSA Z259)

### Asset types

**HAR — Full-Body Harness**
- CSA Z259.10
- triggerConfig: PRE_USE (daily) / use CALENDAR 12mo for annual cert record
- Checklist: webbing condition (cuts, abrasion, chemical damage, UV degradation), stitching integrity, buckle and adjuster function, D-ring condition and freedom of movement, label legibility (manufacture date, serial number), no deformation or heat damage

**LAN — Lanyard / Energy Absorber**
- CSA Z259.11
- triggerConfig: PRE_USE
- Checklist: shock-pack indicator (deployed = remove from service immediately), webbing/rope condition, snap hook gate function and locking mechanism, stitching at attachment points, no cuts or abrasion, no chemical exposure evidence

**SRL — Self-Retracting Lifeline**
- CSA Z259.14
- triggerConfig: PRE_USE
- Checklist: retraction and braking function (pull-and-release test), housing condition (no cracks or damage), swivel connection condition, lifeline condition (no kinks, corrosion, fraying), carabiner gate and locking function, fall indicator (if present)

**ANC — Anchor Point**
- CSA Z259.16 / O. Reg. 213/91 s.26.1
- triggerConfig: CALENDAR 12mo (structural — inspected by Competent Person; not a pre-use item for the worker)
- Checklist: substrate integrity (concrete/steel anchor point), hardware condition (no corrosion, deformation), load rating label present and legible, anchor certified for rated load (minimum 16 kN per CSA Z259.16), installation record current

**HLL — Horizontal Lifeline**
- CSA Z259.13
- triggerConfig: CALENDAR 12mo
- Checklist: cable or rope condition (no kinks, fraying, corrosion), end terminations and thimbles, tensioner function, intermediate supports, end anchor hardware, system load rating label, engineer certification current

**RKT — Rescue / Retrieval Kit**
- O. Reg. 213/91 — rescue plan requirement
- triggerConfig: CALENDAR 12mo
- Checklist: rope condition and length, pulley and rope-grab function, bag/case integrity, all components present per packing list, inspection date tag current

---

## Form fields (global — apply to all asset types)

| key | label | type | required |
|---|---|---|---|
| `condition_notes` | Condition notes | TEXT | false |
| `last_cert_date` | Last annual cert date | DATE | false |

`last_cert_date` is particularly important for ANC and HLL asset types where the annual certification date is the compliance record.

---

## Form profile flags

```json
{
  "requiresPermit": false,
  "requiresWitness": false,
  "deficienciesEnabled": true,
  "photoRequired": false
}
```

---

## MongoDB document structure

The document goes into the `verticals` collection, same collection as the Ember seed. Top-level shape:

```json
{
  "vertical": "ANCHOR",
  "display_name": "Fall Protection",
  "short_name": "Anchor",
  "regulatory_framework": "OHSA O. Reg. 213/91 · CSA Z259 series",
  "trigger_model": "PRE_USE",
  "role_model": "SINGLE_INSPECTOR",
  "form_profile": { ... },
  "asset_type_registry": [ ... ]
}
```

Field naming convention: `snake_case` in MongoDB (matches existing Ember document). The backend `verticals.py` router returns the document as-is; the Android `VerticalConfig` Kotlin class uses camelCase — Gson handles the mapping via `@SerializedName` annotations already in place from Module 34.

---

## Seed script requirements

- Location: `scripts/seed_anchor_vertical.py`
- Uses Motor async or pymongo sync (match whatever the Ember seed script used)
- Upsert on `{"vertical": "ANCHOR"}` — safe to re-run
- Print confirmation: `Anchor VerticalConfig upserted.`
- Should be runnable standalone: `python scripts/seed_anchor_vertical.py`
- Reads `MONGODB_URL` and `DB_NAME` from environment (or `.env` if python-dotenv is available)

---

## Verification

After seeding, confirm:
1. `GET /api/v1/verticals/{ANCHOR}` returns 200 with full config
2. `GET /api/v1/verticals` returns both EMBER and ANCHOR (for an org with both licensed)
3. Android app fetches and caches Anchor config on next launch (check Room `vertical_configs` table via DB Inspector — should show two rows)

Android verification is observational only — no code changes, just confirm the fetch/cache path works for the new vertical.

---

## What does NOT change

- No Kotlin files
- No Room schema or migrations
- No `EmberVerticalConfig.kt` or `OFCAssetTypes.kt`
- No Android Manifest
- No build.gradle files
- No existing backend routers or models
- The Ember vertical config document in MongoDB — do not touch it
