package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.GattAlias

@Dao
interface GattAliasDao {
    @Query("SELECT * FROM GattAlias")
    fun getAllAliases(): Flow<List<GattAlias>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: GattAlias)

    @Query("DELETE FROM GattAlias WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun deleteAlias(serviceUuid: String, characteristicUuid: String)
}