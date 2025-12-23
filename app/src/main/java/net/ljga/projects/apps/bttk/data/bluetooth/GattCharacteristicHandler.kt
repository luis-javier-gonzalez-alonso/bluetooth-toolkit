package net.ljga.projects.apps.bttk.data.bluetooth

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import java.util.UUID

interface GattCharacteristicHandler {
    val serviceUuid: UUID?
    val characteristicUuid: UUID?
    
    /**
     * Called after services are discovered to allow the handler to perform initial actions 
     * like reading a value or enabling notifications.
     */
    fun onServiceDiscovered(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)
    
    /**
     * Parses the raw byte data into a BluetoothDataPacket.
     * Returns null if the handler is not interested in this specific data.
     */
    fun handleData(characteristic: BluetoothGattCharacteristic, value: ByteArray): BluetoothDataPacket?
}
