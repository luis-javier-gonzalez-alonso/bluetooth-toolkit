package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.database.entities.BluetoothScript

@Dao
interface BluetoothScriptDao {
    @Query("SELECT * FROM bluetooth_scripts ORDER BY name ASC")
    fun getAllScripts(): Flow<List<BluetoothScript>>

    @Query("SELECT * FROM bluetooth_scripts WHERE id = :id")
    suspend fun getScriptById(id: Int): BluetoothScript?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: BluetoothScript): Long

    @Query("DELETE FROM bluetooth_scripts WHERE id = :id")
    suspend fun deleteScript(id: Int)
}