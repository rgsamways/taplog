## ADDED Requirements

### Requirement: QuickRegisterSheet supports camera-driven asset type identification
`QuickRegisterSheet` SHALL provide a camera button adjacent to the asset type dropdown. Tapping it SHALL trigger the same visual identification flow as `AssetRegistrationScreen` — photo capture, API call, pre-selection of the suggested type in the dropdown with an "AI suggested" chip.

#### Scenario: Field Analyst uses camera in QuickRegisterSheet — suggestion accepted
- **WHEN** a Field Analyst taps the camera button in `QuickRegisterSheet`, captures a photo, and the API returns HIGH or MEDIUM confidence
- **THEN** the asset type dropdown pre-selects the suggested type and shows an "AI suggested · [Label]" label beneath the dropdown

#### Scenario: Field Analyst uses camera in QuickRegisterSheet — no suggestion
- **WHEN** a Field Analyst taps the camera button in `QuickRegisterSheet` and the API returns LOW confidence or null
- **THEN** the asset type dropdown remains unselected; no chip or error is shown

#### Scenario: Field Analyst ignores camera button
- **WHEN** a Field Analyst selects an asset type manually from the dropdown without using the camera button
- **THEN** the existing manual selection flow is unchanged; no AI chip is shown
