package net.ljga.projects.apps.bttk.bluetooth.strategy.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.bluetooth.model.DataFormat
import net.ljga.projects.apps.bttk.bluetooth.utils.prettyName
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
            source = "Read: ${characteristic.prettyName()}...",
            format = DataFormat.HEX_ASCII,
            serviceUuid = characteristic.service.uuid.toString(),
            characteristicUuid = characteristic.uuid.toString()
        )
    }
}
