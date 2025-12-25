package net.ljga.projects.apps.bttk.ui.bluetooth

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.DataFrameRepository
import net.ljga.projects.apps.bttk.data.GattServerRepository
import net.ljga.projects.apps.bttk.data.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothController
import net.ljga.projects.apps.bttk.data.bluetooth.GattServerService
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothProfile
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.DataFormat
import net.ljga.projects.apps.bttk.data.local.database.DataFrame
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val savedDeviceRepository: SavedDeviceRepository,
    private val dataFrameRepository: DataFrameRepository,
    private val gattServerRepository: GattServerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(BluetoothUiState())
    
    private var nextServiceIndex = 0
    private val serviceIndices = mutableMapOf<String, Int>()
    private val serviceNextCharIndices = mutableMapOf<String, Int>()

    private val devicesFlow = combine(
        bluetoothController.scannedDevices,
        bluetoothController.pairedDevices,
        savedDeviceRepository.savedDevices
    ) { scanned, paired, saved ->
        Triple(scanned, paired, saved)
    }

    private val gattServerFlow = combine(
        bluetoothController.isGattServerRunning,
        bluetoothController.gattServerServices
    ) { isRunning, services ->
        isRunning to services
    }

    val state = combine(
        devicesFlow,
        savedDeviceRepository.gattAliases,
        dataFrameRepository.dataFrames,
        gattServerFlow,
        _state
    ) { (scannedDevices, pairedDevices, savedDevices), aliases, dataFrames, (isGattServerRunning, gattServerServices), state ->

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
            gattAliases = aliases,
            savedDataFrames = dataFrames,
            isGattServerRunning = isGattServerRunning,
            gattServerServices = gattServerServices,
            localAddress = bluetoothController.localAddress
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

        // Load saved GATT Server config
        viewModelScope.launch {
            val data = gattServerRepository.config.first()
            nextServiceIndex = data.nextServiceIndex
            serviceIndices.putAll(data.serviceIndices)
            serviceNextCharIndices.putAll(data.serviceNextCharIndices)
            data.services.forEach { service ->
                bluetoothController.addGattService(service)
            }
        }
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

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        bluetoothController.writeCharacteristic(serviceUuid, characteristicUuid, data)
    }

    fun readDescriptors(serviceUuid: String, characteristicUuid: String) {
        bluetoothController.readDescriptors(serviceUuid, characteristicUuid)
    }

    fun saveDataFrame(name: String, data: ByteArray) {
        viewModelScope.launch {
            dataFrameRepository.add(name, data)
        }
    }

    fun deleteDataFrame(uid: Int) {
        viewModelScope.launch {
            dataFrameRepository.remove(uid)
        }
    }

    // GATT Server Actions
    fun toggleGattServer() {
        if (state.value.isGattServerRunning) {
            val intent = Intent(context, GattServerService::class.java).apply {
                action = GattServerService.ACTION_STOP
            }
            context.startService(intent)
            _state.update { it.copy(dataLogs = emptyList()) }
        } else {
            _state.update { it.copy(dataLogs = emptyList()) }
            val intent = Intent(context, GattServerService::class.java).apply {
                action = GattServerService.ACTION_START
            }
            context.startForegroundService(intent)
        }
    }

    fun clearGattServer() {
        bluetoothController.clearGattServices()
        nextServiceIndex = 0
        serviceIndices.clear()
        serviceNextCharIndices.clear()
        saveGattServerConfig()
    }

    fun addGattService(uuid: String = UUID.randomUUID().toString()) {
        val newService = BluetoothServiceDomain(
            uuid = uuid,
            characteristics = emptyList()
        )
        serviceIndices[uuid] = nextServiceIndex++
        serviceNextCharIndices[uuid] = 0
        bluetoothController.addGattService(newService)
        saveGattServerConfig()
    }

    fun removeGattService(serviceUuid: String) {
        bluetoothController.removeGattService(serviceUuid)
        // Note: we don't decrement nextServiceIndex to keep it incremental
        saveGattServerConfig()
    }

    fun generateCharacteristicUuid(serviceUuid: String): String {
        val sIndex = serviceIndices[serviceUuid] ?: 0
        val cIndex = serviceNextCharIndices[serviceUuid] ?: 0
        val baseUuid = serviceUuid.substring(8)
        val prefix = "a${sIndex.toString(16)}${cIndex.toString(16).padStart(2, '0')}".padStart(8, '0')
        return "$prefix$baseUuid"
    }

    fun addCharacteristicToService(
        serviceUuid: String, 
        charUuid: String,
        properties: List<String>,
        permissions: List<String>,
        initialValue: String? = null
    ) {
        val currentServices = state.value.gattServerServices
        val service = currentServices.find { it.uuid == serviceUuid } ?: return
        val cIndex = serviceNextCharIndices[serviceUuid] ?: 0
        
        serviceNextCharIndices[serviceUuid] = cIndex + 1
        
        val newChar = BluetoothCharacteristicDomain(
            uuid = charUuid,
            properties = properties,
            permissions = permissions,
            initialValue = initialValue
        )
        
        val updatedService = service.copy(
            characteristics = service.characteristics + newChar
        )
        
        bluetoothController.updateGattService(updatedService)
        saveGattServerConfig()
    }

    fun removeCharacteristicFromService(serviceUuid: String, charUuid: String) {
        val currentServices = state.value.gattServerServices
        val service = currentServices.find { it.uuid == serviceUuid } ?: return
        
        val updatedService = service.copy(
            characteristics = service.characteristics.filter { it.uuid != charUuid }
        )
        
        bluetoothController.updateGattService(updatedService)
        saveGattServerConfig()
    }

    private fun saveGattServerConfig() {
        viewModelScope.launch {
            gattServerRepository.saveConfig(
                bluetoothController.gattServerServices.value, 
                nextServiceIndex,
                serviceIndices,
                serviceNextCharIndices
            )
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
    val gattAliases: Map<String, String> = emptyMap(),
    val savedDataFrames: List<DataFrame> = emptyList(),
    val isGattServerRunning: Boolean = false,
    val gattServerServices: List<BluetoothServiceDomain> = emptyList(),
    val localAddress: String? = null
)
