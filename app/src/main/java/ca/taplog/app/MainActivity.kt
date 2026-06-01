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
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import ca.taplog.app.ui.ember.EmberViewModel
import ca.taplog.app.ui.ember.EntryEventScreen
import ca.taplog.app.ui.ember.InspectionFormScreen
import ca.taplog.app.ui.ember.OpenDeficienciesScreen
import ca.taplog.app.ui.ember.OrganisationSetupScreen
import ca.taplog.app.ui.ember.SiteDetailScreen
import ca.taplog.app.ui.ember.SiteListScreen
import ca.taplog.app.ui.ember.SiteRegistrationScreen
import ca.taplog.app.ui.ember.SplashScreen
import ca.taplog.app.ui.theme.TapLogTheme

class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null

    private val viewModel: EmberViewModel by viewModels {
        val app = application as TapLogApplication
        EmberViewModel.Factory(app.repository, app.inspectorPreferences, app.reportRepository)
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
                    var showSplash by remember { mutableStateOf(true) }
                    var isAuthenticated by remember { mutableStateOf<Boolean?>(null) }
                    val registryReady by app.verticalRegistryReady.collectAsState()

                    LaunchedEffect(Unit) {
                        val token = app.inspectorPreferences.getAuthToken()
                        isAuthenticated = !token.isNullOrBlank()
                    }

                    if (showSplash) {
                        SplashScreen(
                            registryReady = registryReady,
                            onSplashComplete = { showSplash = false }
                        )
                    } else when (isAuthenticated) {
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
                            EmberScanScreen(viewModel)
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
            // Handled by LaunchedEffect above — show nothing while transitioning
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

    when (val state = scanState) {

        is EmberViewModel.ScanState.OrganisationSetup -> {
            OrganisationSetupScreen(
                onSave = { name, city, province ->
                    viewModel.saveOrganisation(name, city, province)
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
                onSiteSelected = { viewModel.selectSite(it) },
                onAddSite = { viewModel.showAddSite() },
                onShowDeficiencies = { viewModel.showOpenDeficiencies() }
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
                onStartScanning = { viewModel.resetScanState() },
                onAddSite = { viewModel.showAddSite() },
                onBack = { viewModel.showSiteList() }
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
            }
            AssetDetailScreen(
                asset = state.asset,
                inspections = inspections,
                scanEvents = scanEvents,
                siteName = currentSite?.name ?: "",
                source = assetDetailSource,
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
            AssetRegistrationScreen(
                nfcTagId = tagId ?: "",
                siteName = currentSite?.name ?: "",
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
    }
}
