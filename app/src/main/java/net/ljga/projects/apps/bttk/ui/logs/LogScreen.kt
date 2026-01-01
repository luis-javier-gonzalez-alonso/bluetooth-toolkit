package net.ljga.projects.apps.bttk.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import net.ljga.projects.apps.bttk.R
import net.ljga.projects.apps.bttk.data.database.AppDatabase
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

enum class LogLevel(val labelResId: Int, val pattern: String) {
    VERBOSE(R.string.log_level_verbose, " V/"),
    DEBUG(R.string.log_level_debug, " D/"),
    INFO(R.string.log_level_info, " I/"),
    WARN(R.string.log_level_warn, " W/"),
    ERROR(R.string.log_level_error, " E/")
}

@HiltViewModel
class LogViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {
    private val _allLogs = MutableStateFlow<List<String>>(emptyList())
    private val _selectedLevel = MutableStateFlow(LogLevel.INFO)
    
    val selectedLevel = _selectedLevel.asStateFlow()

    private val isFineGrainLoggingEnabled = database.appSettingsDao().getSettings()
        .map { it?.isFineGrainLoggingEnabled ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val logs = combine(_allLogs, _selectedLevel, isFineGrainLoggingEnabled) { logs, level, fineGrainEnabled ->
        val levelsToInclude = LogLevel.values().filter { 
            val isFineGrain = it == LogLevel.VERBOSE || it == LogLevel.DEBUG
            if (!fineGrainEnabled && isFineGrain) return@filter false
            it.ordinal >= level.ordinal
        }
        logs.filter { line ->
            levelsToInclude.any { line.contains(it.pattern) } || line.startsWith("System:")
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val availableLevels = isFineGrainLoggingEnabled.map { fineGrainEnabled ->
        LogLevel.values().filter { level ->
            if (!fineGrainEnabled && (level == LogLevel.VERBOSE || level == LogLevel.DEBUG)) false
            else true
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, LogLevel.values().filter { it != LogLevel.VERBOSE && it != LogLevel.DEBUG })

    private var process: Process? = null

    init {
        startLogcat()
    }

    private fun startLogcat() {
        viewModelScope.launch(Dispatchers.IO) {
            val pid = android.os.Process.myPid()
            _allLogs.update { it + "System: Starting log collection for PID $pid..." }
            
            try {
                process = Runtime.getRuntime().exec("logcat --pid=$pid -v time")
                val reader = BufferedReader(InputStreamReader(process?.inputStream))
                
                while (isActive) {
                    val line = reader.readLine() ?: break
                    _allLogs.update { current ->
                        (current + line).takeLast(1000)
                    }
                }
            } catch (e: Exception) {
                _allLogs.update { it + "System Error: ${e.message}" }
            }
        }
    }

    fun setLevel(level: LogLevel) {
        _selectedLevel.value = level
    }

    fun clearLogs() {
        _allLogs.value = listOf("System: Logs cleared")
        viewModelScope.launch(Dispatchers.IO) {
            try { Runtime.getRuntime().exec("logcat -c") } catch (e: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        process?.destroy()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onBackClick: () -> Unit,
    viewModel: LogViewModel = hiltViewModel()
) {
    val logs by viewModel.logs.collectAsState()
    val selectedLevel by viewModel.selectedLevel.collectAsState()
    val availableLevels by viewModel.availableLevels.collectAsState()
    val listState = rememberLazyListState()
    var showFilterMenu by remember { mutableStateOf(false) }

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.system_logs)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter))
                        }
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            availableLevels.forEach { level ->
                                DropdownMenuItem(
                                    text = { 
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = selectedLevel == level,
                                                onClick = null
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(stringResource(level.labelResId))
                                        }
                                    },
                                    onClick = {
                                        viewModel.setLevel(level)
                                        showFilterMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.clearLogs() }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.clear_logs))
                    }
                }
            )
        }
    ) { padding ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(padding),
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
                        fontSize = 11.sp,
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
        log.contains(" W/") -> Color(0xFFFFA500)
        log.contains(" I/") -> Color.Cyan
        log.contains(" D/") -> Color.Green
        else -> Color.White
    }
}
