package ca.taplog.app.ui.ember

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ca.taplog.app.data.Asset
import ca.taplog.app.data.Deficiency
import ca.taplog.app.data.DeficiencySeverity
import ca.taplog.app.data.FieldType
import ca.taplog.app.data.InspectionResult
import ca.taplog.app.data.InspectorClaims
import ca.taplog.app.data.VerticalRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectionFormScreen(
    asset: Asset,
    siteName: String = "",
    inspectorClaims: InspectorClaims?,
    onSubmit: (
        result: InspectionResult,
        notes: String?,
        deficiencies: List<Deficiency>
    ) -> Unit,
    onCancel: () -> Unit
) {
    BackHandler { onCancel() }

    val config = remember(asset.vertical) { VerticalRegistry.get(asset.vertical) }
    val formProfile = config.formProfile

    var selectedOption by remember { mutableStateOf<ca.taplog.app.data.ResultOption?>(null) }
    val fieldValues = remember { mutableStateMapOf<String, String>() }
    var deficiencies by remember { mutableStateOf<List<Deficiency>>(emptyList()) }
    var showDeficiencyDialog by remember { mutableStateOf(false) }
    var resultError by remember { mutableStateOf(false) }

    if (showDeficiencyDialog) {
        AddDeficiencyDialog(
            onAdd = { deficiency ->
                deficiencies = deficiencies + deficiency
                showDeficiencyDialog = false
            },
            onDismiss = { showDeficiencyDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = asset.name,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (siteName.isNotBlank())
                                "$siteName — ${asset.location}"
                            else
                                asset.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Cancel"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Pre-inspection checklist (sourced from VerticalConfig)
            val assetTypeEntry = remember(asset.assetType) {
                config.assetTypeRegistry.find { it.code == asset.assetType }
            }
            val checklistItems = assetTypeEntry?.checklistItems ?: emptyList()
            if (checklistItems.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Inspection Checklist",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            assetTypeEntry?.label ?: asset.assetType,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        checklistItems.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "${index + 1}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(24.dp)
                                )
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            // Verified inspector identity (read-only)
            InspectorIdentityCard(inspectorClaims)

            // Result selector — driven by formProfile.resultOptions
            Column {
                Text(
                    "Result",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (resultError) {
                    Text(
                        "Select a result",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    formProfile.resultOptions.forEach { option ->
                        FilterChip(
                            selected = selectedOption?.code == option.code,
                            onClick = { selectedOption = option; resultError = false },
                            label = {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Dynamic fields — driven by formProfile.fields
            formProfile.fields
                .filter { field ->
                    field.applicableAssetTypes.isEmpty() ||
                        asset.assetType in field.applicableAssetTypes
                }
                .forEach { field ->
                    when (field.type) {
                        FieldType.TEXT -> {
                            OutlinedTextField(
                                value = fieldValues[field.key] ?: "",
                                onValueChange = { fieldValues[field.key] = it },
                                label = { Text(field.label) },
                                placeholder = { Text("Optional observations...") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        FieldType.NUMBER -> {
                            OutlinedTextField(
                                value = fieldValues[field.key] ?: "",
                                onValueChange = { fieldValues[field.key] = it },
                                label = { Text(field.label) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        FieldType.BOOLEAN -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = fieldValues[field.key] == "true",
                                    onCheckedChange = { checked ->
                                        fieldValues[field.key] = checked.toString()
                                    }
                                )
                                Text(field.label, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        else -> {
                            // Future field types (SINGLE_SELECT, MULTI_SELECT, DATE, PHOTO)
                            // rendered as plain text inputs until dedicated renderers are built
                            OutlinedTextField(
                                value = fieldValues[field.key] ?: "",
                                onValueChange = { fieldValues[field.key] = it },
                                label = { Text(field.label) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }
                }

            // Deficiencies section
            if (formProfile.deficienciesEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Deficiencies",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showDeficiencyDialog = true }) {
                        Text("+ Add")
                    }
                }

                if (deficiencies.isEmpty()) {
                    Text(
                        "None recorded",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    deficiencies.forEach { deficiency ->
                        DeficiencyChip(deficiency = deficiency)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    resultError = selectedOption == null
                    if (!resultError) {
                        onSubmit(
                            InspectionResult.valueOf(selectedOption!!.code),
                            fieldValues["notes"]?.trim()?.ifBlank { null },
                            deficiencies
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Submit Inspection", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Cancel", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun InspectorIdentityCard(claims: InspectorClaims?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                "Inspector",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
            )
            Text(
                claims?.name ?: "Not signed in",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            claims?.certNumber?.let {
                Text(
                    "Cert #$it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun DeficiencyChip(deficiency: Deficiency) {
    val severityColor = when (deficiency.severity) {
        DeficiencySeverity.CRITICAL -> MaterialTheme.colorScheme.error
        DeficiencySeverity.HIGH -> MaterialTheme.colorScheme.errorContainer
        DeficiencySeverity.MEDIUM -> MaterialTheme.colorScheme.tertiary
        DeficiencySeverity.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    var thumbnail by remember(deficiency.photoPath) { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(deficiency.photoPath) {
        deficiency.photoPath?.let { path ->
            thumbnail = withContext(Dispatchers.IO) {
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                BitmapFactory.decodeFile(path, opts)
            }
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    deficiency.code,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    deficiency.description,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                deficiency.severity.name,
                style = MaterialTheme.typography.labelSmall,
                color = severityColor,
                fontWeight = FontWeight.Bold
            )
            thumbnail?.let { bmp ->
                Spacer(modifier = Modifier.width(8.dp))
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Deficiency photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

private fun createPhotoFile(context: android.content.Context): Pair<File, Uri> {
    val dir = File(context.getExternalFilesDir(null), "TapLog/photos").also { it.mkdirs() }
    val file = File(dir, "def_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "ca.taplog.app.fileprovider", file)
    return Pair(file, uri)
}

@Composable
fun AddDeficiencyDialog(
    onAdd: (Deficiency) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var severity by remember { mutableStateOf(DeficiencySeverity.MEDIUM) }
    var capturedPhotoPath by remember { mutableStateOf<String?>(null) }
    var pendingPhotoFile by remember { mutableStateOf<File?>(null) }
    var thumbnail by remember { mutableStateOf<android.graphics.Bitmap?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            capturedPhotoPath = pendingPhotoFile?.absolutePath
            pendingPhotoFile?.absolutePath?.let { path ->
                val opts = BitmapFactory.Options().apply { inSampleSize = 8 }
                thumbnail = BitmapFactory.decodeFile(path, opts)
            }
        } else {
            pendingPhotoFile?.delete()
            pendingPhotoFile = null
            capturedPhotoPath = null
            thumbnail = null
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val (file, uri) = createPhotoFile(context)
            pendingPhotoFile = file
            takePictureLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val (file, uri) = createPhotoFile(context)
            pendingPhotoFile = file
            takePictureLauncher.launch(uri)
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Deficiency") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Code") },
                    placeholder = { Text("e.g. OFC-6.2.1") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Severity",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DeficiencySeverity.entries.forEach { s ->
                        FilterChip(
                            selected = severity == s,
                            onClick = { severity = s },
                            label = {
                                Text(
                                    s.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = { launchCamera() }) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Take photo",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            if (capturedPhotoPath != null) "Retake" else "Photo",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    thumbnail?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured photo preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && description.isNotBlank()) {
                        onAdd(
                            Deficiency(
                                inspectionId = "",
                                assetId = "",
                                code = code.trim(),
                                description = description.trim(),
                                severity = severity,
                                photoPath = capturedPhotoPath
                            )
                        )
                    }
                }
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
