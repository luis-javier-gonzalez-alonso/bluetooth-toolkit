package net.ljga.projects.apps.bttk.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.AppSettingsEntity

@Dao
interface AppSettingsDao {
    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getSettings(): Flow<AppSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateSettings(settings: AppSettingsEntity)
}
