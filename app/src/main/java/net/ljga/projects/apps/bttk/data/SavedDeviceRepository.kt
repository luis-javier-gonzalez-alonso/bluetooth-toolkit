package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.local.database.SavedDevice
import net.ljga.projects.apps.bttk.data.local.database.SavedDeviceDao
import javax.inject.Inject

interface SavedDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
    suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>)
    suspend fun forgetDevice(address: String)
}

class DefaultSavedDeviceRepository @Inject constructor(
    private val savedDeviceDao: SavedDeviceDao
) : SavedDeviceRepository {

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> =
        savedDeviceDao.getSavedDevices().map { devices ->
            devices.map { it.toDomain() }
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
