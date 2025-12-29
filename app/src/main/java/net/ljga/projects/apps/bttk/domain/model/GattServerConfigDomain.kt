package net.ljga.projects.apps.bttk.domain.model

data class GattServerConfigDomain(
    val id: Int = 1,
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int = 0
)
