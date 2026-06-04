package ca.taplog.app

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.first
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.IntentCompat
import ca.taplog.app.data.AssetDetailSource
import ca.taplog.app.ui.auth.AuthViewModel
import ca.taplog.app.ui.auth.EmailVerificationScreen
import ca.taplog.app.ui.auth.LoginScreen
import ca.taplog.app.ui.auth.NewDeviceScreen
import ca.taplog.app.ui.auth.RegistrationScreen
import android.content.Intent
import ca.taplog.app.data.RoleModel
import ca.taplog.app.data.VerticalRegistry
import ca.taplog.app.ui.ember.AssetDetailScreen
import ca.taplog.app.ui.ember.AssetListScreen
import ca.taplog.app.ui.ember.AssetRegistrationScreen
import ca.taplog.app.ui.ember.CalendarScreen
import ca.taplog.app.ui.ember.ContactsScreen
import ca.taplog.app.data.UserRole
import ca.taplog.app.ui.ember.DashboardScreen
import ca.taplog.app.ui.ember.ServiceRequestScreen
import ca.taplog.app.ui.ember.ServiceRequestTrailScreen
import ca.taplog.app.ui.ember.FieldAnalystDashboardScreen
import ca.taplog.app.ui.ember.FieldAnalystScanScreen
import ca.taplog.app.ui.ember.QuickRegisterSheet
import ca.taplog.app.ui.ember.RoleSelectionScreen
import ca.taplog.app.ui.ember.TasksScreen
import ca.taplog.app.ui.ember.EmberViewModel
import ca.taplog.app.ui.ember.EntryEventScreen
import ca.taplog.app.ui.ember.InspectionFormScreen
import ca.taplog.app.ui.ember.OpenDeficienciesScreen
import ca.taplog.app.ui.ember.OrganisationSetupScreen
import ca.taplog.app.ui.ember.SiteDetailScreen
import ca.taplog.app.ui.ember.SiteListScreen
import ca.taplog.app.ui.ember.SiteRegistrationScreen
import ca.taplog.app.ui.ember.SplashScreen
import ca.taplog.app.ui.ember.UnregisteredTagScreen
import ca.taplog.app.ui.ember.VisitSetupScreen
import ca.taplog.app.ui.theme.TapLogTheme

