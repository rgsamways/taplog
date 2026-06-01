## 1. Backend — Auth Infrastructure

- [x] 1.1 Add `python-jose[cryptography]` and `passlib[bcrypt]` to `requirements.txt` and `pyproject.toml`
- [x] 1.2 Create `app/auth.py` — `hash_password`, `verify_password`, `create_access_token`, `create_refresh_token`, `decode_token`, `send_verification_email` (via Resend httpx POST)
- [x] 1.3 Add auth Pydantic models to `app/models.py`: `RegisterIn`, `VerifyEmailIn`, `LoginIn`, `RegisterDeviceIn`, `RefreshIn`, `AuthOut` (token + inspector payload)
- [x] 1.4 Create `app/routers/auth.py` — `POST /api/v1/auth/register`: hash password, insert inspector (emailVerified=false), send code via `send_verification_email`
- [x] 1.5 `POST /api/v1/auth/verify-email`: validate code from `pending_verifications`, mark inspector verified, issue JWT pair, return `AuthOut`
- [x] 1.6 `POST /api/v1/auth/login`: verify password, check `emailVerified`, check device in `devices` — 200+JWT if known, 403+send code if new device
- [x] 1.7 `POST /api/v1/auth/register-device`: validate new-device code, insert device record, issue JWT pair, return `AuthOut`
- [x] 1.8 `POST /api/v1/auth/refresh`: decode refresh token, issue new access token, return `AuthOut`
- [x] 1.9 Create `app/routers/organisations.py` — `POST /api/v1/organisations` (Bearer auth, upsert by `_id`)
- [x] 1.10 Create `app/routers/sites.py` — `POST /api/v1/sites` (Bearer auth, upsert by `_id`)
- [x] 1.11 Add `OrganisationIn/Out`, `SiteIn/Out` Pydantic models to `app/models.py`

## 2. Backend — Swap API Key for Bearer on Existing Endpoints

- [x] 2.1 Create `app/dependencies.py` — `get_current_inspector(token: str = Depends(oauth2_scheme))` dependency that decodes and validates JWT
- [x] 2.2 Update `app/routers/assets.py` — replace `x-api-key` header verification with `Depends(get_current_inspector)`
- [x] 2.3 Update `app/routers/inspections.py` — same Bearer auth swap
- [x] 2.4 Update `app/routers/deficiencies.py` — same Bearer auth swap
- [x] 2.5 Update `app/routers/scan_events.py` — same Bearer auth swap
- [x] 2.6 Update `app/routers/tag_events.py` — same Bearer auth swap
- [x] 2.7 Register all new routers in `app/main.py`: auth, organisations, sites

## 3. Backend — Deploy

- [x] 3.1 Add `JWT_SECRET` and `RESEND_API_KEY` to Railway environment variables
- [x] 3.2 Create TTL index on `pending_verifications.expiresAt` in MongoDB Atlas (15-minute expiry)
- [x] 3.3 Create unique index on `inspectors.certNumber` in MongoDB Atlas
- [x] 3.4 Push to GitHub → Railway auto-deploy → verify all 5 auth endpoints via `/docs`
- [x] 3.5 Test full flow: register → verify-email → login known device → login new device → register-device → refresh

## 4. Android — DataStore and Device Identity

- [x] 4.1 Rewrite `data/InspectorPreferences.kt` — keys: `AUTH_TOKEN`, `REFRESH_TOKEN`, `INSPECTOR_ID`, `DEVICE_ID` only; remove `INSPECTOR_NAME`, `CERT_NUMBER`
- [x] 4.2 Add `ensureDeviceId(): String` — generates UUID on first call, persists, returns same value forever
- [x] 4.3 Add `suspend fun getAuthToken(): String?` and `suspend fun getRefreshToken(): String?` for synchronous use in interceptor
- [x] 4.4 Add `suspend fun storeAuthResult(token: String, refreshToken: String, inspectorId: String)` — stores all three atomically
- [x] 4.5 Add `suspend fun clearAuth()` — clears token, refreshToken, inspectorId (keeps deviceId)
- [x] 4.6 Add `fun decodeJwtClaims(token: String): InspectorClaims?` — base64 decode JWT payload, return `data class InspectorClaims(...)` or null on failure

## 5. Android — Auth API and Interceptor

- [x] 5.1 Create `data/AuthApiService.kt` — 5 Retrofit endpoints: `register`, `verifyEmail`, `login`, `registerDevice`, `refresh`; no auth header (auth endpoints handle their own)
- [x] 5.2 Create `data/AuthInterceptor.kt` — reads token via `runBlocking { prefs.getAuthToken() }`, adds `Authorization: Bearer` header; on 401: calls refresh synchronously, stores new tokens, retries once; on second 401: calls `runBlocking { prefs.clearAuth() }`
- [x] 5.3 Update `data/RetrofitClient.kt` — add `AuthInterceptor` to `OkHttpClient`; expose separate `authApiService` built WITHOUT the interceptor (auth endpoints must not inject a token)
- [x] 5.4 Remove `private val apiKey: String` from `data/SyncRepository.kt` and all `@Header("x-api-key")` references in `data/TapLogApiService.kt`

## 6. Android — Sync Models and TapLogApiService

