package net.ljga.projects.apps.bttk.ui.bluetooth

import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceDetailScreen(
    device: BluetoothDeviceDomain?,
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
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(text = "Device not found")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
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
                if (device.uuids.isNotEmpty()) {
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
