## 1. API Key Setup

- [x] 1.1 Add `ANTHROPIC_API_KEY=sk-ant-...` to `local.properties` (gitignored — add manually before testing)
- [x] 1.2 Add `buildConfigField("String", "ANTHROPIC_API_KEY", "\"${localProperties["ANTHROPIC_API_KEY"] ?: ""}\"")` to `app/build.gradle.kts`
- [x] 1.3 Verify `local.properties` is in `.gitignore` — add if missing

## 2. AiRepository — New File

- [x] 2.1 Create `AiRepository.kt` in `ca.taplog.app.data`
- [x] 2.2 Define `data class AssetIdentificationResult(val code: String?, val confidence: String, val reasoning: String)`
- [x] 2.3 Implement `suspend fun identifyAsset(imagePath: String, vertical: VerticalConfig): AssetIdentificationResult?` using OkHttp
- [x] 2.4 Construct prompt from `vertical.assetTypeRegistry` — each type as `"- ${it.code}: ${it.label} — ${it.description}"`
- [x] 2.5 Scale image to max 1024px before base64 encoding using `BitmapFactory` + `Bitmap.createScaledBitmap`
- [x] 2.6 Build OkHttp request to `https://api.anthropic.com/v1/messages` with `x-api-key: BuildConfig.ANTHROPIC_API_KEY`, `anthropic-version: 2023-06-01`, JSON body
- [x] 2.7 JSON body: `model = "claude-sonnet-4-6"`, `max_tokens = 256`, `messages` array with one user message containing image (base64) + text prompt
- [x] 2.8 Parse JSON response: extract `content[0].text`, parse as JSON for `code`, `confidence`, `reasoning`
- [x] 2.9 Delete temp photo file in `finally` block — always, success or failure
- [x] 2.10 Return `null` on any exception (network error, parse error, timeout) — never throw to caller
- [x] 2.11 Set OkHttp call timeout to 15 seconds (`callTimeout(15, TimeUnit.SECONDS)`)

## 3. TapLogApplication — Wire AiRepository

- [x] 3.1 Add `val aiRepository: AiRepository by lazy { AiRepository() }` to `TapLogApplication.kt`

## 4. EmberViewModel — identifyAsset

- [x] 4.1 Add `AiRepository` parameter to `EmberViewModel` constructor (nullable, default null)
- [x] 4.2 Add `private val _identificationLoading = MutableStateFlow(false)` and `val identificationLoading: StateFlow<Boolean>`
- [x] 4.3 Add `private val _suggestedAssetCode = MutableStateFlow<String?>(null)` and `val suggestedAssetCode: StateFlow<String?>`
- [x] 4.4 Add `fun identifyAsset(imagePath: String)` — launches coroutine, sets loading true, calls `aiRepository?.identifyAsset(imagePath, verticalConfig)`, sets `_suggestedAssetCode` if HIGH/MEDIUM, sets loading false
- [x] 4.5 Add `fun clearSuggestedAssetCode()` — sets `_suggestedAssetCode.value = null`
- [x] 4.6 Get the current vertical's config inside `identifyAsset` from `VerticalRegistry.get(currentAsset?.vertical ?: TapLogVertical.EMBER)` or from the active vertical context
- [x] 4.7 Update `EmberViewModel.Factory` to accept and pass `aiRepository`
- [x] 4.8 Update `MainActivity` factory instantiation to pass `app.aiRepository`

## 5. AssetTypePickerDialog — suggestedCode support

- [x] 5.1 Add `suggestedCode: String? = null` parameter to `AssetTypePickerDialog`
- [x] 5.2 When `suggestedCode != null`, scroll the list to the matching item on first composition (`LazyListState.animateScrollToItem`)
- [x] 5.3 Add "AI suggested" chip (`SuggestionChip`) on the matching item row — teal colour, "AI suggested · [Label]" text
- [x] 5.4 Pre-select the suggested type: if `suggestedCode` matches an item, set it as the initial selection

## 6. AssetRegistrationScreen — camera button

- [x] 6.1 Read `module-31-context.md` for exact UX placement before editing
- [x] 6.2 Add camera icon `IconButton` adjacent to the asset type row (inline, to the right of the label)
- [x] 6.3 Wire `rememberLauncherForActivityResult(TakePicture)` for photo capture to a temp file in `context.cacheDir`
- [x] 6.4 On photo captured: call `viewModel.identifyAsset(tempFilePath)`
- [x] 6.5 Collect `identificationLoading` — replace camera icon with `CircularProgressIndicator` when true; show "Identifying…" placeholder in asset type field
- [x] 6.6 Collect `suggestedAssetCode` — when non-null, open `AssetTypePickerDialog` with `suggestedCode` pre-filled
- [x] 6.7 Call `viewModel.clearSuggestedAssetCode()` when dialog is dismissed

## 7. QuickRegisterSheet — camera button

- [x] 7.1 Add camera icon `IconButton` adjacent to the asset type dropdown in `QuickRegisterSheet`
- [x] 7.2 Wire `rememberLauncherForActivityResult(TakePicture)` for photo capture to temp cache file
- [x] 7.3 On photo captured: call `viewModel.identifyAsset(tempFilePath)`
- [x] 7.4 Collect `identificationLoading` — show spinner on camera button when loading
- [x] 7.5 Collect `suggestedAssetCode` — when non-null, pre-select matching item in the asset type `DropdownMenu` and show "AI suggested · [Label]" below the dropdown
- [x] 7.6 Call `viewModel.clearSuggestedAssetCode()` when sheet is dismissed

## 8. imePadding Fix (noted during Module 43 testing)

- [x] 8.1 Add `.imePadding()` to the scrollable `Column` in `VisitSetupScreen.kt` so "Begin Visit" is reachable with keyboard up

## 9. Build Verification

- [x] 9.1 `./gradlew assembleDebug` passes — no errors
- [x] 9.2 On device: open `AssetRegistrationScreen`, tap camera button → photo captures → "Identifying…" shows → picker opens (with or without suggestion depending on photo content)
- [x] 9.3 On device: open `QuickRegisterSheet` via "Add Manually" in Field Analyst scan screen → camera button visible → same flow
- [ ] 9.4 API error test: temporarily set `ANTHROPIC_API_KEY` to an invalid value → picker opens normally with no pre-selection and no crash
- [ ] 9.5 Verify temp photo file is deleted from cache after identification (use Device File Explorer to check)
