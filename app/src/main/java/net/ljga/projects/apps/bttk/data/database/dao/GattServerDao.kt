package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattServerEntity

@Dao
interface GattServerDao {
    @Query("SELECT * FROM gatt_servers")
    fun getAllServers(): Flow<List<GattServerEntity>>

    @Query("SELECT * FROM gatt_servers WHERE id = :id")
    suspend fun getServerById(id: Int): GattServerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveServer(server: GattServerEntity): Long

    @Delete
    suspend fun deleteServer(server: GattServerEntity)
}
