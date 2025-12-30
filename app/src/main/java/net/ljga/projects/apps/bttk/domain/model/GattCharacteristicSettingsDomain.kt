package net.ljga.projects.apps.bttk.domain.model

data class GattCharacteristicSettingsDomain(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String = "",
    val fields: List<ParserFieldDomain> = emptyList(),
    val template: String = ""
)
