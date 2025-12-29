package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain

@Serializable
data class GattServerStateData(
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int,
    val serviceIndices: Map<String, Int> = emptyMap(),
    val serviceNextCharIndices: Map<String, Int> = emptyMap(),
    val deviceName: String? = null
)

interface GattServerRepository {
    val config: Flow<GattServerStateData>

    suspend fun saveConfig(
        services: List<BluetoothServiceDomain>,
        nextServiceIndex: Int,
        serviceIndices: Map<String, Int>,
        serviceNextCharIndices: Map<String, Int>,
        deviceName: String?
    )
}
