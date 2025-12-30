package net.ljga.projects.apps.bttk.domain.model.process

class ReadGattCharacteristicRequest : GattCharacteristicRequest {
    constructor(serviceUuid: String, characteristicUuid: String) :
            super(serviceUuid, characteristicUuid)
}