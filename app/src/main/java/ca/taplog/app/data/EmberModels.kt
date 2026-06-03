package ca.taplog.app.data

import androidx.room.*
import java.util.UUID

// --- Enums ---

enum class InspectionResult { PASS, FAIL, REQUIRES_ATTENTION }
enum class DeficiencySeverity { LOW, MEDIUM, HIGH, CRITICAL }
enum class AssetDetailSource { FROM_SCAN, FROM_LIST }
enum class SubscriptionTier { SOLO, TEAM, COMPANY }
enum class SubscriptionStatus { TRIAL, ACTIVE, PAST_DUE, CANCELLED }
enum class RetireReason { DAMAGED, LOST, REPLACED, REMOVED }
enum class ScanEventType { BROWSE, INSPECTION }

enum class TapLogVertical { EMBER, ANCHOR, HATCH, NEWEL, MAST, CRANE, SEAM, SPAN }

enum class UserRole {
    INSPECTOR, FIELD_ANALYST, OWNER_COMMERCIAL, OWNER_RESIDENTIAL, TENANT, CARETAKER, SUPERVISOR
}

enum class TagEventRole {
    OWNER, FIELD_ANALYST, INSPECTOR, CARETAKER, TENANT
}

// --- Organisation ---

@Entity(tableName = "organisations")
data class Organisation(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val city: String? = null,
    val province: String = "ON",
    val subscriptionTier: SubscriptionTier = SubscriptionTier.SOLO,
    val subscriptionStatus: SubscriptionStatus = SubscriptionStatus.TRIAL,
    val licensedVerticals: List<String> = listOf("EMBER"),
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Site ---

@Entity(
    tableName = "sites",
    foreignKeys = [ForeignKey(
        entity = Organisation::class,
        parentColumns = ["id"],
        childColumns = ["organisationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("organisationId")]
)
data class Site(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val organisationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String = "ON",
    val postalCode: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
    val notes: String? = null,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Asset ---

@Entity(
    tableName = "assets",
    foreignKeys = [ForeignKey(
        entity = Site::class,
        parentColumns = ["id"],
        childColumns = ["siteId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("siteId")]
)
data class Asset(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val nfcTagId: String,
    val siteId: String,
    val name: String,
    val assetType: String,
    val location: String,
    val installDate: Long,
    val lastInspectedAt: Long? = null,
    val nextInspectionDue: Long? = null,
    val isActive: Boolean = true,
    val isSynced: Boolean = false,
    val vertical: TapLogVertical = TapLogVertical.EMBER,
    val registeredByRole: TagEventRole = TagEventRole.OWNER,
    val registeredByUserId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

// --- Inspection ---

@Entity(
    tableName = "inspections",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["assetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assetId")]
)
data class Inspection(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val inspectorName: String,
    val inspectorCertNumber: String,
    val inspectedAt: Long = System.currentTimeMillis(),
    val latitude: Double? = null,
    val longitude: Double? = null,
    val result: InspectionResult,
    val notes: String? = null,
    val isSynced: Boolean = false
)

// --- Deficiency ---

@Entity(
    tableName = "deficiencies",
    foreignKeys = [ForeignKey(
        entity = Inspection::class,
        parentColumns = ["id"],
        childColumns = ["inspectionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("inspectionId")]
)
data class Deficiency(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val inspectionId: String,
    val assetId: String,
    val code: String,
    val description: String,
    val severity: DeficiencySeverity,
    val photoPath: String? = null,
    val resolvedAt: Long? = null,
    val isSynced: Boolean = false
)

// --- ScanEvent ---

@Entity(
    tableName = "scan_events",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["assetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assetId")]
)
data class ScanEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val tagId: String,
    val assetId: String,
    val inspectorId: String? = null,
    val inspectorName: String,
    val scannedAt: Long = System.currentTimeMillis(),
    val eventType: ScanEventType,
    val isSynced: Boolean = false
)

// --- TagEvent ---

@Entity(
    tableName = "tag_events",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["assetId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("assetId")]
)
data class TagEvent(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val assetId: String,
    val tagId: String,
    val attachedAt: Long,
    val retiredAt: Long? = null,
    val retiredReason: RetireReason? = null,
    val retiredByInspectorId: String? = null,
    val registeredByRole: TagEventRole = TagEventRole.OWNER,
    val registeredByUserId: String? = null,
    val registeredByName: String? = null,
    val registeredByCertNumber: String? = null,
    val isSynced: Boolean = false
)

// --- DeficiencyWithAsset (non-entity JOIN result) ---

data class DeficiencyWithAsset(
    val id: String,
    val inspectionId: String,
    val assetId: String,
    val code: String,
    val description: String,
    val severity: DeficiencySeverity,
    val photoPath: String? = null,
    val resolvedAt: Long? = null,
    val isSynced: Boolean = false,
    val assetName: String,
    val buildingName: String   // kept for display — sourced from Site.name going forward
)