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
        val properties = getPropertiesString(characteristic.properties)
        val permissions = getPermissionsString(characteristic.permissions)
        val info = "Discovered Characteristic: ${characteristic.uuid}\n" +
                   "Service: ${characteristic.service.uuid}\n" +
                   "Properties: $properties\n" +
                   "Permissions: $permissions"
        
        return BluetoothDataPacket(
            data = byteArrayOf(),
            text = info,
            source = "GATT Metadata",
            format = DataFormat.STRUCTURED
        )
    }

    override fun handleData(characteristic: BluetoothGattCharacteristic, value: ByteArray): BluetoothDataPacket? {
        return BluetoothDataPacket(
            data = value,
            source = "GATT: ${characteristic.uuid.toString().take(8)}...",
            format = DataFormat.HEX_ASCII
        )
    }

    private fun getPropertiesString(properties: Int): String {
        val props = mutableListOf<String>()
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) props.add("READ")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) props.add("WRITE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) props.add("WRITE_NO_RESPONSE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) props.add("NOTIFY")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) props.add("INDICATE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) props.add("BROADCAST")
        if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) props.add("EXTENDED")
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) props.add("SIGNED_WRITE")
        return if (props.isEmpty()) "NONE" else props.joinToString("|")
    }

    private fun getPermissionsString(permissions: Int): String {
        val perms = mutableListOf<String>()
        if (permissions and BluetoothGattCharacteristic.PERMISSION_READ != 0) perms.add("READ")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED != 0) perms.add("READ_ENCRYPTED")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM != 0) perms.add("READ_MITM")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_WRITE != 0) perms.add("WRITE")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED != 0) perms.add("WRITE_ENCRYPTED")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM != 0) perms.add("WRITE_MITM")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED != 0) perms.add("WRITE_SIGNED")
        if (permissions and BluetoothGattCharacteristic.PERMISSION_WRITE_SIGNED_MITM != 0) perms.add("WRITE_SIGNED_MITM")
        return if (perms.isEmpty()) "NONE" else perms.joinToString("|")
    }
}
