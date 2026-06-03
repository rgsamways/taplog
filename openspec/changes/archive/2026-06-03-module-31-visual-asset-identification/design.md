## Context

TapLog currently calls the Anthropic API indirectly (Module 33 AI co-pilot is deferred). This is the first direct Android-to-Anthropic API call in the app. The pattern established here — `AiRepository`, `BuildConfig` key injection, OkHttp call, base64 image, structured JSON response — will be the template for Module 33 as well.

Photo capture infrastructure already exists from Module 30 (`TakePicture` contract, `rememberLauncherForActivityResult`, FileProvider). This module reuses it with a twist: the photo is transient, not persisted.

The feature exists in two contexts:
1. **`AssetRegistrationScreen`** — inspector registering a new asset after tapping an unregistered tag (existing flow)
2. **`QuickRegisterSheet`** — Field Analyst rapid-registering during a scouting visit (Module 43, just shipped)

Both contexts use the same `AiRepository` call and the same result handling.

## Goals / Non-Goals

**Goals:**
- Camera button on both registration surfaces triggers Claude vision identification
- HIGH/MEDIUM confidence → `AssetTypePickerDialog` opens with the type pre-selected and an "AI suggested" chip
- LOW confidence or null → picker opens normally, no chip, no error
- API error or timeout → same as LOW — silent fallback, no disruption to workflow
- Photo is strictly transient — cache dir, deleted after response
- `ANTHROPIC_API_KEY` never committed to git

**Non-Goals:**
- No photo persistence — this is identification only, not evidence capture
- No backend proxy — direct call from Android in v1 (key management is local.properties only)
- No multi-image identification — one photo, one suggestion
- Does not replace the manual picker — always additive

## Decisions

### D1: Direct Android-to-Anthropic vs backend proxy
**Decision:** Direct call from Android using OkHttp.
**Rationale:** Simplest path, no backend changes needed, key lives in local.properties (gitignored). The risk — API key in the APK — is acceptable for dev/pilot phase. Before public distribution, migrate to backend proxy. Document this as a known limitation.
**Alternative:** Backend proxy at `/api/v1/identify` — deferred until commercial launch.

### D2: OkHttp directly vs Anthropic SDK
**Decision:** OkHttp directly (already in dependencies).
**Rationale:** The Anthropic Android SDK doesn't exist as a standalone artifact. Adding the JVM SDK introduces unnecessary dependency weight. The API surface we need is a single POST with a JSON body and a base64 image — trivially implemented with OkHttp + Gson (already present).
**Alternative:** Add `com.anthropic:sdk` JVM artifact — rejected for now, revisit if call complexity grows.

### D3: Prompt placement — `AiRepository` vs `EmberViewModel`
**Decision:** Prompt is constructed in `AiRepository.identifyAsset(imagePath, vertical)` — it receives the full `VerticalConfig` and builds the asset type list from `vertical.assetTypeRegistry`.
**Rationale:** The prompt is tightly coupled to the API call mechanics. The ViewModel should not know about prompt formatting. `AiRepository` owns the full AI interaction boundary.

### D4: Where the camera button lives in `QuickRegisterSheet`
**Decision:** Camera icon button next to the asset type dropdown label row — same visual position as in `AssetRegistrationScreen`.
**Rationale:** Consistent placement means muscle memory transfers. Field Analyst and Inspector use the same gesture in the same position.

### D5: Confidence threshold for pre-selection
**Decision:** HIGH or MEDIUM → pre-select. LOW or null → no pre-selection.
**Rationale:** LOW confidence means Claude isn't sure — showing a wrong suggestion that the user accepts would be worse than no suggestion. The fallback is silent and the picker opens as normal. Inspector/Field Analyst always has final say regardless of confidence level.

## Risks / Trade-offs

- **API key in APK** → Anyone who decompiles the debug APK can extract the key. Mitigation: `local.properties` is gitignored, key is not in the repo. Production mitigation: backend proxy before public App Store release.
- **Network latency** → Identification adds 1–3 seconds. Mitigation: show spinner on camera button and "Identifying…" placeholder in the asset type field. If user doesn't want to wait, they can just use the manual picker (camera button tap is optional).
- **Base64 image size** → A full-res photo from Pixel 10 Pro XL can be 8–15MB. Mitigation: scale the image down to 1024px max dimension before encoding — Claude vision doesn't benefit from full resolution for asset type identification.
- **CAMERA permission** → Already granted from Module 30. No new permission needed.

## Migration Plan

No Room migration, no backend migration. `ANTHROPIC_API_KEY` must be added to `local.properties` manually before testing. Add to AGENTS.md / session handoff so future sessions know about it.

## Open Questions

- Image scaling: scale to 1024px max on the Android side before base64 encoding, or trust the camera to not be excessively large? (Decision: scale to 1024px — predictable payload size, faster API response.)
- Timeout: what's the right OkHttp timeout for the identification call? (Decision: 15 seconds — generous enough for slow connections, short enough to not feel hung.)
