## Why

Inspectors currently have no way to hand a report to a client or attach one to a work order. Fire inspection companies need a deliverable — a dated, signed-off PDF that records what was inspected, by whom, and what was found. Without it, TapLog is a logging tool but not a reporting tool, which limits its value to companies whose workflow includes written deliverables.

## What Changes

- New `PdfReportGenerator` — Android `PdfDocument`-based (zero new dependencies); generates one PDF per inspection
- Report content: organisation name; site name, address, client name, contact name; asset name, OFC type label, location, tag ID; inspection date, inspector name, cert number, result; deficiency list (code, description, severity); next inspection due date
- New `ReportRepository` — writes PDF to `getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS, "TapLog")`, returns `Uri` via `FileProvider`
- "Share Report" button on `AssetDetailScreen` for each inspection in the history list
- Android share sheet launched via `ShareCompat.IntentBuilder` so inspector can email or message it to a client
- `FileProvider` registered in `AndroidManifest.xml` with a `file_paths.xml` resource

## Capabilities

### New Capabilities

- `inspection-report`: One PDF per inspection, generated on demand, shared via Android share sheet

### Modified Capabilities

<!-- None — no existing spec-level requirements change -->

## Impact

**Android:**
- NEW `data/PdfReportGenerator.kt` — `PdfDocument` canvas rendering, returns `File`
- NEW `data/ReportRepository.kt` — writes PDF, creates `FileProvider` URI, exposes `generateAndShare()`
- NEW `res/xml/file_paths.xml` — `FileProvider` path configuration
- MODIFIED `AndroidManifest.xml` — `FileProvider` authority `ca.taplog.app.fileprovider` registered
- MODIFIED `ui/ember/AssetDetailScreen.kt` — "Share Report" action per inspection history card
- MODIFIED `ui/ember/EmberViewModel.kt` — `shareReport(inspection, asset, site, org, deficiencies)` function; collects full hierarchy for report, triggers share intent
- MODIFIED `app/MainActivity.kt` — collect `currentOrganisation` and `currentSite` to pass to `EmberScanScreen`; handle share intent result if needed
- MODIFIED `app/TapLogApplication.kt` — wire `ReportRepository`

**No backend changes.** PDF is generated and shared entirely on-device.

**No new dependencies.** `android.graphics.pdf.PdfDocument` is part of the Android framework (API 19+, covered by `minSdk = 26`).
