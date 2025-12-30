package net.ljga.projects.apps.bttk.domain.device_connection.model

import net.ljga.projects.apps.bttk.domain.model.ParserFieldDomain

data class GattCharacteristicSettingsDomain(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String = "",
    val fields: List<ParserFieldDomain> = emptyList(),
    val template: String = ""
)