package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

interface BluetoothDeviceRepository {
    val savedDevices: Flow<List<BluetoothDeviceDomain>>
    suspend fun saveDevice(device: BluetoothDeviceDomain)
    suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>)
    suspend fun forgetDevice(address: String)
}
