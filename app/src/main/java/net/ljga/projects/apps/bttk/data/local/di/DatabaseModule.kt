package net.ljga.projects.apps.bttk.data.local.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.local.database.AppDatabase
import net.ljga.projects.apps.bttk.data.local.database.CharacteristicParserDao
import net.ljga.projects.apps.bttk.data.local.database.DataFrameDao
import net.ljga.projects.apps.bttk.data.local.database.GattAliasDao
import net.ljga.projects.apps.bttk.data.local.database.GattServerDao
import net.ljga.projects.apps.bttk.data.local.database.SavedDeviceDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class DatabaseModule {
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
    @Singleton
    fun provideAppDatabase(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "BluetoothToolkit"
        ).fallbackToDestructiveMigration()
            .build()
    }
}
