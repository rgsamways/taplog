package ca.taplog.app.ui.ember

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.taplog.app.data.Asset
import ca.taplog.app.data.AssetDetailSource
import ca.taplog.app.data.Deficiency
import ca.taplog.app.data.EmberRepository
import ca.taplog.app.data.GeocodingRepository
import ca.taplog.app.data.Inspection
import ca.taplog.app.data.InspectionResult
import ca.taplog.app.data.InspectorClaims
import ca.taplog.app.data.InspectorPreferences
import ca.taplog.app.data.OFCCategory
import ca.taplog.app.data.Organisation
import ca.taplog.app.data.RetireReason
import ca.taplog.app.data.ScanEvent
import ca.taplog.app.data.ScanEventType
import ca.taplog.app.data.Site
import ca.taplog.app.data.AiRepository
import ca.taplog.app.data.ServiceRequest
import ca.taplog.app.data.ServiceRequestStatus
import ca.taplog.app.data.TagEvent
import ca.taplog.app.data.TagEventRole
import ca.taplog.app.data.TapLogVertical
import ca.taplog.app.data.UserRole
import ca.taplog.app.data.VerticalRegistry
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar

class EmberViewModel(
    private val repository: EmberRepository,
    private val inspectorPreferences: InspectorPreferences,
    private val reportRepository: ca.taplog.app.data.ReportRepository? = null,
    private val geocodingRepository: GeocodingRepository? = null,
    private val aiRepository: AiRepository? = null
) : ViewModel() {

    // --- Scan state ---

    sealed class ScanState {
        object OrganisationSetup : ScanState()
        object Dashboard : ScanState()          // home screen
        object SiteList : ScanState()           // full-screen site list ("See all")
        object Calendar : ScanState()
        object Tasks : ScanState()
        object Contacts : ScanState()
        data class SiteSelected(val site: Site, val fromSiteList: Boolean = false) : ScanState()
        object AssetList : ScanState()
        object OpenDeficiencies : ScanState()
        object Idle : ScanState()
        object Scanning : ScanState()
        data class AssetFound(val asset: Asset) : ScanState()
        data class Inspecting(val asset: Asset) : ScanState()
        data class AwaitingReplacementTag(val asset: Asset, val reason: RetireReason) : ScanState()
        object AssetNotFound : ScanState()
        data class Error(val message: String) : ScanState()
        // Field Analyst states
        object FieldAnalystDashboard : ScanState()
        object VisitSetup : ScanState()
        object FieldAnalystScanning : ScanState()
        data class UnregisteredTag(val tagId: String) : ScanState()
        // Service request states
        data class ServiceRequestForm(val asset: Asset) : ScanState()
        data class ServiceRequestTrail(val asset: Asset) : ScanState()
    }

    // --- Dashboard data classes ---

    data class DashboardStats(
        val inspectionsThisMonth: Int,
        val openDeficiencies: Int,
        val overdueSiteCount: Int,
        val totalSites: Int
    )

    data class SiteWithOverdueCount(
        val site: Site,
        val overdueCount: Int,
        val mostOverdueDays: Int,
        val mostOverdueAssetName: String
    )

    // --- Calendar ---

    enum class CalendarUrgency { UPCOMING, DUE_SOON, OVERDUE }

    data class CalendarEvent(
        val assetId: String,
        val assetName: String,
        val siteName: String,
        val assetTypeCode: String,
        val dueDate: java.time.LocalDate,
        val urgency: CalendarUrgency
    )

    // --- Tasks ---

    enum class TaskType { OPEN_DEFICIENCY, OVERDUE_ASSET }

    data class Task(
        val id: String,
        val type: TaskType,
        val title: String,
        val siteName: String,
        val severity: ca.taplog.app.data.DeficiencySeverity? = null,
        val daysOverdue: Int? = null,
        val createdAt: Long
    )

    // --- Contacts ---

    data class SiteContact(
        val name: String,
        val phone: String?,
        val role: String,
        val siteName: String,
        val siteId: String
    )

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _lastScannedTagId = MutableStateFlow<String?>(null)
    val lastScannedTagId: StateFlow<String?> = _lastScannedTagId.asStateFlow()

    private val _assetDetailSource = MutableStateFlow(AssetDetailSource.FROM_SCAN)
    val assetDetailSource: StateFlow<AssetDetailSource> = _assetDetailSource.asStateFlow()

    // --- Inspector identity (from JWT) ---

    val inspectorClaims: StateFlow<InspectorClaims?> = inspectorPreferences.authToken
        .map { token -> inspectorPreferences.decodeJwtClaims(token) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // --- User role ---

    val userRole: StateFlow<UserRole> = inspectorPreferences.userRole
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.INSPECTOR)

    fun setUserRole(role: UserRole) {
        viewModelScope.launch { inspectorPreferences.setUserRole(role) }
    }

    // --- Field Analyst visit state ---

    private val _activeVisitSiteId = MutableStateFlow<String?>(null)
    val activeVisitSiteId: StateFlow<String?> = _activeVisitSiteId.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val fieldAnalystAssetCount: StateFlow<Int> = _activeVisitSiteId
        .flatMapLatest { siteId ->
            if (siteId != null) repository.getAssetsForSite(siteId).map { it.size }
            else flowOf(0)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startVisit(site: Site) {
        _activeVisitSiteId.value = site.id
        _currentSite.value = site
        _scanState.value = ScanState.FieldAnalystScanning
    }

    fun endVisit() {
        _activeVisitSiteId.value = null
        _scanState.value = ScanState.FieldAnalystDashboard
    }

    fun showFieldAnalystDashboard() {
        _scanState.value = ScanState.FieldAnalystDashboard
    }

    fun showVisitSetup() {
        _scanState.value = ScanState.VisitSetup
    }

    fun showFieldAnalystScanning() {
        _scanState.value = ScanState.FieldAnalystScanning
    }

    fun saveSiteForVisit(
        name: String, address: String, city: String,
        postalCode: String?, buildingType: String,
        ownerName: String?, ownerPhone: String?, notes: String?
    ) {
        viewModelScope.launch {
            val org = _organisation.value ?: return@launch
            val site = Site(
                organisationId = org.id,
                name = name,
                address = address,
                city = city,
                postalCode = postalCode,
                clientName = ownerName,
                clientPhone = ownerPhone,
                notes = if (notes != null) "[$buildingType] $notes" else "[$buildingType]"
            )
            repository.saveSite(site)
            geocodingRepository?.let { geo ->
                launch {
                    val coords = geo.geocode(address, city, "ON")
                    if (coords != null) repository.saveSite(site.copy(latitude = coords.first, longitude = coords.second))
                }
            }
            startVisit(site)
        }
    }

    fun showUnregisteredTag(tagId: String) {
        _lastScannedTagId.value = tagId
        _scanState.value = ScanState.UnregisteredTag(tagId)
    }

    fun registerFieldAnalystAsset(
        tagId: String,
        assetType: String,
        name: String,
        location: String,
        siteId: String
    ) {
        viewModelScope.launch {
            val inspectorId = inspectorPreferences.inspectorId.first()
            val claims = inspectorClaims.value
            val asset = Asset(
                nfcTagId = tagId,
                siteId = siteId,
                name = name,
                assetType = assetType,
                location = location,
                installDate = System.currentTimeMillis(),
                registeredByRole = TagEventRole.FIELD_ANALYST,
                registeredByUserId = inspectorId
            )
            repository.saveAsset(asset)
            repository.insertTagEvent(
                TagEvent(
                    assetId = asset.id,
                    tagId = tagId,
                    attachedAt = System.currentTimeMillis(),
                    registeredByRole = TagEventRole.FIELD_ANALYST,
                    registeredByUserId = inspectorId,
                    registeredByName = claims?.name
                )
            )
        }
    }

    // --- Organisation ---

    private val _organisation = MutableStateFlow<Organisation?>(null)
    val organisation: StateFlow<Organisation?> = _organisation.asStateFlow()

    fun loadOrganisation(forceRole: UserRole? = null) {
        viewModelScope.launch {
            val org = repository.getOrganisation()
            _organisation.value = org
            val role = forceRole ?: inspectorPreferences.userRole.first()
            _scanState.value = when {
                org == null -> ScanState.OrganisationSetup
                role == UserRole.FIELD_ANALYST -> ScanState.FieldAnalystDashboard
                else -> ScanState.Dashboard
            }
        }
    }

    fun saveOrganisation(name: String, city: String, province: String) {
        viewModelScope.launch {
            val org = Organisation(
                name = name,
                city = city,
                province = province
            )
            repository.saveOrganisation(org)
            _organisation.value = org
            val role = inspectorPreferences.userRole.first()
            _scanState.value = when (role) {
                UserRole.FIELD_ANALYST -> ScanState.FieldAnalystDashboard
                else -> ScanState.Dashboard
            }
        }
    }

    // --- Sites ---

    private val _currentSite = MutableStateFlow<Site?>(null)
    val currentSite: StateFlow<Site?> = _currentSite.asStateFlow()

    val sites = _organisation
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _siteAssets = MutableStateFlow<List<Asset>>(emptyList())
    val siteAssets: StateFlow<List<Asset>> = _siteAssets.asStateFlow()

    fun getSitesForOrg() = _organisation.value?.let {
        repository.getSitesForOrganisation(it.id)
    }

    fun saveSite(
        name: String,
        address: String,
        city: String,
        province: String,
        postalCode: String?,
        clientName: String?,
        clientPhone: String?,
        contactName: String?,
        contactPhone: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val org = _organisation.value ?: return@launch
            val site = Site(
                organisationId = org.id,
                name = name,
                address = address,
                city = city,
                province = province,
                postalCode = postalCode,
                clientName = clientName,
                clientPhone = clientPhone,
                contactName = contactName,
                contactPhone = contactPhone,
                notes = notes
            )
            repository.saveSite(site)
            _scanState.value = ScanState.SiteList

            // Geocode in background — non-blocking, non-fatal
            geocodingRepository?.let { geo ->
                launch {
                    val coords = geo.geocode(address, city, province)
                    if (coords != null) {
                        repository.saveSite(site.copy(latitude = coords.first, longitude = coords.second))
                    }
                }
            }
        }
    }

    fun geocodeUnresolvedSites(sites: List<Site>) {
        val geo = geocodingRepository ?: return
        viewModelScope.launch {
            sites.filter { it.latitude == null }.forEach { site ->
                val coords = geo.geocode(site.address, site.city, site.province)
                if (coords != null) {
                    repository.saveSite(site.copy(latitude = coords.first, longitude = coords.second))
                }
            }
        }
    }

    fun selectSite(site: Site, fromSiteList: Boolean = false) {
        _currentSite.value = site
        viewModelScope.launch {
            repository.getAssetsForSite(site.id).collect { assets ->
                _siteAssets.value = assets
            }
        }
        _scanState.value = ScanState.SiteSelected(site, fromSiteList)
    }

    fun showDashboard() {
        _currentSite.value = null
        _scanState.value = ScanState.Dashboard
    }

    fun selectSiteById(siteId: String) {
        viewModelScope.launch {
            val site = repository.getSiteById(siteId) ?: return@launch
            selectSite(site)
        }
    }

    fun showSiteList() {
        _scanState.value = ScanState.SiteList
    }

    fun showCalendar() { _scanState.value = ScanState.Calendar }
    fun showTasks() { _scanState.value = ScanState.Tasks }
    fun showContacts() { _scanState.value = ScanState.Contacts }

    fun showAddSite() {
        _scanState.value = ScanState.Idle
    }

    // --- Navigation ---

    fun selectAsset(asset: Asset) {
        _assetDetailSource.value = AssetDetailSource.FROM_LIST
        _scanState.value = ScanState.AssetFound(asset)
    }

    fun showOpenDeficiencies() {
        _scanState.value = ScanState.OpenDeficiencies
    }

    fun showAssetList() {
        _scanState.value = ScanState.AssetList
    }

    fun startInspection(asset: Asset) {
        _scanState.value = ScanState.Inspecting(asset)
    }

    fun resetScanState() {
        _scanState.value = _currentSite.value?.let {
            ScanState.SiteSelected(it)
        } ?: ScanState.Dashboard
    }

    // --- Asset data ---

    val activeAssets = repository.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openDeficienciesWithAsset = repository.getOpenDeficienciesWithAsset()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard ---

    private val sitesForOrg: kotlinx.coroutines.flow.Flow<List<Site>> =
        _organisation.flatMapLatest { org ->
            if (org != null) repository.getSitesForOrganisation(org.id)
            else flowOf(emptyList())
        }

    val allInspections = repository.getAllInspections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dashboardStats: StateFlow<DashboardStats> =
        combine(
            sitesForOrg,
            openDeficienciesWithAsset,
            activeAssets,
            allInspections
        ) { sites, deficiencies, assets, inspections ->
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfMonth = cal.timeInMillis
            val inspectionsThisMonth = inspections.count { it.inspectedAt >= startOfMonth }
            val overdueAssets = assets.filter { (it.nextInspectionDue ?: Long.MAX_VALUE) < now }
            val overdueSiteIds = overdueAssets.map { it.siteId }.toSet()
            DashboardStats(
                inspectionsThisMonth = inspectionsThisMonth,
                openDeficiencies = deficiencies.size,
                overdueSiteCount = overdueSiteIds.size,
                totalSites = sites.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats(0, 0, 0, 0))

    val overdueSites: StateFlow<List<SiteWithOverdueCount>> =
        combine(sitesForOrg, activeAssets) { sites, assets ->
            val now = System.currentTimeMillis()
            val overdueAssets = assets.filter { (it.nextInspectionDue ?: Long.MAX_VALUE) < now }
            val bySite = overdueAssets.groupBy { it.siteId }
            sites.mapNotNull { site ->
                val siteOverdue = bySite[site.id] ?: return@mapNotNull null
                val mostOverdue = siteOverdue.minByOrNull { it.nextInspectionDue ?: 0L }!!
                val daysOverdue = ((now - (mostOverdue.nextInspectionDue ?: now)) /
                        (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                SiteWithOverdueCount(
                    site = site,
                    overdueCount = siteOverdue.size,
                    mostOverdueDays = daysOverdue,
                    mostOverdueAssetName = mostOverdue.name
                )
            }.sortedByDescending { it.mostOverdueDays }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Calendar events ---

    val calendarEvents: StateFlow<Map<LocalDate, List<CalendarEvent>>> =
        repository.getAssetsWithDueDates()
            .map { assetsWithSite ->
                val today = LocalDate.now(ZoneId.systemDefault())
                val sevenDaysFromNow = today.plusDays(7)
                assetsWithSite.mapNotNull { aws ->
                    val dueMillis = aws.asset.nextInspectionDue ?: return@mapNotNull null
                    val dueDate = java.time.Instant.ofEpochMilli(dueMillis)
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    val urgency = when {
                        dueDate.isBefore(today) -> CalendarUrgency.OVERDUE
                        !dueDate.isAfter(sevenDaysFromNow) -> CalendarUrgency.DUE_SOON
                        else -> CalendarUrgency.UPCOMING
                    }
                    CalendarEvent(
                        assetId = aws.asset.id,
                        assetName = aws.asset.name,
                        siteName = aws.siteName,
                        assetTypeCode = aws.asset.assetType,
                        dueDate = dueDate,
                        urgency = urgency
                    )
                }.groupBy { it.dueDate }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Unified tasks ---

    val tasks: StateFlow<List<Task>> =
        combine(openDeficienciesWithAsset, activeAssets) { deficiencies, assets ->
            val now = System.currentTimeMillis()
            val overdueTasks = assets
                .filter { (it.nextInspectionDue ?: Long.MAX_VALUE) < now }
                .map { asset ->
                    val daysOverdue = ((now - (asset.nextInspectionDue ?: now)) /
                            (1000L * 60 * 60 * 24)).toInt().coerceAtLeast(1)
                    Task(
                        id = asset.id,
                        type = TaskType.OVERDUE_ASSET,
                        title = "${asset.name} overdue",
                        siteName = "",
                        daysOverdue = daysOverdue,
                        createdAt = asset.nextInspectionDue ?: now
                    )
                }
            val deficiencyTasks = deficiencies.map { d ->
                Task(
                    id = d.id,
                    type = TaskType.OPEN_DEFICIENCY,
                    title = d.description.take(60),
                    siteName = d.buildingName,
                    severity = d.severity,
                    createdAt = 0L
                )
            }
            val severityOrder = mapOf(
                ca.taplog.app.data.DeficiencySeverity.CRITICAL to 0,
                ca.taplog.app.data.DeficiencySeverity.HIGH to 1,
                ca.taplog.app.data.DeficiencySeverity.MEDIUM to 3,
                ca.taplog.app.data.DeficiencySeverity.LOW to 4
            )
            (deficiencyTasks + overdueTasks).sortedWith(
                compareBy { task ->
                    when (task.type) {
                        TaskType.OPEN_DEFICIENCY -> severityOrder[task.severity] ?: 5
                        TaskType.OVERDUE_ASSET -> 2
                    }
                }
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Contacts ---

    val allContacts: StateFlow<List<SiteContact>> =
        sitesForOrg
            .map { sites ->
                val seen = mutableSetOf<String>()
                val result = mutableListOf<SiteContact>()
                sites.forEach { site ->
                    if (!site.clientName.isNullOrBlank()) {
                        val key = "${site.clientName}|${site.clientPhone}"
                        if (seen.add(key)) {
                            result.add(SiteContact(site.clientName, site.clientPhone, "Client", site.name, site.id))
                        }
                    }
                    if (!site.contactName.isNullOrBlank() && site.contactName != site.clientName) {
                        val key = "${site.contactName}|${site.contactPhone}"
                        if (seen.add(key)) {
                            result.add(SiteContact(site.contactName, site.contactPhone, "Contact", site.name, site.id))
                        }
                    }
                }
                result.sortedBy { it.name }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun resolveDeficiency(deficiencyId: String) {
        viewModelScope.launch {
            repository.resolveDeficiency(deficiencyId)
        }
    }

    // --- NFC tag scanned ---

    fun onNfcTagScanned(tagId: String) {
        if (_currentSite.value == null) {
            _scanState.value = ScanState.Error("Select a site before scanning")
            return
        }

        _lastScannedTagId.value = tagId

        val currentState = _scanState.value
        if (currentState is ScanState.AwaitingReplacementTag) {
            viewModelScope.launch {
                val existing = repository.getAssetByTagId(tagId)
                if (existing != null && existing.id != currentState.asset.id) {
                    _scanState.value = ScanState.Error(
                        "Tag is already registered to \"${existing.name}\" — scan a different tag"
                    )
                    return@launch
                }
                val inspectorId = inspectorPreferences.inspectorId.first()
                repository.replaceTag(currentState.asset, tagId, currentState.reason, inspectorId)
                _scanState.value = ScanState.AssetFound(currentState.asset.copy(nfcTagId = tagId))
            }
            return
        }

        _scanState.value = ScanState.Scanning
        viewModelScope.launch {
            val asset = repository.getAssetByTagId(tagId)
            if (asset != null) {
                insertScanEvent(asset, ScanEventType.BROWSE)
            }
            _assetDetailSource.value = AssetDetailSource.FROM_SCAN
            _scanState.value = if (asset != null) {
                ScanState.AssetFound(asset)
            } else {
                ScanState.UnregisteredTag(tagId)
            }
        }
    }

    // --- Save asset ---

    fun saveAsset(
        nfcTagId: String,
        name: String,
        assetTypeCode: String,
        location: String,
        installDateMillis: Long,
        inspectionIntervalMonths: Int
    ) {
        viewModelScope.launch {
            val site = _currentSite.value ?: return@launch
            val intervalMillis = inspectionIntervalMonths * 30L * 24 * 60 * 60 * 1000
            val asset = Asset(
                nfcTagId = nfcTagId,
                siteId = site.id,
                name = name,
                assetType = assetTypeCode,
                location = location,
                installDate = installDateMillis,
                nextInspectionDue = installDateMillis + intervalMillis
            )
            repository.saveAsset(asset)
            repository.insertTagEvent(
                TagEvent(
                    assetId = asset.id,
                    tagId = nfcTagId,
                    attachedAt = System.currentTimeMillis()
                )
            )
            _assetDetailSource.value = AssetDetailSource.FROM_SCAN
            _scanState.value = ScanState.AssetFound(asset)
        }
    }

    // --- Save inspection ---

    fun saveInspection(
        asset: Asset,
        result: InspectionResult,
        notes: String?,
        latitude: Double?,
        longitude: Double?,
        deficiencies: List<Deficiency> = emptyList()
    ) {
        viewModelScope.launch {
            val claims = inspectorClaims.value
            val inspection = Inspection(
                assetId = asset.id,
                inspectorName = claims?.name ?: "",
                inspectorCertNumber = claims?.certNumber ?: "",
                result = result,
                notes = notes,
                latitude = latitude,
                longitude = longitude
            )
            repository.saveInspection(inspection)

            if (deficiencies.isNotEmpty()) {
                deficiencies.map { it.copy(inspectionId = inspection.id) }
                    .forEach { repository.saveDeficiency(it) }
            }

            insertScanEvent(asset, ScanEventType.INSPECTION)

            val ofcType = OFCCategory.findByCode(asset.assetType)
            val intervalMonths = ofcType?.inspectionIntervalMonths?.toLong() ?: 12L
            val nextDue = inspection.inspectedAt + (intervalMonths * 30 * 24 * 60 * 60 * 1000)
            repository.updateAssetInspectionDates(asset.id, inspection.inspectedAt, nextDue)

            _scanState.value = ScanState.AssetFound(
                asset.copy(
                    lastInspectedAt = inspection.inspectedAt,
                    nextInspectionDue = nextDue
                )
            )
        }
    }

    // --- Scan event helpers ---

    private suspend fun insertScanEvent(asset: Asset, eventType: ScanEventType) {
        val inspectorId = inspectorPreferences.inspectorId.first()
        val name = inspectorClaims.value?.name ?: ""
        repository.insertScanEvent(
            ScanEvent(
                tagId = asset.nfcTagId,
                assetId = asset.id,
                inspectorId = inspectorId,
                inspectorName = name,
                eventType = eventType
            )
        )
    }

    fun logScanEvent(asset: Asset, eventType: ScanEventType) {
        viewModelScope.launch { insertScanEvent(asset, eventType) }
    }

    // --- Scan history ---

    private val _currentAssetScanEvents = MutableStateFlow<List<ScanEvent>>(emptyList())
    val scanEventsForCurrentAsset: StateFlow<List<ScanEvent>> = _currentAssetScanEvents.asStateFlow()

    fun loadScanEventsForAsset(assetId: String) {
        viewModelScope.launch {
            repository.getScanEventsForAsset(assetId).collect { events ->
                _currentAssetScanEvents.value = events
            }
        }
    }

    // --- Tag event history ---

    private val _currentAssetTagEvents = MutableStateFlow<List<TagEvent>>(emptyList())
    val tagEventsForCurrentAsset: StateFlow<List<TagEvent>> = _currentAssetTagEvents.asStateFlow()

    fun loadTagEventsForAsset(assetId: String) {
        viewModelScope.launch {
            repository.getTagEventsForAsset(assetId).collect { events ->
                _currentAssetTagEvents.value = events
            }
        }
    }

    private val _birthingTagEvent = MutableStateFlow<TagEvent?>(null)
    val birthingTagEvent: StateFlow<TagEvent?> = _birthingTagEvent.asStateFlow()

    fun loadBirthingTagEvent(assetId: String) {
        viewModelScope.launch {
            _birthingTagEvent.value = repository.getFirstTagEventForAsset(assetId)
        }
    }

    // --- Service requests ---

    private val _serviceRequestsForCurrentAsset = MutableStateFlow<List<ServiceRequest>>(emptyList())
    val serviceRequestsForCurrentAsset: StateFlow<List<ServiceRequest>> = _serviceRequestsForCurrentAsset.asStateFlow()

    fun loadServiceRequestsForAsset(assetId: String) {
        viewModelScope.launch {
            repository.getServiceRequestsForAsset(assetId).collect { requests ->
                _serviceRequestsForCurrentAsset.value = requests
            }
        }
    }

    fun sendServiceRequest(
        asset: Asset,
        contractorName: String?,
        contractorPhone: String?,
        contractorEmail: String?,
        notes: String?
    ) {
        viewModelScope.launch {
            val inspectorId = inspectorPreferences.inspectorId.first() ?: "local"
            val role = inspectorPreferences.userRole.first()
            repository.insertServiceRequest(
                ServiceRequest(
                    assetId = asset.id,
                    siteId = asset.siteId,
                    requestedById = inspectorId,
                    requestedByRole = role,
                    contractorName = contractorName?.takeIf { it.isNotBlank() },
                    contractorPhone = contractorPhone?.takeIf { it.isNotBlank() },
                    contractorEmail = contractorEmail?.takeIf { it.isNotBlank() },
                    notes = notes?.takeIf { it.isNotBlank() },
                    sentAtMs = System.currentTimeMillis()
                )
            )
            _scanState.value = ScanState.AssetFound(asset)
        }
    }

    fun showAssetNotFound() {
        _scanState.value = ScanState.AssetNotFound
    }

    fun startManualAssetRegistration() {
        _lastScannedTagId.value = java.util.UUID.randomUUID().toString()
        _scanState.value = ScanState.AssetNotFound
    }

    fun showServiceRequestForm(asset: Asset) {
        _scanState.value = ScanState.ServiceRequestForm(asset)
    }

    fun showServiceRequestTrail(asset: Asset) {
        _scanState.value = ScanState.ServiceRequestTrail(asset)
    }

    fun markExpiredServiceRequestsNoResponse() {
        viewModelScope.launch { repository.markExpiredRequestsNoResponse() }
    }

    // --- AI asset identification ---

    private val _identificationLoading = MutableStateFlow(false)
    val identificationLoading: StateFlow<Boolean> = _identificationLoading.asStateFlow()

    private val _suggestedAssetCode = MutableStateFlow<String?>(null)
    val suggestedAssetCode: StateFlow<String?> = _suggestedAssetCode.asStateFlow()

    fun identifyAsset(imagePath: String) {
        val ai = aiRepository ?: return
        viewModelScope.launch {
            _identificationLoading.value = true
            try {
                val vertical = runCatching {
                    VerticalRegistry.get(TapLogVertical.EMBER)
                }.getOrNull() ?: return@launch
                val result = ai.identifyAsset(imagePath, vertical)
                if (result != null && (result.confidence == "HIGH" || result.confidence == "MEDIUM") && result.code != null) {
                    _suggestedAssetCode.value = result.code
                }
            } finally {
                _identificationLoading.value = false
            }
        }
    }

    fun clearSuggestedAssetCode() {
        _suggestedAssetCode.value = null
    }

    // --- Tag replacement ---

    fun startReplacementFlow(asset: Asset, reason: RetireReason) {
        _scanState.value = ScanState.AwaitingReplacementTag(asset, reason)
    }

    fun cancelReplacement() {
        val state = _scanState.value
        if (state is ScanState.AwaitingReplacementTag) {
            _scanState.value = ScanState.AssetFound(state.asset)
        }
    }

    // --- Inspection history ---

    private val _currentAssetInspections = MutableStateFlow<List<Inspection>>(emptyList())
    val currentAssetInspections: StateFlow<List<Inspection>> = _currentAssetInspections.asStateFlow()

    fun loadInspectionsForAsset(assetId: String) {
        viewModelScope.launch {
            repository.getInspectionsForAsset(assetId).collect { inspections ->
                _currentAssetInspections.value = inspections
            }
        }
    }

    // --- PDF report sharing ---

    private val _shareReportEvent = MutableSharedFlow<Intent>(extraBufferCapacity = 1)
    val shareReportEvent: SharedFlow<Intent> = _shareReportEvent.asSharedFlow()

    fun shareReport(inspection: Inspection, asset: Asset) {
        val repo = reportRepository ?: return
        viewModelScope.launch {
            val site = repository.getSiteById(asset.siteId) ?: return@launch
            val org = repository.getOrganisation() ?: return@launch
            val deficiencies = repository.getDeficienciesForInspection(inspection.id)
            val uri = repo.generateAndGetUri(inspection, asset, site, org, deficiencies)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            _shareReportEvent.emit(intent)
        }
    }

    // --- Factory ---

    class Factory(
        private val repository: EmberRepository,
        private val inspectorPreferences: InspectorPreferences,
        private val reportRepository: ca.taplog.app.data.ReportRepository? = null,
        private val geocodingRepository: GeocodingRepository? = null,
        private val aiRepository: AiRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EmberViewModel(repository, inspectorPreferences, reportRepository, geocodingRepository, aiRepository) as T
        }
    }
}
