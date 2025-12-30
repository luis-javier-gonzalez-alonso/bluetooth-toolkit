package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicSettingsEntity

@Dao
interface GattCharacteristicSettingsDao {
    @Query("SELECT * FROM gatt_characteristic_settings")
    fun getAll(): Flow<List<GattCharacteristicSettingsEntity>>

    @Query("SELECT * FROM gatt_characteristic_settings WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun get(serviceUuid: String, characteristicUuid: String): GattCharacteristicSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(settings: GattCharacteristicSettingsEntity)

    @Query("DELETE FROM gatt_characteristic_settings WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun delete(serviceUuid: String, characteristicUuid: String)
}
