package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain

interface DataFrameRepository {
    val dataFrames: Flow<List<DataFrameDomain>>

    suspend fun add(name: String, data: ByteArray)
    suspend fun remove(uid: Int)
}
