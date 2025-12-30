package net.ljga.projects.apps.bttk.ui.device_connection

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
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.repository.GattScriptRepository
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptOperationDomain
import javax.inject.Inject

@HiltViewModel
class BluetoothScriptViewModel @Inject constructor(
    private val scriptRepository: GattScriptRepository,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository
) : ViewModel() {

    val allScripts: StateFlow<List<BluetoothScriptDomain>> = scriptRepository.getAllScripts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val knownDevices: StateFlow<List<BluetoothDeviceDomain>> = bluetoothDeviceRepository.savedDevices
        .map { devices -> devices.filter { it.services.isNotEmpty() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentScript = MutableStateFlow<BluetoothScriptDomain?>(null)
    val currentScript: StateFlow<BluetoothScriptDomain?> = _currentScript.asStateFlow()

    fun createNewScript() {
        _currentScript.value = BluetoothScriptDomain(name = "New Script", operations = emptyList())
    }

    fun editScript(script: BluetoothScriptDomain) {
        _currentScript.value = script
    }

    fun updateCurrentScriptName(name: String) {
        _currentScript.update { it?.copy(name = name) }
    }

    fun addOperation(operation: BluetoothScriptOperationDomain) {
        _currentScript.update { script ->
            script?.copy(operations = (script.operations) + operation)
        }
    }

    fun removeOperation(index: Int) {
        _currentScript.update { script ->
            script?.copy(operations = script.operations.toMutableList().apply { removeAt(index) })
        }
    }
    
    fun updateOperation(index: Int, operation: BluetoothScriptOperationDomain) {
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
