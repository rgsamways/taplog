# Module 34 — Architectural Amendments
> Generated from strategy session 2026-05-31
> Drop into `openspec/changes/module-34-vertical-engine/` alongside the existing spec files.
> These amendments must be folded into the spec BEFORE `/opsx:apply` is run.

---

## Amendment 1 — ResultOption replaces List<String> for result options

### Problem
`InspectionFormProfile.resultOptions: List<String>` captures labels only. Downstream logic
after a result (remove from service, notify authority, issue certificate) varies by vertical
and cannot be encoded in a label string.

### Change
Replace `resultOptions: List<String>` in `InspectionFormProfile` with `resultOptions: List<ResultOption>`.

### New models

```kotlin
data class ResultOption(
    val code: String,       // "PASS", "FAIL", "CONDITIONAL", "REMOVE_FROM_SERVICE", etc.
    val label: String,      // display label — "Pass", "Remove From Service", etc.
    val action: ResultAction
)

enum class ResultAction {
    NONE,                   // log result and continue — no special handling
    REMOVE_FROM_SERVICE,    // asset flagged as out of service in Asset record
    NOTIFY_AUTHORITY,       // regulatory body notification required — surfaced in UI
    ISSUE_CERTIFICATE,      // certificate number field required before submission
    DELIVER_REPORT          // report delivered to third party (Newel home inspection)
}
```

### Updated InspectionFormProfile

```kotlin
data class InspectionFormProfile(
    val resultOptions: List<ResultOption>,   // WAS: List<String>
    val fields: List<FormField>,
    val requiresPermit: Boolean,
    val requiresWitness: Boolean,
    val deficienciesEnabled: Boolean,
    val photoRequired: Boolean
)
```

### Ember seed document example

```kotlin
resultOptions = listOf(
    ResultOption("PASS", "Pass", ResultAction.NONE),
    ResultOption("CONDITIONAL", "Conditional", ResultAction.NONE),
    ResultOption("FAIL", "Fail", ResultAction.NONE)
)
```

### Anchor seed document example

```kotlin
resultOptions = listOf(
    ResultOption("IN_SERVICE", "In service", ResultAction.NONE),
    ResultOption("REMOVE", "Remove from service", ResultAction.REMOVE_FROM_SERVICE)
)
```

### Notes
- `ResultAction.NONE` is the safe default — existing Ember behaviour is unchanged
- `REMOVE_FROM_SERVICE` sets a flag on the Asset record — exact field TBD in a future module
- `NOTIFY_AUTHORITY` and `ISSUE_CERTIFICATE` surface a UI prompt; backend handling is a future module
- `DELIVER_REPORT` is Newel-specific — behaviour TBD when Newel is configured

---

## Amendment 2 — TriggerConfig replaces intervalMonths: Int?

### Problem
`VerticalAssetType.intervalMonths: Int?` uses null to signal event-driven verticals. Fleet
vehicles are inspected on mileage or engine hours, not calendar months. A null hack forces
Fleet into an awkward workaround and loses the trigger semantics entirely.

### Change
Replace `intervalMonths: Int?` in `VerticalAssetType` with `triggerConfig: TriggerConfig`.

### New model

```kotlin
data class TriggerConfig(
    val type: TriggerModel,
    val intervalMonths: Int? = null,    // CALENDAR verticals
    val intervalMiles: Int? = null,     // MILEAGE verticals (Fleet)
    val intervalHours: Int? = null      // ENGINE_HOURS verticals (Fleet, Crane)
)
```

### Updated VerticalAssetType

```kotlin
data class VerticalAssetType(
    val code: String,
    val label: String,
    val description: String,
    val triggerConfig: TriggerConfig,   // WAS: intervalMonths: Int?
    val checklistItems: List<String>,
    val defaultFormFields: List<String>
)
```

### Ember seed document example

```kotlin
triggerConfig = TriggerConfig(
    type = TriggerModel.CALENDAR,
    intervalMonths = 12
)
```

### Fleet seed document example (future)

```kotlin
triggerConfig = TriggerConfig(
    type = TriggerModel.MILEAGE,
    intervalMiles = 5000
)
```

### Notes
- All existing Ember asset types use `TriggerModel.CALENDAR` with `intervalMonths` set
- `nextInspectionDue` calculation logic in the ViewModel reads `triggerConfig.type` first,
  then the appropriate interval field
- `TriggerModel.ENTRY_EVENT` (Hatch) has no interval — all interval fields are null
- Add `MILEAGE` and `ENGINE_HOURS` to the `TriggerModel` enum:

```kotlin
enum class TriggerModel {
    CALENDAR,
    PRE_USE,
    ENTRY_EVENT,
    ON_DEMAND,
    MILEAGE,        // ADD
    ENGINE_HOURS    // ADD
}
```

---

## Amendment 3 — Inspection cardinality: deferred, design doc required

### Problem
The current model assumes one NFC tap = one asset = one inspection form. This breaks for:
- **Hatch** — one entry event covers a space, not a single asset
- **Fleet** — one vehicle inspection covers dozens of sub-systems in a single session
- **Anchor** — pre-use checks are often done in batches across a crew's equipment

Changing the asset.id anchor is load-bearing across Room FKs, sync payload, scan events,
and the entire ScanState machine. This is not a safe change to make speculatively inside
Module 34.

### Decision
**Do not address in Module 34.** Inspection cardinality is a named future module.

### Action
Create `openspec/design-docs/inspection-cardinality.md` as a stub with the problem statement.
Exclude Fleet and Hatch multi-asset flows from scope until that design doc is resolved.
Hatch in Module 36 gets a single-asset EntryEventScreen — the multi-asset session model
is a future enhancement.

### Stub content for inspection-cardinality.md

```
# Design Doc — Inspection cardinality
> Status: Open — not yet designed
> Blocking: Fleet (FLEET), Hatch multi-asset sessions, Anchor batch pre-use

## Problem
asset.id is the FK anchor for Inspection, Deficiency, ScanEvent, TagEvent.
One tap = one asset = one inspection is load-bearing throughout the data model.

Fleet, Hatch (session), and Anchor (batch) need a session-scoped or space-scoped
inspection that covers multiple assets or a physical space rather than a single asset.

## Questions to answer before designing
- Does a session get its own UUID, or does it decompose into per-asset inspections?
- How does NFC scanning work inside a session — does each tap add to the session?
- What does the sync payload look like for a session vs. per-asset inspections?
- How does the PDF report render for a session inspection?

## Verticals affected
- HATCH — entry event covers a confined space (not an asset)
- FLEET — vehicle inspection covers the whole vehicle
- ANCHOR — batch pre-use check across multiple harnesses

## Verticals NOT affected (single-asset model is correct)
- EMBER, LIFT, NEWEL, APEX, VAULT, VALVE, SPAN, MAST, GRID, SEAM, CRANE, RACK, WARD
```

---

## Summary of changes to make before /opsx:apply

| # | File | Change |
|---|---|---|
| 1 | `VerticalModels.kt` (new) | Add `ResultOption`, `ResultAction`, `TriggerConfig` |
| 2 | `VerticalModels.kt` | Add `MILEAGE`, `ENGINE_HOURS` to `TriggerModel` enum |
| 3 | `InspectionFormProfile` | `resultOptions: List<ResultOption>` (was `List<String>`) |
| 4 | `VerticalAssetType` | `triggerConfig: TriggerConfig` (was `intervalMonths: Int?`) |
| 5 | Ember seed document | Update to use `ResultOption` and `TriggerConfig` structures |
| 6 | `openspec/design-docs/` | Create `inspection-cardinality.md` stub |

All other module-34 spec files are unaffected.
