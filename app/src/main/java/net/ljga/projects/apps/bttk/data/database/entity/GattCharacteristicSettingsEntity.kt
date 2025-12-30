package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Entity(
    tableName = "gatt_characteristic_settings",
    primaryKeys = ["serviceUuid", "characteristicUuid"]
)
data class GattCharacteristicSettingsEntity(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String,
    @TypeConverters(ParserFieldsConverter::class)
    val fields: List<ParserField>,
    val template: String
)

@Serializable
data class ParserField(
    val name: String,
    val offset: Int,
    val length: Int = 0,
    val type: FieldType,
    val endianness: Endianness = Endianness.LITTLE_ENDIAN
)

@Serializable
enum class FieldType(val size: Int?) {
    U8(1), U16(2), U32(4), U64(8),
    I8(1), I16(2), I32(4), I64(8),
    FLOAT(4), DOUBLE(8),
    STRING(null);
}

@Serializable
enum class Endianness {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

class ParserFieldsConverter {
    @TypeConverter
    fun fromFieldsList(value: List<ParserField>): String {
        return Json.encodeToString(value)
    }

    @TypeConverter
    fun toFieldsList(value: String): List<ParserField> {
        return Json.decodeFromString(value)
    }
}
