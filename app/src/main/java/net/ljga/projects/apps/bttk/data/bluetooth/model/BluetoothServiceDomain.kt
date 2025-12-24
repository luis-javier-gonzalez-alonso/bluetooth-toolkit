package net.ljga.projects.apps.bttk.data.bluetooth.model

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothServiceDomain(
    val uuid: String,
    val characteristics: List<BluetoothCharacteristicDomain>
)