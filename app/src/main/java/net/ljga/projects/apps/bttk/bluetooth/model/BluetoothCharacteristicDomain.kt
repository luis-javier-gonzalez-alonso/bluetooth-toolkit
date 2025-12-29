package net.ljga.projects.apps.bttk.bluetooth.model

import android.bluetooth.BluetoothGattCharacteristic
import kotlinx.serialization.Serializable

@Serializable
data class BluetoothCharacteristicDomain(
    val uuid: String,
    val properties: List<String>,
    val permissions: List<String> = emptyList(),
    val descriptors: List<String> = emptyList(),
    val initialValue: String? = null
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
            if (permissions.isEmpty()) {
                // Backward compatibility / Default behavior
                if ("READ" in properties) perms = perms or BluetoothGattCharacteristic.PERMISSION_READ
                if ("WRITE" in properties || "WRITE_NO_RESPONSE" in properties) perms = perms or BluetoothGattCharacteristic.PERMISSION_WRITE
            } else {
                if ("READ" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_READ
                if ("WRITE" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_WRITE
                if ("READ_ENCRYPTED" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED
                if ("WRITE_ENCRYPTED" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED
                if ("READ_ENCRYPTED_MITM" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM
                if ("WRITE_ENCRYPTED_MITM" in permissions) perms = perms or BluetoothGattCharacteristic.PERMISSION_WRITE_ENCRYPTED_MITM
            }
            return perms
        }
}
