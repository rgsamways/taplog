### Requirement: Inspector can photograph a deficiency during inspection
The system SHALL allow the inspector to take a photo of a deficiency at the time of recording it in `AddDeficiencyDialog`. The photo SHALL be stored in app-private external storage and the file path SHALL be persisted in `Deficiency.photoPath`.

#### Scenario: Camera permission granted, photo taken
- **WHEN** the inspector taps the camera button in `AddDeficiencyDialog` and grants the `CAMERA` permission
- **THEN** the system camera app opens with a pre-created file URI; on capture, the photo is written to `TapLog/photos/def_<timestamp>.jpg` and a thumbnail is shown in the dialog

#### Scenario: Photo taken, deficiency saved
- **WHEN** the inspector confirms the deficiency after taking a photo
- **THEN** the `Deficiency` record is saved with `photoPath` set to the absolute path of the captured file

#### Scenario: No photo taken
- **WHEN** the inspector creates a deficiency without tapping the camera button
- **THEN** the deficiency is saved with `photoPath = null` — photo is optional

#### Scenario: Camera permission denied
- **WHEN** the inspector denies the `CAMERA` permission request
- **THEN** the camera does not launch and the deficiency can still be saved without a photo

### Requirement: CAMERA permission requested at point of use
The system SHALL request the `CAMERA` permission at the moment the inspector taps the camera button, not at app launch.

#### Scenario: Permission already granted
- **WHEN** the inspector taps the camera button and CAMERA permission is already granted
- **THEN** the camera launches immediately without a permission dialog

#### Scenario: Permission not yet granted
- **WHEN** the inspector taps the camera button and CAMERA permission has not been granted
- **THEN** the system permission dialog is shown; on grant, the camera launches

### Requirement: Photo thumbnail displayed on deficiency chip
The system SHALL display a 48dp thumbnail of the photo in `DeficiencyChip` (InspectionFormScreen) and `DeficiencyWithAssetCard` (OpenDeficienciesScreen) when `Deficiency.photoPath` is non-null.

#### Scenario: Deficiency has a photo
- **WHEN** a deficiency with a non-null `photoPath` is displayed in either screen
- **THEN** a 48dp thumbnail of the photo is shown inline with the deficiency details

#### Scenario: Deficiency has no photo
- **WHEN** a deficiency with `photoPath = null` is displayed
- **THEN** no image space is shown — layout is unchanged from pre-Module 30

### Requirement: Photo path persisted through Room and sync
The system SHALL write `Deficiency.photoPath` to Room when the deficiency is saved. The path SHALL be included in `DeficiencySyncRequest.photoPath` on the next sync (existing field — no sync model changes needed).

#### Scenario: Deficiency with photo synced
- **WHEN** a deficiency with a non-null `photoPath` is synced to the backend
- **THEN** the `photo_path` field in the backend document contains the local file path string (photo binary is not uploaded in Module 30)
