package net.ljga.projects.apps.bttk.bluetooth.utils

import net.ljga.projects.apps.bttk.database.entities.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.database.entities.Endianness
import net.ljga.projects.apps.bttk.database.entities.FieldType
import net.ljga.projects.apps.bttk.database.entities.ParserField
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CharacteristicParser {

    fun parse(data: ByteArray, config: CharacteristicParserConfig): String {
        val values = mutableMapOf<String, Any>()

        for (field in config.fields) {
            try {
                val value = parseField(data, field)
                values[field.name] = value
            } catch (e: Exception) {
                values[field.name] = "Error"
            }
        }

        var result = config.template
        for ((name, value) in values) {
            result = result.replace("{$name}", value.toString())
        }

        return result
    }

    private fun parseField(data: ByteArray, field: ParserField): Any {
        val length = field.type.length ?: field.length
        val buffer = ByteBuffer.wrap(data, field.offset, length)
        buffer.order(
            when (field.endianness) {
                Endianness.LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
                Endianness.BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
            }
        )

        return when (field.type) {
            FieldType.U8 -> buffer.get().toUByte()
            FieldType.U16 -> buffer.getShort().toUShort()
            FieldType.U32 -> buffer.getInt().toUInt()
            FieldType.U64 -> buffer.getLong().toULong()
            FieldType.I8 -> buffer.get()
            FieldType.I16 -> buffer.getShort()
            FieldType.I32 -> buffer.getInt()
            FieldType.I64 -> buffer.getLong()
            FieldType.FLOAT -> buffer.getFloat()
            FieldType.DOUBLE -> buffer.getDouble()
            FieldType.STRING -> String(buffer.array(), Charsets.UTF_8)
        }
    }
}
