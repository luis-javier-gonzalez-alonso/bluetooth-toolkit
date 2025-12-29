package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicParserEntity

@Dao
interface GattCharacteristicParserDao {
    @Query("SELECT * FROM gatt_characteristic_parsers")
    fun getAllConfigs(): Flow<List<GattCharacteristicParserEntity>>

    @Query("SELECT * FROM gatt_characteristic_parsers WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    fun getConfig(
        serviceUuid: String,
        characteristicUuid: String
    ): Flow<GattCharacteristicParserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: GattCharacteristicParserEntity)

    @Query("DELETE FROM gatt_characteristic_parsers WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}
