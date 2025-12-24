package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import java.util.*

class DefaultGattCharacteristicHandler : GattCharacteristicHandler {
    override val serviceUuid: UUID? = null
    override val characteristicUuid: UUID? = null

    @SuppressLint("MissingPermission")
    override fun onServiceDiscovered(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): BluetoothDataPacket? {
        // We no longer emit metadata packets for every characteristic discovered
        // as we now emit a single structured GATT packet for the whole device.
        return null
    }

    override fun handleData(characteristic: BluetoothGattCharacteristic, value: ByteArray): BluetoothDataPacket? {
        return BluetoothDataPacket(
            data = value,
            source = "GATT: ${characteristic.uuid.toString().take(8)}...",
            format = DataFormat.HEX_ASCII,
            serviceUuid = characteristic.service.uuid.toString(),
            characteristicUuid = characteristic.uuid.toString()
        )
    }
}
