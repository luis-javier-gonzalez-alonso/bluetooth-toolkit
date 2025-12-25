package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattServerScreen(
    onBackClick: () -> Unit,
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var newServiceUuid by remember { mutableStateOf("") }

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
                .padding(16.dp)
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text(
                text = "Services",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newServiceUuid,
                    onValueChange = { newServiceUuid = it },
                    label = { Text("Service UUID") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (newServiceUuid.isNotBlank()) {
                        viewModel.addGattService(newServiceUuid)
                        newServiceUuid = ""
                    } else {
                        viewModel.addGattService()
                    }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(state.gattServerServices) { service ->
                    GattServiceItem(
                        service = service,
                        onDeleteService = { viewModel.removeGattService(service.uuid) },
                        onAddCharacteristic = { viewModel.addCharacteristicToService(service.uuid) }
                    )
                }
            }
        }
    }
}

@Composable
fun GattServiceItem(
    service: BluetoothServiceDomain,
    onDeleteService: () -> Unit,
    onAddCharacteristic: () -> Unit
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
                GattCharacteristicItem(characteristic = characteristic)
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
fun GattCharacteristicItem(characteristic: BluetoothCharacteristicDomain) {
    Column(modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
        Text(text = "UUID: ${characteristic.uuid}", style = MaterialTheme.typography.bodySmall)
        Text(
            text = "Props: ${characteristic.properties.joinToString(", ")}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}
