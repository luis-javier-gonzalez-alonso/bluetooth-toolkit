package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain

interface GattServerRepository {
    fun getAllServers(): Flow<List<GattServerStateDomain>>
    suspend fun getServerById(id: Int): GattServerStateDomain?
    suspend fun saveServer(server: GattServerStateDomain): Int
    suspend fun deleteServer(server: GattServerStateDomain)
}