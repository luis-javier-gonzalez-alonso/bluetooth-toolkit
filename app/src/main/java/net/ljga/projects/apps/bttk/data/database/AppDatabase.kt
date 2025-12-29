package net.ljga.projects.apps.bttk.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ljga.projects.apps.bttk.database.dao.BluetoothScriptDao
import net.ljga.projects.apps.bttk.database.dao.CharacteristicParserDao
import net.ljga.projects.apps.bttk.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.database.dao.GattAliasDao
import net.ljga.projects.apps.bttk.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.database.dao.SavedDeviceDao
import net.ljga.projects.apps.bttk.database.entities.BluetoothScript
import net.ljga.projects.apps.bttk.database.entities.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.database.entities.DataFrame
import net.ljga.projects.apps.bttk.database.entities.GattAlias
import net.ljga.projects.apps.bttk.database.entities.GattServerConfig
import net.ljga.projects.apps.bttk.database.entities.ParserFieldsConverter
import net.ljga.projects.apps.bttk.database.entities.SavedDevice
import net.ljga.projects.apps.bttk.database.entities.ScriptOperationsConverter

@Database(
    entities = [
        DataFrame::class,
        SavedDevice::class,
        GattAlias::class,
        GattServerConfig::class,
        CharacteristicParserConfig::class,
        BluetoothScript::class],
    version = 7,
    exportSchema = true
)
@TypeConverters(ParserFieldsConverter::class, ScriptOperationsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataFrameDao(): DataFrameDao
    abstract fun savedDeviceDao(): SavedDeviceDao
    abstract fun gattAliasDao(): GattAliasDao
    abstract fun gattServerDao(): GattServerDao
    abstract fun characteristicParserDao(): CharacteristicParserDao
    abstract fun bluetoothScriptDao(): BluetoothScriptDao
}
