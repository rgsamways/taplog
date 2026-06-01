### Requirement: PDF report generated on demand for each inspection
The system SHALL generate a PDF inspection report on demand when the inspector taps "Share Report" on an inspection history entry. The PDF SHALL be generated entirely on-device using `android.graphics.pdf.PdfDocument` with no network call required.

#### Scenario: Inspector taps Share Report
- **WHEN** the inspector taps "Share Report" on an inspection history card on `AssetDetailScreen`
- **THEN** the app generates a PDF for that inspection and launches the Android share sheet

#### Scenario: PDF generated while offline
- **WHEN** the device has no network connectivity and the inspector taps "Share Report"
- **THEN** the PDF is generated and the share sheet is shown — no connectivity is required

### Requirement: PDF report contains the full compliance record
The generated PDF SHALL include all fields required to identify the inspection event: organisation name; site name, address, client name, and on-site contact name; asset name, OFC type label (not code), location, and NFC tag ID; inspection date, inspector name, certificate number, and result; a list of deficiencies (code, description, severity); and the next inspection due date.

#### Scenario: Passed inspection with no deficiencies
- **WHEN** the report is generated for a PASS inspection with no deficiencies
- **THEN** the PDF shows "No deficiencies recorded" in the deficiencies section

#### Scenario: Failed inspection with deficiencies
- **WHEN** the report is generated for a FAIL or REQUIRES ATTENTION inspection with deficiencies
- **THEN** each deficiency appears with its code, description, and severity label

#### Scenario: Many deficiencies
- **WHEN** the inspection has more than 10 deficiencies
- **THEN** the PDF shows the first 10 and a note "… and N more not shown. See full record in TapLog."

### Requirement: Report shared via Android share sheet
The system SHALL share the generated PDF via `ShareCompat.IntentBuilder` so the inspector can send it to any app that accepts PDF files (email, messaging, Drive, etc.).

#### Scenario: Share sheet launched
- **WHEN** the PDF has been generated successfully
- **THEN** the Android share sheet appears, allowing the inspector to choose a destination app

### Requirement: FileProvider used for URI sharing
The system SHALL use a `FileProvider` with authority `ca.taplog.app.fileprovider` to produce a content URI for the generated PDF. Direct `file://` URIs SHALL NOT be used.

#### Scenario: PDF URI is a content URI
- **WHEN** the share intent is built
- **THEN** the URI passed to the intent is a `content://ca.taplog.app.fileprovider/...` URI, not a `file://` URI
