package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.local.database.GattServerConfig
import net.ljga.projects.apps.bttk.data.local.database.GattServerDao
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GattServerStateData(
    val services: List<BluetoothServiceDomain>,
    val nextServiceIndex: Int,
    val serviceIndices: Map<String, Int> = emptyMap(),
    val serviceNextCharIndices: Map<String, Int> = emptyMap()
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
        serviceNextCharIndices: Map<String, Int>
    ) {
        val data = GattServerStateData(services, nextServiceIndex, serviceIndices, serviceNextCharIndices)
        val json = Json.encodeToString(data)
        gattServerDao.saveConfig(GattServerConfig(servicesJson = json))
    }
}
