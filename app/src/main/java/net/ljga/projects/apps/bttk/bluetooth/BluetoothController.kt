package net.ljga.projects.apps.bttk.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothProfile
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
    val connectedAddress: StateFlow<String?>
    val isScanning: StateFlow<Boolean>
    val errors: Flow<String>
    val incomingData: StateFlow<List<BluetoothDataPacket>>
    
    // GATT Server
    val isGattServerRunning: StateFlow<Boolean>
    val gattServerServices: StateFlow<List<BluetoothServiceDomain>>
    val localAddress: String?
    val gattServerLogs: StateFlow<List<BluetoothDataPacket>>

    fun startDiscovery(): Boolean
    fun stopDiscovery()
    
    fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothProfile? = null)
    fun disconnect()
    
    fun pairDevice(address: String)
    fun forgetDevice(address: String)
    fun checkReachability(address: String)
    fun refreshPairedDevices()
    
    fun readCharacteristic(serviceUuid: String, characteristicUuid: String)
    fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean)
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray)
    fun readDescriptors(serviceUuid: String, characteristicUuid: String)
    
    fun emitPacket(packet: BluetoothDataPacket)
    
    // GATT Server operations
    fun startGattServer(deviceName: String? = null)
    fun stopGattServer()
    fun addGattService(service: BluetoothServiceDomain)
    fun removeGattService(serviceUuid: String)
    fun clearGattServices()
    fun updateGattService(service: BluetoothServiceDomain)
    
    fun release()
}
