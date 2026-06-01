package ca.taplog.app.ui.ember

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ca.taplog.app.data.OFCAssetType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetRegistrationScreen(
    nfcTagId: String,
    siteName: String = "",
    onSave: (
        name: String,
        assetTypeCode: String,
        location: String,
        installDateMillis: Long,
        inspectionIntervalMonths: Int
    ) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedOFCType by remember { mutableStateOf<OFCAssetType?>(null) }
    var location by remember { mutableStateOf("") }
    var installDateText by remember { mutableStateOf("") }
    var showPicker by remember { mutableStateOf(false) }
    var showDateError by remember { mutableStateOf(false) }

    BackHandler { onCancel() }

    if (showPicker) {
        AssetTypePickerDialog(
            onTypeSelected = { type ->
                selectedOFCType = type
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Register New Asset",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (siteName.isNotBlank()) {
                            Text(
                                text = siteName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = "Tag ID: $nfcTagId",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Asset name") },
                placeholder = { Text("e.g. Lobby Extinguisher 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // OFC asset type picker
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedOFCType?.label ?: "",
                    onValueChange = {},
                    label = { Text("Asset type") },
                    placeholder = { Text("Select type…") },
                    trailingIcon = {
                        Text("▾", style = MaterialTheme.typography.bodyLarge)
                    },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showPicker = true }
                )
            }

            selectedOFCType?.let { type ->
                Text(
                    text = "Interval: ${intervalLabel(type.inspectionIntervalMonths)} · Code: ${type.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                placeholder = { Text("e.g. Main Lobby, 1st Floor") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = installDateText,
                onValueChange = {
                    installDateText = it
                    showDateError = false
                },
                label = { Text("Install date (YYYY-MM-DD)") },
                placeholder = { Text("e.g. 2024-01-15") },
                singleLine = true,
                isError = showDateError,
                supportingText = if (showDateError) {
                    { Text("Enter date as YYYY-MM-DD") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    val installMillis = parseInstallDate(installDateText)
                    if (installMillis == null) {
                        showDateError = true
                        return@Button
                    }
                    val type = selectedOFCType ?: return@Button
                    onSave(
                        name.trim(),
                        type.code,
                        location.trim(),
                        installMillis,
                        type.inspectionIntervalMonths
                    )
                },
                enabled = name.isNotBlank()
                        && selectedOFCType != null
                        && location.isNotBlank()
                        && installDateText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Register Asset", style = MaterialTheme.typography.titleMedium)
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

private fun parseInstallDate(text: String): Long? {
    return try {
        val parts = text.trim().split("-")
        if (parts.size != 3) return null
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        if (month !in 1..12 || day !in 1..31) return null
        java.util.Calendar.getInstance().apply {
            set(year, month - 1, day, 0, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
    } catch (e: Exception) {
        null
    }
}

private fun intervalLabel(months: Int): String = when (months) {
    6    -> "every 6 months"
    12   -> "annual"
    else -> "every $months months"
}