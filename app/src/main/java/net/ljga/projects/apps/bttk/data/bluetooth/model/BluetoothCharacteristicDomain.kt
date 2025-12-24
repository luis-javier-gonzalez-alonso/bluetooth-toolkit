package net.ljga.projects.apps.bttk.data.bluetooth.model

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothCharacteristicDomain(
    val uuid: String,
    val properties: List<String>,
    val descriptors: List<String> = emptyList()
)