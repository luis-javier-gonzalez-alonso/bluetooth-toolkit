package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
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
    val listState = rememberLazyListState()

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
                    value = newServiceRawUuid,
                    onValueChange = { input ->
                        val hexOnly = input.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }
                        if (hexOnly.length <= 32) {
                            newServiceRawUuid = hexOnly
                            isError = false
                        }
                    },
                    label = { Text("Service UUID (Hex)") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    isError = isError,
                    visualTransformation = UuidVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        errorBorderColor = errorBorderColor.value,
                        errorLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    val validateAndAdd = {
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
                    }
                    validateAndAdd()
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Service")
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp)
            ) {
                items(state.gattServerServices, key = { it.uuid }) { service ->
                    GattServiceItem(
                        service = service,
                        onDeleteService = { viewModel.removeGattService(service.uuid) },
                        onAddCharacteristic = { viewModel.addCharacteristicToService(service.uuid) },
                        onDeleteCharacteristic = { charUuid -> 
                            viewModel.removeCharacteristicFromService(service.uuid, charUuid)
                        }
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
                text = "Props: ${characteristic.properties.joinToString(", ")}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary
            )
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
