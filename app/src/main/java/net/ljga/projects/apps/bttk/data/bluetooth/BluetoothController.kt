package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothController {
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
    val isConnected: StateFlow<Boolean>
    val errors: Flow<String>

    fun startDiscovery()
    fun stopDiscovery()

    fun connectToDevice(device: BluetoothDeviceDomain)
    fun disconnect()

    fun release()
}
