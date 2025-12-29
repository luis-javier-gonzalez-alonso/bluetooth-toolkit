package net.ljga.projects.apps.bttk.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.data.repository.GattServerRepository
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DatabaseGattServerRepository @Inject constructor(
    private val gattServerDao: GattServerDao
) : GattServerRepository {
    override val config: Flow<GattServerStateDomain> = gattServerDao.getConfig().map {
        it?.toDomain() ?: GattServerStateDomain(emptyList(), 0)
    }

    override suspend fun saveConfig(state: GattServerStateDomain) {
        gattServerDao.saveConfig(state.toEntity())
    }
}
