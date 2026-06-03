package ca.taplog.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- OrganisationDao ---

@Dao
interface OrganisationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(organisation: Organisation)

    @Update
    suspend fun update(organisation: Organisation)

    @Query("SELECT * FROM organisations WHERE id = :id")
    suspend fun getById(id: String): Organisation?

    @Query("SELECT * FROM organisations LIMIT 1")
    suspend fun getFirst(): Organisation?

    @Query("SELECT * FROM organisations WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Organisation>

    @Query("UPDATE organisations SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// --- SiteDao ---

@Dao
interface SiteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(site: Site)

    @Update
    suspend fun update(site: Site)

    @Query("SELECT * FROM sites WHERE organisationId = :orgId AND isActive = 1 ORDER BY name ASC")
    fun getByOrganisation(orgId: String): Flow<List<Site>>

    @Query("SELECT * FROM sites WHERE id = :id")
    suspend fun getById(id: String): Site?

    @Query("SELECT * FROM sites WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Site>

    @Query("UPDATE sites SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// --- AssetDao ---

@Dao
interface AssetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(asset: Asset)

    @Update
    suspend fun update(asset: Asset)

    @Query("SELECT * FROM assets WHERE nfcTagId = :tagId AND isActive = 1 LIMIT 1")
    suspend fun getByTagId(tagId: String): Asset?

    @Query("SELECT * FROM assets WHERE isActive = 1 ORDER BY nextInspectionDue ASC")
    fun getAll(): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE siteId = :siteId AND isActive = 1 ORDER BY nextInspectionDue ASC")
    fun getBySite(siteId: String): Flow<List<Asset>>

    @Query("SELECT * FROM assets WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Asset>

    @Query("UPDATE assets SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("UPDATE assets SET lastInspectedAt = :time, nextInspectionDue = :nextDue WHERE id = :id")
    suspend fun updateInspectionDates(id: String, time: Long, nextDue: Long)

    @Query("""
        SELECT a.*, s.name as siteName
        FROM assets a
        INNER JOIN sites s ON a.siteId = s.id
        WHERE a.isActive = 1 AND a.nextInspectionDue IS NOT NULL
        ORDER BY a.nextInspectionDue ASC
    """)
    fun getAssetsWithDueDates(): Flow<List<AssetWithSite>>
}

// --- InspectionDao ---

@Dao
interface InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(inspection: Inspection)

    @Query("SELECT * FROM inspections WHERE assetId = :assetId ORDER BY inspectedAt DESC")
    fun getByAsset(assetId: String): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections ORDER BY inspectedAt DESC")
    fun getAll(): Flow<List<Inspection>>

    @Query("SELECT * FROM inspections WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Inspection>

    @Query("UPDATE inspections SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// --- ScanEventDao ---

@Dao
interface ScanEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(scanEvent: ScanEvent)

    @Query("SELECT * FROM scan_events WHERE assetId = :assetId ORDER BY scannedAt DESC")
    fun getByAsset(assetId: String): Flow<List<ScanEvent>>

    @Query("SELECT * FROM scan_events WHERE isSynced = 0")
    suspend fun getUnsynced(): List<ScanEvent>

    @Query("UPDATE scan_events SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// --- TagEventDao ---

@Dao
interface TagEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tagEvent: TagEvent)

    @Query("SELECT * FROM tag_events WHERE assetId = :assetId ORDER BY attachedAt DESC")
    fun getByAsset(assetId: String): Flow<List<TagEvent>>

    @Query("SELECT * FROM tag_events WHERE assetId = :assetId ORDER BY attachedAt ASC LIMIT 1")
    suspend fun getFirstForAsset(assetId: String): TagEvent?

    @Query("SELECT * FROM tag_events WHERE isSynced = 0")
    suspend fun getUnsynced(): List<TagEvent>

    @Query("UPDATE tag_events SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)
}

// --- AssetWithSite (non-entity JOIN result) ---

data class AssetWithSite(
    @Embedded val asset: Asset,
    @ColumnInfo(name = "siteName") val siteName: String
)

// --- DeficiencyDao ---

@Dao
interface DeficiencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(deficiency: Deficiency)

    @Query("UPDATE deficiencies SET resolvedAt = :time WHERE id = :id")
    suspend fun markResolved(id: String, time: Long)

    @Query("""
        SELECT d.*, a.name as assetName, s.name as buildingName
        FROM deficiencies d
        INNER JOIN assets a ON d.assetId = a.id
        INNER JOIN sites s ON a.siteId = s.id
        WHERE d.resolvedAt IS NULL
        ORDER BY d.severity DESC
    """)
    fun getOpenWithAsset(): Flow<List<DeficiencyWithAsset>>

    @Query("SELECT * FROM deficiencies WHERE isSynced = 0")
    suspend fun getUnsynced(): List<Deficiency>

    @Query("UPDATE deficiencies SET isSynced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    @Query("SELECT * FROM deficiencies WHERE inspectionId = :inspectionId ORDER BY severity DESC")
    suspend fun getByInspection(inspectionId: String): List<Deficiency>
}