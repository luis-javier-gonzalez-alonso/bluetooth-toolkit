package net.ljga.projects.apps.bttk.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entities.SavedDevice

@Dao
interface SavedDeviceDao {
    @Query("SELECT * FROM saved_devices")
    fun getSavedDevices(): Flow<List<SavedDevice>>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun insertDevice(device: SavedDevice)

    @Query("SELECT * FROM saved_devices WHERE address = :address")
    suspend fun getDevice(address: String): SavedDevice?

    @Query("DELETE FROM saved_devices WHERE address = :address")
    suspend fun deleteDevice(address: String)
}