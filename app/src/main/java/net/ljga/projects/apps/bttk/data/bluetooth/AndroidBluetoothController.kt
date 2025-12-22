package net.ljga.projects.apps.bttk.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private val _errors = MutableSharedFlow<String>()
    override val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private var isReceiverRegistered = false
    private val deviceFoundReceiver = DeviceFoundReceiver { device ->
        _scannedDevices.update { devices ->
            val newDevice = device.toBluetoothDeviceDomain()
            if (devices.any { it.address == newDevice.address }) devices else devices + newDevice
        }
    }

    init {
        updatePairedDevices()
    }

    override fun startDiscovery() {
        if (!hasPermission(getScanPermission())) {
            _errors.tryEmit("Missing scan permission")
            return
        }

        if (bluetoothAdapter == null) {
            _errors.tryEmit("Bluetooth not supported on this device")
            return
        }

        if (!bluetoothAdapter!!.isEnabled) {
            _errors.tryEmit("Bluetooth is disabled")
            return
        }

        updatePairedDevices()
        _scannedDevices.value = emptyList()

        if (!isReceiverRegistered) {
            context.registerReceiver(
                deviceFoundReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            isReceiverRegistered = true
        }

        bluetoothAdapter?.startDiscovery()
    }

    override fun stopDiscovery() {
        if (!hasPermission(getScanPermission())) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain) {
        _isConnected.value = true
    }

    override fun disconnect() {
        _isConnected.value = false
    }

    override fun release() {
        if (isReceiverRegistered) {
            try {
                context.unregisterReceiver(deviceFoundReceiver)
                isReceiverRegistered = false
            } catch (e: Exception) {
                // Ignore
            }
        }
        stopDiscovery()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(getConnectPermission())) return

        bluetoothAdapter
            ?.bondedDevices
            ?.map { it.toBluetoothDeviceDomain() }
            ?.let { devices ->
                _pairedDevices.update { devices }
            }
    }

    private fun hasPermission(permission: String): Boolean {
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun getScanPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_SCAN
        } else {
            Manifest.permission.ACCESS_FINE_LOCATION
        }
    }

    private fun getConnectPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }
    }

    private fun BluetoothDevice.toBluetoothDeviceDomain(): BluetoothDeviceDomain {
        return BluetoothDeviceDomain(
            name = name,
            address = address
        )
    }
}
