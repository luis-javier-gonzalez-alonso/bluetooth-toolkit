package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothController
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val savedDeviceRepository: SavedDeviceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    val state = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        savedDeviceRepository.savedDevices,
        bluetoothController.isScanning,
        _state
    ) { scannedDevices, pairedDevices, savedDevices, isScanning, state ->
        
        // Merge information for all unique addresses to ensure consistency
        val allAddresses = (scannedDevices.map { it.address } + 
                            pairedDevices.map { it.address } + 
                            savedDevices.map { it.address }).distinct()
        
        val mergedDevicesMap = allAddresses.associateWith { address ->
            val scanned = scannedDevices.find { it.address == address }
            val paired = pairedDevices.find { it.address == address }
            val saved = savedDevices.find { it.address == address }
            
            val allForAddress = listOfNotNull(scanned, paired, saved)
            
            BluetoothDeviceDomain(
                address = address,
                // Pick the first non-null, non-blank name available
                name = allForAddress.mapNotNull { it.name }.firstOrNull { it.isNotBlank() },
                // Device is in range if any source says it is
                isInRange = allForAddress.any { it.isInRange },
                // System bond state is authoritative if available (from paired or scanned)
                bondState = paired?.bondState ?: scanned?.bondState ?: saved?.bondState ?: 10,
                // Type is usually consistent, pick first non-zero
                type = allForAddress.map { it.type }.firstOrNull { it != 0 } ?: 0,
                // Accumulate all known UUIDs
                uuids = allForAddress.flatMap { it.uuids }.distinct(),
                // RSSI is most accurate from scanned devices
                rssi = scanned?.rssi ?: paired?.rssi ?: saved?.rssi
            )
        }

        val updatedPaired = pairedDevices.mapNotNull { mergedDevicesMap[it.address] }
        val updatedSaved = savedDevices.mapNotNull { mergedDevicesMap[it.address] }
        
        val pairedAddresses = updatedPaired.map { it.address }.toSet()
        val savedAddresses = updatedSaved.map { it.address }.toSet()

        // Scanned devices only show what is NOT already in paired or saved lists
        val filteredScannedDevices = scannedDevices
            .mapNotNull { mergedDevicesMap[it.address] }
            .filter { it.address !in pairedAddresses && it.address !in savedAddresses }

        state.copy(
            scannedDevices = filteredScannedDevices,
            pairedDevices = updatedPaired,
            savedDevices = updatedSaved,
            isRefreshing = isScanning
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error) }
        }.launchIn(viewModelScope)
    }

    fun startScan() {
        bluetoothController.startDiscovery()
    }

    fun stopScan() {
        bluetoothController.stopDiscovery()
    }

    fun checkReachability(device: BluetoothDeviceDomain) {
        bluetoothController.checkReachability(device.address)
    }

    fun connectToDevice(device: BluetoothDeviceDomain) {
        _state.update { it.copy(isConnecting = true) }
        bluetoothController.connectToDevice(device)
    }

    fun disconnectFromDevice() {
        bluetoothController.disconnect()
    }

    fun pairDevice(device: BluetoothDeviceDomain) {
        bluetoothController.pairDevice(device.address)
    }
    
    fun forgetDevice(device: BluetoothDeviceDomain) {
        bluetoothController.forgetDevice(device.address)
    }

    fun saveDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            savedDeviceRepository.saveDevice(device)
        }
    }

    fun forgetSavedDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            savedDeviceRepository.forgetDevice(device.address)
        }
    }

    fun showDeviceDetails(device: BluetoothDeviceDomain?) {
        _state.update { it.copy(selectedDevice = device) }
    }
    
    override fun onCleared() {
        super.onCleared()
        bluetoothController.release()
    }
}

data class BluetoothUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val pairedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val savedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedDevice: BluetoothDeviceDomain? = null
)
