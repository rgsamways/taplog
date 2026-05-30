package ca.taplog.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val nfcTagId: String,
    val name: String,
    val assetType: String,
    val location: String,
    val buildingName: String,
    val installDate: Long,
    val lastInspectedAt: Long? = null,
    val nextInspectionDue: Long? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "inspections",
    foreignKeys = [ForeignKey(
        entity = Asset::class,
        parentColumns = ["id"],
        childColumns = ["assetId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Inspection(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
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

@Entity(
    tableName = "deficiencies",
    foreignKeys = [ForeignKey(
        entity = Inspection::class,
        parentColumns = ["id"],
        childColumns = ["inspectionId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class Deficiency(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val inspectionId: String,
    val code: String,
    val description: String,
    val severity: DeficiencySeverity,
    val photoPath: String? = null,
    val resolvedAt: Long? = null
)

enum class InspectionResult {
    PASS,
    FAIL,
    REQUIRES_ATTENTION
}

enum class DeficiencySeverity {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}