package net.ljga.projects.apps.bttk.domain.device_scan

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.utils.DeviceFoundReceiver
import javax.inject.Inject

private const val TAG = "DeviceScanController"

class DeviceScanController @Inject constructor(
    private val context: Context
) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean>
        get() = _isScanning.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private var isReceiverRegistered = false
    private val deviceFoundReceiver = DeviceFoundReceiver { device, rssi ->
        Log.v(TAG, "Device found: ${device.address} (RSSI: $rssi)")
        val newDevice = device.toBluetoothDeviceDomain(isInRange = true, rssi = rssi)

        _scannedDevices.update { devices ->
            if (devices.any { it.address == newDevice.address }) {
                devices.map { if (it.address == newDevice.address) newDevice else it }
            } else {
                devices + newDevice
            }
        }
    }

    private val scanStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    Log.i(TAG, "Bluetooth discovery started")
                    _isScanning.value = true
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    Log.i(TAG, "Bluetooth discovery finished")
                    _isScanning.value = false
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        registerScanStateReceiver()
    }

    fun startDiscovery(): Boolean {
        Log.d(TAG, "Starting discovery requested")
        if (!hasPermission(getScanPermission())) {
            Log.e(TAG, "Missing scan permission")
            _errors.tryEmit("Missing scan permission")
            return false
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.w(TAG, "Bluetooth not available or disabled")
            _errors.tryEmit("Bluetooth not available")
            return false
        }

        _scannedDevices.value = emptyList()

        registerReceiver()
        val started = bluetoothAdapter?.startDiscovery() == true
        if (started) {
            Log.i(TAG, "Native discovery command successful")
            _isScanning.value = true
        } else {
            Log.e(TAG, "Native discovery command failed")
        }
        return started
    }

    fun stopDiscovery() {
        Log.d(TAG, "Stopping discovery requested")
        if (!hasPermission(getScanPermission())) return
        bluetoothAdapter?.cancelDiscovery()
    }

    fun checkReachability(address: String) {
        Log.d(TAG, "Checking reachability for: $address")
        if (!hasPermission(getConnectPermission())) {
            Log.w(TAG, "Missing connect permission for reachability check")
            return
        }
        registerReceiver()
        bluetoothAdapter?.getRemoteDevice(address)?.fetchUuidsWithSdp()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            Log.v(TAG, "Registering device found receiver")
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_UUID)
            }
            context.registerReceiver(deviceFoundReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun registerScanStateReceiver() {
        Log.v(TAG, "Registering scan state receiver")
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(scanStateReceiver, filter)
        }
    }

    fun release() {
        Log.d(TAG, "Releasing controller")
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(deviceFoundReceiver)
            } catch (e: Exception) {
                Log.w(TAG, "Error unregistering deviceFoundReceiver: ${e.message}")
            }
            isReceiverRegistered = false
        }
        try {
            context.unregisterReceiver(scanStateReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering scanStateReceiver: ${e.message}")
        }
        stopDiscovery()
    }

    private fun hasPermission(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun getScanPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION

    private fun getConnectPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH

    private fun BluetoothDevice.toBluetoothDeviceDomain(
        isInRange: Boolean = false,
        rssi: Int? = null
    ) =
        BluetoothDeviceDomain(
            name = name,
            address = address,
            isInRange = isInRange,
            bondState = bondState,
            type = type,
            uuids = uuids?.map { it.toString() } ?: emptyList(),
            rssi = rssi
        )
}
