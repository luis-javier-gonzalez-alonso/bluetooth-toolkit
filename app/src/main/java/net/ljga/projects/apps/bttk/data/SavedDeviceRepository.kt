package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.bluetooth.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.local.database.SavedDevice
import net.ljga.projects.apps.bttk.data.local.database.SavedDeviceDao
import javax.inject.Inject

interface SavedDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
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
        savedDeviceDao.insertDevice(SavedDevice(address = device.address, name = device.name))
    }

    override suspend fun forgetDevice(address: String) {
        savedDeviceDao.deleteDevice(address)
    }
}

private fun SavedDevice.toDomain(): BluetoothDeviceDomain {
    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isInRange = false // Default to false, will be updated in UI state
    )
}
