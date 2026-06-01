## ADDED Requirements

### Requirement: Inspector registration creates a verified account
The system SHALL allow a new inspector to register with name, email, certificate number, password, and confirm-password. On submission the backend SHALL create an unverified account and send a 6-digit verification code to the provided email via Resend. Certificate number SHALL be unique — duplicate registration SHALL be rejected with an appropriate error message.

#### Scenario: Successful registration submission
- **WHEN** inspector completes RegistrationScreen with valid name, email, cert number, matching passwords and taps Register
- **THEN** backend creates an unverified inspector record, sends a 6-digit code to the email, and the app transitions to EmailVerificationScreen

#### Scenario: Duplicate certificate number
- **WHEN** inspector submits registration with a cert number already in the `inspectors` collection
- **THEN** backend returns 409 and the app shows an inline error "Certificate number already registered"

#### Scenario: Password mismatch
- **WHEN** inspector submits registration with non-matching password and confirm-password fields
- **THEN** the app shows a validation error before any network call is made

### Requirement: Email verification activates the account and issues a JWT
The system SHALL allow the inspector to submit the 6-digit code from their email. On correct code submission the backend SHALL mark the account as verified and return a JWT access token, refresh token, and inspector payload.

#### Scenario: Correct code entered
- **WHEN** inspector enters the correct 6-digit code on EmailVerificationScreen
- **THEN** backend marks account `emailVerified = true`, issues JWT, app stores tokens and `inspectorId` in DataStore, transitions to the main app

#### Scenario: Incorrect or expired code
- **WHEN** inspector enters an incorrect code or the 15-minute TTL has elapsed
- **THEN** backend returns 400/410 and the app shows "Invalid or expired code — tap Resend to get a new one"

#### Scenario: Resend code
- **WHEN** inspector taps "Resend code" on EmailVerificationScreen
- **THEN** backend issues a new code (invalidating the previous) and sends it to the same email

### Requirement: Login succeeds on a known device and fails with a challenge on a new device
The system SHALL allow a registered, verified inspector to log in with email, password, and device ID. If the device ID is already registered, a JWT SHALL be issued. If the device ID is unrecognised, the backend SHALL return HTTP 403 and send a new-device verification email.

#### Scenario: Login on known device
- **WHEN** inspector submits LoginScreen with correct credentials and the device ID matches a registered device
- **THEN** backend returns JWT, app stores tokens, transitions to main app

#### Scenario: Login on new device
- **WHEN** inspector submits LoginScreen with correct credentials but the device ID is not registered
- **THEN** backend returns HTTP 403, sends a 6-digit code to the inspector's email, app transitions to NewDeviceScreen

#### Scenario: Incorrect password
- **WHEN** inspector submits LoginScreen with wrong password
- **THEN** backend returns 401 and the app shows "Incorrect email or password"

#### Scenario: Unverified account attempts login
- **WHEN** inspector attempts login before verifying their email
- **THEN** backend returns 403 with detail "Email not verified" and the app shows this message

### Requirement: New device challenge registers the device and issues a JWT
The system SHALL allow the inspector to register a new device by submitting the 6-digit code sent during login. On correct code submission the backend SHALL register the device and return a JWT.

#### Scenario: Correct new-device code
- **WHEN** inspector enters the correct code on NewDeviceScreen
- **THEN** backend creates a device record, issues JWT, app stores tokens and `inspectorId`, transitions to main app

#### Scenario: Incorrect new-device code
- **WHEN** inspector enters an incorrect code on NewDeviceScreen
- **THEN** backend returns 400 and app shows "Invalid or expired code"

### Requirement: Auth gate at app startup
The system SHALL check for a stored access token on every app launch. If no token is present, the auth flow SHALL be shown. If a token is present, the main app SHALL be shown directly without requiring re-login.

#### Scenario: Token present at startup
- **WHEN** app launches and DataStore contains a non-empty `authToken`
- **THEN** app proceeds directly to the main app (SiteList) without showing any auth screen

#### Scenario: No token at startup
- **WHEN** app launches and DataStore contains no `authToken`
- **THEN** app shows LoginScreen (or RegistrationScreen if navigated to)

### Requirement: Access token refresh and force logout
The system SHALL automatically refresh the access token when a sync request returns HTTP 401. If the refresh also fails, the system SHALL clear all stored tokens and transition to the auth flow.

#### Scenario: Token refresh on 401
- **WHEN** a sync request returns HTTP 401 and a refresh token is stored
- **THEN** AuthInterceptor calls POST /api/v1/auth/refresh, stores new tokens, and retries the original request once

#### Scenario: Refresh token expired
- **WHEN** the refresh call also returns 401 or 400
- **THEN** AuthInterceptor clears all DataStore tokens and the next app interaction shows the auth flow

### Requirement: Device ID is permanent
The system SHALL generate a UUID device ID exactly once at first launch and store it permanently in DataStore. This ID SHALL never be rotated or regenerated.

#### Scenario: First launch generates device ID
- **WHEN** app launches for the first time and no `deviceId` is found in DataStore
- **THEN** a UUID is generated and stored; all subsequent launches use the same value

#### Scenario: Subsequent launches reuse device ID
- **WHEN** app launches after device ID has been set
- **THEN** the existing device ID is used; no new UUID is generated

### Requirement: Verified inspector identity in inspection form
The system SHALL display the authenticated inspector's name and certificate number as read-only identity information on InspectionFormScreen. These fields SHALL NOT be editable text inputs.

#### Scenario: Inspection form shows verified identity
- **WHEN** authenticated inspector opens InspectionFormScreen
- **THEN** a read-only card shows the inspector's name and cert number decoded from the stored JWT — no OutlinedTextField for these fields

#### Scenario: Inspection submitted with verified identity
- **WHEN** authenticated inspector submits an inspection
- **THEN** `Inspection.inspectorName` and `Inspection.inspectorCertNumber` are set from the JWT claims, not from user input
