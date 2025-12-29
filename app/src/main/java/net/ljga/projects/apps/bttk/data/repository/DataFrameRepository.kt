package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.database.entities.DataFrame

interface DataFrameRepository {
    val dataFrames: Flow<List<DataFrame>>

    suspend fun add(name: String, data: ByteArray)
    suspend fun remove(uid: Int)
}
