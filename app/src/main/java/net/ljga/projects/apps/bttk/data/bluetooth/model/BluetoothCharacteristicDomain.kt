package net.ljga.projects.apps.bttk.data.bluetooth.model

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import kotlinx.serialization.Serializable

@Serializable
data class BluetoothCharacteristicDomain(
    val uuid: String,
    val properties: List<String>,
    val descriptors: List<String> = emptyList()
) {
    val propertyInts: Int
        get() {
            var props = 0
            if ("READ" in properties) props = props or BluetoothGattCharacteristic.PROPERTY_READ
            if ("WRITE" in properties) props = props or BluetoothGattCharacteristic.PROPERTY_WRITE
            if ("WRITE_NO_RESPONSE" in properties) props = props or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE
            if ("NOTIFY" in properties) props = props or BluetoothGattCharacteristic.PROPERTY_NOTIFY
            if ("INDICATE" in properties) props = props or BluetoothGattCharacteristic.PROPERTY_INDICATE
            return props
        }

    val permissionInts: Int
        get() {
            var perms = 0
            if ("READ" in properties) perms = perms or BluetoothGattCharacteristic.PERMISSION_READ
            if ("WRITE" in properties || "WRITE_NO_RESPONSE" in properties) perms = perms or BluetoothGattCharacteristic.PERMISSION_WRITE
            return perms
        }
}
