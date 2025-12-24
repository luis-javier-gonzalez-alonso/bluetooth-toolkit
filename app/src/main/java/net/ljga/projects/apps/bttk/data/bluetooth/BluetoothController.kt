package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothProfile

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
    val connectedAddress: StateFlow<String?>
    val isScanning: StateFlow<Boolean>
    val errors: Flow<String>
    val incomingData: Flow<BluetoothDataPacket>

    fun startDiscovery()
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
    
    fun emitPacket(packet: BluetoothDataPacket)
    
    fun release()
}
