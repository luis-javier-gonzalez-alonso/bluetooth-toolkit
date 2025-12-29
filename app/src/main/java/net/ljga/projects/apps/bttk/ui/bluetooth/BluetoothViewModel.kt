package net.ljga.projects.apps.bttk.ui.bluetooth

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.repository.CharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.data.repository.GattServerRepository
import net.ljga.projects.apps.bttk.data.repository.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothController
import net.ljga.projects.apps.bttk.data.bluetooth.GattServerService
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothProfile
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.DataFormat
import net.ljga.projects.apps.bttk.data.bluetooth.utils.CharacteristicParser
import net.ljga.projects.apps.bttk.data.database.entities.BluetoothScript
import net.ljga.projects.apps.bttk.data.database.entities.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.database.entities.DataFrame
import net.ljga.projects.apps.bttk.data.database.entities.ScriptOperationType
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothController: BluetoothController,
    private val savedDeviceRepository: SavedDeviceRepository,
    private val dataFrameRepository: DataFrameRepository,
    private val gattServerRepository: GattServerRepository,
    private val characteristicParserRepository: CharacteristicParserRepository,
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

    private val logsFlow = combine(
        bluetoothController.incomingData,
        bluetoothController.gattServerLogs,
        characteristicParserRepository.getAllConfigs()
    ) { incoming, gatt, configs -> 
        val configMap = configs.associateBy { "${it.serviceUuid}-${it.characteristicUuid}" }
        val enrichedIncoming = incoming.map { packet ->
            val config = configMap["${packet.serviceUuid}-${packet.characteristicUuid}"]
            if (config != null && packet.data.isNotEmpty()) {
                packet.copy(text = CharacteristicParser.parse(packet.data, config), format = DataFormat.STRUCTURED)
            } else {
                packet
            }
        }
        Triple(enrichedIncoming, gatt, configMap) 
    }

    private val repositoryFlow = combine(
        savedDeviceRepository.gattAliases,
        dataFrameRepository.dataFrames
    ) { aliases, dataFrames -> aliases to dataFrames }

    val state = combine(
        devicesFlow,
        repositoryFlow,
        gattServerFlow,
        logsFlow,
        _state
    ) { (scannedDevices, pairedDevices, savedDevices), (aliases, dataFrames), (isGattServerRunning, gattServerServices), (incomingData, gattServerLogs, configMap), state ->

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
            dataLogs = incomingData,
            gattServerLogs = gattServerLogs,
            localAddress = bluetoothController.localAddress,
            parserConfigs = configMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BluetoothUiState())

    init {
        bluetoothController.isConnected.onEach { isConnected ->
            val wasConnecting = _state.value.isConnecting
            _state.update { it.copy(isConnected = isConnected, isConnecting = if (isConnected) false else it.isConnecting) }
            
            if (isConnected && wasConnecting && _state.value.scriptToRun != null) {
                runScript(_state.value.scriptToRun!!)
            }
        }.launchIn(viewModelScope)

        bluetoothController.isScanning.onEach { isScanning ->
            _state.update { it.copy(isRefreshing = isScanning) }
        }.launchIn(viewModelScope)

        bluetoothController.errors.onEach { error ->
            _state.update { it.copy(errorMessage = error, isConnecting = false, scriptToRun = null) }
            if (error.contains("Bluetooth not available", ignoreCase = true)) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }.launchIn(viewModelScope)

        // Handle service discovery persistence
        bluetoothController.incomingData
            .mapNotNull { it.lastOrNull() }
            .onEach { packet ->
                if (packet.format == DataFormat.GATT_STRUCTURE && packet.gattServices != null) {
                    packet.source?.let { address ->
                        savedDeviceRepository.updateServices(address, packet.gattServices)
                    }
                }
            }.launchIn(viewModelScope)

        // Load saved GATT Server config
        viewModelScope.launch {
            val data = gattServerRepository.config.first()
            nextServiceIndex = data.nextServiceIndex
            serviceIndices.putAll(data.serviceIndices)
            serviceNextCharIndices.putAll(data.serviceNextCharIndices)
            _state.update { it.copy(gattServerDeviceName = data.deviceName ?: "") }
            data.services.forEach { service ->
                bluetoothController.addGattService(service)
            }
        }
    }

    private fun runScript(script: BluetoothScript) {
        viewModelScope.launch {
            _state.update { it.copy(scriptToRun = null) }
            bluetoothController.emitPacket(BluetoothDataPacket(
                data = byteArrayOf(),
                source = "Script",
                format = DataFormat.STRUCTURED,
                text = "Starting script: ${script.name}"
            ))
            
            script.operations.forEach { op ->
                when (op.type) {
                    ScriptOperationType.READ -> {
                        if (op.serviceUuid != null && op.characteristicUuid != null) {
                            bluetoothController.readCharacteristic(op.serviceUuid, op.characteristicUuid)
                        }
                    }
                    ScriptOperationType.WRITE -> {
                        if (op.serviceUuid != null && op.characteristicUuid != null && op.data != null) {
                            bluetoothController.writeCharacteristic(op.serviceUuid, op.characteristicUuid, op.data)
                        }
                    }
                    ScriptOperationType.DELAY -> {
                        op.delayMs?.let { delay(it) }
                    }
                }
                // Small delay between operations to avoid GATT congestion
                delay(100)
            }
            
            bluetoothController.emitPacket(BluetoothDataPacket(
                data = byteArrayOf(),
                source = "Script",
                format = DataFormat.STRUCTURED,
                text = "Script completed: ${script.name}"
            ))
        }
    }

    fun connectAndRunScript(device: BluetoothDeviceDomain, script: BluetoothScript) {
        _state.update { it.copy(isConnecting = true, selectedDevice = device, scriptToRun = script) }
        bluetoothController.connectToDevice(device, BluetoothProfile.GATT)
    }

    fun setGattServerDeviceName(name: String) {
        _state.update { it.copy(gattServerDeviceName = name) }
        saveGattServerConfig()
    }

    fun startScan() {
        val success = bluetoothController.startDiscovery()
        if (!success) {
            Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_SHORT).show()
        }
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

    fun saveParserConfig(config: CharacteristicParserConfig) {
        viewModelScope.launch {
            characteristicParserRepository.saveConfig(config)
        }
    }

    fun deleteParserConfig(serviceUuid: String, characteristicUuid: String) {
        viewModelScope.launch {
            characteristicParserRepository.deleteConfig(serviceUuid, characteristicUuid)
        }
    }

    // GATT Server Actions
    fun toggleGattServer() {
        if (state.value.isGattServerRunning) {
            val intent = Intent(context, GattServerService::class.java).apply {
                action = GattServerService.ACTION_STOP
            }
            context.startService(intent)
        } else {
            // Check if bluetooth is enabled before starting service
            val isEnabled = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter?.isEnabled == true
            if (!isEnabled) {
                Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(context, GattServerService::class.java).apply {
                action = GattServerService.ACTION_START
                if (state.value.gattServerDeviceName.isNotBlank()) {
                    putExtra(GattServerService.EXTRA_DEVICE_NAME, state.value.gattServerDeviceName)
                }
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
                serviceNextCharIndices,
                state.value.gattServerDeviceName
            )
        }
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
    val gattServerLogs: List<BluetoothDataPacket> = emptyList(),
    val profilesToSelect: List<BluetoothProfile> = emptyList(),
    val enabledNotifications: Set<String> = emptySet(),
    val gattAliases: Map<String, String> = emptyMap(),
    val savedDataFrames: List<DataFrame> = emptyList(),
    val isGattServerRunning: Boolean = false,
    val gattServerServices: List<BluetoothServiceDomain> = emptyList(),
    val localAddress: String? = null,
    val gattServerDeviceName: String = "",
    val parserConfigs: Map<String, CharacteristicParserConfig> = emptyMap(),
    val scriptToRun: BluetoothScript? = null
)
