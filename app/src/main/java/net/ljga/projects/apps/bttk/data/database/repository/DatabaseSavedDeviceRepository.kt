package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.database.dao.GattAliasDao
import net.ljga.projects.apps.bttk.database.dao.SavedDeviceDao
import net.ljga.projects.apps.bttk.database.entities.GattAlias
import net.ljga.projects.apps.bttk.database.entities.SavedDevice
import net.ljga.projects.apps.bttk.domain.repository.SavedDeviceRepository
import net.ljga.projects.apps.bttk.data.toDomain
import javax.inject.Inject

class DatabaseSavedDeviceRepository @Inject constructor(
    private val savedDeviceDao: SavedDeviceDao,
    private val gattAliasDao: GattAliasDao
) : SavedDeviceRepository {

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> =
        savedDeviceDao.getSavedDevices().map { devices ->
            devices.map { it.toDomain() }
        }

    override val gattAliases: Flow<Map<String, String>> =
        gattAliasDao.getAllAliases().map { aliases ->
            aliases.associate { "${it.serviceUuid}-${it.characteristicUuid}" to it.alias }
        }

    override suspend fun saveDevice(device: BluetoothDeviceDomain) {
        val existing = savedDeviceDao.getDevice(device.address)
        val servicesJson = if (device.services.isNotEmpty()) {
            Json.encodeToString(device.services)
        } else {
            existing?.servicesJson
        }

        savedDeviceDao.insertDevice(
            SavedDevice(
                address = device.address,
                name = device.name,
                servicesJson = servicesJson
            )
        )
    }

    override suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>) {
        val existing = savedDeviceDao.getDevice(address)
        if (existing != null) {
            savedDeviceDao.insertDevice(
                existing.copy(servicesJson = Json.encodeToString(services))
            )
        }
    }

    override suspend fun forgetDevice(address: String) {
        savedDeviceDao.deleteDevice(address)
    }

    override suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        if (alias.isBlank()) {
            gattAliasDao.deleteAlias(serviceUuid, characteristicUuid)
        } else {
            gattAliasDao.insertAlias(GattAlias(serviceUuid, characteristicUuid, alias))
        }
    }
}
