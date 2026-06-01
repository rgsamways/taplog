package ca.taplog.app.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("MMMM d, yyyy 'at' h:mm a", Locale.getDefault())

    fun generate(
        inspection: Inspection,
        asset: Asset,
        site: Site,
        org: Organisation,
        deficiencies: List<Deficiency>,
        context: Context
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 in points
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        drawReport(canvas, inspection, asset, site, org, deficiencies)

        document.finishPage(page)

        val dir = File(context.getExternalFilesDir(null), "TapLog").also { it.mkdirs() }
        val file = File(dir, "taplog_report_${inspection.id.take(8)}.pdf")
        file.outputStream().use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun drawReport(
        canvas: Canvas,
        inspection: Inspection,
        asset: Asset,
        site: Site,
        org: Organisation,
        deficiencies: List<Deficiency>
    ) {
        val margin = 48f
        val pageWidth = 595f
        val contentWidth = pageWidth - margin * 2
        var y = margin

        // --- Paints ---
        val headerPaint = Paint().apply {
            textSize = 22f
            color = Color.parseColor("#1565C0")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subheaderPaint = Paint().apply {
            textSize = 11f
            color = Color.parseColor("#546E7A")
            isAntiAlias = true
        }
        val sectionTitlePaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#1565C0")
            isFakeBoldText = true
            isAntiAlias = true
            letterSpacing = 0.08f
        }
        val labelPaint = Paint().apply {
            textSize = 9f
            color = Color.parseColor("#78909C")
            isAntiAlias = true
        }
        val valuePaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#212121")
            isAntiAlias = true
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 0.5f
        }
        val resultPassPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#2E7D32")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val resultFailPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#C62828")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val resultAttnPaint = Paint().apply {
            textSize = 12f
            color = Color.parseColor("#E65100")
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            textSize = 10f
            color = Color.parseColor("#424242")
            isAntiAlias = true
        }
        val footerPaint = Paint().apply {
            textSize = 9f
            color = Color.parseColor("#9E9E9E")
            isAntiAlias = true
        }

        // ── Header ──────────────────────────────────────────────────────────
        canvas.drawText("Inspection Report", margin, y + 22f, headerPaint)
        y += 30f
        canvas.drawText(org.name, margin, y, subheaderPaint)
        y += 14f
        canvas.drawText(
            "Generated ${dateTimeFormat.format(Date())}",
            margin, y, subheaderPaint
        )
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint.apply { strokeWidth = 1.5f; color = Color.parseColor("#1565C0") })
        y += 16f

        // ── Site / Client ────────────────────────────────────────────────────
        canvas.drawText("SITE & CLIENT", margin, y, sectionTitlePaint)
        y += 14f
        y = drawLabelValue(canvas, margin, y, "Site", site.name, labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Address", "${site.address}, ${site.city}, ${site.province}", labelPaint, valuePaint)
        site.clientName?.let { y = drawLabelValue(canvas, margin, y, "Client", it, labelPaint, valuePaint) }
        site.contactName?.let { y = drawLabelValue(canvas, margin, y, "On-site contact", it, labelPaint, valuePaint) }
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint.apply { strokeWidth = 0.5f; color = Color.parseColor("#E0E0E0") })
        y += 14f

        // ── Asset ────────────────────────────────────────────────────────────
        canvas.drawText("ASSET", margin, y, sectionTitlePaint)
        y += 14f
        y = drawLabelValue(canvas, margin, y, "Name", asset.name, labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Type", OFCCategory.labelForCode(asset.assetType), labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Location", asset.location, labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Tag ID", asset.nfcTagId, labelPaint, valuePaint)
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint.apply { strokeWidth = 0.5f; color = Color.parseColor("#E0E0E0") })
        y += 14f

        // ── Inspection ───────────────────────────────────────────────────────
        canvas.drawText("INSPECTION", margin, y, sectionTitlePaint)
        y += 14f
        y = drawLabelValue(canvas, margin, y, "Date", dateTimeFormat.format(Date(inspection.inspectedAt)), labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Inspector", inspection.inspectorName, labelPaint, valuePaint)
        y = drawLabelValue(canvas, margin, y, "Certificate #", inspection.inspectorCertNumber, labelPaint, valuePaint)

        // Result with colour
        canvas.drawText("Result", margin, y, labelPaint)
        val resultText = inspection.result.name.replace("_", " ")
        val resultPaint = when (inspection.result) {
            InspectionResult.PASS -> resultPassPaint
            InspectionResult.FAIL -> resultFailPaint
            InspectionResult.REQUIRES_ATTENTION -> resultAttnPaint
        }
        canvas.drawText(resultText, margin + 120f, y, resultPaint)
        y += 14f

        inspection.notes?.let {
            y = drawLabelValue(canvas, margin, y, "Notes", it, labelPaint, bodyPaint)
        }
        y += 6f
        canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint.apply { strokeWidth = 0.5f; color = Color.parseColor("#E0E0E0") })
        y += 14f

        // ── Deficiencies ─────────────────────────────────────────────────────
        canvas.drawText("DEFICIENCIES", margin, y, sectionTitlePaint)
        y += 14f

        if (deficiencies.isEmpty()) {
            canvas.drawText("No deficiencies recorded.", margin, y, bodyPaint.apply { color = Color.parseColor("#2E7D32") })
            y += 14f
        } else {
            val shown = deficiencies.take(10)
            shown.forEachIndexed { idx, def ->
                val severityColor = when (def.severity) {
                    DeficiencySeverity.CRITICAL -> Color.parseColor("#B71C1C")
                    DeficiencySeverity.HIGH -> Color.parseColor("#C62828")
                    DeficiencySeverity.MEDIUM -> Color.parseColor("#E65100")
                    DeficiencySeverity.LOW -> Color.parseColor("#546E7A")
                }
                val sevPaint = Paint().apply { textSize = 9f; color = severityColor; isFakeBoldText = true; isAntiAlias = true }
                canvas.drawText("${idx + 1}.", margin, y, valuePaint)
                canvas.drawText(def.code, margin + 18f, y, valuePaint.apply { isFakeBoldText = true })
                canvas.drawText(def.severity.name, pageWidth - margin - 50f, y, sevPaint)
                y += 12f
                val desc = if (def.description.length > 80) def.description.take(80) + "…" else def.description
                canvas.drawText(desc, margin + 18f, y, bodyPaint.apply { color = Color.parseColor("#424242"); isFakeBoldText = false })
                y += 14f
            }
            if (deficiencies.size > 10) {
                canvas.drawText(
                    "… and ${deficiencies.size - 10} more not shown. See full record in TapLog.",
                    margin, y, footerPaint
                )
                y += 14f
            }
        }

        // ── Footer ───────────────────────────────────────────────────────────
        val footerY = 810f
        canvas.drawLine(margin, footerY - 10f, pageWidth - margin, footerY - 10f,
            dividerPaint.apply { strokeWidth = 0.5f; color = Color.parseColor("#E0E0E0") })

        asset.nextInspectionDue?.let {
            canvas.drawText(
                "Next inspection due: ${dateFormat.format(Date(it))}",
                margin, footerY, footerPaint
            )
        }
        canvas.drawText(
            "Generated by TapLog  ·  taplog.ca",
            pageWidth - margin - 160f, footerY, footerPaint
        )
    }

    private fun drawLabelValue(
        canvas: Canvas,
        x: Float,
        y: Float,
        label: String,
        value: String,
        labelPaint: Paint,
        valuePaint: Paint
    ): Float {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x + 120f, y, valuePaint)
        return y + 14f
    }
}
