package net.ljga.projects.apps.bttk.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.database.entities.CharacteristicParserConfig

@Dao
interface CharacteristicParserDao {
    @Query("SELECT * FROM CharacteristicParserConfig")
    fun getAllConfigs(): Flow<List<CharacteristicParserConfig>>

    @Query("SELECT * FROM CharacteristicParserConfig WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfig?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: CharacteristicParserConfig)

    @Query("DELETE FROM CharacteristicParserConfig WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}
