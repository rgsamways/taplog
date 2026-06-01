## Context

TapLog is a single-Activity Compose app with ViewModel state routing. Auth state lives in a new `AuthViewModel` in `ui/auth/`. The main app logic stays in `EmberViewModel`. `MainActivity` acts as the root router: if no auth token → show auth flow, otherwise → main app.

`InspectorPreferences` currently stores `inspectorName` and `certNumber` from a manual profile form. After this module it stores only `authToken`, `refreshToken`, `inspectorId`, `deviceId`. The inspector's name and cert number are available by decoding the JWT payload (base64, no library) — they are NOT stored separately in DataStore.

The backend is FastAPI + Motor on Railway. JWT secret and Resend API key are Railway environment variables, never committed.

## Goals / Non-Goals

**Goals:**
- Every inspection record is signed by a verified inspector identity (name + cert + device + org all in JWT)
- New device challenge prevents credential sharing across devices without email access
- `organisationId` flows into the JWT payload so the backend can scope records by org
- Organisations and sites sync to backend post-auth (previously local-only)
- `inspectorId` back-fills into `ScanEvent` and `TagEvent` going forward

**Non-Goals:**
- Encrypted token storage (plain DataStore is sufficient for pilot; `EncryptedSharedPreferences` is a v1.1 hardening)
- Biometric unlock
- Inspector role hierarchy / supervisor approval flows
- Multi-org membership
- Token rotation on every request (standard 7-day access + 30-day refresh is sufficient)

## Decisions

### Decision 1: Separate AuthViewModel from EmberViewModel

`AuthViewModel` owns the auth state machine (Unauthenticated, Registering, VerifyingEmail, LoggingIn, NewDevice, Authenticated). `EmberViewModel` is unaware of auth. `MainActivity` checks token presence once at startup: no token → mount `AuthFlow(authViewModel)`, token present → mount `EmberScanScreen(viewModel)`. On `AuthViewModel` emitting `Authenticated`, `MainActivity` switches to the main app without recreating the Activity.

**Alternative considered:** Inject auth state into `EmberViewModel`. Rejected — entangles two concerns that should be independently testable.

### Decision 2: DeviceId generated once at install, stored permanently

On first launch, `InspectorPreferences.ensureDeviceId()` checks for an existing `deviceId` in DataStore. If absent, generates `UUID.randomUUID().toString()`, stores it, and never changes it. This UUID is included in the JWT payload, linking every record to a specific device installation.

**Alternative considered:** Use Android `Settings.Secure.ANDROID_ID`. Rejected — changes on factory reset and is not available in test environments.

### Decision 3: AuthInterceptor uses runBlocking for synchronous token read

OkHttp interceptors are synchronous. `AuthInterceptor` uses `runBlocking { inspectorPreferences.getAuthToken() }` to read the token. The interceptor then:
1. Adds `Authorization: Bearer <token>` to the request
2. Proceeds with the original request
3. On 401: calls `POST /api/v1/auth/refresh` synchronously, stores new tokens, retries once
4. On second 401 (refresh expired): clears all tokens in DataStore

**Alternative considered:** Cache token in memory in `AuthInterceptor`. Valid but requires cache invalidation on login/logout. `runBlocking` on the DataStore read is fast enough (sub-millisecond for a hot DataStore) and avoids the cache coherence problem.

### Decision 4: Inspector name and cert number decoded from JWT, not stored in DataStore

DataStore stores only `authToken`, `refreshToken`, `inspectorId`, `deviceId`. Inspector name and cert number are decoded from the JWT access token payload using `java.util.Base64.decode` (no external library, API 26+ which matches `minSdk`). A utility `fun decodeJwtClaims(token: String): InspectorClaims?` in `InspectorPreferences` extracts the payload. `EmberViewModel` exposes `inspectorClaims: StateFlow<InspectorClaims?>` derived from the stored token.

**Alternative considered:** Store name/cert in DataStore alongside auth fields. Rejected — the spec explicitly says DataStore is stripped to auth fields only, and storing them creates a stale-data risk if the backend updates the inspector's profile.

### Decision 5: InspectionFormScreen shows verified identity as read-only, not editable

After authentication, the inspector's identity is verified. The inspection form replaces the `OutlinedTextField` fields for name and cert with a read-only `Card` showing the authenticated inspector's name, cert number, and organisation. Editing is not possible — the record is signed by whoever is logged in.

### Decision 6: Org/site sync runs first in syncAll, before assets

Sites must exist on the backend before assets can be accepted (assets reference `siteId`). Organisations must exist before sites. Updated sync order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events.

`OrganisationSyncRequest` and `SiteSyncRequest` are added to `SyncModels.kt`. The `Site` entity gains `isSynced: Boolean = false` — which it was missing from MIGRATION_2_3 (acceptable for pilot since device always does fresh install).

### Decision 7: New-device 403 is handled transparently by AuthViewModel

When `POST /api/v1/auth/login` returns HTTP 403, the backend has already sent a verification email. `AuthViewModel` transitions to `NewDevice` state, showing `NewDeviceScreen`. The user enters the 6-digit code; `AuthViewModel` calls `POST /api/v1/auth/register-device` → on 200, stores tokens and transitions to `Authenticated`.

### Decision 8: Auth endpoints do NOT require Bearer token; all sync endpoints do

`/api/v1/auth/*` endpoints use no auth (they ARE the auth). All other endpoints use `Authorization: Bearer`. The `x-api-key` header is removed from all Android sync calls; `AuthInterceptor` injects Bearer automatically. The API key remains on the Railway backend as a server-to-server escape hatch but is not used by the Android client.

## Risks / Trade-offs

- **runBlocking in interceptor** → on a cold DataStore read (first request after app start) this could add ~5ms. Acceptable for inspection field use; no user-facing impact.
- **JWT decode without verification** → `decodeJwtClaims()` decodes the payload but does NOT verify the signature (no public key on device). This is intentional: the backend verifies signatures; the device only reads display claims. A tampered token would be rejected by the backend on the next sync.
- **Refresh token stored in plain DataStore** → acceptable for pilot. If device is compromised, the worst case is unauthorized sync until token expiry. `EncryptedSharedPreferences` is the Module 28.1 hardening.
- **No token rotation on every request** → 7-day access token means a stolen token is valid for up to 7 days. Acceptable for pilot; short-lived tokens + rotation is a post-pilot hardening.

## Migration Plan

1. Deploy backend changes (auth.py, routers, env vars) before Android release
2. Verify all 5 auth endpoints live at Railway URL
3. Install updated Android app → clear app storage first (Device → Storage → Clear storage)
4. Register → verify email → login → confirm JWT stored → test sync with Bearer token
5. No data migration needed — pilot user has no existing records

**Rollback:** Revert Android APK to Module 27 build. Backend can keep auth endpoints live — existing API key endpoints are removed, but no production traffic exists yet.
