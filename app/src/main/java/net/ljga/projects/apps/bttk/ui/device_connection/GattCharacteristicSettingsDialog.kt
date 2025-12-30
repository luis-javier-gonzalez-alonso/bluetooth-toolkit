package net.ljga.projects.apps.bttk.ui.device_connection

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import net.ljga.projects.apps.bttk.domain.model.EndiannessDomain
import net.ljga.projects.apps.bttk.domain.model.FieldTypeDomain
import net.ljga.projects.apps.bttk.domain.model.GattCharacteristicSettingsDomain
import net.ljga.projects.apps.bttk.domain.model.ParserFieldDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GattCharacteristicSettingsDialog(
    serviceUuid: String,
    characteristicUuid: String,
    initialSettings: GattCharacteristicSettingsDomain?,
    onSave: (settings: GattCharacteristicSettingsDomain) -> Unit,
    onDismiss: () -> Unit
) {
    var alias by remember { mutableStateOf(initialSettings?.alias ?: "") }
    var template by remember { mutableStateOf(initialSettings?.template ?: "") }
    var fields by remember { mutableStateOf(initialSettings?.fields ?: emptyList()) }
    var showFieldDialog by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }

    val updateOffsets = { list: List<ParserFieldDomain> ->
        var currentOffset = 0
        list.map { field ->
            val updated = field.copy(offset = currentOffset)
            currentOffset += updated.length
            updated
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Characteristic Settings") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = alias,
                    onValueChange = { alias = it },
                    label = { Text("Alias") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Parser", style = MaterialTheme.typography.titleSmall)
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = template,
                    onValueChange = { template = it },
                    label = { Text("Template (e.g. Value is {val1})") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fields", style = MaterialTheme.typography.titleSmall)
                    IconButton(onClick = { 
                        editIndex = null
                        showFieldDialog = true 
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(fields) { index, field ->
                        FieldRow(
                            field,
                            onDelete = { 
                                fields = updateOffsets(fields.toMutableList().apply { removeAt(index) })
                            },
                            onEdit = { 
                                editIndex = index
                                showFieldDialog = true 
                            })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    GattCharacteristicSettingsDomain(
                        serviceUuid,
                        characteristicUuid,
                        alias,
                        fields,
                        template
                    )
                )
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    if (showFieldDialog) {
        FieldDialog(
            field = editIndex?.let { fields[it] },
            onDismiss = { showFieldDialog = false },
            onConfirm = { field ->
                val newList = fields.toMutableList()
                if (editIndex != null) {
                    newList[editIndex!!] = field
                } else {
                    newList.add(field)
                }
                fields = updateOffsets(newList)
                showFieldDialog = false
            }
        )
    }
}

@Composable
fun FieldRow(field: ParserFieldDomain, onDelete: () -> Unit, onEdit: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("Offset: ${field.offset}, Type: ${field.type}, Len: ${field.length}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FieldDialog(
    field: ParserFieldDomain? = null,
    onDismiss: () -> Unit,
    onConfirm: (ParserFieldDomain) -> Unit
) {
    var name by remember { mutableStateOf(field?.name ?: "") }
    var length by remember { mutableStateOf(field?.length?.toString() ?: "1") }
    var type by remember { mutableStateOf(field?.type ?: FieldTypeDomain.U8) }
    var endianness by remember { mutableStateOf(field?.endianness ?: EndiannessDomain.LITTLE_ENDIAN) }

    var typeExpanded by remember { mutableStateOf(false) }
    var endianExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (field == null) "Add Field" else "Edit Field") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Field Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = length,
                    onValueChange = { length = it },
                    label = { Text("Length") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = type.length == null
                )

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        FieldTypeDomain.entries.forEach { fieldType ->
                            DropdownMenuItem(
                                text = { Text(fieldType.name) },
                                onClick = {
                                    type = fieldType
                                    fieldType.length?.let { length = it.toString() }
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                ExposedDropdownMenuBox(expanded = endianExpanded, onExpandedChange = { endianExpanded = it }) {
                    OutlinedTextField(
                        value = endianness.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Endianness") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endianExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = endianExpanded, onDismissRequest = { endianExpanded = false }) {
                        EndiannessDomain.entries.forEach { end ->
                            DropdownMenuItem(text = { Text(end.name) }, onClick = { endianness = end; endianExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    ParserFieldDomain(
                        name = name,
                        offset = 0, // Offset will be recalculated by parent
                        length = length.toIntOrNull() ?: 0,
                        type = type,
                        endianness = endianness
                    )
                )
            }, enabled = name.isNotBlank()) {
                Text(if (field == null) "Add" else "Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
