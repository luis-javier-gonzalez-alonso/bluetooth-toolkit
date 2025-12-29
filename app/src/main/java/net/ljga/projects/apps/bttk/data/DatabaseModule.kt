package net.ljga.projects.apps.bttk.data

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.database.AppDatabase
import net.ljga.projects.apps.bttk.data.database.dao.GattScriptDao
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicParserDao
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicAliasDao
import net.ljga.projects.apps.bttk.data.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothDeviceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room
            .databaseBuilder(appContext, AppDatabase::class.java, "BluetoothToolkit")
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    fun provideDataFrameDao(appDatabase: AppDatabase): DataFrameDao {
        return appDatabase.dataFrameDao()
    }

    @Provides
    fun provideSavedDeviceDao(appDatabase: AppDatabase): BluetoothDeviceDao {
        return appDatabase.savedDeviceDao()
    }

    @Provides
    fun provideGattServerDao(appDatabase: AppDatabase): GattServerDao {
        return appDatabase.gattServerDao()
    }

    @Provides
    fun provideGattAliasDao(appDatabase: AppDatabase): GattCharacteristicAliasDao {
        return appDatabase.gattAliasDao()
    }

    @Provides
    fun provideCharacteristicParserDao(appDatabase: AppDatabase): GattCharacteristicParserDao {
        return appDatabase.characteristicParserDao()
    }

    @Provides
    fun provideBluetoothScriptDao(appDatabase: AppDatabase): GattScriptDao {
        return appDatabase.bluetoothScriptDao()
    }
}