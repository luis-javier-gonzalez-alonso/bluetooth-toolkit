package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.utils.prettyCharacteristicName
import net.ljga.projects.apps.bttk.database.entities.BluetoothScript
import net.ljga.projects.apps.bttk.database.entities.BluetoothScriptOperation
import net.ljga.projects.apps.bttk.database.entities.ScriptOperationType
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@Composable
fun BluetoothScriptDialog(
    viewModel: BluetoothScriptViewModel,
    onScriptSelected: (BluetoothScript) -> Unit,
    onDismiss: () -> Unit
) {
    val scripts by viewModel.allScripts.collectAsState()
    val currentScript by viewModel.currentScript.collectAsState()
    val knownDevices by viewModel.knownDevices.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column {
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                        Text("Select Script", modifier = Modifier.padding(16.dp))
                    }
                    Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                        Text("Edit/Create", modifier = Modifier.padding(16.dp))
                    }
                }

                if (selectedTab == 0) {
                    ScriptListTab(
                        scripts = scripts,
                        onSelect = onScriptSelected,
                        onEdit = {
                            viewModel.editScript(it)
                            selectedTab = 1
                        },
                        onDelete = viewModel::deleteScript,
                        onNew = {
                            viewModel.createNewScript()
                            selectedTab = 1
                        },
                        onClose = onDismiss
                    )
                } else {
                    ScriptEditTab(
                        script = currentScript,
                        knownDevices = knownDevices,
                        onSave = {
                            viewModel.saveScript()
                            selectedTab = 0
                        },
                        onCancel = {
                            viewModel.cancelEditing()
                            selectedTab = 0
                        },
                        onUpdateName = viewModel::updateCurrentScriptName,
                        onAddOperation = viewModel::addOperation,
                        onUpdateOperation = viewModel::updateOperation,
                        onRemoveOperation = viewModel::removeOperation
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptListTab(
    scripts: List<BluetoothScript>,
    onSelect: (BluetoothScript) -> Unit,
    onEdit: (BluetoothScript) -> Unit,
    onDelete: (Int) -> Unit,
    onNew: () -> Unit,
    onClose: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Available Scripts") },
                actions = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNew) {
                Icon(Icons.Default.Add, contentDescription = "New Script")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(scripts) { script ->
                ListItem(
                    headlineContent = { Text(script.name) },
                    supportingContent = { Text("${script.operations.size} operations") },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEdit(script) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                            IconButton(onClick = { onDelete(script.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    },
                    modifier = Modifier.clickable { onSelect(script) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditTab(
    script: BluetoothScript?,
    knownDevices: List<BluetoothDeviceDomain>,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onUpdateName: (String) -> Unit,
    onAddOperation: (BluetoothScriptOperation) -> Unit,
    onUpdateOperation: (Int, BluetoothScriptOperation) -> Unit,
    onRemoveOperation: (Int) -> Unit
) {
    if (script == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Select a script to edit or create a new one")
        }
        return
    }

    var showAddOpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    TextField(
                        value = script.name,
                        onValueChange = onUpdateName,
                        label = { Text("Script Name") },
                        singleLine = true
                    )
                },
                actions = {
                    IconButton(onClick = onSave) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddOpDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Operation")
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = padding) {
            items(script.operations.size) { index ->
                val op = script.operations[index]
                ListItem(
                    headlineContent = { Text(op.type.name) },
                    supportingContent = {
                        Column {
                            op.serviceUuid?.let { Text("Service: $it") }
                            op.characteristicUuid?.let { Text("Char: ${it.prettyCharacteristicName()}") }
                            op.data?.let { Text("Data: ${it.joinToString("") { "%02X".format(it) }}") }
                            op.delayMs?.let { Text("Delay: ${it}ms") }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = { onRemoveOperation(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove")
                        }
                    }
                )
            }
        }
    }

    if (showAddOpDialog) {
        AddOperationDialog(
            knownDevices = knownDevices,
            onDismiss = { showAddOpDialog = false },
            onAdd = {
                onAddOperation(it)
                showAddOpDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOperationDialog(
    knownDevices: List<BluetoothDeviceDomain>,
    onDismiss: () -> Unit,
    onAdd: (BluetoothScriptOperation) -> Unit
) {
    var type by remember { mutableStateOf(ScriptOperationType.READ) }
    var serviceUuid by remember { mutableStateOf("") }
    var charUuid by remember { mutableStateOf("") }
    var dataHex by remember { mutableStateOf("") }
    var delayMs by remember { mutableStateOf("") }

    var selectedDevice by remember { mutableStateOf<BluetoothDeviceDomain?>(null) }
    var deviceMenuExpanded by remember { mutableStateOf(false) }
    var serviceMenuExpanded by remember { mutableStateOf(false) }
    var charMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Operation") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = type == ScriptOperationType.READ, onClick = { type = ScriptOperationType.READ })
                    Text("Read")
                    RadioButton(selected = type == ScriptOperationType.WRITE, onClick = { type = ScriptOperationType.WRITE })
                    Text("Write")
                    RadioButton(selected = type == ScriptOperationType.DELAY, onClick = { type = ScriptOperationType.DELAY })
                    Text("Delay")
                }

                if (type != ScriptOperationType.DELAY) {
                    Text("Quick Select from Device", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 8.dp))
                    ExposedDropdownMenuBox(
                        expanded = deviceMenuExpanded,
                        onExpandedChange = { deviceMenuExpanded = it }
                    ) {
                        TextField(
                            value = selectedDevice?.name ?: selectedDevice?.address ?: "Select Device",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = deviceMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = deviceMenuExpanded,
                            onDismissRequest = { deviceMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("None") },
                                onClick = {
                                    selectedDevice = null
                                    deviceMenuExpanded = false
                                }
                            )
                            knownDevices.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device.name ?: device.address) },
                                    onClick = {
                                        selectedDevice = device
                                        deviceMenuExpanded = false
                                        serviceUuid = ""
                                        charUuid = ""
                                    }
                                )
                            }
                        }
                    }

                    if (selectedDevice != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        ExposedDropdownMenuBox(
                            expanded = serviceMenuExpanded,
                            onExpandedChange = { serviceMenuExpanded = it }
                        ) {
                            TextField(
                                value = serviceUuid.ifEmpty { "Select Service" },
                                onValueChange = { serviceUuid = it },
                                label = { Text("Service UUID") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceMenuExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = serviceMenuExpanded,
                                onDismissRequest = { serviceMenuExpanded = false }
                            ) {
                                selectedDevice!!.services.forEach { service ->
                                    DropdownMenuItem(
                                        text = { Text(service.uuid) },
                                        onClick = {
                                            serviceUuid = service.uuid
                                            serviceMenuExpanded = false
                                            charUuid = ""
                                        }
                                    )
                                }
                            }
                        }

                        val selectedService = selectedDevice!!.services.find { it.uuid == serviceUuid }
                        if (selectedService != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = charMenuExpanded,
                                onExpandedChange = { charMenuExpanded = it }
                            ) {
                                TextField(
                                    value = charUuid.ifEmpty { "Select Characteristic" },
                                    onValueChange = { charUuid = it },
                                    label = { Text("Characteristic UUID") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = charMenuExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = charMenuExpanded,
                                    onDismissRequest = { charMenuExpanded = false }
                                ) {
                                    selectedService.characteristics.forEach { char ->
                                        DropdownMenuItem(
                                            text = { Text(char.uuid.prettyCharacteristicName()) },
                                            onClick = {
                                                charUuid = char.uuid
                                                charMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        TextField(value = serviceUuid, onValueChange = { serviceUuid = it }, label = { Text("Service UUID") }, modifier = Modifier.fillMaxWidth())
                        TextField(value = charUuid, onValueChange = { charUuid = it }, label = { Text("Characteristic UUID") }, modifier = Modifier.fillMaxWidth())
                    }
                }
                
                if (type == ScriptOperationType.WRITE) {
                    TextField(value = dataHex, onValueChange = { dataHex = it }, label = { Text("Data (Hex)") }, modifier = Modifier.fillMaxWidth())
                }

                if (type == ScriptOperationType.DELAY) {
                    TextField(value = delayMs, onValueChange = { delayMs = it }, label = { Text("Delay (ms)") }, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val data = if (type == ScriptOperationType.WRITE) {
                    try {
                        dataHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                    } catch (e: Exception) { null }
                } else null
                
                onAdd(BluetoothScriptOperation(
                    type = type,
                    serviceUuid = if (type != ScriptOperationType.DELAY) serviceUuid else null,
                    characteristicUuid = if (type != ScriptOperationType.DELAY) charUuid else null,
                    data = data,
                    delayMs = delayMs.toLongOrNull()
                ))
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
