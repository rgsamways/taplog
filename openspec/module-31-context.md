# Module 31 — Context File
> For: `/opsx:propose "module-31-visual-asset-identification"`
> Written: 2026-05-31
> Depends on: Module 30 (photo capture infrastructure must exist)

---

## What this module is

When an inspector registers a new asset, they currently pick the asset type by searching/browsing `AssetTypePickerDialog`. This module adds an alternative entry point: photograph the asset → AI suggests the matching `VerticalAssetType` → inspector confirms or overrides.

This is **Visual Asset Identification** — the feature first named in LB-001 when the two-step picker was built.

---

## Scope

**In scope:**
- Camera button added to `AssetRegistrationScreen` — triggers photo capture
- Photo sent to Claude vision API (claude-sonnet-4-20250514) with a prompt that includes the current vertical's `assetTypeRegistry`
- API returns a suggested `VerticalAssetType.code`
- Suggested type is pre-selected in `AssetTypePickerDialog` — inspector confirms or changes
- Loading state while API call is in flight
- Graceful fallback if API call fails or returns low confidence — dialog opens normally with no pre-selection

**Out of scope:**
- This does not replace the manual picker — it pre-selects it. Inspector always has final say.
- No new Room entities or migrations
- No backend changes — API call goes directly to Anthropic from the Android app
- Binary photo storage — photo is used transiently for identification only, not saved to disk

---

## Where the camera button lives

Camera icon button near the asset type field row on `AssetRegistrationScreen`. Tap → capture photo → show loading indicator → pre-select result in picker → open picker. Identification happens before the picker opens, so the picker opens with the right item already highlighted.

---

## Photo capture

Reuse the `TakePicture` contract from Module 30 (`rememberLauncherForActivityResult`). The photo is:
- Captured to a temp file in cache dir (not `TapLog/photos/` — this is transient)
- Sent to API as base64
- Deleted after the API response is received (success or failure)
- Never saved to Room, never synced

---

## API call

**Model:** `claude-sonnet-4-20250514` (vision-capable)

**Prompt structure:**
```
You are identifying a physical asset in a regulated trades inspection app.

The inspector is registering an asset in the ${vertical.displayName} vertical.
The available asset types are:
${assetTypeRegistry.joinToString("\n") { "- ${it.code}: ${it.label} — ${it.description}" }}

Look at this photo and identify which asset type best matches what you see.
Respond with a JSON object only:
{
  "code": "ASSET_TYPE_CODE",
  "confidence": "HIGH" | "MEDIUM" | "LOW",
  "reasoning": "one sentence"
}

If you cannot identify the asset with at least MEDIUM confidence, set code to null.
```

**Response handling:**
- HIGH or MEDIUM confidence → pre-select the code in `AssetTypePickerDialog`, show a chip ("AI suggested: Wet Chemical Extinguisher")
- LOW or null code → open picker normally, no pre-selection, no error shown
- API error / timeout → open picker normally, no pre-selection, log error silently

---

## API key handling

- `ANTHROPIC_API_KEY` stored in `local.properties` → `BuildConfig.ANTHROPIC_API_KEY`
- Added to `.gitignore` — never committed
- Called directly from the ViewModel via a new `AiRepository` — no backend proxy in v1

---

## New files

- `AiRepository.kt` in `ca.taplog.app.data` — single method: `identifyAsset(imagePath: String, vertical: VerticalConfig): AssetIdentificationResult?`
- `AssetIdentificationResult` data class: `code: String?, confidence: String, reasoning: String`

## Modified files

- `AssetRegistrationScreen.kt` — camera button near asset type field, loading state, passes suggested code to picker
- `AssetTypePickerDialog.kt` — accepts optional `suggestedCode: String?` param; highlights the pre-selected type; shows "AI suggested" chip on that item
- `EmberViewModel.kt` — `identifyAsset()` suspend function, `identificationLoading: StateFlow<Boolean>`
- `TapLogApplication.kt` — register `AiRepository` as lazy val

---

## UX states on AssetRegistrationScreen

1. Default — asset type field shows "Select type" with camera icon
2. Loading — camera icon replaced with spinner, field shows "Identifying…"
3. Suggested — picker opens with pre-selected type; chip reads "AI suggested · [Label]"
4. No suggestion — picker opens normally, no chip

---

## What does NOT change

- `AssetTypePickerDialog` search and category browse — unchanged and still the primary path
- Module 30 photo infrastructure for deficiencies — completely separate; do not touch
- VerticalConfig / VerticalRegistry — read-only inputs to the prompt
- Room schema — no migrations
- Backend — no changes
