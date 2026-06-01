## Why

TapLog's inspection records are currently unsigned — any name and certificate number can be entered into the form, creating compliance liability for fire inspection customers who need tamper-evident proof of verified inspector identity. Authenticated identity is a hard requirement before any pilot user can rely on TapLog for regulatory submissions.

## What Changes

**Backend:**
- New `app/auth.py` — JWT creation/validation, password hashing (`passlib[bcrypt]`), Resend email dispatch
- New `app/routers/auth.py` — 5 endpoints: `register`, `verify-email`, `login`, `register-device`, `refresh`
- New `app/routers/organisations.py` — `POST /api/v1/organisations` (Bearer auth)
- New `app/routers/sites.py` — `POST /api/v1/sites` (Bearer auth)
- `app/models.py` — auth request/response Pydantic models; registration/login/verification payloads
- `app/main.py` — register auth, organisations, sites routers
- **BREAKING** All existing sync endpoints (`/assets`, `/inspections`, `/deficiencies`, `/scan_events`, `/tag_events`) swap `x-api-key` header for `Authorization: Bearer <token>`
- New MongoDB collections: `inspectors`, `devices`, `pending_verifications`
- `.env` — add `JWT_SECRET`, `RESEND_API_KEY`

**Android:**
- New package `ui/auth/` — `AuthViewModel`, `LoginScreen`, `RegistrationScreen`, `EmailVerificationScreen`, `NewDeviceScreen`
- New `data/AuthApiService.kt` — Retrofit interface for 5 auth endpoints
- **BREAKING** `data/InspectorPreferences.kt` — stripped to `authToken`, `refreshToken`, `inspectorId`, `deviceId` only; `inspectorName` and `certNumber` keys removed; `deviceId` generated once at install
- `data/RetrofitClient.kt` — `AuthInterceptor` added to OkHttp client; injects `Authorization: Bearer` on all requests; handles 401 → refresh → retry → force logout
- **BREAKING** `data/TapLogApiService.kt` — API key header removed; Bearer injected by interceptor; `POST /organisations` and `POST /sites` added
- **BREAKING** `data/SyncRepository.kt` — API key parameter removed; `syncOrganisation`, `syncSite` added; sync order updated: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
- `data/SyncModels.kt` — `OrganisationSyncRequest`, `SiteSyncRequest`, wire extensions
- `ui/ember/EmberViewModel.kt` — `inspectorName`/`certNumber` DataStore flows removed; `inspectorId` populated from `InspectorPreferences.inspectorId` on every `ScanEvent` and `TagEvent`
- `ui/ember/InspectionFormScreen.kt` — inspector name and cert number displayed as read-only identity chip (from decoded JWT), not editable fields
- `app/MainActivity.kt` — auth gate at startup; routes to auth flow if no valid token, main app if authenticated
- `app/TapLogApplication.kt` — wire `AuthApiService`, `AuthViewModel` factory accessible from MainActivity

## Capabilities

### New Capabilities

- `inspector-auth`: Full inspector identity lifecycle — registration with email verification, login with device registration, new-device challenge, JWT token management, and force-logout on expired refresh
- `org-site-sync`: Organisations and sites synced to backend after authentication; previously local-only records become part of the persistent multi-device record

### Modified Capabilities

- `scan-log`: `ScanEvent.inspectorId` now populated from `InspectorPreferences.inspectorId` (set from JWT) — no longer null for authenticated sessions
- `tag-lifecycle`: `TagEvent.retiredByInspectorId` now populated from `InspectorPreferences.inspectorId` — no longer null for tag replacements performed by authenticated inspectors

## Impact

**Android dependencies:** none new — JWT decoded via `java.util.Base64` (API 26+, covered by `minSdk = 26`)

**Backend dependencies added:** `python-jose[cryptography]`, `passlib[bcrypt]`, `resend` (or direct `httpx` to Resend API — already a dependency)

**Breaking changes:** API key header removed from all sync endpoints; `InspectorPreferences` keys for `inspectorName`/`certNumber` deleted. No backward compatibility required — pilot user has no existing data.

**Railway env vars required:** `JWT_SECRET` (strong random string), `RESEND_API_KEY` (from resend.com dashboard)
