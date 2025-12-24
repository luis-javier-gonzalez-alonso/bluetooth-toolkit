package net.ljga.projects.apps.bttk.ui.bluetooth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothController
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothProfile
import net.ljga.projects.apps.bttk.data.bluetooth.DataFormat
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
        savedDeviceRepository.gattAliases,
        _state
    ) { scannedDevices, pairedDevices, savedDevices, aliases, state ->

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
                name = allForAddress.mapNotNull { it.name }.firstOrNull { it.isNotBlank() },
                isInRange = allForAddress.any { it.isInRange },
                bondState = paired?.bondState ?: scanned?.bondState ?: saved?.bondState ?: 10,
                type = allForAddress.map { it.type }.firstOrNull { it != 0 } ?: 0,
                uuids = allForAddress.flatMap { it.uuids }.distinct(),
                rssi = scanned?.rssi ?: paired?.rssi ?: saved?.rssi,
                services = allForAddress.firstOrNull { it.services.isNotEmpty() }?.services ?: emptyList()
            )
        }

        val updatedPaired = pairedDevices.mapNotNull { mergedDevicesMap[it.address] }
        val updatedSaved = savedDevices.mapNotNull { mergedDevicesMap[it.address] }

        val pairedAddresses = updatedPaired.map { it.address }.toSet()
        val savedAddresses = updatedSaved.map { it.address }.toSet()

        val filteredScannedDevices = scannedDevices
            .mapNotNull { mergedDevicesMap[it.address] }
            .filter { it.address !in pairedAddresses && it.address !in savedAddresses }

        val selectedDevice = state.selectedDevice?.let { mergedDevicesMap[it.address] } ?: state.selectedDevice

        state.copy(
            scannedDevices = filteredScannedDevices,
            pairedDevices = updatedPaired,
            savedDevices = updatedSaved,
            selectedDevice = selectedDevice,
            gattAliases = aliases
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            _state.update { it.copy(isConnected = isConnected, isConnecting = if (isConnected) false else it.isConnecting) }
        }.launchIn(viewModelScope)

        bluetoothController.isScanning.onEach { isScanning ->
            _state.update { it.copy(isRefreshing = isScanning) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error, isConnecting = false) }
        }.launchIn(viewModelScope)

        bluetoothController.incomingData.onEach { packet ->
            if (packet.format == DataFormat.GATT_STRUCTURE && packet.gattServices != null) {
                state.value.selectedDevice?.let { device ->
                    savedDeviceRepository.updateServices(device.address, packet.gattServices)
                }
            }
            _state.update { it.copy(dataLogs = (it.dataLogs + packet).takeLast(100)) }
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

    fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothProfile? = null) {
        val availableProfiles = device.uuids.mapNotNull { BluetoothProfile.fromUuid(it) }.distinct()
        
        if (profile == null && availableProfiles.size > 1) {
            _state.update { it.copy(selectedDevice = device, profilesToSelect = availableProfiles) }
        } else {
            _state.update { it.copy(isConnecting = true, selectedDevice = device, profilesToSelect = emptyList()) }
            bluetoothController.connectToDevice(device, profile)
        }
    }

    fun dismissProfileSelection() {
        _state.update { it.copy(profilesToSelect = emptyList()) }
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

    fun refreshPairedDevices() {
        bluetoothController.refreshPairedDevices()
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        bluetoothController.readCharacteristic(serviceUuid, characteristicUuid)
    }

    fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {
        _state.update { currentState ->
            val key = "$serviceUuid-$characteristicUuid"
            val newEnabledNotifications = if (enable) {
                currentState.enabledNotifications + key
            } else {
                currentState.enabledNotifications - key
            }
            currentState.copy(enabledNotifications = newEnabledNotifications)
        }
        bluetoothController.toggleNotification(serviceUuid, characteristicUuid, enable)
    }

    fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        viewModelScope.launch {
            savedDeviceRepository.saveAlias(serviceUuid, characteristicUuid, alias)
        }
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
    val selectedDevice: BluetoothDeviceDomain? = null,
    val dataLogs: List<BluetoothDataPacket> = emptyList(),
    val profilesToSelect: List<BluetoothProfile> = emptyList(),
    val enabledNotifications: Set<String> = emptySet(),
    val gattAliases: Map<String, String> = emptyMap()
)