- [x] 6.1 Add `OrganisationSyncRequest` and `SiteSyncRequest` to `data/SyncModels.kt`
- [x] 6.2 Add `Organisation.toSyncRequest()` and `Site.toSyncRequest()` extension functions
- [x] 6.3 Add `syncOrganisation` and `syncSite` to `data/TapLogApiService.kt` (no explicit auth header — injected by interceptor)
- [x] 6.4 Add `OrganisationDao` methods to `EmberDAO.kt`: `getUnsynced(): List<Organisation>` and `markSynced(id: String)`
- [x] 6.5 Add `SiteDao.getUnsynced()` and `SiteDao.markSynced(id: String)` to `EmberDAO.kt`
- [x] 6.6 Add `Organisation.isSynced: Boolean = false` field to `EmberModels.kt`; `Site.isSynced` already present — verified
- [x] 6.7 Write `MIGRATION_4_5` in `AppDatabase.kt` — `ALTER TABLE organisations ADD COLUMN isSynced INTEGER NOT NULL DEFAULT 0`; bump `@Database` version to 5
- [x] 6.8 Update `data/SyncRepository.kt` — add `syncOrganisation`, `syncSite`; update `syncAll()` order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events; remove `apiKey` parameter
- [x] 6.9 Update `app/TapLogApplication.kt` — wire `AuthInterceptor` with `inspectorPreferences` reference; update `SyncRepository` constructor (no apiKey)

## 7. Android — AuthViewModel and Auth Screens

- [x] 7.1 Create `ui/auth/AuthViewModel.kt` — sealed `AuthState`: `Login`, `Registering`, `VerifyingEmail(email)`, `LoggingIn`, `NewDevice(email)`, `Authenticated`; functions: `register`, `verifyEmail`, `login`, `registerDevice`, `resendCode`; exposes `authState: StateFlow<AuthState>` and `errorMessage: StateFlow<String?>`
- [x] 7.2 Create `ui/auth/LoginScreen.kt` — email + password fields; "Log in" button; "Create account" link; passes to `AuthViewModel.login(email, password, deviceId)`
- [x] 7.3 Create `ui/auth/RegistrationScreen.kt` — name, email, cert number, password, confirm-password; inline validation; calls `AuthViewModel.register(...)`
- [x] 7.4 Create `ui/auth/EmailVerificationScreen.kt` — 6-digit code entry (single `OutlinedTextField`, numeric keyboard); "Verify" button; "Resend code" text button; calls `AuthViewModel.verifyEmail(code)`
- [x] 7.5 Create `ui/auth/NewDeviceScreen.kt` — explanation ("A verification code was sent to your email"); 6-digit code entry; "Verify Device" button; calls `AuthViewModel.registerDevice(code)`

## 8. Android — MainActivity Auth Gate

- [x] 8.1 Add `AuthViewModel.Factory` to `TapLogApplication.kt`
- [x] 8.2 Update `app/MainActivity.kt` — at startup, read `inspectorPreferences.authToken` once: no token → show `AuthFlow`; token present → show `EmberScanScreen`
- [x] 8.3 Add `@Composable fun AuthFlow(authViewModel: AuthViewModel, onAuthenticated: () -> Unit)` — `when(authState)` block routing all auth states to their screens; on `Authenticated` state calls `onAuthenticated()`
- [x] 8.4 Wire `AuthViewModel` into `MainActivity` via `viewModels { app.authViewModelFactory }`

## 9. Android — EmberViewModel and InspectionFormScreen

- [x] 9.1 Remove `inspectorName: StateFlow<String>` and `certNumber: StateFlow<String>` from `EmberViewModel` (backed by old DataStore keys)
- [x] 9.2 Remove `saveInspectorProfile(name, cert)` from `EmberViewModel`
- [x] 9.3 Add `inspectorClaims: StateFlow<InspectorClaims?>` to `EmberViewModel` — derived from `inspectorPreferences.authToken.map { decodeJwtClaims(it) }`
- [x] 9.4 Update `EmberViewModel.insertScanEvent()` — set `inspectorId = inspectorPreferences.inspectorId.first()` and `inspectorName = inspectorClaims.value?.name ?: ""`
- [x] 9.5 Update `EmberViewModel.replaceTag()` — set `retiredByInspectorId = inspectorPreferences.inspectorId.first()` in the TagEvent retirement record
- [x] 9.6 Update `EmberViewModel.saveInspection()` — `inspectorName` and `inspectorCertNumber` sourced from `inspectorClaims.value`, not passed as parameters
- [x] 9.7 Update `saveInspection()` signature — remove `inspectorName: String` and `inspectorCertNumber: String` parameters; derive from JWT claims internally
- [x] 9.8 Update `InspectionFormScreen.kt` — remove `savedName`/`savedCertNumber` parameters and their `OutlinedTextField`s; add a read-only `InspectorIdentityCard` composable showing name and cert from JWT claims passed as a parameter
- [x] 9.9 Update `MainActivity.kt` `Inspecting` state handler — remove `savedName`/`savedCertNumber` props; pass `inspectorClaims` to `InspectionFormScreen`
- [x] 9.10 Update `MainActivity.kt` `Inspecting` submit lambda — remove `saveInspectorProfile` call; update `saveInspection` call to match new signature

## 10. End-to-End Verification

- [x] 10.1 Clear app storage on device (Settings → Apps → TapLog → Clear storage)
- [x] 10.2 Register → check email → verify → confirm main app loads
- [x] 10.3 Force-quit app → reopen → confirm no login required (token present)
- [x] 10.4 Create site → sync → confirm in MongoDB Atlas `sites` collection
- [x] 10.5 Tap NFC tag → confirm `ScanEvent` has non-null `inspectorId` in local DB
- [x] 10.6 Submit inspection → confirm `Inspection.inspectorName` comes from JWT, not typed input
- [x] 10.7 Log out (clear storage) → log in on "new device" (change deviceId in DataStore manually or reinstall) → confirm new-device email flow
