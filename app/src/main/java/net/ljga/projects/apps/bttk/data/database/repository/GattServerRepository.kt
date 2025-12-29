package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.database.entity.GattServerConfig
import net.ljga.projects.apps.bttk.data.database.dao.GattServerDao
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GattServerStateData(
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int,
    val serviceIndices: Map<String, Int> = emptyMap(),
    val serviceNextCharIndices: Map<String, Int> = emptyMap(),
    val deviceName: String? = null
)

@Singleton
class GattServerRepository @Inject constructor(
    private val gattServerDao: GattServerDao
) {
    val config: Flow<GattServerStateData> = gattServerDao.getConfig().map {
        it?.servicesJson?.let { json ->
            try {
                Json.decodeFromString<GattServerStateData>(json)
            } catch (e: Exception) {
                try {
                    val services = Json.decodeFromString<List<BluetoothServiceDomain>>(json)
                    GattServerStateData(services, services.size)
                } catch (e2: Exception) {
                    GattServerStateData(emptyList(), 0)
                }
            }
        } ?: GattServerStateData(emptyList(), 0)
    }

    suspend fun saveConfig(
        services: List<BluetoothServiceDomain>, 
        nextServiceIndex: Int,
        serviceIndices: Map<String, Int>,
        serviceNextCharIndices: Map<String, Int>,
        deviceName: String?
    ) {
        val data = GattServerStateData(services, nextServiceIndex, serviceIndices, serviceNextCharIndices, deviceName)
        val json = Json.encodeToString(data)
        gattServerDao.saveConfig(GattServerConfig(servicesJson = json))
    }
}
