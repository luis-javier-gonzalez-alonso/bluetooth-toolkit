package net.ljga.projects.apps.bttk.domain

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
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.utils.DeviceFoundReceiver
import javax.inject.Inject

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

    // TODO remove paired section? it seems we can still connect without pairing. Maybe needed for SPP?
//    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
//    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
//        get() = _pairedDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean>
        get() = _isScanning.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: Flow<String>
        get() = _errors.asSharedFlow()

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

//        _pairedDevices.update { devices ->
//            devices.map {
//                if (it.address == newDevice.address) it.copy(
//                    isInRange = true,
//                    rssi = rssi,
//                    uuids = newDevice.uuids
//                ) else it
//            }
//        }
    }

    private val scanStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _isScanning.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isScanning.value = false
//                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> updatePairedDevices()
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
//        updatePairedDevices()
        registerScanStateReceiver()
    }

    fun startDiscovery(): Boolean {
        if (!hasPermission(getScanPermission())) {
            _errors.tryEmit("Missing scan permission")
            return false
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            _errors.tryEmit("Bluetooth not available")
            return false
        }

//        updatePairedDevices()

//        _pairedDevices.update { devices -> devices.map { it.copy(isInRange = false, rssi = null) } }
        _scannedDevices.value = emptyList()

        registerReceiver()
        val started = bluetoothAdapter?.startDiscovery() == true
        if (started) {
            _isScanning.value = true
        }
        return started
    }

    fun stopDiscovery() {
        if (!hasPermission(getScanPermission())) return
        bluetoothAdapter?.cancelDiscovery()
    }

//    fun pairDevice(address: String) {
//        if (!hasPermission(getConnectPermission())) return
//        bluetoothAdapter?.getRemoteDevice(address)?.createBond()
//    }

//    fun forgetDevice(address: String) {
//        if (!hasPermission(getConnectPermission())) return
//        val device = bluetoothAdapter?.getRemoteDevice(address)
//        try {
//            device?.let {
//                it.javaClass.getMethod("removeBond").invoke(it)
//                updatePairedDevices()
//            }
//        } catch (e: Exception) {
//            _errors.tryEmit("Failed to forget device")
//        }
//    }

    fun checkReachability(address: String) {
        if (!hasPermission(getConnectPermission())) return
        registerReceiver()
        bluetoothAdapter?.getRemoteDevice(address)?.fetchUuidsWithSdp()
    }

//    fun refreshPairedDevices() {
//        updatePairedDevices()
//    }

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(scanStateReceiver, filter)
        }
    }

    fun release() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(deviceFoundReceiver)
            } catch (e: Exception) {
            }
            isReceiverRegistered = false
        }
        try {
            context.unregisterReceiver(scanStateReceiver)
        } catch (e: Exception) {
        }
        stopDiscovery()
    }

//    private fun updatePairedDevices() {
//        if (!hasPermission(getConnectPermission())) return
//        _pairedDevices.update {
//            bluetoothAdapter?.bondedDevices?.map { it.toBluetoothDeviceDomain() } ?: emptyList()
//        }
//    }

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
