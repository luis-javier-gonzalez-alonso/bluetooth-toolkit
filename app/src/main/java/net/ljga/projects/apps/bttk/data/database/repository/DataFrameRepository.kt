package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.DataFrame
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import javax.inject.Inject

interface DataFrameRepository {
    val dataFrames: Flow<List<DataFrame>>

    suspend fun add(name: String, data: ByteArray)
    suspend fun remove(uid: Int)
}

class DefaultDataFrameRepository @Inject constructor(
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
