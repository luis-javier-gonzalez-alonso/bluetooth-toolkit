package net.ljga.projects.apps.bttk.ui.device_scan

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun DeviceScanScreen(
    viewModel: DeviceScanViewModel,
//    scriptViewModel: BluetoothScriptViewModel = hiltViewModel(),
    onDeviceClick: (BluetoothDeviceDomain) -> Unit,
    onDetailsClick: (BluetoothDeviceDomain) -> Unit,
    onGattServerClick: () -> Unit,
    onAboutClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogsClick: () -> Unit
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

    SharedTransitionLayout {
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

            // Scrim overlay
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            expanded = false
                        }
                )
            }

            // Morphing FAB menu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                val sharedElementKey = "fab_to_menu"

                AnimatedContent(
                    targetState = expanded,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(220, delayMillis = 90)
                                ))
                            .togetherWith(fadeOut(animationSpec = tween(90)))
                    },
                    label = "FAB Morph"
                ) { isExpanded ->
                    if (!isExpanded) {
                        Surface(
                            onClick = { expanded = true },
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = sharedElementKey),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = { _, _ ->
                                        spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioLowBouncy
                                        )
                                    }
                                )
                                .size(56.dp),
                            shadowElevation = 6.dp
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Menu, contentDescription = "Actions")
                            }
                        }
                    } else {
                        val configuration = LocalConfiguration.current
                        val screenWidth = configuration.screenWidthDp.dp
                        val screenHeight = configuration.screenHeightDp.dp

                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(24.dp), // 50% rounding approximation for the sheet size
                            modifier = Modifier
                                .sharedBounds(
                                    rememberSharedContentState(key = sharedElementKey),
                                    animatedVisibilityScope = this@AnimatedContent,
                                    boundsTransform = { _, _ ->
                                        spring(
                                            stiffness = Spring.StiffnessMediumLow,
                                            dampingRatio = Spring.DampingRatioLowBouncy
                                        )
                                    }
                                )
                                .size(width = screenWidth * 0.5f, height = screenHeight * 0.5f),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "GATT Server",
                                            fontSize = 17.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onGattServerClick()
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 14.dp
                                    )
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "Settings",
                                            fontSize = 17.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onSettingsClick()
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 14.dp
                                    )
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "System Logs",
                                            fontSize = 17.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onLogsClick()
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 14.dp
                                    )
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            "About",
                                            fontSize = 17.sp,
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    onClick = {
                                        expanded = false
                                        onAboutClick()
                                    },
                                    contentPadding = PaddingValues(
                                        horizontal = 24.dp,
                                        vertical = 14.dp
                                    )
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
                text = " \u2304 Pull down to scan \u2304",
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
