package net.ljga.projects.apps.bttk.domain.model

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
