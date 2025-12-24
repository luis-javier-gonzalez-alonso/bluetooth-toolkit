package net.ljga.projects.apps.bttk.data.bluetooth.model

import kotlinx.serialization.Serializable
import java.util.UUID

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val isInRange: Boolean = false,
    val bondState: Int = 10, // BluetoothDevice.BOND_NONE
    val type: Int = 0,      // BluetoothDevice.DEVICE_TYPE_UNKNOWN
    val uuids: List<String> = emptyList(),
    val rssi: Int? = null,
    val services: List<BluetoothServiceDomain> = emptyList()
)

@Serializable
data class BluetoothServiceDomain(
    val uuid: String,
    val characteristics: List<BluetoothCharacteristicDomain>
)

@Serializable
data class BluetoothCharacteristicDomain(
    val uuid: String,
    val properties: List<String>,
    val descriptors: List<String> = emptyList()
)

data class BluetoothDataPacket(
    val timestamp: Long = System.currentTimeMillis(),
    val data: ByteArray = byteArrayOf(),
    val source: String? = null,
    val format: DataFormat = DataFormat.HEX_ASCII,
    val text: String? = null, // Optional pre-formatted text
    val gattServices: List<BluetoothServiceDomain>? = null,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BluetoothDataPacket

        if (timestamp != other.timestamp) return false
        if (!data.contentEquals(other.data)) return false
        if (source != other.source) return false
        if (format != other.format) return false
        if (text != other.text) return false
        if (gattServices != other.gattServices) return false
        if (serviceUuid != other.serviceUuid) return false
        if (characteristicUuid != other.characteristicUuid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = timestamp.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (source?.hashCode() ?: 0)
        result = 31 * result + format.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (gattServices?.hashCode() ?: 0)
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        result = 31 * result + (characteristicUuid?.hashCode() ?: 0)
        return result
    }
}

enum class DataFormat {
    HEX_ASCII, // For raw binary streams
    STRUCTURED,  // For human-readable strings like service info
    GATT_STRUCTURE
}

enum class BluetoothConnectionState(val state: Int) {
    STATE_CONNECTED(0),
    STATE_DISCONNECTED(1);

    companion object {
        fun fromInt(newValue: Int): BluetoothConnectionState? {
            return entries.find { it.state == newValue }
        }
    }
}

enum class BluetoothProfile(val displayName: String, val uuid: UUID?) {
    SPP("Serial Port (SPP)", UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")),
    GATT("Generic Attribute (GATT)", null),
    HID("Human Interface Device", UUID.fromString("00001124-0000-1000-8000-00805f9b34fb")),
    A2DP("Advanced Audio (A2DP)", UUID.fromString("0000110b-0000-1000-8000-00805f9b34fb")),
    BATTERY("Battery Service", UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb"));

    companion object {
        fun fromUuid(uuidString: String): BluetoothProfile? {
            return entries.find { it.uuid?.toString().equals(uuidString, ignoreCase = true) }
        }
    }
}
