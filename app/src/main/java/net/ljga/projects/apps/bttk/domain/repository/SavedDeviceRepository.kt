package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

interface SavedDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    val gattAliases: Flow<Map<String, String>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
    suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>)
    suspend fun forgetDevice(address: String)
    suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String)
}
