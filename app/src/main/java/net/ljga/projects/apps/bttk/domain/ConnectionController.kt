package net.ljga.projects.apps.bttk.domain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.strategy.BluetoothConnection
import net.ljga.projects.apps.bttk.domain.strategy.GattBluetoothConnection
import net.ljga.projects.apps.bttk.domain.strategy.SppBluetoothConnection
import javax.inject.Inject

@SuppressLint("MissingPermission")
class ConnectionController @Inject constructor(
    private val context: Context,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository
) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _connections = MutableStateFlow(HashMap<String, BluetoothConnection>())
    val connections: StateFlow<Map<String, BluetoothConnection>>
        get() = _connections.asStateFlow()

    private val _connectionLogs = MutableStateFlow(HashMap<String, List<BluetoothDataPacket>>())
    val connectionLogs: StateFlow<Map<String, List<BluetoothDataPacket>>>
        get() = _connectionLogs.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private val connectionJobs: MutableMap<String, Job> = HashMap()
    private val scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    fun connect(device: BluetoothDeviceDomain, profile: BluetoothConnectionType?) {
        val address = device.address

        if (_connections.value[address] != null) {
            // Already connected or connecting
            return
        }

        if (!hasPermission(getConnectPermission())) {
            _errors.tryEmit("Missing connect permission")
            return
        }

        val adapter = bluetoothAdapter ?: return

        val strategy: BluetoothConnection = when (profile) {
            BluetoothConnectionType.SPP -> SppBluetoothConnection(adapter)
            BluetoothConnectionType.GATT -> GattBluetoothConnection(adapter, context)
            else -> GattBluetoothConnection(adapter, context)
        }

        _connections.update { it[address] = strategy; it }
        _connectionLogs.update { it[address] = emptyList(); it }

        val value = scope.launch {
            try {
                strategy.connect(address).collect { packet ->
                    logBluetoothData(address, packet)
                }
            } catch (e: Exception) {
                _errors.tryEmit("Connection failed: ${e.message}")
            } finally {
                disconnect(address)
            }
        }
        connectionJobs[address] = value
    }

    fun disconnect(address: String) {
        connectionJobs[address]?.cancel()
        connectionJobs.remove(address)
        _connections.value[address]?.disconnect()
        _connections.update { it.remove(address); it }
        _connectionLogs.update { it.remove(address); it }
    }

    fun process(address: String, request: ProcessRequest) {
        _connections.value[address]?.process(request)?.let {
            logBluetoothData(address, it)
        }
    }

    fun logBluetoothData(address: String, packet: BluetoothDataPacket) {
        if (packet.format == DataFormat.GATT_STRUCTURE && packet.gattServices != null && packet.source != null) {
            scope.launch {
                bluetoothDeviceRepository.updateServices(address, packet.gattServices)
            }
        }
        _connectionLogs.update { it[address] = (it[address]!! + packet).takeLast(100); it }
    }

    private fun hasPermission(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun getConnectPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
}
