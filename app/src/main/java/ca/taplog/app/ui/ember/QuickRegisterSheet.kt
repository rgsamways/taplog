package ca.taplog.app.ui.ember

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ca.taplog.app.data.TapLogVertical
import ca.taplog.app.data.VerticalRegistry
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickRegisterSheet(
    tagId: String,
    siteId: String,
    isManual: Boolean = false,
    viewModel: EmberViewModel? = null,
    onRegister: (tagId: String, assetType: String, name: String, location: String, siteId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val assetTypes = remember {
        runCatching {
            VerticalRegistry.get(TapLogVertical.EMBER).assetTypeRegistry
        }.getOrDefault(emptyList())
    }

    var selectedType by remember { mutableStateOf(assetTypes.firstOrNull()) }
    var assetName by remember { mutableStateOf(selectedType?.label ?: "") }
    var location by remember { mutableStateOf("") }
    var typeDropdownExpanded by remember { mutableStateOf(false) }

    val identificationLoading by (viewModel?.identificationLoading?.collectAsState() ?: remember { mutableStateOf(false) })
    val suggestedCode by (viewModel?.suggestedAssetCode?.collectAsState() ?: remember { mutableStateOf<String?>(null) })

    val tempPhotoFile = remember { File(context.cacheDir, "ai_identify_qs_${System.currentTimeMillis()}.jpg") }
    val tempPhotoUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", tempPhotoFile)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && tempPhotoFile.exists()) {
            viewModel?.identifyAsset(tempPhotoFile.absolutePath)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) cameraLauncher.launch(tempPhotoUri)
    }
    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(tempPhotoUri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Pre-select type when AI suggestion arrives
    LaunchedEffect(suggestedCode) {
        if (suggestedCode != null) {
            val match = assetTypes.find { it.code == suggestedCode }
            if (match != null) {
                selectedType = match
                assetName = match.label
            }
        }
    }

    // Update name when type changes
    LaunchedEffect(selectedType) {
        if (selectedType != null && assetName == (assetTypes.firstOrNull()?.label ?: "")) {
            assetName = selectedType!!.label
        }
    }

    ModalBottomSheet(onDismissRequest = {
        viewModel?.clearSuggestedAssetCode()
        onDismiss()
    }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Register Asset",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )

            // Tag ID chip
            SuggestionChip(
                onClick = {},
                label = {
                    Text(
                        if (isManual) "Manual entry" else tagId.take(16) + if (tagId.length > 16) "…" else "",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )

            // Asset type dropdown + camera button
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(
                    expanded = typeDropdownExpanded,
                    onExpandedChange = { typeDropdownExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = if (identificationLoading) "Identifying…" else (selectedType?.label ?: "Select type"),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeDropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false }
                    ) {
                        assetTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = {
                                    selectedType = type
                                    assetName = type.label
                                    typeDropdownExpanded = false
                                    viewModel?.clearSuggestedAssetCode()
                                }
                            )
                        }
                    }
                }
                if (identificationLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    IconButton(onClick = { launchCamera() }) {
                        Icon(Icons.Default.CameraAlt, contentDescription = "Identify with camera", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (suggestedCode != null && selectedType?.code == suggestedCode) {
                Text(
                    "AI suggested · ${selectedType?.label ?: ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            OutlinedTextField(
                value = assetName,
                onValueChange = { assetName = it },
                label = { Text("Asset name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location in building") },
                placeholder = { Text("e.g. Kitchen, Main lobby, Boiler room") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(Modifier.height(4.dp))

            Button(
                onClick = {
                    if (selectedType != null && assetName.isNotBlank()) {
                        onRegister(tagId, selectedType!!.code, assetName.trim(), location.trim(), siteId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedType != null && assetName.isNotBlank()
            ) {
                Text("Register Asset")
            }
        }
    }
}
