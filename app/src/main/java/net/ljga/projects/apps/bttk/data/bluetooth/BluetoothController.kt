package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
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
    
    fun release()
}

data class BluetoothDataPacket(
    val timestamp: Long = System.currentTimeMillis(),
    val data: ByteArray,
    val source: String? = null
)