// Sealed class to drive the top-level screen slot
private sealed class AppScreen {
    object Splash : AppScreen()
    object App    : AppScreen()
}

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    private val viewModel: EmberViewModel by viewModels {
        val app = application as TapLogApplication
        EmberViewModel.Factory(app.repository, app.inspectorPreferences, app.reportRepository, app.geocodingRepository, app.aiRepository)
    }

    private val authViewModel: AuthViewModel by viewModels {
        (application as TapLogApplication).authViewModelFactory
    }

    private val nfcReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("NFC_DEBUG", "BroadcastReceiver fired — action: ${intent.action}")
            handleNfcIntent(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        setContent {
            TapLogTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val app = application as TapLogApplication
                    var screen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }
                    var isAuthenticated by remember { mutableStateOf<Boolean?>(null) }
                    val registryReady by app.verticalRegistryReady.collectAsState()
                    val userRole by viewModel.userRole.collectAsState()
                    // null = not yet checked, true = role is set, false = needs selection
                    var userRoleChecked by remember { mutableStateOf(false) }
                    var needsRoleSelection by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val token = app.inspectorPreferences.getAuthToken()
                        isAuthenticated = !token.isNullOrBlank()
                        val hasStoredRole = app.inspectorPreferences.isUserRoleSet.first()
                        needsRoleSelection = !hasStoredRole
                        userRoleChecked = true
                    }

                    // Crossfade between Splash and App — 400ms fade, no flash
                    Crossfade(
                        targetState = screen,
                        animationSpec = tween(durationMillis = 400),
                        label = "splash_to_app"
                    ) { currentScreen ->
                        when (currentScreen) {
                            is AppScreen.Splash -> {
                                SplashScreen(
                                    registryReady = registryReady,
                                    onSplashComplete = { screen = AppScreen.App }
                                )
                            }
                            is AppScreen.App -> {
                                when (isAuthenticated) {
                                    null -> Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator() }

                                    false -> AuthFlow(
                                        authViewModel = authViewModel,
                                        onAuthenticated = {
                                            isAuthenticated = true
                                            viewModel.loadOrganisation()
                                        }
                                    )

                                    true -> {
                                        LaunchedEffect(Unit) { viewModel.loadOrganisation() }
                                        if (userRoleChecked && needsRoleSelection) {
                                            RoleSelectionScreen(
                                                onSelectInspector = {
                                                    viewModel.setUserRole(UserRole.INSPECTOR)
                                                    needsRoleSelection = false
                                                    viewModel.loadOrganisation(forceRole = UserRole.INSPECTOR)
                                                },
                                                onSelectFieldAnalyst = {
                                                    viewModel.setUserRole(UserRole.FIELD_ANALYST)
                                                    needsRoleSelection = false
                                                    viewModel.loadOrganisation(forceRole = UserRole.FIELD_ANALYST)
                                                }
                                            )
                                        } else {
                                            EmberScanScreen(viewModel)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val filter = IntentFilter().apply {
            addAction(NfcAdapter.ACTION_NDEF_DISCOVERED)
            addAction(NfcAdapter.ACTION_TECH_DISCOVERED)
            addAction(NfcAdapter.ACTION_TAG_DISCOVERED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(nfcReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(nfcReceiver, filter)
        }

        val intent = Intent(NfcAdapter.ACTION_TAG_DISCOVERED).apply {
            setPackage(packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
        try { unregisterReceiver(nfcReceiver) } catch (_: Exception) { }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d("NFC_DEBUG", "onNewIntent fired — action: ${intent.action}")
        handleNfcIntent(intent)
    }

    private fun handleNfcIntent(intent: Intent) {
        val tag = IntentCompat.getParcelableExtra(intent, NfcAdapter.EXTRA_TAG, android.nfc.Tag::class.java)
        tag?.id?.let { bytes ->
            val tagId = bytes.joinToString("") { "%02x".format(it) }
            Log.d("NFC_DEBUG", "Tag ID extracted: $tagId")
            viewModel.onNfcTagScanned(tagId)
        }
    }
}

@Composable
fun AuthFlow(authViewModel: AuthViewModel, onAuthenticated: () -> Unit) {
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Authenticated) {
            onAuthenticated()
        }
    }

    when (val state = authState) {
        is AuthViewModel.AuthState.Login,
        is AuthViewModel.AuthState.LoggingIn -> {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { authViewModel.showRegistration() }
            )
        }

        is AuthViewModel.AuthState.Registering -> {
            RegistrationScreen(viewModel = authViewModel)
        }

        is AuthViewModel.AuthState.VerifyingEmail -> {
            EmailVerificationScreen(
                email = state.email,
                viewModel = authViewModel
            )
        }

        is AuthViewModel.AuthState.NewDevice -> {
            NewDeviceScreen(
                email = state.email,
                viewModel = authViewModel
            )
        }

        is AuthViewModel.AuthState.Authenticated -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun EmberScanScreen(viewModel: EmberViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.shareReportEvent.collect { intent ->
            context.startActivity(Intent.createChooser(intent, "Share Report"))
        }
    }

    val scanState by viewModel.scanState.collectAsState()
    val tagId by viewModel.lastScannedTagId.collectAsState()
    val inspectorClaims by viewModel.inspectorClaims.collectAsState()
    val activeAssets by viewModel.activeAssets.collectAsState()
    val deficiencies by viewModel.openDeficienciesWithAsset.collectAsState()
    val inspections by viewModel.currentAssetInspections.collectAsState()
    val scanEvents by viewModel.scanEventsForCurrentAsset.collectAsState()
    val assetDetailSource by viewModel.assetDetailSource.collectAsState()
    val organisation by viewModel.organisation.collectAsState()
    val currentSite by viewModel.currentSite.collectAsState()
    val siteAssets by viewModel.siteAssets.collectAsState()
    val calendarEvents by viewModel.calendarEvents.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val allContacts by viewModel.allContacts.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val birthingTagEvent by viewModel.birthingTagEvent.collectAsState()
    val serviceRequests by viewModel.serviceRequestsForCurrentAsset.collectAsState()
    val activeVisitSiteId by viewModel.activeVisitSiteId.collectAsState()
    val fieldAnalystAssetCount by viewModel.fieldAnalystAssetCount.collectAsState()

    // QuickRegisterSheet state — shown as overlay on top of FieldAnalystScanScreen
    var showQuickRegister by remember { mutableStateOf(false) }
    var quickRegisterTagId by remember { mutableStateOf("") }
    var quickRegisterIsManual by remember { mutableStateOf(false) }

    when (val state = scanState) {

        is EmberViewModel.ScanState.OrganisationSetup -> {
            OrganisationSetupScreen(
                onSave = { name, city, province ->
                    viewModel.saveOrganisation(name, city, province)
                }
            )
        }

        is EmberViewModel.ScanState.Dashboard -> {
            val dashboardStats by viewModel.dashboardStats.collectAsState()
            val overdueSites by viewModel.overdueSites.collectAsState()
            val orgId = organisation?.id
            val sitesFlow = if (orgId != null) viewModel.getSitesForOrg() else null

            DashboardScreen(
                organisationName = organisation?.name ?: "",
                dashboardStats = dashboardStats,
                overdueSites = overdueSites,
                sitesFlow = sitesFlow,
                calendarEvents = calendarEvents,
                tasks = tasks,
                contacts = allContacts,
                onScan = { viewModel.showSiteList() },
                onAddSite = { viewModel.showAddSite() },
                onShowDeficiencies = { viewModel.showOpenDeficiencies() },
                onSiteSelected = { viewModel.selectSite(it) },
                onOverdueSiteTapped = { viewModel.selectSite(it) },
                onShowAllSites = { viewModel.showSiteList() },
                onGeocodeUnresolved = { viewModel.geocodeUnresolvedSites(it) },
                onShowCalendar = { viewModel.showCalendar() },
                onShowTasks = { viewModel.showTasks() },
                onShowContacts = { viewModel.showContacts() },
                onAssetSelected = { assetId ->
                    val asset = activeAssets.find { it.id == assetId }
                    if (asset != null) viewModel.selectAsset(asset)
                }
            )
        }

        is EmberViewModel.ScanState.SiteList -> {
            val orgId = organisation?.id
            val sitesFlow = if (orgId != null) viewModel.getSitesForOrg() else null

            SiteListScreen(
                sitesFlow = sitesFlow,
                organisationName = organisation?.name ?: "",
                deficiencyCount = deficiencies.size,
                onSiteSelected = { viewModel.selectSite(it, fromSiteList = true) },
                onAddSite = { viewModel.showAddSite() },
                onShowDeficiencies = { viewModel.showOpenDeficiencies() },
                onGeocodeUnresolved = { viewModel.geocodeUnresolvedSites(it) },
                onBack = { viewModel.showDashboard() }
            )
        }

        is EmberViewModel.ScanState.SiteSelected -> {
            LaunchedEffect(state.site.id) {
                viewModel.selectSite(state.site)
            }
            SiteDetailScreen(
                site = state.site,
                assets = siteAssets,
                onAssetSelected = { viewModel.selectAsset(it) },
                onStartScanning = { viewModel.startManualAssetRegistration() },
                onAddSite = { viewModel.showAddSite() },
                onBack = {
                    if (state.fromSiteList) viewModel.showSiteList()
                    else viewModel.showDashboard()
                }
            )
        }

        is EmberViewModel.ScanState.Idle -> {
            SiteRegistrationScreen(
                onSave = { name, address, city, province, postalCode,
                           clientName, clientPhone, contactName, contactPhone, notes ->
                    viewModel.saveSite(
                        name, address, city, province, postalCode,
                        clientName, clientPhone, contactName, contactPhone, notes
                    )
                },
                onCancel = { viewModel.showSiteList() }
            )
        }

        is EmberViewModel.ScanState.Scanning -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Scanning...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        is EmberViewModel.ScanState.AssetFound -> {
            LaunchedEffect(state.asset.id) {
                viewModel.loadInspectionsForAsset(state.asset.id)
                viewModel.loadScanEventsForAsset(state.asset.id)
                viewModel.loadBirthingTagEvent(state.asset.id)
                viewModel.loadServiceRequestsForAsset(state.asset.id)
            }
            AssetDetailScreen(
                asset = state.asset,
                inspections = inspections,
                scanEvents = scanEvents,
                birthingTagEvent = birthingTagEvent,
                serviceRequests = serviceRequests,
                siteName = currentSite?.name ?: "",
                source = assetDetailSource,
                onRequestInspection = { viewModel.showServiceRequestForm(state.asset) },
                onViewServiceRequestTrail = { viewModel.showServiceRequestTrail(state.asset) },
                onStartInspection = { viewModel.startInspection(state.asset) },
                onStartReplacement = { reason -> viewModel.startReplacementFlow(state.asset, reason) },
                onShareReport = { inspection -> viewModel.shareReport(inspection, state.asset) },
                onBack = {
                    if (assetDetailSource == AssetDetailSource.FROM_LIST) {
                        currentSite?.let { viewModel.selectSite(it) } ?: viewModel.showSiteList()
                    } else {
                        viewModel.resetScanState()
                    }
                }
            )
        }

        is EmberViewModel.ScanState.AwaitingReplacementTag -> {
            BackHandler { viewModel.cancelReplacement() }
            Scaffold { padding ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text("Tap the new NFC tag", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Replacing tag on \"${state.asset.name}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Reason: ${state.reason.name.lowercase().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { viewModel.cancelReplacement() }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }

        is EmberViewModel.ScanState.AssetNotFound -> {
            // Legacy fallback — should not be reached after migration to UnregisteredTag
            AssetRegistrationScreen(
                nfcTagId = tagId ?: "",
                siteName = currentSite?.name ?: "",
                viewModel = viewModel,
                onSave = { name, assetTypeCode, location, installDateMillis, intervalMonths ->
                    viewModel.saveAsset(
                        nfcTagId = tagId ?: "",
                        name = name,
                        assetTypeCode = assetTypeCode,
                        location = location,
                        installDateMillis = installDateMillis,
                        inspectionIntervalMonths = intervalMonths
                    )
                },
                onCancel = { viewModel.resetScanState() }
            )
        }

        is EmberViewModel.ScanState.UnregisteredTag -> {
            UnregisteredTagScreen(
                tagId = state.tagId,
                userRole = userRole,
                onRegisterAsFieldAnalyst = { tid ->
                    quickRegisterTagId = tid
                    quickRegisterIsManual = false
                    showQuickRegister = true
                },
                onRegisterAsInspector = { viewModel.showAssetNotFound() },
                onDismiss = { viewModel.resetScanState() }
            )
            if (showQuickRegister && activeVisitSiteId != null) {
                QuickRegisterSheet(
                    tagId = quickRegisterTagId,
                    siteId = activeVisitSiteId!!,
                    isManual = quickRegisterIsManual,
                    viewModel = viewModel,
                    onRegister = { tid, assetType, name, location, siteId ->
                        viewModel.registerFieldAnalystAsset(tid, assetType, name, location, siteId)
                        showQuickRegister = false
                        viewModel.showFieldAnalystDashboard()
                    },
                    onDismiss = { showQuickRegister = false }
                )
            }
        }

        is EmberViewModel.ScanState.FieldAnalystDashboard -> {
            val orgId = organisation?.id
            val sitesFlow = if (orgId != null) viewModel.getSitesForOrg() else null
            val sites by (sitesFlow?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList<ca.taplog.app.data.Site>()) })
            val activeSite = sites.find { it.id == activeVisitSiteId }

            FieldAnalystDashboardScreen(
                activeVisitSite = activeSite,
                activeVisitAssetCount = fieldAnalystAssetCount,
                recentSites = sites.take(5),
                totalAssetsTagged = activeAssets.size,
                onStartNewVisit = { viewModel.showVisitSetup() },
                onResumeVisit = { viewModel.showFieldAnalystScanning() },
                onSiteSelected = { site -> viewModel.startVisit(site) }
            )
        }

        is EmberViewModel.ScanState.VisitSetup -> {
            VisitSetupScreen(
                onBeginVisit = { name, address, city, postalCode, type, ownerName, ownerPhone, notes ->
                    viewModel.saveSiteForVisit(
                        name = name, address = address, city = city,
                        postalCode = postalCode, buildingType = type,
                        ownerName = ownerName, ownerPhone = ownerPhone, notes = notes
                    )
                },
                onCancel = { viewModel.showFieldAnalystDashboard() }
            )
        }

        is EmberViewModel.ScanState.FieldAnalystScanning -> {
            val activeSiteId = activeVisitSiteId
            val assetsThisVisit = if (activeSiteId != null) activeAssets.filter { it.siteId == activeSiteId } else emptyList()
            var inlineAsset by remember { mutableStateOf<ca.taplog.app.data.Asset?>(null) }

            FieldAnalystScanScreen(
                siteName = currentSite?.name ?: "",
                assetCount = fieldAnalystAssetCount,
                assetsThisVisit = assetsThisVisit,
                inlineAsset = inlineAsset,
                onFinishVisit = { viewModel.endVisit() },
                onAddManually = {
                    quickRegisterTagId = java.util.UUID.randomUUID().toString()
                    quickRegisterIsManual = true
                    showQuickRegister = true
                },
                onDismissInlineAsset = { inlineAsset = null }
            )

            if (showQuickRegister && activeSiteId != null) {
                QuickRegisterSheet(
                    tagId = quickRegisterTagId,
                    siteId = activeSiteId,
                    isManual = quickRegisterIsManual,
                    viewModel = viewModel,
                    onRegister = { tid, assetType, name, location, siteId ->
                        viewModel.registerFieldAnalystAsset(tid, assetType, name, location, siteId)
                        showQuickRegister = false
                    },
                    onDismiss = { showQuickRegister = false }
                )
            }
        }

        is EmberViewModel.ScanState.Inspecting -> {
            val roleModel = VerticalRegistry.get(state.asset.vertical).roleModel
            if (roleModel == RoleModel.MULTI_ROLE) {
                EntryEventScreen(
                    assetName = state.asset.name,
                    onBack = { viewModel.selectAsset(state.asset) }
                )
            } else {
                InspectionFormScreen(
                    asset = state.asset,
                    siteName = currentSite?.name ?: "",
                    inspectorClaims = inspectorClaims,
                    onSubmit = { result, notes, defs ->
                        viewModel.saveInspection(state.asset, result, notes, null, null, defs)
                    },
                    onCancel = { viewModel.selectAsset(state.asset) }
                )
            }
        }

        is EmberViewModel.ScanState.AssetList -> {
            AssetListScreen(
                assets = activeAssets,
                onAssetSelected = { viewModel.selectAsset(it) },
                onBack = { viewModel.resetScanState() }
            )
        }

        is EmberViewModel.ScanState.OpenDeficiencies -> {
            OpenDeficienciesScreen(
                deficiencies = deficiencies,
                onResolve = { viewModel.resolveDeficiency(it) },
                onBack = { viewModel.resetScanState() }
            )
        }

        is EmberViewModel.ScanState.Calendar -> {
            CalendarScreen(
                calendarEvents = calendarEvents,
                onAssetSelected = { assetId ->
                    val asset = activeAssets.find { it.id == assetId }
                    if (asset != null) viewModel.selectAsset(asset)
                },
                onBack = { viewModel.showDashboard() }
            )
        }

        is EmberViewModel.ScanState.Tasks -> {
            TasksScreen(
                tasks = tasks,
                onAssetSelected = { assetId ->
                    val asset = activeAssets.find { it.id == assetId }
                    if (asset != null) viewModel.selectAsset(asset)
                },
                onBack = { viewModel.showDashboard() }
            )
        }

        is EmberViewModel.ScanState.Contacts -> {
            ContactsScreen(
                contacts = allContacts,
                onSiteSelected = { siteId -> viewModel.selectSiteById(siteId) },
                onBack = { viewModel.showDashboard() }
            )
        }

        is EmberViewModel.ScanState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                    OutlinedButton(onClick = { viewModel.resetScanState() }) {
                        Text("Go Back")
                    }
                }
            }
        }

        is EmberViewModel.ScanState.ServiceRequestForm -> {
            ServiceRequestScreen(
                asset = state.asset,
                onSend = { name, phone, email, notes ->
                    viewModel.sendServiceRequest(state.asset, name, phone, email, notes)
                },
                onCancel = { viewModel.selectAsset(state.asset) }
            )
        }

        is EmberViewModel.ScanState.ServiceRequestTrail -> {
            LaunchedEffect(state.asset.id) {
                viewModel.loadServiceRequestsForAsset(state.asset.id)
            }
            ServiceRequestTrailScreen(
                asset = state.asset,
                serviceRequests = serviceRequests,
                onBack = { viewModel.selectAsset(state.asset) }
            )
        }
    }
}
