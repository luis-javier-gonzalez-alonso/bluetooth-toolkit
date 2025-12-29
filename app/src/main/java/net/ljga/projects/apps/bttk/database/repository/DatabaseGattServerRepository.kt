package net.ljga.projects.apps.bttk.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.database.entities.GattServerConfig
import net.ljga.projects.apps.bttk.data.repository.GattServerRepository
import net.ljga.projects.apps.bttk.data.repository.GattServerStateData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseGattServerRepository @Inject constructor(
    private val gattServerDao: GattServerDao
) : GattServerRepository {
    override val config: Flow<GattServerStateData> = gattServerDao.getConfig().map {
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

    override suspend fun saveConfig(
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
