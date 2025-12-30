package net.ljga.projects.apps.bttk.domain.gatt_server.model

import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

data class GattServerStateDomain(
    val id: Int = 0,
    val name: String,
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int,
    val serviceIndices: Map<String, Int> = emptyMap(),
    val serviceNextCharIndices: Map<String, Int> = emptyMap(),
    val deviceName: String? = null
)