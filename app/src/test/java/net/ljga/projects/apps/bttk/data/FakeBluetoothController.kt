package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.ljga.projects.apps.bttk.bluetooth.BluetoothController
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothProfile
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain

class FakeBluetoothController : BluetoothController {
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>> = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _connectedAddress = MutableStateFlow<String?>(null)
    override val connectedAddress: StateFlow<String?> = _connectedAddress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: Flow<String> = _errors.asSharedFlow()

    private val _incomingData = MutableStateFlow<List<BluetoothDataPacket>>(emptyList())
    override val incomingData: StateFlow<List<BluetoothDataPacket>> = _incomingData.asStateFlow()

    private val _isGattServerRunning = MutableStateFlow(false)
    override val isGattServerRunning: StateFlow<Boolean> = _isGattServerRunning.asStateFlow()

    private val _gattServerServices = MutableStateFlow<List<BluetoothServiceDomain>>(emptyList())
    override val gattServerServices: StateFlow<List<BluetoothServiceDomain>> = _gattServerServices.asStateFlow()

    override val localAddress: String? = "00:11:22:33:44:55"

    private val _gattServerLogs = MutableStateFlow<List<BluetoothDataPacket>>(emptyList())
    override val gattServerLogs: StateFlow<List<BluetoothDataPacket>> = _gattServerLogs.asStateFlow()

    override fun startDiscovery(): Boolean {
        _isScanning.value = true
        return true
    }

    override fun stopDiscovery() {
        _isScanning.value = false
    }

    override fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothProfile?) {
        _isConnected.value = true
        _connectedAddress.value = device.address
    }

    override fun disconnect() {
        _isConnected.value = false
        _connectedAddress.value = null
    }

    override fun pairDevice(address: String) {}
    override fun forgetDevice(address: String) {}
    override fun checkReachability(address: String) {}
    override fun refreshPairedDevices() {}

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {}
    override fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {}
    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {}
    override fun readDescriptors(serviceUuid: String, characteristicUuid: String) {}

    override fun emitPacket(packet: BluetoothDataPacket) {
        _incomingData.value = _incomingData.value + packet
    }

    override fun startGattServer(deviceName: String?) {
        _isGattServerRunning.value = true
    }

    override fun stopGattServer() {
        _isGattServerRunning.value = false
    }

    override fun addGattService(service: BluetoothServiceDomain) {
        _gattServerServices.value = _gattServerServices.value + service
    }

    override fun removeGattService(serviceUuid: String) {
        _gattServerServices.value = _gattServerServices.value.filter { it.uuid != serviceUuid }
    }

    override fun clearGattServices() {
        _gattServerServices.value = emptyList()
    }

    override fun updateGattService(service: BluetoothServiceDomain) {
        _gattServerServices.value = _gattServerServices.value.map {
            if (it.uuid == service.uuid) service else it
        }
    }

    override fun release() {}
}
