package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.DataFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    device: BluetoothDeviceDomain?,
    isConnected: Boolean,
    logs: List<BluetoothDataPacket>,
    enabledNotifications: Set<String> = emptySet(),
    gattAliases: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onReadCharacteristic: (String, String) -> Unit = { _, _ -> },
    onToggleNotification: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSaveAlias: (String, String, String) -> Unit = { _, _, _ -> }
) {
    var showAliasDialog by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = device?.name ?: "Unknown Device", style = MaterialTheme.typography.titleMedium)
                        Text(text = if (isConnected) "Connected" else "Connecting...", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onDisconnectClick) {
                        Text("Disconnect")
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
            if (isConnected && device != null && device.services.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "Characteristics", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        LazyColumn(modifier = Modifier.heightIn(max = 250.dp)) {
                            device.services.forEach { service ->
                                items(service.characteristics) { characteristic ->
                                    val aliasKey = "${service.uuid}-${characteristic.uuid}"
                                    val alias = gattAliases[aliasKey] ?: ""
                                    CharacteristicRow(
                                        serviceUuid = service.uuid,
                                        characteristicUuid = characteristic.uuid,
                                        alias = alias,
                                        properties = characteristic.properties,
                                        isNotifying = enabledNotifications.contains(aliasKey),
                                        onRead = { onReadCharacteristic(service.uuid, characteristic.uuid) },
                                        onToggleNotify = { onToggleNotification(service.uuid, characteristic.uuid, it) },
                                        onEditAlias = { showAliasDialog = Triple(service.uuid, characteristic.uuid, alias) }
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (!isConnected) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Data Log Section
            Text(
                text = "Data Log",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall
            )
            
            LogListView(
                logs = logs,
                gattAliases = gattAliases,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }

    if (showAliasDialog != null) {
        val (serviceUuid, charUuid, currentAlias) = showAliasDialog!!
        var text by remember { mutableStateOf(currentAlias) }
        AlertDialog(
            onDismissRequest = { showAliasDialog = null },
            title = { Text("Set Alias") },
            text = {
                Column {
                    Text("UUID: ${charUuid.take(8)}...", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = { Text("Alias") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSaveAlias(serviceUuid, charUuid, text)
                    showAliasDialog = null
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAliasDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CharacteristicRow(
    serviceUuid: String,
    characteristicUuid: String,
    alias: String,
    properties: List<String>,
    isNotifying: Boolean,
    onRead: () -> Unit,
    onToggleNotify: (Boolean) -> Unit,
    onEditAlias: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onEditAlias, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Alias", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = characteristicUuid.take(8) + "..." + (if (alias.isNotBlank()) " ($alias)" else ""),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        Row(
            modifier = Modifier.width(180.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Box(modifier = Modifier.width(50.dp), contentAlignment = Alignment.CenterEnd) {
                if (properties.contains("READ")) {
                    Text(
                        text = "READ",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { onRead() }
                            .padding(4.dp)
                    )
                }
            }
            
            Box(modifier = Modifier.width(55.dp), contentAlignment = Alignment.CenterEnd) {
                if (properties.contains("WRITE") || properties.contains("WRITE_NO_RESPONSE")) {
                    Text(
                        text = "WRITE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }

            Box(modifier = Modifier.width(75.dp), contentAlignment = Alignment.CenterEnd) {
                if (properties.contains("NOTIFY") || properties.contains("INDICATE")) {
                    Text(
                        text = "NOTIFY",
                        style = MaterialTheme.typography.labelSmall.copy(
                            textDecoration = if (isNotifying) TextDecoration.None else TextDecoration.LineThrough
                        ),
                        color = if (isNotifying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier
                            .clickable { onToggleNotify(!isNotifying) }
                            .padding(4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun LogListView(
    logs: List<BluetoothDataPacket>,
    gattAliases: Map<String, String> = emptyMap(),
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        items(logs) { packet ->
            LogEntry(packet, gattAliases)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun LogEntry(packet: BluetoothDataPacket, gattAliases: Map<String, String>) {
    val timeFormatter = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = timeFormatter.format(packet.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
            packet.source?.let { source ->
                val alias = if (packet.serviceUuid != null && packet.characteristicUuid != null) {
                    gattAliases["${packet.serviceUuid}-${packet.characteristicUuid}"]
                } else null
                
                Text(
                    text = if (alias != null) "$source ($alias)" else source,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        when (packet.format) {
            DataFormat.HEX_ASCII -> RawDataEntry(packet.data)
            DataFormat.STRUCTURED -> StructuredDataEntry(packet.text ?: String(packet.data))
            DataFormat.GATT_STRUCTURE -> {
                val gattText = remember(packet.gattServices) {
                    packet.gattServices?.let { formatGattServices(it, gattAliases) } ?: "No services discovered"
                }
                StructuredDataEntry(gattText)
            }
        }
    }
}

private fun formatGattServices(services: List<BluetoothServiceDomain>, gattAliases: Map<String, String>): String {
    return services.joinToString("\n") { service ->
        "Service: ${service.uuid}\n" + service.characteristics.joinToString("\n") { char ->
            val alias = gattAliases["${service.uuid}-${char.uuid}"]
            val aliasSuffix = if (alias != null) " ($alias)" else ""
            "  └─ ${char.uuid.take(8)}...$aliasSuffix (${char.properties.joinToString(", ")})"
        }
    }
}

@Composable
fun RawDataEntry(data: ByteArray) {
    val bytesPerRow = 16
    val chunks = remember(data) { data.toList().chunked(bytesPerRow) }
    
    chunks.forEach { chunk ->
        val chunkBytes = chunk.toByteArray()
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = chunkBytes.toHexString(padTo = bytesPerRow),
                modifier = Modifier.weight(2.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = chunkBytes.toAsciiString(padTo = bytesPerRow),
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.tertiary,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StructuredDataEntry(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(vertical = 2.dp)
    )
}

fun ByteArray.toHexString(padTo: Int = 0): String {
    val hex = joinToString(" ") { "%02X".format(it) }
    return if (padTo > size) {
        val padding = "   ".repeat(padTo - size)
        hex + padding
    } else hex
}

fun ByteArray.toAsciiString(padTo: Int = 0): String {
    val ascii = String(map { if (it in 32..126) it.toByte() else '.'.toByte() }.toByteArray())
    return if (padTo > size) {
        ascii + " ".repeat(padTo - size)
    } else ascii
}
