## Why

Asset type selection is the slowest step in the registration flow — the inspector has to browse or search a 35-item list to find the right type for something they're looking at right now. A camera is always in hand. Now that Field Analyst mode exists (Module 43), rapid asset registration is a core workflow: point at the asset, tap the tag, identify, move on. Claude vision makes this instant.

## What Changes

- New `AiRepository.kt` — single method: photograph an asset → Claude vision API → suggested `VerticalAssetType.code` + confidence
- `ANTHROPIC_API_KEY` added to `local.properties` / `BuildConfig` — direct Android-to-Anthropic call, no backend proxy
- Camera button added to `AssetRegistrationScreen` — tap → capture → "Identifying…" → picker opens pre-selected
- Camera button added to `QuickRegisterSheet` (Field Analyst mode) — same flow, same result
- `AssetTypePickerDialog` updated to accept optional `suggestedCode` — highlights the AI-suggested item with a chip "AI suggested · [Label]"
- `EmberViewModel` gains `identifyAsset()` and `identificationLoading: StateFlow<Boolean>`
- Photo used transiently — captured to cache, sent as base64, deleted after response. Never persisted.
- Graceful fallback: LOW confidence or API error → picker opens normally, no pre-selection, no error shown to user

## Capabilities

### New Capabilities

- `visual-asset-identification`: Camera-driven asset type suggestion using Claude vision. Covers photo capture trigger, API call, confidence-based response handling, suggested type pre-selection in picker, and graceful fallback.

### Modified Capabilities

- `field-analyst-mode`: `QuickRegisterSheet` gains a camera button for AI-driven type suggestion (new requirement on an existing screen)

## Impact

- **Android:** New `AiRepository.kt`, modified `AssetRegistrationScreen.kt`, `QuickRegisterSheet.kt`, `AssetTypePickerDialog.kt`, `EmberViewModel.kt`, `TapLogApplication.kt`
- **Dependencies:** Anthropic SDK / direct HTTPS call to `api.anthropic.com` — no new Gradle dep needed if using OkHttp (already present)
- **Secrets:** `ANTHROPIC_API_KEY` in `local.properties` (gitignored) — not committed, not synced to backend
- **No Room changes, no backend changes, no new migrations**
- **No breaking changes** to existing inspector registration flow — camera button is additive
