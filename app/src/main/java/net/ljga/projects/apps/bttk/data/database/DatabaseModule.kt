package net.ljga.projects.apps.bttk.data.database

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.database.dao.BluetoothScriptDao
import net.ljga.projects.apps.bttk.database.dao.CharacteristicParserDao
import net.ljga.projects.apps.bttk.database.dao.DataFrameDao
import net.ljga.projects.apps.bttk.database.dao.GattAliasDao
import net.ljga.projects.apps.bttk.database.dao.GattServerDao
import net.ljga.projects.apps.bttk.database.dao.SavedDeviceDao
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
    fun provideSavedDeviceDao(appDatabase: AppDatabase): SavedDeviceDao {
        return appDatabase.savedDeviceDao()
    }

    @Provides
    fun provideGattServerDao(appDatabase: AppDatabase): GattServerDao {
        return appDatabase.gattServerDao()
    }

    @Provides
    fun provideGattAliasDao(appDatabase: AppDatabase): GattAliasDao {
        return appDatabase.gattAliasDao()
    }

    @Provides
    fun provideCharacteristicParserDao(appDatabase: AppDatabase): CharacteristicParserDao {
        return appDatabase.characteristicParserDao()
    }

    @Provides
    fun provideBluetoothScriptDao(appDatabase: AppDatabase): BluetoothScriptDao {
        return appDatabase.bluetoothScriptDao()
    }
}