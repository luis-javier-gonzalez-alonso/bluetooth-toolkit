package net.ljga.projects.apps.bttk.domain.utils

import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain
import net.ljga.projects.apps.bttk.domain.model.EndiannessDomain
import net.ljga.projects.apps.bttk.domain.model.FieldTypeDomain
import net.ljga.projects.apps.bttk.domain.model.ParserFieldDomain
import java.nio.ByteBuffer
import java.nio.ByteOrder

object CharacteristicParser {

    fun parse(data: ByteArray, config: CharacteristicParserConfigDomain): String {
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

    private fun parseField(data: ByteArray, field: ParserFieldDomain): Any {
        val length = field.type.length ?: field.length
        val buffer = ByteBuffer.wrap(data, field.offset, length)
        buffer.order(
            when (field.endianness) {
                EndiannessDomain.LITTLE_ENDIAN -> ByteOrder.LITTLE_ENDIAN
                EndiannessDomain.BIG_ENDIAN -> ByteOrder.BIG_ENDIAN
            }
        )

        return when (field.type) {
            FieldTypeDomain.U8 -> buffer.get().toUByte()
            FieldTypeDomain.U16 -> buffer.getShort().toUShort()
            FieldTypeDomain.U32 -> buffer.getInt().toUInt()
            FieldTypeDomain.U64 -> buffer.getLong().toULong()
            FieldTypeDomain.I8 -> buffer.get()
            FieldTypeDomain.I16 -> buffer.getShort()
            FieldTypeDomain.I32 -> buffer.getInt()
            FieldTypeDomain.I64 -> buffer.getLong()
            FieldTypeDomain.FLOAT -> buffer.getFloat()
            FieldTypeDomain.DOUBLE -> buffer.getDouble()
            FieldTypeDomain.STRING -> String(buffer.array(), Charsets.UTF_8)
        }
    }
}
