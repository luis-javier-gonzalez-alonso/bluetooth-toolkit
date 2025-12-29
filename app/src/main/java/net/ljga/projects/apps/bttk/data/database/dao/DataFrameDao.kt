package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entities.DataFrame

@Dao
interface DataFrameDao {
    @Query("SELECT * FROM dataframe ORDER BY name ASC")
    fun getDataFrames(): Flow<List<DataFrame>>

    @Insert
    suspend fun insertDataFrame(item: DataFrame)

    @Query("DELETE FROM dataframe WHERE uid = :uid")
    suspend fun deleteDataFrame(uid: Int)
}