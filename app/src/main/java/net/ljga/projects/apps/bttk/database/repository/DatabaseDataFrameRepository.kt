package net.ljga.projects.apps.bttk.database.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.entities.DataFrame
import net.ljga.projects.apps.bttk.data.repository.DataFrameRepository
import javax.inject.Inject

class DatabaseDataFrameRepository @Inject constructor(
    private val dataFrameDao: DataFrameDao
) : DataFrameRepository {

    override val dataFrames: Flow<List<DataFrame>> = dataFrameDao.getDataFrames()

    override suspend fun add(name: String, data: ByteArray) {
        dataFrameDao.insertDataFrame(DataFrame(name = name, data = data))
    }

    override suspend fun remove(uid: Int) {
        dataFrameDao.deleteDataFrame(uid)
    }
}