package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [DataFrame::class, SavedDevice::class, GattAlias::class, GattServerConfig::class], version = 4, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataFrameDao(): DataFrameDao
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun gattAliasDao(): GattAliasDao
    abstract fun gattServerDao(): GattServerDao
}
