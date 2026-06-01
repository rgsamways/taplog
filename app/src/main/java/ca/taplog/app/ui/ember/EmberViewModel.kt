package ca.taplog.app.ui.ember

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.taplog.app.data.Asset
import ca.taplog.app.data.AssetDetailSource
import ca.taplog.app.data.Deficiency
import ca.taplog.app.data.EmberRepository
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
import ca.taplog.app.data.TagEvent
import android.content.Intent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmberViewModel(
    private val repository: EmberRepository,
    private val inspectorPreferences: InspectorPreferences,
    private val reportRepository: ca.taplog.app.data.ReportRepository? = null
) : ViewModel() {

    // --- Scan state ---

    sealed class ScanState {
        object OrganisationSetup : ScanState()
        object SiteList : ScanState()
        data class SiteSelected(val site: Site) : ScanState()
        object AssetList : ScanState()
        object OpenDeficiencies : ScanState()
        object Idle : ScanState()
        object Scanning : ScanState()
        data class AssetFound(val asset: Asset) : ScanState()
        data class Inspecting(val asset: Asset) : ScanState()
        data class AwaitingReplacementTag(val asset: Asset, val reason: RetireReason) : ScanState()
        object AssetNotFound : ScanState()
        data class Error(val message: String) : ScanState()
    }

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

    // --- Organisation ---

    private val _organisation = MutableStateFlow<Organisation?>(null)
    val organisation: StateFlow<Organisation?> = _organisation.asStateFlow()

    fun loadOrganisation() {
        viewModelScope.launch {
            val org = repository.getOrganisation()
            _organisation.value = org
            _scanState.value = if (org == null) {
                ScanState.OrganisationSetup
            } else {
                ScanState.SiteList
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
            _scanState.value = ScanState.SiteList
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
        }
    }

    fun selectSite(site: Site) {
        _currentSite.value = site
        viewModelScope.launch {
            repository.getAssetsForSite(site.id).collect { assets ->
                _siteAssets.value = assets
            }
        }
        _scanState.value = ScanState.SiteSelected(site)
    }

    fun showSiteList() {
        _currentSite.value = null
        _scanState.value = ScanState.SiteList
    }

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
        } ?: ScanState.SiteList
    }

    // --- Asset data ---

    val activeAssets = repository.getAllAssets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val openDeficienciesWithAsset = repository.getOpenDeficienciesWithAsset()
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
                ScanState.AssetNotFound
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

            _scanState.value = ScanState.AssetFound(asset)
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
        private val reportRepository: ca.taplog.app.data.ReportRepository? = null
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return EmberViewModel(repository, inspectorPreferences, reportRepository) as T
        }
    }
}
