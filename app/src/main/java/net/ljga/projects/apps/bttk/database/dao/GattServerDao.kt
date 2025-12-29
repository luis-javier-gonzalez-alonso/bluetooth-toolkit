package net.ljga.projects.apps.bttk.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entities.GattServerConfig

@Dao
interface GattServerDao {
    @Query("SELECT * FROM gatt_server_config WHERE id = 1")
    fun getConfig(): Flow<GattServerConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveConfig(config: GattServerConfig)
}
