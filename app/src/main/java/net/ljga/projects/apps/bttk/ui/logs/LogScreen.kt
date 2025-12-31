package net.ljga.projects.apps.bttk.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor() : ViewModel() {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private var process: Process? = null

    init {
        startLogcat()
    }

    private fun startLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // We don't clear (logcat -c) here to allow viewing recent history
                process = Runtime.getRuntime().exec("logcat -v time")
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                
                val pid = android.os.Process.myPid().toString()
                
                while (isActive) {
                    val line = reader.readLine() ?: break
                    // Filter by PID (most reliable way to get app logs) or relevant tags
                    if (line.contains("($pid)") || 
                        line.contains(" $pid ") ||
                        line.contains("Gatt") || 
                        line.contains("Bluetooth") ||
                        line.contains("net.ljga.projects.apps.bttk")) {
                        
                        _logs.update { current ->
                            (current + line).takeLast(500) // Reduced buffer for better performance
                        }
                    }
                    // Small delay to prevent UI thread saturation if logs are flooding
                    if (_logs.value.size % 10 == 0) {
                        delay(5)
                    }
                }
            } catch (e: Exception) {
                _logs.update { it + "Error reading logs: ${e.message}" }
            }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                Runtime.getRuntime().exec("logcat -c")
            } catch (e: Exception) {
                // Ignore clear failures
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        process?.destroy()
        process = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onBackClick: () -> Unit,
    viewModel: LogViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("System Logs") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Logs")
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = Color.Black
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(logs) { log ->
                    Text(
                        text = log,
                        color = getLogColor(log),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(vertical = 1.dp)
                    )
                }
            }
        }
    }
}

fun getLogColor(log: String): Color {
    return when {
        log.contains(" E/") -> Color.Red
        log.contains(" W/") -> Color(0xFFFFA500) // Orange
        log.contains(" I/") -> Color.Cyan
        log.contains(" D/") -> Color.Green
        else -> Color.White
    }
}
