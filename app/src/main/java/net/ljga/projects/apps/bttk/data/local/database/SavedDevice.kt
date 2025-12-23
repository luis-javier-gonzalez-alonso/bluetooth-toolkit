package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "saved_devices")
data class SavedDevice(
    @PrimaryKey val address: String,
    val name: String?,
    val servicesJson: String? = null
)

@Dao
interface SavedDeviceDao {
    @Query("SELECT * FROM saved_devices")
    fun getSavedDevices(): Flow<List<SavedDevice>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: SavedDevice)

    @Query("SELECT * FROM saved_devices WHERE address = :address")
    suspend fun getDevice(address: String): SavedDevice?

    @Query("DELETE FROM saved_devices WHERE address = :address")
    suspend fun deleteDevice(address: String)
}
