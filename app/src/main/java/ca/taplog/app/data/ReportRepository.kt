package ca.taplog.app.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider

class ReportRepository(private val context: Context) {

    suspend fun generateAndGetUri(
        inspection: Inspection,
        asset: Asset,
        site: Site,
        org: Organisation,
        deficiencies: List<Deficiency>
    ): Uri {
        val file = PdfReportGenerator.generate(inspection, asset, site, org, deficiencies, context)
        return FileProvider.getUriForFile(context, "ca.taplog.app.fileprovider", file)
    }
}
