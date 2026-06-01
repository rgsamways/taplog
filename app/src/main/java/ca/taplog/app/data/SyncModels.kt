package ca.taplog.app.data

// ── Request models ──────────────────────────────────────────────────────────

data class OrganisationSyncRequest(
    val id: String,
    val name: String,
    val phone: String?,
    val email: String?,
    val address: String?,
    val city: String?,
    val province: String,
    val subscriptionTier: String,
    val subscriptionStatus: String,
    val licensedVerticals: List<String>,
    val createdAt: Long
)

data class SiteSyncRequest(
    val id: String,
    val organisationId: String,
    val name: String,
    val address: String,
    val city: String,
    val province: String,
    val postalCode: String?,
    val clientName: String?,
    val clientPhone: String?,
    val contactName: String?,
    val contactPhone: String?,
    val notes: String?,
    val isActive: Boolean,
    val createdAt: Long
)

data class AssetSyncRequest(
    val id: String,
    val nfcTagId: String,
    val siteId: String,
    val name: String,
    val assetType: String,
    val location: String,
    val installDate: Long,
    val lastInspectedAt: Long?,
    val nextInspectionDue: Long?,
    val isActive: Boolean,
    val createdAt: Long
)

data class InspectionSyncRequest(
    val id: String,
    val assetId: String,
    val inspectorName: String,
    val inspectorCertNumber: String,
    val inspectedAt: Long,
    val latitude: Double?,
    val longitude: Double?,
    val result: String,
    val notes: String?
)

data class DeficiencySyncRequest(
    val id: String,
    val inspectionId: String,
    val code: String,
    val description: String,
    val severity: String,
    val photoPath: String?,
    val resolvedAt: Long?
)

data class ScanEventSyncRequest(
    val id: String,
    val tagId: String,
    val assetId: String,
    val inspectorId: String?,
    val inspectorName: String,
    val scannedAt: Long,
    val eventType: String
)

data class TagEventSyncRequest(
    val id: String,
    val assetId: String,
    val tagId: String,
    val attachedAt: Long,
    val retiredAt: Long?,
    val retiredReason: String?,
    val retiredByInspectorId: String?
)

// ── Response model ──────────────────────────────────────────────────────────

data class SyncResponse(
    val id: String,
    val syncedAt: Long
)

// ── Mapping extensions ──────────────────────────────────────────────────────

fun Organisation.toSyncRequest() = OrganisationSyncRequest(
    id = id,
    name = name,
    phone = phone,
    email = email,
    address = address,
    city = city,
    province = province,
    subscriptionTier = subscriptionTier.name,
    subscriptionStatus = subscriptionStatus.name,
    licensedVerticals = licensedVerticals,
    createdAt = createdAt
)

fun Site.toSyncRequest() = SiteSyncRequest(
    id = id,
    organisationId = organisationId,
    name = name,
    address = address,
    city = city,
    province = province,
    postalCode = postalCode,
    clientName = clientName,
    clientPhone = clientPhone,
    contactName = contactName,
    contactPhone = contactPhone,
    notes = notes,
    isActive = isActive,
    createdAt = createdAt
)

fun Asset.toSyncRequest() = AssetSyncRequest(
    id = id,
    nfcTagId = nfcTagId,
    siteId = siteId,
    name = name,
    assetType = assetType,
    location = location,
    installDate = installDate,
    lastInspectedAt = lastInspectedAt,
    nextInspectionDue = nextInspectionDue,
    isActive = isActive,
    createdAt = createdAt
)

fun Inspection.toSyncRequest() = InspectionSyncRequest(
    id = id,
    assetId = assetId,
    inspectorName = inspectorName,
    inspectorCertNumber = inspectorCertNumber,
    inspectedAt = inspectedAt,
    latitude = latitude,
    longitude = longitude,
    result = result.name,
    notes = notes
)

fun Deficiency.toSyncRequest() = DeficiencySyncRequest(
    id = id,
    inspectionId = inspectionId,
    code = code,
    description = description,
    severity = severity.name,
    photoPath = photoPath,
    resolvedAt = resolvedAt
)

fun ScanEvent.toSyncRequest() = ScanEventSyncRequest(
    id = id,
    tagId = tagId,
    assetId = assetId,
    inspectorId = inspectorId,
    inspectorName = inspectorName,
    scannedAt = scannedAt,
    eventType = eventType.name
)

fun TagEvent.toSyncRequest() = TagEventSyncRequest(
    id = id,
    assetId = assetId,
    tagId = tagId,
    attachedAt = attachedAt,
    retiredAt = retiredAt,
    retiredReason = retiredReason?.name,
    retiredByInspectorId = retiredByInspectorId
)