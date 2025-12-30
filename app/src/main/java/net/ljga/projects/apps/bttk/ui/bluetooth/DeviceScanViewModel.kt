package net.ljga.projects.apps.bttk.ui.bluetooth

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.domain.DeviceScanController
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicSettingsRepository
import javax.inject.Inject

@HiltViewModel
class DeviceScanViewModel @Inject constructor(
    private val deviceScanController: DeviceScanController,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val gattCharacteristicSettingsRepository: GattCharacteristicSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ServiceScanUiState())
    
    private val devicesFlow = combine(
        deviceScanController.scannedDevices,
        bluetoothDeviceRepository.savedDevices
    ) { scanned, saved ->
        Pair(scanned, saved)
    }

    val state = combine(
        devicesFlow,
        gattCharacteristicSettingsRepository.allSettings,
        _state
    ) { (scannedDevices, savedDevices), gattCharacteristicSettings, state ->

        val aliases = gattCharacteristicSettings.associate { "${it.serviceUuid}-${it.characteristicUuid}" to it.alias }

        val allAddresses = (scannedDevices.map { it.address } +
                savedDevices.map { it.address }).distinct()

        val mergedDevicesMap = allAddresses.associateWith { address ->
            val scanned = scannedDevices.find { it.address == address }
            val saved = savedDevices.find { it.address == address }

            val allForAddress = listOfNotNull(scanned, saved)

            BluetoothDeviceDomain(
                address = address,
                name = allForAddress.mapNotNull { it.name }.firstOrNull { it.isNotBlank() },
                isInRange = allForAddress.any { it.isInRange },
                bondState = saved?.bondState ?: scanned?.bondState ?: 10,
                type = allForAddress.map { it.type }.firstOrNull { it != 0 } ?: 0,
                uuids = allForAddress.flatMap { it.uuids }.distinct(),
                rssi = saved?.rssi ?: scanned?.rssi,
                services = allForAddress.firstOrNull { it.services.isNotEmpty() }?.services ?: emptyList()
            )
        }

        val updatedSaved = savedDevices.mapNotNull { mergedDevicesMap[it.address] }
        val savedAddresses = updatedSaved.map { it.address }.toSet()

        val filteredScannedDevices = scannedDevices
            .mapNotNull { mergedDevicesMap[it.address] }
            .filter { it.address !in savedAddresses }

        val selectedDevice = state.selectedDevice?.let { mergedDevicesMap[it.address] } ?: state.selectedDevice

        state.copy(
            scannedDevices = filteredScannedDevices,
            savedDevices = updatedSaved,
            selectedDevice = selectedDevice,
            gattAliases = aliases
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ServiceScanUiState())

    init {
        deviceScanController.isScanning.onEach { isScanning ->
            _state.update { it.copy(isRefreshing = isScanning) }
        }.launchIn(viewModelScope)

        deviceScanController.errors.onEach { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            _state.update {
                it.copy(errorMessage = error)
            }
        }.launchIn(viewModelScope)
    }

    fun startScan() {
        val success = deviceScanController.startDiscovery()
        if (!success) {
            Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopScan() {
        deviceScanController.stopDiscovery()
    }

    fun checkReachability(device: BluetoothDeviceDomain) {
        deviceScanController.checkReachability(device.address)
    }

    fun saveDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            bluetoothDeviceRepository.saveDevice(device)
        }
    }

    fun forgetSavedDevice(device: BluetoothDeviceDomain) {
        viewModelScope.launch {
            bluetoothDeviceRepository.forgetDevice(device.address)
        }
    }

    fun showDeviceDetails(device: BluetoothDeviceDomain?) {
        _state.update { it.copy(selectedDevice = device) }
    }
}

data class ServiceScanUiState(
    val scannedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val savedDevices: List<BluetoothDeviceDomain> = emptyList(),
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedDevice: BluetoothDeviceDomain? = null,
    val gattAliases: Map<String, String> = emptyMap()
)
