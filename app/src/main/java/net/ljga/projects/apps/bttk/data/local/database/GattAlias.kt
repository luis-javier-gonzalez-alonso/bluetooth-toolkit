package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(primaryKeys = ["serviceUuid", "characteristicUuid"])
data class GattAlias(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String
)

@Dao
interface GattAliasDao {
    @Query("SELECT * FROM GattAlias")
    fun getAllAliases(): Flow<List<GattAlias>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlias(alias: GattAlias)

    @Query("DELETE FROM GattAlias WHERE serviceUuid = :serviceUuid AND characteristicUuid = :characteristicUuid")
    suspend fun deleteAlias(serviceUuid: String, characteristicUuid: String)
}
