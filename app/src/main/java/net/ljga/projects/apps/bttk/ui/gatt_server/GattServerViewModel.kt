package net.ljga.projects.apps.bttk.ui.gatt_server

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.domain.GattServerController
import net.ljga.projects.apps.bttk.domain.GattServerService
import net.ljga.projects.apps.bttk.domain.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain
import net.ljga.projects.apps.bttk.domain.repository.GattServerRepository
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class GattServerViewModel @Inject constructor(
    private val gattServerController: GattServerController,
    private val gattServerRepository: GattServerRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(GattServerUiState())

    private var nextServiceIndex = 0
    private val serviceIndices = mutableMapOf<String, Int>()
    private val serviceNextCharIndices = mutableMapOf<String, Int>()
    private var currentServerProfileId: Int = 0

    val state = combine(
        gattServerRepository.getAllServers(),
        gattServerController.isGattServerRunning,
        gattServerController.gattServerServices,
        gattServerController.gattServerLogs,
        _state
    ) { servers, isGattServerRunning, gattServerServices, gattServerLogs, state ->
        state.copy(
            isGattServerRunning = isGattServerRunning,
            gattServerServices = gattServerServices,
            gattServerLogs = gattServerLogs,
            availableGattServers = servers,
            currentGattServerId = currentServerProfileId
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GattServerUiState())

    init {
        gattServerController.errors.onEach { error: String ->
            _state.update {
                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                it.copy(errorMessage = error)
            }
        }.launchIn(viewModelScope)
    }

    fun setGattServerDeviceName(name: String) {
        _state.update { it.copy(gattServerDeviceName = name) }
        saveGattServerConfig()
    }

    fun toggleGattServer() {
        if (state.value.isGattServerRunning) {
            val intent = Intent(context, GattServerService::class.java).apply {
                action = "net.ljga.projects.apps.bttk.GattServerService.ACTION_STOP"
            }
            context.startService(intent)
        } else {
            // Check if bluetooth is enabled before starting service
            val isEnabled =
                (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter?.isEnabled == true
            if (!isEnabled) {
                Toast.makeText(context, "Bluetooth not available", Toast.LENGTH_SHORT).show()
                return
            }

            val intent = Intent(context, GattServerService::class.java).apply {
                action = "net.ljga.projects.apps.bttk.GattServerService.ACTION_START"
                if (state.value.gattServerDeviceName.isNotBlank()) {
                    putExtra("EXTRA_DEVICE_NAME", state.value.gattServerDeviceName)
                }
            }
            context.startForegroundService(intent)
        }
    }

    fun loadGattServerProfile(id: Int) {
        viewModelScope.launch {
            val server = gattServerRepository.getServerById(id) ?: return@launch

            // Stop server if running before switching? Or just update it
            gattServerController.clearGattServices()

            currentServerProfileId = server.id
            nextServiceIndex = server.nextServiceIndex
            serviceIndices.clear()
            serviceIndices.putAll(server.serviceIndices)
            serviceNextCharIndices.clear()
            serviceNextCharIndices.putAll(server.serviceNextCharIndices)

            _state.update { it.copy(gattServerDeviceName = server.deviceName ?: "") }

            server.services.forEach { service ->
                gattServerController.addGattService(service)
            }
        }
    }

    fun createNewGattServerProfile(name: String) {
        viewModelScope.launch {
            val newProfile = GattServerStateDomain(
                name = name,
                services = emptyList(),
                nextServiceIndex = 0,
                deviceName = "Device Name"
            )
            val id = gattServerRepository.saveServer(newProfile)
            loadGattServerProfile(id)
        }
    }

    fun deleteGattServerProfile(id: Int) {
        viewModelScope.launch {
            val server = gattServerRepository.getServerById(id)
            if (server != null) {
                gattServerRepository.deleteServer(server)
                if (currentServerProfileId == id) {
                    clearGattServer()
                    currentServerProfileId = 0
                }
            }
        }
    }

    fun clearGattServer() {
        gattServerController.clearGattServices()
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
        gattServerController.addGattService(newService)
        saveGattServerConfig()
    }

    fun removeGattService(serviceUuid: String) {
        gattServerController.removeGattService(serviceUuid)
        // Note: we don't decrement nextServiceIndex to keep it incremental
        saveGattServerConfig()
    }

    fun generateCharacteristicUuid(serviceUuid: String): String {
        val sIndex = serviceIndices[serviceUuid] ?: 0
        val cIndex = serviceNextCharIndices[serviceUuid] ?: 0
        val baseUuid = serviceUuid.substring(8)
        val prefix =
            "a${sIndex.toString(16)}${cIndex.toString(16).padStart(2, '0')}".padStart(8, '0')
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

        gattServerController.updateGattService(updatedService)
        saveGattServerConfig()
    }

    fun removeCharacteristicFromService(serviceUuid: String, charUuid: String) {
        val currentServices = state.value.gattServerServices
        val service = currentServices.find { it.uuid == serviceUuid } ?: return

        val updatedService = service.copy(
            characteristics = service.characteristics.filter { it.uuid != charUuid }
        )

        gattServerController.updateGattService(updatedService)
        saveGattServerConfig()
    }

    private fun saveGattServerConfig() {
        viewModelScope.launch {
            if (currentServerProfileId == 0) return@launch

            val currentServer =
                gattServerRepository.getServerById(currentServerProfileId) ?: return@launch

            val stateDomain = GattServerStateDomain(
                id = currentServerProfileId,
                name = currentServer.name,
                services = gattServerController.gattServerServices.value,
                nextServiceIndex = nextServiceIndex,
                serviceIndices = serviceIndices.toMap(),
                serviceNextCharIndices = serviceNextCharIndices.toMap(),
                deviceName = state.value.gattServerDeviceName
            )
            gattServerRepository.saveServer(stateDomain)
        }
    }
}

data class GattServerUiState(
    val errorMessage: String? = null,
    val gattServerLogs: List<BluetoothDataPacket> = emptyList(),
    val enabledNotifications: Set<String> = emptySet(),
    val gattAliases: Map<String, String> = emptyMap(),
    val isGattServerRunning: Boolean = false,
    val gattServerServices: List<BluetoothServiceDomain> = emptyList(),
    val gattServerDeviceName: String = "",
    val availableGattServers: List<GattServerStateDomain> = emptyList(),
    val currentGattServerId: Int = 0
)
