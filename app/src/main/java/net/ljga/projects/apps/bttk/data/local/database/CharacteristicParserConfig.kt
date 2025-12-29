package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class Endianness {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

@Serializable
enum class FieldType {
    U8, U16, U32, S8, S16, S32, FLOAT32, STRING
}

@Serializable
data class ParserField(
    val name: String,
    val offset: Int,
    val length: Int,
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
