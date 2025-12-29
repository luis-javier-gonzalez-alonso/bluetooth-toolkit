package net.ljga.projects.apps.bttk.domain.strategy.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import java.util.UUID

interface GattCharacteristicHandler {
    val serviceUuid: UUID?
    val characteristicUuid: UUID?
    
    /**
     * Called after services are discovered. 
     * Returns an optional packet to log discovery information (e.g. metadata).
     */
    fun onServiceDiscovered(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): BluetoothDataPacket?
    
    /**
     * Parses the raw byte data into a BluetoothDataPacket.
     * Returns null if the handler is not interested in this specific data.
     */
    fun handleData(characteristic: BluetoothGattCharacteristic, value: ByteArray): BluetoothDataPacket?
}
