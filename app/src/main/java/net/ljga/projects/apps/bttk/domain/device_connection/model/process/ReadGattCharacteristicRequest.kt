package net.ljga.projects.apps.bttk.domain.device_connection.model.process

class ReadGattCharacteristicRequest(serviceUuid: String, characteristicUuid: String) :
    GattCharacteristicRequest(serviceUuid, characteristicUuid)