package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.BluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.local.database.BluetoothScript
import net.ljga.projects.apps.bttk.data.local.database.BluetoothScriptOperation
import javax.inject.Inject

@HiltViewModel
class BluetoothScriptViewModel @Inject constructor(
    private val scriptRepository: BluetoothScriptRepository,
    private val savedDeviceRepository: SavedDeviceRepository
) : ViewModel() {

    val allScripts: StateFlow<List<BluetoothScript>> = scriptRepository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knownDevices: StateFlow<List<BluetoothDeviceDomain>> = savedDeviceRepository.savedDevices
        .map { devices -> devices.filter { it.services.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScript = MutableStateFlow<BluetoothScript?>(null)
    val currentScript: StateFlow<BluetoothScript?> = _currentScript.asStateFlow()

    fun createNewScript() {
        _currentScript.value = BluetoothScript(name = "New Script", operations = emptyList())
    }

    fun editScript(script: BluetoothScript) {
        _currentScript.value = script
    }

    fun updateCurrentScriptName(name: String) {
        _currentScript.update { it?.copy(name = name) }
    }

    fun addOperation(operation: BluetoothScriptOperation) {
        _currentScript.update { script ->
            script?.copy(operations = script.operations + operation)
        }
    }

    fun removeOperation(index: Int) {
        _currentScript.update { script ->
            script?.copy(operations = script.operations.toMutableList().apply { removeAt(index) })
        }
    }
    
    fun updateOperation(index: Int, operation: BluetoothScriptOperation) {
        _currentScript.update { script ->
            script?.copy(operations = script.operations.toMutableList().apply { set(index, operation) })
        }
    }

    fun saveScript() {
        val script = _currentScript.value ?: return
        viewModelScope.launch {
            scriptRepository.saveScript(script)
            _currentScript.value = null
        }
    }
    
    fun deleteScript(id: Int) {
        viewModelScope.launch {
            scriptRepository.deleteScript(id)
        }
    }

    fun cancelEditing() {
        _currentScript.value = null
    }
}
