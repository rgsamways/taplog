# Module 34 — Vertical Engine: Architecture Context
> Generated from strategy session 2026-05-31
> Save this file to `openspec/module-34-context.md` in the repo so CLI reads it automatically.

---

## What this module is

Module 34 is the most important architectural module since Module 28. It transforms TapLog from
an Ember-specific app into a vertical factory. After this module, adding a new vertical means
creating one config document in MongoDB and one seed script. No new screens, no new ViewModels,
no new Room migrations.

---

## Key decisions

1. **Backend-driven config** — VerticalConfig lives in MongoDB, fetched at app startup, cached
   locally in Room. A new vertical can be added to the backend without a Play Store update.

2. **Ember's OFCAssetTypes.kt is migrated into the backend** as the first vertical document.
   The Kotlin file becomes a local fallback only (or is removed entirely once the cache is proven).

3. **InspectionFormScreen is refactored to be field-driven** — it reads
   `VerticalRegistry.get(asset.vertical).formProfile.fields` and renders whatever fields are
   defined. No hardcoded Ember layout remains.

4. **Multi-role (Hatch) gets a stub EntryEventScreen** — `roleModel == MULTI_ROLE` routes to a
   new screen shell. The standard InspectionFormScreen remains untouched for all single-inspector
   verticals.

5. **Room version 6 → 7** — adds a `vertical_configs` cache table.

6. **`licensedVerticals: List[str]` added to Organisation** — billing hook for later. The
   `GET /api/v1/verticals` endpoint returns only configs for verticals the org is licensed for.

---

## Data structures

### Android (Kotlin)

```kotlin
enum class TriggerModel {
    CALENDAR,       // Ember — scheduled, interval-based
    PRE_USE,        // Anchor — before each use
    ENTRY_EVENT,    // Hatch — every physical entry
    ON_DEMAND       // Newel — customer-initiated
}

enum class RoleModel {
    SINGLE_INSPECTOR,   // Ember, Anchor annual, Newel
    MULTI_ROLE          // Hatch — supervisor + attendant + entrant
}

enum class FieldType {
    TEXT,
    NUMBER,
    BOOLEAN,
    SINGLE_SELECT,
    MULTI_SELECT,
    DATE,
    PHOTO
}

data class FormField(
    val key: String,
    val label: String,
    val type: FieldType,
    val required: Boolean,
    val applicableAssetTypes: List<String>  // empty = applies to all types
)

data class InspectionFormProfile(
    val resultOptions: List<String>,
    val fields: List<FormField>,
    val requiresPermit: Boolean,
    val requiresWitness: Boolean,
    val deficienciesEnabled: Boolean,
    val photoRequired: Boolean
)

data class VerticalAssetType(
    val code: String,
    val label: String,
    val description: String,
    val intervalMonths: Int?,               // null for event-driven verticals
    val checklistItems: List<String>,
    val defaultFormFields: List<String>     // keys from FormField
)

data class VerticalConfig(
    val vertical: TapLogVertical,
    val displayName: String,
    val shortName: String,
    val regulatoryFramework: String,
    val triggerModel: TriggerModel,
    val roleModel: RoleModel,
    val formProfile: InspectionFormProfile,
    val assetTypeRegistry: List<VerticalAssetType>
)

object VerticalRegistry {
    private val registry = mutableMapOf<TapLogVertical, VerticalConfig>()

    fun register(config: VerticalConfig) {
        registry[config.vertical] = config
    }

    fun get(vertical: TapLogVertical): VerticalConfig =
        registry[vertical] ?: error("Vertical not registered: $vertical")

    fun all(): List<VerticalConfig> = registry.values.toList()
}
```

### Backend (Python / MongoDB)

```python
# New collection: verticals
# One document per vertical, full VerticalConfig as JSON

# New endpoints:
# GET /api/v1/verticals          — returns configs for org's licensedVerticals
# GET /api/v1/verticals/{code}   — returns single vertical config

# Organisation model addition:
# licensed_verticals: List[str] = ["EMBER"]  # billing hook
```

---

## Vertical catalogue (all 18)

### Life Safety
| Code | Name | Domain | Regulation |
|---|---|---|---|
| EMBER | Ember | Fire safety | Ontario Fire Code / CAN/ULC-S536 |
| ANCHOR | Anchor | Fall protection | CSA Z259 / O.Reg 213/91 |
| HATCH | Hatch | Confined space | O.Reg 632/05 |
| LIFT | Lift | Elevators / escalators | TSSA / ASME A17.1 |

### Property
| Code | Name | Domain | Regulation |
|---|---|---|---|
| NEWEL | Newel | Home inspection | OAHI / ASHI |
| APEX | Apex | HVAC | TSSA / HRAI |
| VAULT | Vault | Electrical | ESA / CSA C22.1 |
| VALVE | Valve | Plumbing | OBC / MMAH |

### Infrastructure
| Code | Name | Domain | Regulation |
|---|---|---|---|
| SPAN | Span | Bridges / structures | MTO / CSA S6 |
| MAST | Mast | Towers / telecoms | Industry Canada / CSA |
| GRID | Grid | Power / utilities | IESO / ESA |
| SEAM | Seam | Welding / pressure vessels | TSSA / CSA B51 |

### Industrial
| Code | Name | Domain | Regulation |
|---|---|---|---|
| CRANE | Crane | Crane and hoist | MOL / CSA B167 |
| FLEET | Fleet | Fleet vehicles | MTO / CVOR |
| RACK | Rack | Warehouse racking | MOL / RMI |
| WARD | Ward | Healthcare equipment | Health Canada / CSA Z32 |

---

## What does NOT change in this module

- Sync pipeline — untouched
- Auth / JWT — untouched
- PDF report generator — reads inspection record, doesn't care how it was created
- All existing Ember behaviour — identical, now driven by data instead of hardcoded layout
- Room migrations 1–6 — untouched

---

## Scope boundary

- Module 34: vertical engine + Ember migrated to backend config + InspectionFormScreen refactored
- Module 35: Anchor config added to backend (zero Android changes)
- Module 36: Hatch config + EntryEventScreen for MULTI_ROLE flow
- Binary photo upload: Module 30b (separate, already flagged)
- Admin portal / org invite flows: V2
