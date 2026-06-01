package ca.taplog.app.data

import android.util.Log

private const val TAG = "SyncRepository"

class SyncRepository(
    private val organisationDao: OrganisationDao,
    private val siteDao: SiteDao,
    private val assetDao: AssetDao,
    private val inspectionDao: InspectionDao,
    private val deficiencyDao: DeficiencyDao,
    private val scanEventDao: ScanEventDao,
    private val tagEventDao: TagEventDao,
    private val apiService: TapLogApiService
) {

    suspend fun syncOrganisation(org: Organisation): SyncResult {
        return try {
            val response = apiService.syncOrganisation(org.toSyncRequest())
            when (response.code()) {
                200 -> { organisationDao.markSynced(org.id); SyncResult.Success }
                409 -> { organisationDao.markSynced(org.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "Org failed ${response.code()}: ${org.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Org exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncSite(site: Site): SyncResult {
        return try {
            val response = apiService.syncSite(site.toSyncRequest())
            when (response.code()) {
                200 -> { siteDao.markSynced(site.id); SyncResult.Success }
                409 -> { siteDao.markSynced(site.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "Site failed ${response.code()}: ${site.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Site exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncAsset(asset: Asset): SyncResult {
        return try {
            val response = apiService.syncAsset(asset.toSyncRequest())
            when (response.code()) {
                200 -> { assetDao.markSynced(asset.id); SyncResult.Success }
                409 -> { assetDao.markSynced(asset.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "Asset failed ${response.code()}: ${asset.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Asset exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncTagEvent(tagEvent: TagEvent): SyncResult {
        return try {
            val response = apiService.syncTagEvent(tagEvent.toSyncRequest())
            when (response.code()) {
                200 -> { tagEventDao.markSynced(tagEvent.id); SyncResult.Success }
                409 -> { tagEventDao.markSynced(tagEvent.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "TagEvent failed ${response.code()}: ${tagEvent.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TagEvent exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncInspection(inspection: Inspection): SyncResult {
        return try {
            val response = apiService.syncInspection(inspection.toSyncRequest())
            when (response.code()) {
                200 -> { inspectionDao.markSynced(inspection.id); SyncResult.Success }
                409 -> { inspectionDao.markSynced(inspection.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "Inspection failed ${response.code()}: ${inspection.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Inspection exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncDeficiency(deficiency: Deficiency): SyncResult {
        return try {
            val response = apiService.syncDeficiency(deficiency.toSyncRequest())
            when (response.code()) {
                200 -> { deficiencyDao.markSynced(deficiency.id); SyncResult.Success }
                409 -> { deficiencyDao.markSynced(deficiency.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "Deficiency failed ${response.code()}: ${deficiency.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Deficiency exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncScanEvent(scanEvent: ScanEvent): SyncResult {
        return try {
            val response = apiService.syncScanEvent(scanEvent.toSyncRequest())
            when (response.code()) {
                200 -> { scanEventDao.markSynced(scanEvent.id); SyncResult.Success }
                409 -> { scanEventDao.markSynced(scanEvent.id); SyncResult.Conflict("409") }
                else -> { Log.e(TAG, "ScanEvent failed ${response.code()}: ${scanEvent.id}"); SyncResult.Failure("HTTP ${response.code()}") }
            }
        } catch (e: Exception) {
            Log.e(TAG, "ScanEvent exception: ${e.message}")
            SyncResult.Failure(e.message ?: "Unknown error")
        }
    }

    suspend fun syncAll(): SyncBatchResult {
        var synced = 0; var conflicts = 0; var failures = 0

        // Sync order: organisations → sites → assets → tag_events → inspections → deficiencies → scan_events
        for (org in organisationDao.getUnsynced()) {
            when (syncOrganisation(org)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (site in siteDao.getUnsynced()) {
            when (syncSite(site)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (asset in assetDao.getUnsynced()) {
            when (syncAsset(asset)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (tagEvent in tagEventDao.getUnsynced()) {
            when (syncTagEvent(tagEvent)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (inspection in inspectionDao.getUnsynced()) {
            when (syncInspection(inspection)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (deficiency in deficiencyDao.getUnsynced()) {
            when (syncDeficiency(deficiency)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }
        for (scanEvent in scanEventDao.getUnsynced()) {
            when (syncScanEvent(scanEvent)) {
                is SyncResult.Success -> synced++
                is SyncResult.Conflict -> conflicts++
                is SyncResult.Failure -> failures++
            }
        }

        Log.i(TAG, "syncAll — synced=$synced conflicts=$conflicts failures=$failures")
        return SyncBatchResult(synced, conflicts, failures)
    }
}

data class SyncBatchResult(
    val synced: Int,
    val conflicts: Int,
    val failures: Int
) {
    val shouldRetry: Boolean get() = failures > 0
}
