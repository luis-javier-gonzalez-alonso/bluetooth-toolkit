package net.ljga.projects.apps.bttk.domain.model.process

open class GattCharacteristicRequest(
    val serviceUuid: String,
    val characteristicUuid: String
) : ProcessRequest {
}