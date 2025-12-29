package net.ljga.projects.apps.bttk.domain.model

enum class EndiannessDomain {
    LITTLE_ENDIAN,
    BIG_ENDIAN
}

enum class FieldTypeDomain(val length: Int?) {
    U8(1), U16(2), U32(4), U64(8),
    I8(1), I16(2), I32(4), I64(8),
    FLOAT(4), DOUBLE(8),
    STRING(null);
}

data class ParserFieldDomain(
    val name: String,
    val offset: Int,
    val length: Int = 0,
    val type: FieldTypeDomain,
    val endianness: EndiannessDomain = EndiannessDomain.LITTLE_ENDIAN
)

data class CharacteristicParserConfigDomain(
    val serviceUuid: String,
    val characteristicUuid: String,
    val fields: List<ParserFieldDomain>,
    val template: String
)
