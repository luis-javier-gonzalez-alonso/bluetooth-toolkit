package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectionScreen(
    device: BluetoothDeviceDomain?,
    isConnected: Boolean,
    logs: List<BluetoothDataPacket>,
    onBackClick: () -> Unit,
    onDisconnectClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text(text = device?.name ?: "Unknown Device", style = MaterialTheme.typography.titleMedium)
                        Text(text = if (isConnected) "Connected" else "Connecting...", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onDisconnectClick) {
                        Text("Disconnect")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Profile Options Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Available Profiles", fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Mocking profile buttons based on UUIDs or device type
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfileButton("GATT")
                        ProfileButton("SPP")
                        ProfileButton("A2DP")
                    }
                }
            }

            // Hex/ASCII Log Section
            Text(
                text = "Data Log",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleSmall
            )
            
            HexLogView(
                logs = logs,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surface)
            )
        }
    }
}

@Composable
fun ProfileButton(name: String) {
    OutlinedButton(
        onClick = { /* TODO: Implement profile interaction */ },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(name, fontSize = 12.sp)
    }
}

@Composable
fun HexLogView(
    logs: List<BluetoothDataPacket>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.padding(horizontal = 8.dp)
    ) {
        items(logs) { packet ->
            LogEntry(packet)
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun LogEntry(packet: BluetoothDataPacket) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(packet.timestamp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.secondary
        )
        
        Row(modifier = Modifier.fillMaxWidth()) {
            // Hex column
            Text(
                text = packet.data.toHexString(),
                modifier = Modifier.weight(1.5f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // ASCII column
            Text(
                text = packet.data.toAsciiString(),
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

fun ByteArray.toHexString(): String {
    return joinToString(" ") { "%02X".format(it) }
}

fun ByteArray.toAsciiString(): String {
    return String(map { if (it in 32..126) it.toByte() else '.'.toByte() }.toByteArray())
}
