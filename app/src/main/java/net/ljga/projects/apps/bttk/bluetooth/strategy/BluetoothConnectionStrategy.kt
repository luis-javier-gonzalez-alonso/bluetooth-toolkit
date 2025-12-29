package net.ljga.projects.apps.bttk.bluetooth.strategy

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDataPacket
import java.util.UUID

interface BluetoothConnectionStrategy {
    val name: String
    val uuid: UUID?
    
    suspend fun connect(address: String): Flow<BluetoothDataPacket>
    suspend fun disconnect()

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {}
    fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {}
    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {}
    fun readDescriptors(serviceUuid: String, characteristicUuid: String) {}
}
