package net.ljga.projects.apps.bttk.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BluetoothServiceDomain(
    val uuid: String,
    val characteristics: List<BluetoothCharacteristicDomain>
)