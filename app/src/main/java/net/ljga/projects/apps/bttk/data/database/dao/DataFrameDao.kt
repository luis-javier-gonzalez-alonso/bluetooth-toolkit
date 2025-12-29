package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.DataFrameEntity

@Dao
interface DataFrameDao {
    @Query("SELECT * FROM data_frames ORDER BY name ASC")
    fun getDataFrames(): Flow<List<DataFrameEntity>>

    @Insert
    suspend fun insertDataFrame(item: DataFrameEntity)

    @Query("DELETE FROM data_frames WHERE uid = :uid")
    suspend fun deleteDataFrame(uid: Int)
}