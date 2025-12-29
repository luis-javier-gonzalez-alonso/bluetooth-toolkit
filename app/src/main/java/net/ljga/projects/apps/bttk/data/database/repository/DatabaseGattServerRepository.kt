package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.domain.repository.GattServerRepository
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseGattServerRepository @Inject constructor(
    private val gattServerDao: GattServerDao
) : GattServerRepository {
    override fun getAllServers(): Flow<List<GattServerStateDomain>> = 
        gattServerDao.getAllServers().map { list -> list.map { it.toDomain() } }

    override suspend fun getServerById(id: Int): GattServerStateDomain? = 
        gattServerDao.getServerById(id)?.toDomain()

    override suspend fun saveServer(server: GattServerStateDomain): Int = 
        gattServerDao.saveServer(server.toEntity()).toInt()

    override suspend fun deleteServer(server: GattServerStateDomain) {
        gattServerDao.deleteServer(server.toEntity())
    }
}
