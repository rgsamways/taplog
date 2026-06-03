# Module 32 — Context File
> For: `/opsx:propose "module-32-checklist-card"`
> Written: 2026-05-31

---

## What this module is

Add a **Pre-Inspection Checklist card** to `InspectionFormScreen`. Before filling in form fields, the inspector sees the checklist items for the specific asset type they are inspecting. They work through the checklist, then complete the form.

The data already exists: `VerticalAssetType.checklistItems: List<String>` is populated for all 35 Ember asset types and all 6 Anchor asset types. This module is purely a UI addition — no data model changes.

---

## Why this matters (LB-006 context)

The inspection form is the moment of professional uncertainty — especially for a newer inspector or an unfamiliar asset type. The checklist turns TapLog from a logging tool into a compliance aid. This was identified in Session 7 as the "Level 1" guidance layer; the AI co-pilot (Module 33) is Level 2. Level 1 must ship first because it works fully offline.

---

## Scope

**In scope:**
- New `ChecklistCard` composable in `ui/ember/`
- Inserted at the top of `InspectionFormScreen`, above the result selector and form fields
- Collapsible — expanded by default on first open for a given asset type, collapsed if inspector has used this asset type before (optional: simple `remember` state, not persisted)
- Tap-to-check UX: each item has a checkbox; checking all items is not required but provides visual progress
- Checklist state is local to the screen session — not persisted to Room, not synced

**Out of scope:**
- No Room changes
- No backend changes
- No VerticalConfig changes — `checklistItems` is already populated
- No persistence of checklist completion state across sessions
- No changes to any screen other than `InspectionFormScreen`

---

## Data source

```kotlin
// In InspectionFormScreen, already available:
val verticalConfig: VerticalConfig  // from VerticalRegistry.get(asset.vertical)
val assetType: VerticalAssetType    // from verticalConfig.assetTypeRegistry.find { it.code == asset.assetType }

// Checklist items:
val checklistItems: List<String> = assetType?.checklistItems ?: emptyList()
```

If `checklistItems` is empty (shouldn't be for Ember/Anchor, might be for future verticals before they're fully configured) — the card is not shown. No empty card.

---

## ChecklistCard composable

```kotlin
@Composable
fun ChecklistCard(
    assetTypeLabel: String,
    checklistItems: List<String>,
    modifier: Modifier = Modifier
)
```

**Visual design:**
- `Card` with `TapLogNavy700` background (matches the form's card style)
- Header row: checklist icon + "Pre-Inspection Checklist" label + expand/collapse chevron + completion count (e.g. "3 / 7")
- Collapsed: shows only header
- Expanded: shows each item as a row with a `Checkbox` and the item text
- Checked items get a strikethrough on the label and muted color — visual confirmation without being punitive
- Completion count in header updates as items are checked
- All-checked state: header shows a green checkmark icon instead of the checklist icon — positive reinforcement

**Placement in InspectionFormScreen:**
```
┌─────────────────────────────┐
│  Asset identity card        │  (existing — asset name, type, location)
├─────────────────────────────┤
│  ChecklistCard ← NEW        │  (expanded by default)
├─────────────────────────────┤
│  Inspector identity card    │  (existing)
├─────────────────────────────┤
│  Result selector            │  (existing)
├─────────────────────────────┤
│  Form fields loop           │  (existing)
├─────────────────────────────┤
│  Deficiencies section       │  (existing, conditional)
└─────────────────────────────┘
```

---

## State management

Local `remember` state inside `ChecklistCard` — a `Set<Int>` of checked item indices:

```kotlin
var checkedItems by remember { mutableStateOf(setOf<Int>()) }
```

Not a ViewModel concern — checklist state does not need to survive screen recomposition or navigation. If the inspector navigates away and back, the checklist resets. This is correct behaviour — they're starting a new inspection pass.

---

## Edge cases

- **Empty checklist** — card not rendered (guard in `InspectionFormScreen` before calling `ChecklistCard`)
- **Very long checklist** (10+ items) — card starts collapsed; inspector expands to work through it
- **Asset type not found in registry** — `assetType` is null → no checklist card shown; form renders normally (same fallback as today)

---

## Modified files

- `InspectionFormScreen.kt` — add `ChecklistCard` call above inspector identity card; derive `checklistItems` from `VerticalAssetType`

## New files

- `ChecklistCard.kt` in `ca.taplog.app.ui.ember/`

---

## What does NOT change

- `VerticalConfig`, `VerticalAssetType`, `VerticalRegistry` — read-only
- All other screens
- Room schema — no migrations
- Backend — no changes
- The form fields loop, result selector, deficiency section — untouched
