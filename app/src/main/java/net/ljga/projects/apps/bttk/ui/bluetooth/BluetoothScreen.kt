package net.ljga.projects.apps.bttk.ui.bluetooth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain

@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val allGranted = perms.values.all { it }
        if (allGranted) {
            viewModel.startScan()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    permissionLauncher.launch(permissions)
                }) {
                    Text(text = "Start scan")
                }
                Button(onClick = viewModel::stopScan) {
                    Text(text = "Stop scan")
                }
            }

            if (state.isConnecting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            BluetoothDeviceList(
                pairedDevices = state.pairedDevices,
                scannedDevices = state.scannedDevices,
                onClick = onDeviceClick,
                onForget = viewModel::forgetDevice,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDeviceDomain>,
    scannedDevices: List<BluetoothDeviceDomain>,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onForget: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "Paired Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (pairedDevices.isEmpty()) {
            item {
                Text(
                    text = "No paired devices found",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        items(pairedDevices) { device ->
            BluetoothDeviceItem(
                device = device,
                onClick = onClick,
                onForget = onForget,
                isPaired = true
            )
        }
        item {
            Text(
                text = "Scanned Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (scannedDevices.isEmpty()) {
            item {
                Text(
                    text = "No scanned devices found",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        items(scannedDevices) { device ->
            BluetoothDeviceItem(
                device = device,
                onClick = onClick,
                onForget = onForget,
                isPaired = false
            )
        }
    }
}

@Composable
fun BluetoothDeviceItem(
    device: BluetoothDeviceDomain,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onForget: (BluetoothDeviceDomain) -> Unit,
    isPaired: Boolean
) {
    var showMenu by remember { mutableStateOf(false) }
    
    // Disable interaction if paired but not in range
    val isEnabled = !isPaired || device.isInRange
    val contentColor = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(enabled = isEnabled) { onClick(device) },
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name ?: "Unknown Device",
                    fontWeight = FontWeight.Medium,
                    color = contentColor
                )
                Text(
                    text = device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor
                )
                if (isPaired && !device.isInRange) {
                    Text(
                        text = "Out of range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
            
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = contentColor
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    if (isPaired) {
                        DropdownMenuItem(
                            text = { Text("Forget") },
                            onClick = {
                                onForget(device)
                                showMenu = false
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            // Show details logic
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Copy Address") },
                        onClick = {
                            // Copy address logic
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}
