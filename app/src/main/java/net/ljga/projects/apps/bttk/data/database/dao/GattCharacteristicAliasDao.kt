package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicAliasEntity

@Dao
interface GattCharacteristicAliasDao {
    @Query("SELECT * FROM gatt_characteristic_alias")
    fun getAllAliases(): Flow<List<GattCharacteristicAliasEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: GattCharacteristicAliasEntity)

    @Query("DELETE FROM gatt_characteristic_alias WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun deleteAlias(serviceUuid: String, characteristicUuid: String)
}