package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(tableName = "gatt_scripts")
data class GattScriptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val operations: List<GattScriptOperation>
)

@Serializable
data class GattScriptOperation(
    val type: GattScriptOperationType,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null,
    val data: ByteArray? = null,
    val delayMs: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as GattScriptOperation
        if (type != other.type) return false
        if (serviceUuid != other.serviceUuid) return false
        if (characteristicUuid != other.characteristicUuid) return false
        if (data != null) {
            if (other.data == null) return false
            if (!data.contentEquals(other.data)) return false
        } else if (other.data != null) return false
        if (delayMs != other.delayMs) return false
        return true
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + (serviceUuid?.hashCode() ?: 0)
        result = 31 * result + (characteristicUuid?.hashCode() ?: 0)
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + (delayMs?.hashCode() ?: 0)
        return result
    }
}


enum class GattScriptOperationType {
    READ, WRITE, DELAY
}

class GattScriptOperationsConverter {
    @TypeConverter
    fun fromOperationsList(operations: List<GattScriptOperation>): String {
        return Json.encodeToString(operations)
    }

    @TypeConverter
    fun toOperationsList(operationsJson: String): List<GattScriptOperation> {
        return Json.decodeFromString(operationsJson)
    }
}

