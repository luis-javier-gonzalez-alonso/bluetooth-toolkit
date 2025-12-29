package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothDeviceEntity

@Dao
interface BluetoothDeviceDao {
    @Query("SELECT * FROM bluetooth_devices")
    fun getSavedDevices(): Flow<List<BluetoothDeviceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: BluetoothDeviceEntity)

    @Query("SELECT * FROM bluetooth_devices WHERE address = :address")
    suspend fun getDevice(address: String): BluetoothDeviceEntity?

    @Query("DELETE FROM bluetooth_devices WHERE address = :address")
    suspend fun deleteDevice(address: String)
}