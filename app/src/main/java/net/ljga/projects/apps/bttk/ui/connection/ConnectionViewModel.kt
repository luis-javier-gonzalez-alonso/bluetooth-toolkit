package net.ljga.projects.apps.bttk.ui.connection

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.domain.ConnectionController
import net.ljga.projects.apps.bttk.domain.model.*
import net.ljga.projects.apps.bttk.domain.model.process.ReadGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.model.process.WriteGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicAliasRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicParserRepository
import net.ljga.projects.apps.bttk.domain.utils.CharacteristicParser
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val connectionController: ConnectionController,
    private val dataFrameRepository: DataFrameRepository,
    private val gattCharacteristicAliasRepository: GattCharacteristicAliasRepository,
    private val gattCharacteristicParserRepository: GattCharacteristicParserRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionUiState())
    val state = combine(
        gattCharacteristicAliasRepository.gattAliases,
        dataFrameRepository.dataFrames,
        gattCharacteristicParserRepository.getAllConfigs(),
        connectionController.connectionLogs,
        _state
    ) { aliases, dataFrames, configs, incoming, state ->

        val address = state.selectedDevice?.address!!

        val configMap = configs.associateBy { "${it.serviceUuid}-${it.characteristicUuid}" }
        val incomingData = incoming[address]?.map { packet ->
            val config = configMap["${packet.serviceUuid}-${packet.characteristicUuid}"]
            if (config != null && packet.data.isNotEmpty()) {
                packet.copy(
                    text = CharacteristicParser.parse(packet.data, config),
                    format = DataFormat.STRUCTURED
                )
            } else {
                packet
            }
        }

        state.copy(
            gattAliases = aliases,
            savedDataFrames = dataFrames,
            dataLogs = incomingData!!,
            parserConfigs = configMap,
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

    fun checkReachability(device: BluetoothDeviceDomain) {
        // TODO Implement reachability check
//        connectionController.checkReachability(device.address)
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
        connectionController.disconnect(state.value.selectedDevice!!.address)
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        connectionController.process(
            state.value.selectedDevice!!.address,
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
            gattCharacteristicAliasRepository.saveAlias(serviceUuid, characteristicUuid, alias)
        }
    }

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        // TODO Implement write characteristic
//        connectionController.writeCharacteristic(serviceUuid, characteristicUuid, data)
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

    fun saveParserConfig(config: CharacteristicParserConfigDomain) {
        viewModelScope.launch {
            gattCharacteristicParserRepository.saveConfig(config)
        }
    }

    fun deleteParserConfig(serviceUuid: String, characteristicUuid: String) {
        viewModelScope.launch {
            gattCharacteristicParserRepository.deleteConfig(serviceUuid, characteristicUuid)
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
    val parserConfigs: Map<String, CharacteristicParserConfigDomain> = emptyMap(),
    val scriptToRun: BluetoothScriptDomain? = null,
)
