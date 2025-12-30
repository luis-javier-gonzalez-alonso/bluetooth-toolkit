package net.ljga.projects.apps.bttk.domain.gatt_server.model

import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

data class GattServerConfigDomain(
    val id: Int = 1,
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int = 0
)