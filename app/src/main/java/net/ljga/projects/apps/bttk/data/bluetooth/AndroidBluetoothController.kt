package net.ljga.projects.apps.bttk.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@SuppressLint("MissingPermission")
class AndroidBluetoothController @Inject constructor(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean>
        get() = _isScanning.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private val _incomingData = MutableSharedFlow<BluetoothDataPacket>()
    override val incomingData: Flow<BluetoothDataPacket>
        get() = _incomingData.asSharedFlow()

    private var isReceiverRegistered = false
    private val deviceFoundReceiver = DeviceFoundReceiver { device, rssi ->
        val newDevice = device.toBluetoothDeviceDomain(isInRange = true, rssi = rssi)
        
        _scannedDevices.update { devices ->
            if (devices.any { it.address == newDevice.address }) {
                devices.map { if (it.address == newDevice.address) newDevice else it }
            } else {
                devices + newDevice
            }
        }

        _pairedDevices.update { devices ->
            devices.map { 
                if (it.address == newDevice.address) it.copy(isInRange = true, rssi = rssi, uuids = newDevice.uuids) else it
            }
        }
    }

    private val scanStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _isScanning.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isScanning.value = false
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> updatePairedDevices()
            }
        }
    }

    private var currentStrategy: BluetoothConnectionStrategy? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        updatePairedDevices()
        registerScanStateReceiver()
    }

    override fun startDiscovery() {
        if (!hasPermission(getScanPermission())) {
            _errors.tryEmit("Missing scan permission")
            return
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            _errors.tryEmit("Bluetooth not available or disabled")
            return
        }

        _pairedDevices.update { devices -> devices.map { it.copy(isInRange = false, rssi = null) } }
        _scannedDevices.value = emptyList()
        
        registerReceiver()
        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(getScanPermission())) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothProfile?) {
        if (!hasPermission(getConnectPermission())) {
            _errors.tryEmit("Missing connect permission")
            return
        }

        val adapter = bluetoothAdapter ?: return
        
        // Select strategy based on profile
        val strategy: BluetoothConnectionStrategy = when (profile) {
            BluetoothProfile.SPP -> SppBluetoothConnectionStrategy(adapter)
            // GATT and others would be implemented here
            else -> {
                // Default to SPP if no profile specified and it's available in UUIDs
                if (device.uuids.any { BluetoothProfile.fromUuid(it) == BluetoothProfile.SPP }) {
                    SppBluetoothConnectionStrategy(adapter)
                } else {
                    _errors.tryEmit("No supported profile selected")
                    return
                }
            }
        }

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                stopDiscovery()
                currentStrategy = strategy
                
                strategy.connect(device.address).collect { packet ->
                    _isConnected.value = true
                    _incomingData.emit(packet)
                }
            } catch (e: IOException) {
                _errors.tryEmit("Connection failed: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        scope.launch {
            currentStrategy?.disconnect()
            currentStrategy = null
            _isConnected.value = false
        }
    }

    override fun pairDevice(address: String) {
        if (!hasPermission(getConnectPermission())) return
        bluetoothAdapter?.getRemoteDevice(address)?.createBond()
    }

    override fun forgetDevice(address: String) {
        if (!hasPermission(getConnectPermission())) return
        val device = bluetoothAdapter?.getRemoteDevice(address)
        try {
            device?.let {
                it.javaClass.getMethod("removeBond").invoke(it)
                updatePairedDevices()
            }
        } catch (e: Exception) {
            _errors.tryEmit("Failed to forget device")
        }
    }

    override fun checkReachability(address: String) {
        if (!hasPermission(getConnectPermission())) return
        registerReceiver()
        bluetoothAdapter?.getRemoteDevice(address)?.fetchUuidsWithSdp()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_UUID)
            }
            context.registerReceiver(deviceFoundReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun registerScanStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        context.registerReceiver(scanStateReceiver, filter)
    }

    override fun release() {
        if (isReceiverRegistered) {
            try { context.unregisterReceiver(deviceFoundReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
        try { context.unregisterReceiver(scanStateReceiver) } catch (e: Exception) {}
        disconnect()
        stopDiscovery()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(getConnectPermission())) return
        _pairedDevices.update { 
            bluetoothAdapter?.bondedDevices?.map { it.toBluetoothDeviceDomain() } ?: emptyList()
        }
    }

    private fun hasPermission(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun getScanPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
    private fun getConnectPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
    
    private fun BluetoothDevice.toBluetoothDeviceDomain(isInRange: Boolean = false, rssi: Int? = null) = BluetoothDeviceDomain(
        name = name,
        address = address,
        isInRange = isInRange,
        bondState = bondState,
        type = type,
        uuids = uuids?.map { it.toString() } ?: emptyList(),
        rssi = rssi
    )
}
