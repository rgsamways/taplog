## ADDED Requirements

### Requirement: Camera button triggers AI asset type identification
The system SHALL provide a camera button adjacent to the asset type field on `AssetRegistrationScreen`. Tapping it SHALL capture a photo, send it to Claude vision API with the current vertical's asset type registry, and pre-select the suggested type in `AssetTypePickerDialog`.

#### Scenario: Camera button tapped — HIGH or MEDIUM confidence result
- **WHEN** the user taps the camera button, captures a photo, and the API returns a result with confidence HIGH or MEDIUM
- **THEN** the asset type field shows "Identifying…" during the API call, then `AssetTypePickerDialog` opens with the suggested type pre-selected and an "AI suggested · [Label]" chip visible on that item

#### Scenario: Camera button tapped — LOW confidence or null code
- **WHEN** the user taps the camera button, captures a photo, and the API returns confidence LOW or a null code
- **THEN** `AssetTypePickerDialog` opens normally with no pre-selection and no chip shown

#### Scenario: API error or timeout
- **WHEN** the user taps the camera button, captures a photo, and the API call fails or times out (15 second limit)
- **THEN** `AssetTypePickerDialog` opens normally with no pre-selection; no error message is shown to the user

#### Scenario: User ignores camera button
- **WHEN** the user does not tap the camera button and manually selects an asset type
- **THEN** the existing manual selection flow is entirely unchanged

### Requirement: Identification photo is transient
The system SHALL NOT persist the identification photo to Room, the sync queue, or the device's media store. The photo SHALL be captured to the app's cache directory, used for the API call, and deleted immediately after the API response is received (whether success or failure).

#### Scenario: Photo cleanup after successful identification
- **WHEN** the API returns a result (any confidence level)
- **THEN** the temporary photo file is deleted from the cache directory before `AssetTypePickerDialog` opens

#### Scenario: Photo cleanup after API error
- **WHEN** the API call throws an exception or times out
- **THEN** the temporary photo file is deleted from the cache directory

### Requirement: Identification prompt includes vertical asset type registry
The system SHALL construct the identification prompt dynamically from the current `VerticalConfig.assetTypeRegistry`. Each asset type in the registry SHALL be included in the prompt as `code: label — description`. The response SHALL be a JSON object with `code` (nullable string), `confidence` (HIGH/MEDIUM/LOW), and `reasoning` (one sentence).

#### Scenario: Ember vertical identification
- **WHEN** the user triggers identification while in the EMBER vertical
- **THEN** the prompt lists all Ember asset types (extinguishers, suppression systems, etc.) and Claude returns the closest matching code

#### Scenario: Anchor vertical identification
- **WHEN** the user triggers identification while in the ANCHOR vertical
- **THEN** the prompt lists all Anchor asset types (harness, lanyard, SRL, anchor point, etc.) and Claude returns the closest matching code

### Requirement: Image scaled before transmission
The system SHALL scale the captured photo to a maximum dimension of 1024px on either axis before base64 encoding and transmission to the API. The original full-resolution photo SHALL be discarded; only the scaled version is transmitted.

#### Scenario: Photo exceeds 1024px
- **WHEN** the captured photo is larger than 1024px in width or height
- **THEN** the image is scaled down proportionally so the largest dimension equals 1024px before encoding

#### Scenario: Photo within 1024px
- **WHEN** the captured photo is 1024px or smaller in both dimensions
- **THEN** the image is transmitted at its original size without scaling

### Requirement: Loading state visible during identification
The system SHALL show a loading state while the identification API call is in flight. The camera button SHALL be replaced with a `CircularProgressIndicator` and the asset type field SHALL show "Identifying…" as placeholder text.

#### Scenario: Identification in progress
- **WHEN** the photo has been captured and the API call is in flight
- **THEN** the camera button is replaced with a spinner and the asset type field shows "Identifying…"; the user cannot trigger another identification

#### Scenario: Identification completes
- **WHEN** the API call returns (success or error)
- **THEN** the loading state clears and the picker opens
