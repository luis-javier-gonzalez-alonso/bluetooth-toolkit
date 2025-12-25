package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.local.database.GattServerConfig
import net.ljga.projects.apps.bttk.data.local.database.GattServerDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GattServerRepository @Inject constructor(
    private val gattServerDao: GattServerDao
) {
    val config: Flow<List<BluetoothServiceDomain>> = gattServerDao.getConfig().map {
        it?.servicesJson?.let { json ->
            try {
                Json.decodeFromString<List<BluetoothServiceDomain>>(json)
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun saveConfig(services: List<BluetoothServiceDomain>) {
        val json = Json.encodeToString(services)
        gattServerDao.saveConfig(GattServerConfig(servicesJson = json))
    }
}
