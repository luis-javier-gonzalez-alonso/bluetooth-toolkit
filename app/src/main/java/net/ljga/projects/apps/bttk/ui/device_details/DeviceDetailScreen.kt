package net.ljga.projects.apps.bttk.ui.device_details

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    device: BluetoothDeviceDomain?,
    gattAliases: Map<String, String> = emptyMap(),
    onBackClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = device?.name ?: "Device Details") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        if (device == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "Device not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                item {
                    DetailItem(label = "Name", value = device.name ?: "Unknown")
                }
                item {
                    DetailItem(label = "Address", value = device.address)
                }
                item {
                    DetailItem(
                        label = "Bond State",
                        value = when (device.bondState) {
                            BluetoothDevice.BOND_BONDED -> "Bonded"
                            BluetoothDevice.BOND_BONDING -> "Bonding"
                            else -> "Not Bonded"
                        }
                    )
                }
                item {
                    DetailItem(
                        label = "Device Type",
                        value = when (device.type) {
                            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                            BluetoothDevice.DEVICE_TYPE_LE -> "Low Energy"
                            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual Mode"
                            else -> "Unknown"
                        }
                    )
                }
                item {
                    DetailItem(label = "In Range", value = if (device.isInRange) "Yes" else "No")
                }
                item {
                    DetailItem(label = "Signal Strength (RSSI)", value = device.rssi?.let { "$it dBm" } ?: "Unavailable")
                }

                if (device.services.isNotEmpty()) {
                    item {
                        Text(
                            text = "GATT Services",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(device.services) { service ->
                        ServiceItem(service, gattAliases)
                    }
                } else if (device.uuids.isNotEmpty()) {
                    item {
                        Text(
                            text = "Services (UUIDs)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(device.uuids) { uuid ->
                        Text(
                            text = uuid,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun ServiceItem(service: BluetoothServiceDomain, gattAliases: Map<String, String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Service: ${service.uuid}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            service.characteristics.forEach { char ->
                Column(modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)) {
                    val alias = gattAliases["${service.uuid}-${char.uuid}"]
                    val displayText = if (alias != null) "${char.uuid} ($alias)" else char.uuid
                    Text(
                        text = "Characteristic: $displayText",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        char.properties.forEach { property ->
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier.padding(vertical = 2.dp)
                            ) {
                                Text(
                                    text = property,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}