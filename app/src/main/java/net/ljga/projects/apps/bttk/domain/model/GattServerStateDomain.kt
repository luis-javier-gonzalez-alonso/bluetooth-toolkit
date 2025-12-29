package net.ljga.projects.apps.bttk.domain.model

data class GattServerStateDomain(
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int,
    val serviceIndices: Map<String, Int> = emptyMap(),
    val serviceNextCharIndices: Map<String, Int> = emptyMap(),
    val deviceName: String? = null
)
