package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.entity.DataFrameEntity
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import javax.inject.Inject

class DatabaseDataFrameRepository @Inject constructor(
    private val dataFrameDao: DataFrameDao
) : DataFrameRepository {

    override val dataFrames: Flow<List<DataFrameDomain>> = dataFrameDao.getDataFrames().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun add(name: String, data: ByteArray) {
        dataFrameDao.insertDataFrame(DataFrameEntity(name = name, data = data))
    }

    override suspend fun remove(uid: Int) {
        dataFrameDao.deleteDataFrame(uid)
    }
}
