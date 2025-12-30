package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothDeviceDao
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothDeviceEntity
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.domain.device_scan.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import javax.inject.Inject

class DatabaseBluetoothDeviceRepository @Inject constructor(
    private val bluetoothDeviceDao: BluetoothDeviceDao
) : BluetoothDeviceRepository {

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> =
        bluetoothDeviceDao.getSavedDevices().map { devices ->
            devices.map { it.toDomain() }
        }

    override suspend fun saveDevice(device: BluetoothDeviceDomain) {
        val existing = bluetoothDeviceDao.getDevice(device.address)
        val servicesJson = if (device.services.isNotEmpty()) {
            Json.encodeToString(device.services)
        } else {
            existing?.servicesJson
        }

        bluetoothDeviceDao.insertDevice(
            BluetoothDeviceEntity(
                address = device.address,
                name = device.name,
                servicesJson = servicesJson
            )
        )
    }

    override suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>) {
        val existing = bluetoothDeviceDao.getDevice(address)
        if (existing != null) {
            bluetoothDeviceDao.insertDevice(
                existing.copy(servicesJson = Json.encodeToString(services))
            )
        }
    }

    override suspend fun forgetDevice(address: String) {
        bluetoothDeviceDao.deleteDevice(address)
    }
}