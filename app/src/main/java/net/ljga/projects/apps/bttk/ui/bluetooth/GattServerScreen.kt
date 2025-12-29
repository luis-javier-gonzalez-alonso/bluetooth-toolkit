package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.bluetooth.utils.prettyCharacteristicName
import java.util.UUID

class UuidVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 32) text.text.substring(0, 32) else text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 7 || i == 11 || i == 15 || i == 19) out += "-"
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 8) return offset
                if (offset <= 12) return offset + 1
                if (offset <= 16) return offset + 2
                if (offset <= 20) return offset + 3
                return offset + 4
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 8) return offset
                if (offset <= 13) return offset - 1
                if (offset <= 18) return offset - 2
                if (offset <= 23) return offset - 3
                return offset - 4
            }
        }

        return TransformedText(AnnotatedString(out), offsetMapping)
    }
}

private fun parseHexNoSpaces(hex: String): ByteArray {
    return try {
        hex.chunked(2)
            .filter { it.length == 2 }
            .map { it.toInt(16).toByte() }
            .toByteArray()
    } catch (e: Exception) {
        byteArrayOf()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattServerScreen(
    onBackClick: () -> Unit,
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var newServiceRawUuid by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val defaultErrorColor = MaterialTheme.colorScheme.error
    val errorBorderColor = remember { Animatable(defaultErrorColor) }
    val scope = rememberCoroutineScope()

    var showAddCharDialog by remember { mutableStateOf<String?>(null) }
    var showClearConfirmation by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GATT Server Configuration") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!state.isGattServerRunning) {
                        IconButton(onClick = { showClearConfirmation = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Server")
                        }
                    }
                    IconButton(onClick = { viewModel.toggleGattServer() }) {
                        Icon(
                            imageVector = if (state.isGattServerRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (state.isGattServerRunning) "Stop Server" else "Start Server",
                            tint = if (state.isGattServerRunning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
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
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Server Status: ${if (state.isGattServerRunning) "RUNNING" else "STOPPED"}",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (state.isGattServerRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = state.isGattServerRunning,
                        onCheckedChange = { viewModel.toggleGattServer() }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.gattServerDeviceName,
                    onValueChange = { viewModel.setGattServerDeviceName(it) },
                    label = { Text("Advertising Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !state.isGattServerRunning,
                    placeholder = { Text("Device Name") }
                )
            }

            if (state.isGattServerRunning) {
                RunningServerView(state)
            } else {
                EditingServerView(
                    state = state,
                    newServiceRawUuid = newServiceRawUuid,
                    onNewServiceRawUuidChange = { newServiceRawUuid = it },
                    isError = isError,
                    onErrorChange = { isError = it },
                    errorBorderColor = errorBorderColor.value,
                    onAddService = {
                        if (newServiceRawUuid.length == 32) {
                            val formattedUuid = buildString {
                                append(newServiceRawUuid.substring(0, 8))
                                append("-")
                                append(newServiceRawUuid.substring(8, 12))
                                append("-")
                                append(newServiceRawUuid.substring(12, 16))
                                append("-")
                                append(newServiceRawUuid.substring(16, 20))
                                append("-")
                                append(newServiceRawUuid.substring(20))
                            }
                            try {
                                UUID.fromString(formattedUuid)
                                viewModel.addGattService(formattedUuid)
                                newServiceRawUuid = ""
                                isError = false
                            } catch (e: Exception) {
                                isError = true
                                scope.launch {
                                    errorBorderColor.animateTo(Color.Red, animationSpec = tween(1000))
                                    errorBorderColor.animateTo(defaultErrorColor, animationSpec = tween(1000))
                                }
                            }
                        } else if (newServiceRawUuid.isBlank()) {
                            viewModel.addGattService()
                        } else {
                            isError = true
                            scope.launch {
                                errorBorderColor.animateTo(Color.Red, animationSpec = tween(1000))
                                errorBorderColor.animateTo(defaultErrorColor, animationSpec = tween(1000))
                            }
                        }
                    },
                    onDeleteService = { viewModel.removeGattService(it) },
                    onAddCharacteristic = { showAddCharDialog = it },
                    onDeleteCharacteristic = { s, c -> viewModel.removeCharacteristicFromService(s, c) }
                )
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Clear GATT Server") },
            text = { Text("Are you sure you want to delete all services and reset all indices? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearGattServer()
                        showClearConfirmation = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    showAddCharDialog?.let { serviceUuid ->
        AddCharacteristicDialog(
            initialUuid = viewModel.generateCharacteristicUuid(serviceUuid),
            onDismiss = { showAddCharDialog = null },
            onConfirm = { uuid, properties, permissions, initialValue ->
                viewModel.addCharacteristicToService(serviceUuid, uuid, properties, permissions, initialValue)
                showAddCharDialog = null
            }
        )
    }
}

@Composable
fun AddCharacteristicDialog(
    initialUuid: String,
    onDismiss: () -> Unit,
    onConfirm: (String, List<String>, List<String>, String?) -> Unit
) {
    var uuidInput by remember { mutableStateOf(initialUuid.replace("-", "")) }
    var initialValueInput by remember { mutableStateOf("") }
    var selectedProperties by remember { mutableStateOf(setOf("READ", "WRITE")) }
    var selectedPermissions by remember { mutableStateOf(setOf("READ", "WRITE")) }

    val properties = listOf("READ", "WRITE", "WRITE_NO_RESPONSE", "NOTIFY", "INDICATE")
    val permissions = listOf("READ", "WRITE", "READ_ENCRYPTED", "WRITE_ENCRYPTED", "READ_ENCRYPTED_MITM", "WRITE_ENCRYPTED_MITM")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Characteristic") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = uuidInput,
                    onValueChange = { input ->
                        val hexOnly = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                        if (hexOnly.length <= 32) {
                            uuidInput = hexOnly
                        }
                    },
                    label = { Text("Characteristic UUID (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = UuidVisualTransformation()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = initialValueInput,
                    onValueChange = { input ->
                        val hexOnly = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it.uppercaseChar() in 'A'..'F' }
                        initialValueInput = hexOnly
                    },
                    label = { Text("Initial Value (Hex)") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("e.g. DEADBEEF") },
                    singleLine = true,
                    visualTransformation = HexVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )

                val data = remember(initialValueInput) { parseHexNoSpaces(initialValueInput) }
                if (data.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Preview (ASCII):", style = MaterialTheme.typography.labelSmall)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = data.toAsciiString(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Properties", style = MaterialTheme.typography.titleSmall)
                properties.forEach { property ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = property in selectedProperties,
                                onClick = {
                                    selectedProperties = if (property in selectedProperties) {
                                        selectedProperties - property
                                    } else {
                                        selectedProperties + property
                                    }
                                },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = property in selectedProperties,
                            onCheckedChange = null
                        )
                        Text(
                            text = property,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Permissions", style = MaterialTheme.typography.titleSmall)
                permissions.forEach { permission ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .selectable(
                                selected = permission in selectedPermissions,
                                onClick = {
                                    selectedPermissions = if (permission in selectedPermissions) {
                                        selectedPermissions - permission
                                    } else {
                                        selectedPermissions + permission
                                    }
                                },
                                role = Role.Checkbox
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = permission in selectedPermissions,
                            onCheckedChange = null
                        )
                        Text(
                            text = permission,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val formattedUuid = if (uuidInput.length == 32) {
                        buildString {
                            append(uuidInput.substring(0, 8))
                            append("-")
                            append(uuidInput.substring(8, 12))
                            append("-")
                            append(uuidInput.substring(12, 16))
                            append("-")
                            append(uuidInput.substring(16, 20))
                            append("-")
                            append(uuidInput.substring(20))
                        }
                    } else {
                        uuidInput
                    }
                    onConfirm(
                        formattedUuid, 
                        selectedProperties.toList(), 
                        selectedPermissions.toList(),
                        if (initialValueInput.isBlank()) null else initialValueInput
                    )
                },
                enabled = uuidInput.length == 32 || uuidInput.length == 4 || uuidInput.length == 8
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun EditingServerView(
    state: BluetoothUiState,
    newServiceRawUuid: String,
    onNewServiceRawUuidChange: (String) -> Unit,
    isError: Boolean,
    onErrorChange: (Boolean) -> Unit,
    errorBorderColor: Color,
    onAddService: () -> Unit,
    onDeleteService: (String) -> Unit,
    onAddCharacteristic: (String) -> Unit,
    onDeleteCharacteristic: (String, String) -> Unit
) {
    val listState = rememberLazyListState()
    
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

        Text(
            text = "Services",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = newServiceRawUuid,
                onValueChange = { input ->
                    val hexOnly = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                    if (hexOnly.length <= 32) {
                        onNewServiceRawUuidChange(hexOnly)
                        onErrorChange(false)
                    }
                },
                label = { Text("Service UUID (Hex)") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = isError,
                visualTransformation = UuidVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    errorBorderColor = errorBorderColor,
                    errorLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onAddService) {
                Icon(Icons.Default.Add, contentDescription = "Add Service")
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp)
        ) {
            items(state.gattServerServices, key = { it.uuid }) { service ->
                GattServiceItem(
                    service = service,
                    onDeleteService = { onDeleteService(service.uuid) },
                    onAddCharacteristic = { onAddCharacteristic(service.uuid) },
                    onDeleteCharacteristic = { charUuid -> 
                        onDeleteCharacteristic(service.uuid, charUuid)
                    }
                )
            }
        }
    }
}

@Composable
fun RunningServerView(state: BluetoothUiState) {
    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Active Services", fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    state.gattServerServices.forEach { service ->
                        item {
                            Text(
                                text = "Service: ${service.uuid.prettyCharacteristicName()}...",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        items(service.characteristics) { characteristic ->
                            Text(
                                text = "  └─ ${characteristic.uuid.prettyCharacteristicName()}... (${characteristic.properties.joinToString(", ")})",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(all = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "Data Log",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.titleSmall
        )
        
        LogListView(
            logs = state.gattServerLogs,
            gattAliases = emptyMap(),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(MaterialTheme.colorScheme.surface)
        )
    }
}

@Composable
fun GattServiceItem(
    service: BluetoothServiceDomain,
    onDeleteService: () -> Unit,
    onAddCharacteristic: () -> Unit,
    onDeleteCharacteristic: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Service", style = MaterialTheme.typography.labelSmall)
                    Text(text = service.uuid, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = onDeleteService) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Service", tint = MaterialTheme.colorScheme.error)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text(text = "Characteristics", style = MaterialTheme.typography.titleSmall)

            service.characteristics.forEach { characteristic ->
                GattCharacteristicItem(
                    characteristic = characteristic,
                    onDelete = { onDeleteCharacteristic(characteristic.uuid) }
                )
            }

            Button(
                onClick = onAddCharacteristic,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Characteristic")
            }
        }
    }
}

@Composable
fun GattCharacteristicItem(
    characteristic: BluetoothCharacteristicDomain,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text(text = "UUID: ${characteristic.uuid}", style = MaterialTheme.typography.bodySmall)
            Text(
                text = "Props: ${characteristic.properties.joinToString(", ")} | Perms: ${characteristic.permissions.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            characteristic.initialValue?.let {
                Text(
                    text = "Initial Value: $it",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove Characteristic",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                modifier = Modifier.padding(4.dp)
            )
        }
    }
}
