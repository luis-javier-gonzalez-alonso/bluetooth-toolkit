package net.ljga.projects.apps.bttk.domain.device_connection.model.process

open class GattCharacteristicRequest(
    val serviceUuid: String,
    val characteristicUuid: String
) : ProcessRequest