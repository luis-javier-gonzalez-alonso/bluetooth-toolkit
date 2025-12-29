package net.ljga.projects.apps.bttk.bluetooth.strategy.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.DataFormat
import java.util.UUID

class BatteryCharacteristicHandler : GattCharacteristicHandler {
    override val serviceUuid: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    override val characteristicUuid: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    private val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override fun onServiceDiscovered(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic): BluetoothDataPacket? {
        if (characteristic.uuid == characteristicUuid) {
            // 1. Initial read
            gatt.readCharacteristic(characteristic)

            // 2. Enable notifications if supported
            if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                gatt.setCharacteristicNotification(characteristic, true)
                characteristic.getDescriptor(CCCD_UUID)?.let { descriptor ->
                    @Suppress("DEPRECATION")
                    descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    gatt.writeDescriptor(descriptor)
                }
            }
            
            return BluetoothDataPacket(
                data = byteArrayOf(),
                text = "Battery Service Found - Monitoring level...",
                source = "Battery",
                format = DataFormat.STRUCTURED,
                serviceUuid = serviceUuid.toString(),
                characteristicUuid = characteristicUuid.toString()
            )
        }
        return null
    }

    override fun handleData(characteristic: BluetoothGattCharacteristic, value: ByteArray): BluetoothDataPacket? {
        if (characteristic.uuid == characteristicUuid && value.isNotEmpty()) {
            val level = value[0].toInt() and 0xFF
            return BluetoothDataPacket(
                data = value,
                text = "Battery Level: $level%",
                source = "Battery",
                format = DataFormat.STRUCTURED,
                serviceUuid = serviceUuid.toString(),
                characteristicUuid = characteristicUuid.toString()
            )
        }
        return null
    }
}
