package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.database.entities.SavedDevice

interface SavedDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    val gattAliases: Flow<Map<String, String>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
    suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>)
    suspend fun forgetDevice(address: String)
    suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String)
}

fun SavedDevice.toDomain(): BluetoothDeviceDomain {
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
