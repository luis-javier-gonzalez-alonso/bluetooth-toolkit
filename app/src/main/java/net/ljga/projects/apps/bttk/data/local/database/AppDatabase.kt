package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [DataFrame::class, SavedDevice::class, GattAlias::class, GattServerConfig::class, CharacteristicParserConfig::class], version = 6, exportSchema = true)
@TypeConverters(ParserFieldsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataFrameDao(): DataFrameDao
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun gattAliasDao(): GattAliasDao
    abstract fun gattServerDao(): GattServerDao
    abstract fun characteristicParserDao(): CharacteristicParserDao
}
