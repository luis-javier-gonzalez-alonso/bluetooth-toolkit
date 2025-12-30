package net.ljga.projects.apps.bttk.domain.device_connection.model

import java.util.UUID

enum class BluetoothConnectionType(val displayName: String, val uuid: UUID?) {
    SPP("Serial Port (SPP)", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")),
    GATT("Generic Attribute (GATT)", null),
    HID("Human Interface Device", UUID.fromString("00001124-0000-1000-8000-00805f9b34fb")),
    A2DP("Advanced Audio (A2DP)", UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb")),
    BATTERY("Battery Service", UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"));

    companion object {
        fun fromUuid(uuidString: String): BluetoothConnectionType? {
            return entries.find { it.uuid?.toString().equals(uuidString, ignoreCase = true) }
        }
    }
}