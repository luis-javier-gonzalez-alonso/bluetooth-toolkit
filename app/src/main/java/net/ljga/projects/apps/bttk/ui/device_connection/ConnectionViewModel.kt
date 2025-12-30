package net.ljga.projects.apps.bttk.ui.device_connection

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.domain.ConnectionController
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import net.ljga.projects.apps.bttk.domain.model.GattCharacteristicSettingsDomain
import net.ljga.projects.apps.bttk.domain.model.ScriptOperationTypeDomain
import net.ljga.projects.apps.bttk.domain.model.process.ReadGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.model.process.WriteGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicSettingsRepository
import net.ljga.projects.apps.bttk.domain.utils.CharacteristicParser
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionController: ConnectionController,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository,
    private val dataFrameRepository: DataFrameRepository,
    private val gattCharacteristicSettingsRepository: GattCharacteristicSettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val connectionFlow = combine(
        connectionController.connectionLogs,
        connectionController.connections
    ) { logs, connections ->
        Pair(logs, connections)
    }

    private val _state = MutableStateFlow(ConnectionUiState())
    val state = combine(
        gattCharacteristicSettingsRepository.allSettings,
        dataFrameRepository.dataFrames,
        connectionFlow,
        bluetoothDeviceRepository.savedDevices,
        _state
    ) { allSettings, dataFrames, (connectionLogs, connections), savedDevices, state ->

        val address = state.selectedDevice?.address ?: return@combine state

        val currentDevice = savedDevices.find { it.address == address } ?: state.selectedDevice
        val isConnected = connections.containsKey(address)

        val settingsMap = allSettings.associateBy { "${it.serviceUuid}-${it.characteristicUuid}" }
        val aliases = settingsMap.mapValues { it.value.alias }.filterValues { it.isNotBlank() }

        val incomingData = connectionLogs[address]?.map { packet ->
            val settings = settingsMap["${packet.serviceUuid}-${packet.characteristicUuid}"]
            if (settings != null && settings.fields.isNotEmpty() && packet.data.isNotEmpty()) {
                val config = CharacteristicParserConfigDomain(
                    serviceUuid = settings.serviceUuid,
                    characteristicUuid = settings.characteristicUuid,
                    fields = settings.fields,
                    template = settings.template
                )
                packet.copy(
                    text = CharacteristicParser.parse(packet.data, config),
                    format = DataFormat.STRUCTURED
                )
            } else {
                packet
            }
        } ?: emptyList()

        state.copy(
            selectedDevice = currentDevice,
            isConnected = isConnected,
            isConnecting = !isConnected && state.isConnecting,
            gattAliases = aliases,
            savedDataFrames = dataFrames,
            dataLogs = incomingData,
            settings = settingsMap,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConnectionUiState())

    init {
        connectionController.errors.onEach { error ->
            _state.update {
                it.copy(
                    errorMessage = error,
                    isConnecting = false,
                    scriptToRun = null
                )
            }
            if (error.contains("Bluetooth not available", ignoreCase = true)) {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            }
        }.launchIn(viewModelScope)
    }

    private fun runScript(script: BluetoothScriptDomain) {
        viewModelScope.launch {
            _state.update { it.copy(scriptToRun = null) }
            connectionController.logBluetoothData(
                state.value.selectedDevice!!.address,
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "Script",
                    format = DataFormat.STRUCTURED,
                    text = "Starting script: ${script.name}"
                )
            )

            script.operations.forEach { op ->
                when (op.type) {
                    ScriptOperationTypeDomain.READ -> {
                        if (op.serviceUuid != null && op.characteristicUuid != null) {
                            connectionController.process(
                                state.value.selectedDevice!!.address,
                                ReadGattCharacteristicRequest(op.serviceUuid, op.characteristicUuid)
                            )
                        }
                    }

                    ScriptOperationTypeDomain.WRITE -> {
                        if (op.serviceUuid != null && op.characteristicUuid != null && op.data != null) {
                            connectionController.process(
                                state.value.selectedDevice!!.address,
                                WriteGattCharacteristicRequest(
                                    op.serviceUuid,
                                    op.characteristicUuid,
                                    op.data
                                )
                            )
                        }
                    }

                    ScriptOperationTypeDomain.DELAY -> {
                        op.delayMs?.let { delay(it) }
                    }
                }
                // Small delay between operations to avoid GATT congestion
                delay(100)
            }

            connectionController.logBluetoothData(
                state.value.selectedDevice!!.address,
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "Script",
                    format = DataFormat.STRUCTURED,
                    text = "Script completed: ${script.name}"
                )
            )
        }
    }

    fun connectAndRunScript(device: BluetoothDeviceDomain, script: BluetoothScriptDomain) {
        _state.update {
            it.copy(
                isConnecting = true,
                selectedDevice = device,
                scriptToRun = script
            )
        }
        connectionController.connect(device, BluetoothConnectionType.GATT)
    }

    fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothConnectionType? = null) {
        val availableProfiles =
            device.uuids.mapNotNull { BluetoothConnectionType.fromUuid(it) }.distinct()

        if (profile == null && availableProfiles.size > 1) {
            _state.update { it.copy(selectedDevice = device, profilesToSelect = availableProfiles) }
        } else {
            _state.update {
                it.copy(
                    isConnecting = true,
                    selectedDevice = device,
                    profilesToSelect = emptyList()
                )
            }
            connectionController.connect(device, profile)
        }
    }

    fun dismissProfileSelection() {
        _state.update { it.copy(profilesToSelect = emptyList()) }
    }

    fun disconnectFromDevice() {
        val address = state.value.selectedDevice?.address ?: return
        connectionController.disconnect(address)
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val address = state.value.selectedDevice?.address ?: return
        connectionController.process(
            address,
            ReadGattCharacteristicRequest(serviceUuid, characteristicUuid)
        )
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
        // TODO: Implement notification toggle
//        connectionController.toggleNotification(serviceUuid, characteristicUuid, enable)
    }

    fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        viewModelScope.launch {
            val currentSettings =
                gattCharacteristicSettingsRepository.getSettings(serviceUuid, characteristicUuid)
                    ?: GattCharacteristicSettingsDomain(serviceUuid, characteristicUuid)
            gattCharacteristicSettingsRepository.saveSettings(currentSettings.copy(alias = alias))
        }
    }

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        val address = state.value.selectedDevice?.address ?: return
        connectionController.process(
            address,
            WriteGattCharacteristicRequest(serviceUuid, characteristicUuid, data)
        )
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

    fun saveSettings(settings: GattCharacteristicSettingsDomain) {
        viewModelScope.launch {
            gattCharacteristicSettingsRepository.saveSettings(settings)
        }
    }

    fun deleteParserConfig(serviceUuid: String, characteristicUuid: String) {
        viewModelScope.launch {
            val currentSettings =
                gattCharacteristicSettingsRepository.getSettings(serviceUuid, characteristicUuid)
            if (currentSettings != null) {
                // Keep alias, but clear parser fields
                gattCharacteristicSettingsRepository.saveSettings(
                    currentSettings.copy(
                        fields = emptyList(),
                        template = ""
                    )
                )
            }
        }
    }
}

data class ConnectionUiState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedDevice: BluetoothDeviceDomain? = null,
    val dataLogs: List<BluetoothDataPacket> = emptyList(),
    val profilesToSelect: List<BluetoothConnectionType> = emptyList(),
    val enabledNotifications: Set<String> = emptySet(),
    val gattAliases: Map<String, String> = emptyMap(),
    val savedDataFrames: List<DataFrameDomain> = emptyList(),
    val settings: Map<String, GattCharacteristicSettingsDomain> = emptyMap(),
    val scriptToRun: BluetoothScriptDomain? = null,
)
