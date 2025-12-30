package net.ljga.projects.apps.bttk.domain.model.process

class WriteGattCharacteristicRequest : GattCharacteristicRequest {
    val data: ByteArray

    constructor(serviceUuid: String, characteristicUuid: String, data: ByteArray) :
            super(serviceUuid, characteristicUuid) {
        this.data = data
    }
}