package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import net.ljga.projects.apps.bttk.data.local.database.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.local.database.Endianness
import net.ljga.projects.apps.bttk.data.local.database.FieldType
import net.ljga.projects.apps.bttk.data.local.database.ParserField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacteristicParserDialog(
    serviceUuid: String,
    characteristicUuid: String,
    initialConfig: CharacteristicParserConfig?,
    onDismiss: () -> Unit,
    onSave: (CharacteristicParserConfig) -> Unit,
    onDelete: () -> Unit
) {
    var template by remember { mutableStateOf(initialConfig?.template ?: "") }
    var fields by remember { mutableStateOf(initialConfig?.fields ?: emptyList()) }
    var showAddField by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Characteristic Parser") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
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
                    IconButton(onClick = { showAddField = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Field")
                    }
                }

                LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                    items(fields) { field ->
                        FieldRow(field, onDelete = { fields = fields - field })
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(CharacteristicParserConfig(serviceUuid, characteristicUuid, fields, template))
                onDismiss()
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (initialConfig != null) {
                    TextButton(onClick = {
                        onDelete()
                        onDismiss()
                    }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Delete")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )

    if (showAddField) {
        val lastField = fields.lastOrNull()
        val nextOffset = if (lastField != null) {
            lastField.offset + lastField.length
        } else 0

        AddFieldDialog(
            defaultOffset = nextOffset,
            onDismiss = { showAddField = false },
            onAdd = { field ->
                fields = fields + field
                showAddField = false
            }
        )
    }
}

@Composable
fun FieldRow(field: ParserField, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(field.name, style = MaterialTheme.typography.bodyMedium, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text("Offset: ${field.offset}, Len: ${field.length}, Type: ${field.type}", style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(20.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFieldDialog(
    defaultOffset: Int,
    onDismiss: () -> Unit, 
    onAdd: (ParserField) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var offset by remember { mutableStateOf(defaultOffset.toString()) }
    var length by remember { mutableStateOf("1") }
    var type by remember { mutableStateOf(FieldType.U8) }
    var endianness by remember { mutableStateOf(Endianness.LITTLE_ENDIAN) }

    var typeExpanded by remember { mutableStateOf(false) }
    var endianExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Field") },
        text = {
            Column {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Field Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = offset, 
                        onValueChange = { offset = it }, 
                        label = { Text("Offset") }, 
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = length, 
                        onValueChange = { length = it }, 
                        label = { Text("Length") }, 
                        modifier = Modifier.weight(1f),
                        enabled = type.length == null
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ExposedDropdownMenuBox(expanded = typeExpanded, onExpandedChange = { typeExpanded = it }) {
                    OutlinedTextField(
                        value = type.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                        FieldType.entries.forEach { fieldType ->
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
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = endianExpanded, onDismissRequest = { endianExpanded = false }) {
                        Endianness.entries.forEach { end ->
                            DropdownMenuItem(text = { Text(end.name) }, onClick = { endianness = end; endianExpanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onAdd(ParserField(name, offset.toIntOrNull() ?: 0, length.toIntOrNull() ?: 1, type, endianness))
            }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
