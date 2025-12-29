package net.ljga.projects.apps.bttk.ui.bluetooth

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScreen(
    viewModel: BluetoothViewModel,
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onGattServerClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showFabMenu by remember { mutableStateOf(false) }

    val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADVERTISE
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

//    LaunchedEffect(state.errorMessage) {
//        state.errorMessage?.let {
//            snackbarHostState.showSnackbar(it)
//        }
//    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showFabMenu = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Actions")
                }
                DropdownMenu(
                    expanded = showFabMenu,
                    onDismissRequest = { showFabMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("GATT Server") },
                        onClick = {
                            showFabMenu = false
                            onGattServerClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Storage, contentDescription = null)
                        }
                    )
                }
            }
        }
    ) { padding ->
        val pullToRefreshState = rememberPullToRefreshState()
        
        PullToRefreshBox(
            isRefreshing = state.isRefreshing,
            state = pullToRefreshState,
            onRefresh = {
                permissionLauncher.launch(permissions)
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            BluetoothDeviceList(
                pairedDevices = state.pairedDevices,
                savedDevices = state.savedDevices,
                scannedDevices = state.scannedDevices,
                isConnecting = state.isConnecting,
                onClick = onDeviceClick,
                onDetailsClick = onDetailsClick,
                onPair = viewModel::pairDevice,
                onForgetPaired = viewModel::forgetDevice,
                onSave = viewModel::saveDevice,
                onForgetSaved = viewModel::forgetSavedDevice,
                onCheckReachability = viewModel::checkReachability,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun BluetoothDeviceList(
    pairedDevices: List<BluetoothDeviceDomain>,
    savedDevices: List<BluetoothDeviceDomain>,
    scannedDevices: List<BluetoothDeviceDomain>,
    isConnecting: Boolean,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onPair: (BluetoothDeviceDomain) -> Unit,
    onForgetPaired: (BluetoothDeviceDomain) -> Unit,
    onSave: (BluetoothDeviceDomain) -> Unit,
    onForgetSaved: (BluetoothDeviceDomain) -> Unit,
    onCheckReachability: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
    ) {
        item {
            Text(
                text = "\u2304 Pull down to scan \u2304",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                textAlign = TextAlign.Center
            )
        }

//        if (isConnecting) {
//            item {
//                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
//            }
//        }

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
            val isAlreadySaved = savedDevices.any { it.address == device.address }
            BluetoothDeviceItem(
                device = device,
                onClick = onClick,
                onDetailsClick = onDetailsClick,
                onForget = { onForgetPaired(device) },
                onSave = if (!isAlreadySaved) { { onSave(device) } } else null,
                onCheckReachability = { onCheckReachability(device) },
                isPaired = true
            )
        }

        item {
            Text(
                text = "Saved Devices",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        if (savedDevices.isEmpty()) {
            item {
                Text(
                    text = "No saved devices found",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        items(savedDevices) { device ->
            val isAlreadyPaired = pairedDevices.any { it.address == device.address }
            BluetoothDeviceItem(
                device = device,
                onClick = onClick,
                onDetailsClick = onDetailsClick,
                onPair = if (!isAlreadyPaired) { { onPair(device) } } else null,
                onForget = { onForgetSaved(device) },
                onCheckReachability = { onCheckReachability(device) },
                isSaved = true
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
                onDetailsClick = onDetailsClick,
                onPair = { onPair(device) },
                onSave = { onSave(device) }
            )
        }
    }
}

@Composable
fun BluetoothDeviceItem(
    device: BluetoothDeviceDomain,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onPair: (() -> Unit)? = null,
    onForget: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCheckReachability: (() -> Unit)? = null,
    isPaired: Boolean = false,
    isSaved: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    
    // Disable interaction if paired or saved but not in range
    val isEnabled = (!isPaired && !isSaved) || device.isInRange
    val contentColor = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text(text = "Forget Device") },
            text = { Text(text = "Are you sure you want to forget ${device.name ?: device.address}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onForget?.invoke()
                        showConfirmDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

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
                if ((isPaired || isSaved) && !device.isInRange) {
                    Text(
                        text = "Out of range",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }
            
            if (device.rssi != null) {
                Text(
                    text = "${device.rssi} dBm",
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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
                    DropdownMenuItem(
                        text = { Text("Details") },
                        onClick = {
                            onDetailsClick(device)
                            showMenu = false
                        }
                    )
                    onPair?.let {
                        DropdownMenuItem(
                            text = { Text("Pair") },
                            onClick = {
                                it()
                                showMenu = false
                            }
                        )
                    }
                    onCheckReachability?.let {
                        DropdownMenuItem(
                            text = { Text("Check Status") },
                            onClick = {
                                it()
                                showMenu = false
                            }
                        )
                    }
                    onSave?.let {
                        DropdownMenuItem(
                            text = { Text("Save") },
                            onClick = {
                                it()
                                showMenu = false
                            }
                        )
                    }
                    onForget?.let {
                        DropdownMenuItem(
                            text = { Text("Forget") },
                            onClick = {
                                showConfirmDialog = true
                                showMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
