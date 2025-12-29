package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicAliasDao
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothDeviceDao
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothDeviceEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicAliasEntity
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.repository.SavedDeviceRepository
import javax.inject.Inject

class DatabaseSavedDeviceRepository @Inject constructor(
    private val bluetoothDeviceDao: BluetoothDeviceDao,
    private val gattCharacteristicAliasDao: GattCharacteristicAliasDao
) : SavedDeviceRepository {

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> =
        bluetoothDeviceDao.getSavedDevices().map { devices ->
            devices.map { it.toDomain() }
        }

    override val gattAliases: Flow<Map<String, String>> =
        gattCharacteristicAliasDao.getAllAliases().map { aliases ->
            aliases.associate { "${it.serviceUuid}-${it.characteristicUuid}" to it.alias }
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

    override suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        if (alias.isBlank()) {
            gattCharacteristicAliasDao.deleteAlias(serviceUuid, characteristicUuid)
        } else {
            gattCharacteristicAliasDao.insertAlias(
                GattCharacteristicAliasEntity(
                    serviceUuid,
                    characteristicUuid,
                    alias
                )
            )
        }
    }
}
