package ca.taplog.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class EmberRepository(
    private val database: AppDatabase,
    private val organisationDao: OrganisationDao,
    private val siteDao: SiteDao,
    private val assetDao: AssetDao,
    private val inspectionDao: InspectionDao,
    private val deficiencyDao: DeficiencyDao,
    private val scanEventDao: ScanEventDao,
    private val tagEventDao: TagEventDao
) {

    // --- Organisation ---

    suspend fun saveOrganisation(organisation: Organisation) =
        organisationDao.insert(organisation)

    suspend fun updateOrganisation(organisation: Organisation) =
        organisationDao.update(organisation)

    suspend fun getOrganisation(): Organisation? =
        organisationDao.getFirst()

    suspend fun getUnsyncedOrganisations(): List<Organisation> =
        organisationDao.getUnsynced()

    suspend fun markOrganisationSynced(id: String) =
        organisationDao.markSynced(id)

    // --- Site ---

    suspend fun saveSite(site: Site) =
        siteDao.insert(site)

    suspend fun updateSite(site: Site) =
        siteDao.update(site)

    fun getSitesForOrganisation(orgId: String): Flow<List<Site>> =
        siteDao.getByOrganisation(orgId)

    suspend fun getSiteById(id: String): Site? =
        siteDao.getById(id)

    suspend fun getUnsyncedSites(): List<Site> =
        siteDao.getUnsynced()

    suspend fun markSiteSynced(id: String) =
        siteDao.markSynced(id)

    // --- Asset ---

    suspend fun saveAsset(asset: Asset) =
        assetDao.insert(asset)

    suspend fun updateAsset(asset: Asset) =
        assetDao.update(asset)

    suspend fun getAssetByTagId(tagId: String): Asset? =
        assetDao.getByTagId(tagId)

    fun getAssetsWithDueDates(): Flow<List<AssetWithSite>> =
        assetDao.getAssetsWithDueDates()

    fun getAllAssets(): Flow<List<Asset>> =
        assetDao.getAll()

    fun getAssetsForSite(siteId: String): Flow<List<Asset>> =
        assetDao.getBySite(siteId)

    suspend fun getUnsyncedAssets(): List<Asset> =
        assetDao.getUnsynced()

    suspend fun markAssetSynced(id: String) =
        assetDao.markSynced(id)

    suspend fun updateAssetInspectionDates(id: String, time: Long, nextDue: Long) =
        assetDao.updateInspectionDates(id, time, nextDue)

    // --- Inspection ---

    suspend fun saveInspection(inspection: Inspection) =
        inspectionDao.insert(inspection)

    fun getInspectionsForAsset(assetId: String): Flow<List<Inspection>> =
        inspectionDao.getByAsset(assetId)

    fun getAllInspections(): Flow<List<Inspection>> =
        inspectionDao.getAll()

    suspend fun getUnsyncedInspections(): List<Inspection> =
        inspectionDao.getUnsynced()

    suspend fun markInspectionSynced(id: String) =
        inspectionDao.markSynced(id)

    // --- Deficiency ---

    suspend fun saveDeficiency(deficiency: Deficiency) =
        deficiencyDao.insert(deficiency)

    suspend fun resolveDeficiency(id: String) =
        deficiencyDao.markResolved(id, System.currentTimeMillis())

    fun getOpenDeficienciesWithAsset(): Flow<List<DeficiencyWithAsset>> =
        deficiencyDao.getOpenWithAsset()

    suspend fun getUnsyncedDeficiencies(): List<Deficiency> =
        deficiencyDao.getUnsynced()

    suspend fun markDeficiencySynced(id: String) =
        deficiencyDao.markSynced(id)

    suspend fun getDeficienciesForInspection(inspectionId: String): List<Deficiency> =
        deficiencyDao.getByInspection(inspectionId)

    // --- ScanEvent ---

    suspend fun insertScanEvent(scanEvent: ScanEvent) =
        scanEventDao.insert(scanEvent)

    fun getScanEventsForAsset(assetId: String): Flow<List<ScanEvent>> =
        scanEventDao.getByAsset(assetId)

    suspend fun getUnsyncedScanEvents(): List<ScanEvent> =
        scanEventDao.getUnsynced()

    suspend fun markScanEventSynced(id: String) =
        scanEventDao.markSynced(id)

    // --- TagEvent ---

    suspend fun insertTagEvent(tagEvent: TagEvent) =
        tagEventDao.insert(tagEvent)

    fun getTagEventsForAsset(assetId: String): Flow<List<TagEvent>> =
        tagEventDao.getByAsset(assetId)

    suspend fun getFirstTagEventForAsset(assetId: String): TagEvent? =
        tagEventDao.getFirstForAsset(assetId)

    suspend fun getUnsyncedTagEvents(): List<TagEvent> =
        tagEventDao.getUnsynced()

    suspend fun markTagEventSynced(id: String) =
        tagEventDao.markSynced(id)

    // --- Tag replacement (atomic transaction) ---

    suspend fun replaceTag(
        asset: Asset,
        newTagId: String,
        reason: RetireReason,
        retiredByInspectorId: String? = null
    ) {
        database.withTransaction {
            // Retire the old tag
            tagEventDao.insert(
                TagEvent(
                    assetId = asset.id,
                    tagId = asset.nfcTagId,
                    attachedAt = asset.createdAt,
                    retiredAt = System.currentTimeMillis(),
                    retiredReason = reason,
                    retiredByInspectorId = retiredByInspectorId
                )
            )
            // Update the asset's active tag ID
            assetDao.update(asset.copy(nfcTagId = newTagId, isSynced = false))
            // Record the new tag attachment
            tagEventDao.insert(
                TagEvent(
                    assetId = asset.id,
                    tagId = newTagId,
                    attachedAt = System.currentTimeMillis()
                )
            )
        }
    }
}
