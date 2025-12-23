package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
    
    fun release()
}

enum class DataFormat {
    HEX_ASCII, // For raw binary streams
    STRUCTURED,  // For human-readable strings like service info
    GATT_STRUCTURE
}

data class BluetoothDataPacket(
    val timestamp: Long = System.currentTimeMillis(),
    val data: ByteArray = byteArrayOf(),
    val source: String? = null,
    val format: DataFormat = DataFormat.HEX_ASCII,
    val text: String? = null, // Optional pre-formatted text
    val gattServices: List<BluetoothServiceDomain>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothDataPacket

        if (timestamp != other.timestamp) return false
        if (!data.contentEquals(other.data)) return false
        if (source != other.source) return false
        if (format != other.format) return false
        if (text != other.text) return false
        if (gattServices != other.gattServices) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (source?.hashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (gattServices?.hashCode() ?: 0)
        return result
    }
}
