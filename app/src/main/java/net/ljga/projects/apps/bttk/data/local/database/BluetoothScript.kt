package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.TypeConverter
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class ScriptOperationType {
    READ, WRITE, DELAY
}

@Serializable
data class BluetoothScriptOperation(
    val type: ScriptOperationType,
    val serviceUuid: String? = null,
    val characteristicUuid: String? = null,
    val data: ByteArray? = null,
    val delayMs: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BluetoothScriptOperation
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

@Entity(tableName = "bluetooth_scripts")
data class BluetoothScript(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val operations: List<BluetoothScriptOperation>
)

class ScriptOperationsConverter {
    @TypeConverter
    fun fromOperationsList(operations: List<BluetoothScriptOperation>): String {
        return Json.encodeToString(operations)
    }

    @TypeConverter
    fun toOperationsList(operationsJson: String): List<BluetoothScriptOperation> {
        return Json.decodeFromString(operationsJson)
    }
}

@Dao
interface BluetoothScriptDao {
    @Query("SELECT * FROM bluetooth_scripts ORDER BY name ASC")
    fun getAllScripts(): Flow<List<BluetoothScript>>

    @Query("SELECT * FROM bluetooth_scripts WHERE id = :id")
    suspend fun getScriptById(id: Int): BluetoothScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: BluetoothScript): Long

    @Query("DELETE FROM bluetooth_scripts WHERE id = :id")
    suspend fun deleteScript(id: Int)
}
