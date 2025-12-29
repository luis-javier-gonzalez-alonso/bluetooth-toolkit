package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptEntity

@Dao
interface GattScriptDao {
    @Query("SELECT * FROM gatt_scripts ORDER BY name ASC")
    fun getAllScripts(): Flow<List<GattScriptEntity>>

    @Query("SELECT * FROM gatt_scripts WHERE id = :id")
    suspend fun getScriptById(id: Int): GattScriptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: GattScriptEntity): Long

    @Query("DELETE FROM gatt_scripts WHERE id = :id")
    suspend fun deleteScript(id: Int)
}