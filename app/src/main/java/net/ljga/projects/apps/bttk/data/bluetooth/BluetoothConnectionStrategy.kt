package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.coroutines.flow.Flow
import java.util.UUID

interface BluetoothConnectionStrategy {
    val name: String
    val uuid: UUID?
    
    suspend fun connect(address: String): Flow<BluetoothDataPacket>
    suspend fun disconnect()

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {}
    fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {}
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {}
}

enum class BluetoothConnectionState(val state: Int) {
    STATE_CONNECTED(0),
    STATE_DISCONNECTED(1);

    companion object {
        fun fromInt(newValue: Int): BluetoothConnectionState? {
            return BluetoothConnectionState.entries.find { it.state == newValue }
        }
    }
}

enum class BluetoothProfile(val displayName: String, val uuid: UUID?) {
    SPP("Serial Port (SPP)", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")),
    GATT("Generic Attribute (GATT)", null),
    HID("Human Interface Device", UUID.fromString("00001124-0000-1000-8000-00805f9b34fb")),
    A2DP("Advanced Audio (A2DP)", UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb")),
    BATTERY("Battery Service", UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"));

    companion object {
        fun fromUuid(uuidString: String): BluetoothProfile? {
            return entries.find { it.uuid?.toString().equals(uuidString, ignoreCase = true) }
        }
    }
}
