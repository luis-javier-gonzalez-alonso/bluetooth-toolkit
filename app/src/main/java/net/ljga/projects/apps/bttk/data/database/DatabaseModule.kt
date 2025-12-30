package net.ljga.projects.apps.bttk.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothDeviceDao
import net.ljga.projects.apps.bttk.data.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicSettingsDao
import net.ljga.projects.apps.bttk.data.database.dao.GattScriptDao
import net.ljga.projects.apps.bttk.data.database.dao.GattServerDao
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
    fun provideBluetoothDeviceDao(appDatabase: AppDatabase): BluetoothDeviceDao {
        return appDatabase.bluetoothDeviceDao()
    }

    @Provides
    fun provideDataFrameDao(appDatabase: AppDatabase): DataFrameDao {
        return appDatabase.dataFrameDao()
    }

    @Provides
    fun provideGattCharacteristicSettingsDao(appDatabase: AppDatabase): GattCharacteristicSettingsDao {
        return appDatabase.gattCharacteristicSettingsDao()
    }

    @Provides
    fun provideGattScriptDao(appDatabase: AppDatabase): GattScriptDao {
        return appDatabase.gattScriptDao()
    }

    @Provides
    fun provideGattServerDao(appDatabase: AppDatabase): GattServerDao {
        return appDatabase.gattServerDao()
    }
}