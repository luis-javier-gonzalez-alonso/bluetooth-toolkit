package net.ljga.projects.apps.bttk.data.bluetooth.utils

import net.ljga.projects.apps.bttk.data.local.database.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.local.database.Endianness
import net.ljga.projects.apps.bttk.data.local.database.FieldType
import net.ljga.projects.apps.bttk.data.local.database.ParserField
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
        if (field.offset >= data.size) return "OOB"
        
        val length = if (field.offset + field.length > data.size) {
            data.size - field.offset
        } else {
            field.length
        }

        val buffer = ByteBuffer.wrap(data, field.offset, length)
        buffer.order(if (field.endianness == Endianness.LITTLE_ENDIAN) ByteOrder.LITTLE_ENDIAN else ByteOrder.BIG_ENDIAN)

        return when (field.type) {
            FieldType.U8 -> buffer.get().toInt() and 0xFF
            FieldType.U16 -> {
                if (length < 2) (buffer.get().toInt() and 0xFF)
                else buffer.short.toInt() and 0xFFFF
            }
            FieldType.U32 -> {
                if (length < 4) 0 // Handle truncated data
                else buffer.int.toLong() and 0xFFFFFFFFL
            }
            FieldType.S8 -> buffer.get().toInt()
            FieldType.S16 -> {
                if (length < 2) buffer.get().toInt()
                else buffer.short.toInt()
            }
            FieldType.S32 -> {
                if (length < 4) 0
                else buffer.int
            }
            FieldType.FLOAT32 -> {
                if (length < 4) 0.0f
                else buffer.float
            }
            FieldType.STRING -> {
                String(data, field.offset, length, Charsets.UTF_8).trim { it <= ' ' }
            }
        }
    }
}
