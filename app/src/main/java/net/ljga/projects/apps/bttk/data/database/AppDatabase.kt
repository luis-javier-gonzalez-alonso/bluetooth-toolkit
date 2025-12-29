package net.ljga.projects.apps.bttk.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import net.ljga.projects.apps.bttk.data.database.dao.GattScriptDao
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicParserDao
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicAliasDao
import net.ljga.projects.apps.bttk.data.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothDeviceDao
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothDeviceEntity
import net.ljga.projects.apps.bttk.data.database.entity.DataFrameEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicAliasEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicParserEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptOperationsConverter
import net.ljga.projects.apps.bttk.data.database.entity.GattServerEntity
import net.ljga.projects.apps.bttk.data.database.entity.ParserFieldsConverter

@Database(
    entities = [
        DataFrameEntity::class,
        BluetoothDeviceEntity::class,
        GattCharacteristicAliasEntity::class,
        GattServerEntity::class,
        GattCharacteristicParserEntity::class,
        GattScriptEntity::class],
    version = 8,
    exportSchema = true
)
@TypeConverters(ParserFieldsConverter::class, GattScriptOperationsConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dataFrameDao(): DataFrameDao
    abstract fun savedDeviceDao(): BluetoothDeviceDao
    abstract fun gattAliasDao(): GattCharacteristicAliasDao
    abstract fun gattServerDao(): GattServerDao
    abstract fun characteristicParserDao(): GattCharacteristicParserDao
    abstract fun bluetoothScriptDao(): GattScriptDao
}
