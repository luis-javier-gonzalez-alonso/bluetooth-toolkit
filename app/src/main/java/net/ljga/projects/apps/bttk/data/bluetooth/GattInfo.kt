package net.ljga.projects.apps.bttk.data.bluetooth

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothServiceDomain(
    val uuid: String,
    val characteristics: List<BluetoothCharacteristicDomain>
)

@Serializable
data class BluetoothCharacteristicDomain(
    val uuid: String,
    val properties: List<String>,
    val descriptors: List<String> = emptyList()
)
