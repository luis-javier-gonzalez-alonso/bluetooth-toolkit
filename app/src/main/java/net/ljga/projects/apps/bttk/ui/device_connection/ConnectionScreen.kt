package net.ljga.projects.apps.bttk.ui.device_connection

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.domain.device_connection.model.GattCharacteristicSettingsDomain
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import net.ljga.projects.apps.bttk.domain.utils.prettyCharacteristicName
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
    settings: Map<String, GattCharacteristicSettingsDomain> = emptyMap(),
    savedDataFrames: List<DataFrameDomain> = emptyList(),
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit,
    onReadCharacteristic: (String, String) -> Unit = { _, _ -> },
    onWriteCharacteristic: (String, String, ByteArray) -> Unit = { _, _, _ -> },
    onToggleNotification: (String, String, Boolean) -> Unit = { _, _, _ -> },
    onSaveAlias: (String, String, String) -> Unit = { _, _, _ -> },
    onSaveDataFrame: (String, ByteArray) -> Unit = { _, _ -> },
    onDeleteDataFrame: (Int) -> Unit = { _ -> },
    onSaveSettings: (GattCharacteristicSettingsDomain) -> Unit = {},
    onDeleteParserConfig: (String, String) -> Unit = { _, _ -> }
) {
    var showWriteDialog by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showConfigureDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

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
                    if (isConnected) {
                        TextButton(onClick = onDisconnectClick) {
                            Text("Disconnect")
                        }
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
            if (isConnected && device != null) {
                if (device.services.isNotEmpty()) {
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
                                        val key = "${service.uuid}-${characteristic.uuid}"
                                        val characteristicSettings = settings[key]
                                        val alias = characteristicSettings?.alias ?: ""
                                        CharacteristicRow(
                                            serviceUuid = service.uuid,
                                            characteristicUuid = characteristic.uuid,
                                            alias = alias,
                                            properties = characteristic.properties,
                                            isNotifying = enabledNotifications.contains(key),
                                            hasParser = characteristicSettings?.fields?.isNotEmpty() == true,
                                            onRead = { onReadCharacteristic(service.uuid, characteristic.uuid) },
                                            onWrite = { showWriteDialog = service.uuid to characteristic.uuid },
                                            onToggleNotify = { onToggleNotification(service.uuid, characteristic.uuid, it) },
                                            onConfigure = { showConfigureDialog = service.uuid to characteristic.uuid }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Discovering services...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } else if (!isConnected) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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

    if (showWriteDialog != null) {
        PacketSelectorDialog(
            serviceUuid = showWriteDialog!!.first,
            characteristicUuid = showWriteDialog!!.second,
            savedDataFrames = savedDataFrames,
            onDismiss = { showWriteDialog = null },
            onSend = { data ->
                onWriteCharacteristic(showWriteDialog!!.first, showWriteDialog!!.second, data)
                showWriteDialog = null
            },
            onSave = onSaveDataFrame,
            onDelete = onDeleteDataFrame
        )
    }

    if (showConfigureDialog != null) {
        val (sUuid, cUuid) = showConfigureDialog!!
        val key = "$sUuid-$cUuid"
        val currentSettings = settings[key] ?: GattCharacteristicSettingsDomain(sUuid, cUuid)
        
        GattCharacteristicSettingsDialog(
            serviceUuid = sUuid,
            characteristicUuid = cUuid,
            initialSettings = currentSettings,
            onSave = { config ->
                onSaveSettings(config)
            },
            onDismiss = { showConfigureDialog = null }
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
    hasParser: Boolean,
    onRead: () -> Unit,
    onWrite: () -> Unit,
    onToggleNotify: (Boolean) -> Unit,
    onConfigure: () -> Unit
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
            IconButton(onClick = onConfigure, modifier = Modifier.size(24.dp)) {
                Icon(
                    Icons.Default.Settings, 
                    contentDescription = "Configure", 
                    modifier = Modifier.size(16.dp), 
                    tint = if (hasParser || alias.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = characteristicUuid.prettyCharacteristicName() + "..." + (if (alias.isNotBlank()) " ($alias)" else ""),
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
                        modifier = Modifier
                            .clickable { onWrite() }
                            .padding(4.dp)
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
fun PacketSelectorDialog(
    serviceUuid: String,
    characteristicUuid: String,
    savedDataFrames: List<DataFrameDomain>,
    onDismiss: () -> Unit,
    onSend: (ByteArray) -> Unit,
    onSave: (String, ByteArray) -> Unit,
    onDelete: (Int) -> Unit
) {
    var hexString by remember { mutableStateOf("") }
    var saveName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Write to Characteristic") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text(text = "Creator", modifier = Modifier.padding(8.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text(text = "Stored", modifier = Modifier.padding(8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = hexString,
                        onValueChange = { 
                            val clean = it.filter { c -> c.isDigit() || c in 'a'..'f' || c in 'A'..'F' }
                            hexString = clean
                        },
                        label = { Text("Hex Data (e.g. 010AFF)") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        visualTransformation = HexVisualTransformation()
                    )

                    val data = remember(hexString) { parseHexNoSpaces(hexString) }
                    if (data.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preview (ASCII):", style = MaterialTheme.typography.labelSmall)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(4.dp)
                                )
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
                    OutlinedTextField(
                        value = saveName,
                        onValueChange = { saveName = it },
                        label = { Text("Save as (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(savedDataFrames) { frame ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        hexString =
                                            frame.data.joinToString("") { "%02X".format(it) }
                                        selectedTab = 0
                                    }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(frame.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        frame.data.joinToString(" ") { "%02X".format(it) },
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(onClick = { onDelete(frame.uid) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            HorizontalDivider()
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                if (selectedTab == 0 && saveName.isNotBlank() && hexString.isNotBlank()) {
                    TextButton(onClick = { 
                        val data = parseHexNoSpaces(hexString)
                        if (data.isNotEmpty()) {
                            onSave(saveName, data)
                            saveName = ""
                        }
                    }) {
                        Text("Save")
                    }
                }
                Button(
                    onClick = {
                        val data = parseHexNoSpaces(hexString)
                        if (data.isNotEmpty()) {
                            onSend(data)
                        }
                    },
                    enabled = hexString.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

class HexVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val out = StringBuilder()
        for (i in text.indices) {
            out.append(text[i])
            if (i % 2 == 1 && i != text.lastIndex) {
                out.append(" ")
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val spaces = (offset - 1) / 2
                return offset + spaces
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val spaces = offset / 3
                return (offset - spaces).coerceAtMost(text.length)
            }
        }

        return TransformedText(AnnotatedString(out.toString()), offsetMapping)
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
                val aliasKey = if (packet.serviceUuid != null && packet.characteristicUuid != null) {
                    "${packet.serviceUuid}-${packet.characteristicUuid}"
                } else null
                val alias = aliasKey?.let { gattAliases[it] }
                val charName = packet.characteristicUuid?.prettyCharacteristicName()?.let { " → $it" } ?: ""
                
                Text(
                    text = if (alias != null) "$source ($alias)" else "$source$charName",
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
            "  └─ ${char.uuid.prettyCharacteristicName()}...$aliasSuffix (${char.properties.joinToString(", ")})"
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
