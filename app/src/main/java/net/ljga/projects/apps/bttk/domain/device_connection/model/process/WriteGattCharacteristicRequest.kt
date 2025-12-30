package net.ljga.projects.apps.bttk.domain.device_connection.model.process

class WriteGattCharacteristicRequest(
    serviceUuid: String,
    characteristicUuid: String,
    val data: ByteArray
) : GattCharacteristicRequest(serviceUuid, characteristicUuid)