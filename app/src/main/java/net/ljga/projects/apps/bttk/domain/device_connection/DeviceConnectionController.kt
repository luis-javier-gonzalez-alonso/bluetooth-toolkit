package net.ljga.projects.apps.bttk.domain.device_connection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
import net.ljga.projects.apps.bttk.domain.device_connection.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.device_connection.strategy.BluetoothConnection
import net.ljga.projects.apps.bttk.domain.device_connection.strategy.GattBluetoothConnection
import net.ljga.projects.apps.bttk.domain.device_connection.strategy.SppBluetoothConnection
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import javax.inject.Inject

private const val TAG = "DeviceConnectionController"

@SuppressLint("MissingPermission")
class DeviceConnectionController @Inject constructor(
    private val context: Context,
    private val bluetoothDeviceRepository: BluetoothDeviceRepository
) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _connections = MutableStateFlow<Map<String, BluetoothConnection>>(emptyMap())
    val connections: StateFlow<Map<String, BluetoothConnection>>
        get() = _connections.asStateFlow()

    private val _connectionLogs = MutableStateFlow<Map<String, List<BluetoothDataPacket>>>(emptyMap())
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
            Log.d(TAG, "Already connected or connecting to $address")
            return
        }

        Log.i(TAG, "Initiating connection to $address (profile: ${profile ?: "default"})")

        if (!hasPermission(getConnectPermission())) {
            Log.e(TAG, "Missing connect permission for $address")
            _errors.tryEmit("Missing connect permission")
            return
        }

        val adapter = bluetoothAdapter ?: run {
            Log.e(TAG, "Bluetooth adapter not available")
            return
        }

        val strategy: BluetoothConnection = when (profile) {
            BluetoothConnectionType.SPP -> {
                Log.d(TAG, "Using SPP strategy for $address")
                SppBluetoothConnection(adapter)
            }
            BluetoothConnectionType.GATT -> {
                Log.d(TAG, "Using GATT strategy for $address")
                GattBluetoothConnection(adapter, context)
            }
            else -> {
                Log.d(TAG, "Using default (GATT) strategy for $address")
                GattBluetoothConnection(adapter, context)
            }
        }

        _connections.update { it + (address to strategy) }
        
        // Reset logs only when starting a new manual connection attempt
        _connectionLogs.update { it + (address to listOf(
            BluetoothDataPacket(
                source = "System",
                text = "Connecting to $address via ${strategy.type}...",
                format = DataFormat.STRUCTURED
            )
        )) }

        val job = scope.launch {
            try {
                Log.v(TAG, "Starting connection job for $address")
                strategy.connect(address).collect { packet ->
                    logBluetoothData(address, packet)
                }
            } catch (e: Exception) {
                val errorMessage = "Connection failed for $address: ${e.message}"
                Log.e(TAG, errorMessage, e)
                _errors.tryEmit(errorMessage)
                logBluetoothData(address, BluetoothDataPacket(
                    source = "System",
                    text = errorMessage,
                    format = DataFormat.STRUCTURED
                ))
            } finally {
                Log.i(TAG, "Connection job finished for $address")
                disconnect(address)
            }
        }
        connectionJobs[address] = job
    }

    fun disconnect(address: String) {
        Log.i(TAG, "Disconnecting from $address")
        connectionJobs[address]?.cancel()
        connectionJobs.remove(address)
        _connections.value[address]?.disconnect()
        _connections.update { it - address }
    }

    fun process(address: String, request: ProcessRequest) {
        Log.d(TAG, "Processing request for $address: ${request.javaClass.simpleName}")
        _connections.value[address]?.process(request)?.let {
            logBluetoothData(address, it)
        } ?: Log.v(TAG, "Request processed without immediate response for $address")
    }

    fun logBluetoothData(address: String, packet: BluetoothDataPacket) {
        if (packet.format == DataFormat.GATT_STRUCTURE && packet.gattServices != null && packet.source != null) {
            Log.d(TAG, "Updating services in repository for $address")
            scope.launch {
                bluetoothDeviceRepository.updateServices(address, packet.gattServices)
            }
        }
        
        Log.v(TAG, "Data received from $address: ${packet.text ?: "${packet.data?.size ?: 0} bytes"}")
        
        _connectionLogs.update { current ->
            val logs = current[address] ?: emptyList()
            current + (address to (logs + packet).takeLast(100))
        }
    }

    private fun hasPermission(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun getConnectPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
}
