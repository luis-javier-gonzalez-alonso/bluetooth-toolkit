package net.ljga.projects.apps.bttk.domain.device_connection.model.process

class ToggleNotificationRequest(
    serviceUuid: String,
    characteristicUuid: String,
    val enable: Boolean
) : GattCharacteristicRequest(serviceUuid, characteristicUuid)
