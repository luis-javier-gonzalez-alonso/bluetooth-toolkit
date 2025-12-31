package net.ljga.projects.apps.bttk.ui.device_scan

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceScanScreen(
    viewModel: DeviceScanViewModel,
//    scriptViewModel: BluetoothScriptViewModel = hiltViewModel(),
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onGattServerClick: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var expanded by remember { mutableStateOf(false) }

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

    if (expanded) {
        BackHandler { expanded = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
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
                    savedDevices = state.savedDevices,
                    scannedDevices = state.scannedDevices,
                    onClick = onDeviceClick,
                    onDetailsClick = onDetailsClick,
                    onSave = viewModel::saveDevice,
                    onForgetSaved = viewModel::forgetSavedDevice,
                    onCheckReachability = viewModel::checkReachability,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Scrim overlay - Rendered after Scaffold but before FAB to allow FAB clicks
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(300, easing = LinearEasing)),
            exit = fadeOut(animationSpec = tween(300, easing = LinearEasing))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        expanded = false
                    }
            )
        }

        // Custom FAB menu placed manually to ensure it's on top of the scrim
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                shadowElevation = 6.dp,
                modifier = Modifier.animateContentSize(
                    animationSpec = tween(durationMillis = 200, easing = LinearEasing),
                    alignment = Alignment.BottomEnd
                )
            ) {
                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        val duration = 350
                        if (targetState) {
                            // Expanding: content appears in the last moments (e.g., 250ms delay)
                            (fadeIn(animationSpec = tween(50, delayMillis = 150, easing = LinearEasing)) +
                             scaleIn(initialScale = 0.95f, animationSpec = tween(50, delayMillis = 150, easing = LinearEasing)))
                                .togetherWith(fadeOut(animationSpec = tween(100, easing = LinearEasing)))
                                .using(SizeTransform(clip = false) { _, _ -> tween(duration, easing = LinearEasing) })
                        } else {
                            // Shrinking: content disappears quickly, FAB icon appears late
                            (fadeIn(animationSpec = tween(50, delayMillis = 0, easing = LinearEasing)) +
                             scaleIn(initialScale = 0.8f, animationSpec = tween(100, delayMillis = 0, easing = LinearEasing)))
                                .togetherWith(fadeOut(animationSpec = tween(100, easing = LinearEasing)))
                                .using(SizeTransform(clip = false) { _, _ -> tween(duration, easing = LinearEasing) })
                        }
                    },
                    contentAlignment = Alignment.BottomEnd,
                    label = "FAB Menu Animation"
                ) { isExpanded ->
                    if (!isExpanded) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clickable { expanded = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Actions")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .width(IntrinsicSize.Max)
                                .widthIn(min = 220.dp)
                                .padding(vertical = 16.dp, horizontal = 8.dp)
                        ) {
                            Text(
                                text = "Actions",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
                            )

                            DropdownMenuItem(
                                text = { Text("GATT Server", fontSize = 17.sp) },
                                onClick = {
                                    expanded = false
                                    onGattServerClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Storage, 
                                        contentDescription = null,
                                        modifier = Modifier.size(26.dp)
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BluetoothDeviceList(
    savedDevices: List<BluetoothDeviceDomain>,
    scannedDevices: List<BluetoothDeviceDomain>,
    onClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onSave: (BluetoothDeviceDomain) -> Unit,
    onForgetSaved: (BluetoothDeviceDomain) -> Unit,
    onCheckReachability: (BluetoothDeviceDomain) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 80.dp)
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
            BluetoothDeviceItem(
                device = device,
                onClick = onClick,
                onDetailsClick = onDetailsClick,
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
    onForget: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCheckReachability: (() -> Unit)? = null,
    isSaved: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val isEnabled = (!isSaved) || device.isInRange
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
                if (isSaved && !device.isInRange) {
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
