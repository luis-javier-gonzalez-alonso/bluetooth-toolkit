package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.database.entities.DataFrame
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import javax.inject.Inject

class DatabaseDataFrameRepository @Inject constructor(
    private val dataFrameDao: DataFrameDao
) : DataFrameRepository {

    override val dataFrames: Flow<List<DataFrameDomain>> = dataFrameDao.getDataFrames().map { entities ->
        entities.map { it.toDomain() }
    }

    override suspend fun add(name: String, data: ByteArray) {
        dataFrameDao.insertDataFrame(DataFrame(name = name, data = data))
    }

    override suspend fun remove(uid: Int) {
        dataFrameDao.deleteDataFrame(uid)
    }
}
