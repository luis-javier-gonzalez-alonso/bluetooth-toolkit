package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.local.database.GattAlias
import net.ljga.projects.apps.bttk.data.local.database.GattAliasDao
import net.ljga.projects.apps.bttk.data.local.database.SavedDevice
import net.ljga.projects.apps.bttk.data.local.database.SavedDeviceDao
import javax.inject.Inject

interface SavedDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    val gattAliases: Flow<Map<String, String>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
    suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>)
    suspend fun forgetDevice(address: String)
    suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String)
}

class DefaultSavedDeviceRepository @Inject constructor(
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

private fun SavedDevice.toDomain(): BluetoothDeviceDomain {
    val services = servicesJson?.let {
        try {
            Json.decodeFromString<List<BluetoothServiceDomain>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()

    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isInRange = false,
        services = services
    )
}
