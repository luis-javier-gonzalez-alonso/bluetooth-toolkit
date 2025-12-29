package net.ljga.projects.apps.bttk.database.entities

import androidx.room.Entity
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class Endianness {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

@Serializable
enum class FieldType(val length: Int?) {
    //    U8(1), U16(2), U32(4), S8(1), S16(2), S32(4), FLOAT32(4), STRING(null);
    U8(1), U16(2), U32(4), U64(8),
    I8(1), I16(2), I32(4), I64(8),
    FLOAT(4), DOUBLE(8),
    STRING(null);
}

@Serializable
data class ParserField(
    val name: String,
    val offset: Int,
    val length: Int = 0,
    val type: FieldType,
    val endianness: Endianness = Endianness.LITTLE_ENDIAN
)

@Entity(primaryKeys = ["serviceUuid", "characteristicUuid"])
data class CharacteristicParserConfig(
    val serviceUuid: String,
    val characteristicUuid: String,
    @TypeConverters(ParserFieldsConverter::class)
    val fields: List<ParserField>,
    val template: String
)

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
