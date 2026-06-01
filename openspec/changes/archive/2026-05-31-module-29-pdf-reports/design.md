## Context

Android's `PdfDocument` API (available since API 19) provides a `Canvas` that accepts standard `Paint`/`TextPaint` drawing calls. It writes directly to an `OutputStream`. No third-party library is needed. The output is a valid PDF that opens in any viewer.

The full data hierarchy for a report (Organisation → Site → Asset → Inspection + Deficiencies) is already available in Room. The ViewModel retrieves it synchronously before launching the share sheet.

`FileProvider` is required to share files from the app's private external storage directory with other apps (email, WhatsApp, etc.). Without it, sharing a `file://` URI fails on API 24+.

## Goals / Non-Goals

**Goals:**
- One PDF per inspection, generated on demand (not pre-generated or cached)
- Covers the full compliance record: org identity, site/client info, asset details, inspection result, deficiencies, next due date
- Shared via the Android share sheet — inspector chooses destination (email, WhatsApp, Drive, etc.)
- Zero new dependencies

**Non-Goals:**
- Company logo or branding on the report (v2)
- Multi-page reports (all inspections are single-page — deficiency lists are short)
- PDF stored on backend or attached to sync record (out of scope for Module 29)
- Batch export of multiple inspections

## Decisions

### Decision 1: PdfDocument over iText

`android.graphics.pdf.PdfDocument` is built into Android, requires no Gradle dependency, and is sufficient for a structured single-page report with text sections. iText adds ~3 MB to the APK and introduces a licensing consideration for commercial use. For a clean text-based report, `PdfDocument` with `Canvas` drawing is the right call.

**Alternative considered:** iText (AGPL). Rejected — dependency overhead, licensing ambiguity, and overkill for v1 text reports.

### Decision 2: Generate on demand, not on inspection submit

Report generation is a user-initiated action from the inspection history card. Generating PDFs on every inspection submit would fill storage unnecessarily. The user taps "Share Report" on a specific inspection → PDF generated → share sheet → file is transient (can be regenerated any time).

**Tradeoff:** Generating from `AssetDetailScreen` requires fetching all report data (org, site, deficiencies for the specific inspection) at the moment of the share tap. This is a fast local DB read — no visible delay.

### Decision 3: Write to getExternalFilesDir(), share via FileProvider

Files in `getExternalFilesDir()` are accessible to the app and, via `FileProvider`, to external apps through content URIs. The file is written synchronously (PDF generation is fast — <50ms for a single page), then the share intent is launched.

The file is named `taplog_report_<inspectionId_prefix>.pdf` — deterministic, overwrites on regenerate, no accumulation of old files.

### Decision 4: All report data fetched in ViewModel before generation

`EmberViewModel.shareReport()` is a `viewModelScope.launch` coroutine that:
1. Fetches `Organisation` (already in ViewModel state)
2. Fetches `Site` by `asset.siteId` (via `repository.getSiteById`)
3. Fetches `Deficiency` list for the specific `inspectionId`
4. Calls `ReportRepository.generateReport(inspection, asset, site, org, deficiencies)`
5. Gets back a `Uri` and fires a share intent via a `SharedFlow` that `MainActivity` observes

`MainActivity` observes a `shareEvent: SharedFlow<Intent>` and calls `startActivity(chooser)`.

### Decision 5: FileProvider authority = `ca.taplog.app.fileprovider`

Standard convention: `<applicationId>.fileprovider`. Registered in `AndroidManifest.xml` as a `<provider>` with `android:exported="false"` and `android:grantUriPermissions="true"`.

## Risks / Trade-offs

- **Canvas-based layout is manual** — no auto-wrap, no table layout engine. Long deficiency descriptions could overflow a line. Mitigation: truncate description at 80 characters with ellipsis, or split into multiple lines manually. For v1, truncation is acceptable.
- **External storage permission** — `getExternalFilesDir()` does NOT require `WRITE_EXTERNAL_STORAGE` permission (it's the app's own directory). No permission request needed.
- **Deficiency list overflow** — if there are many deficiencies, content may exceed one page. For v1, render up to 10 deficiencies with a "… and N more" note if truncated. Full multi-page support is a v2 enhancement.
