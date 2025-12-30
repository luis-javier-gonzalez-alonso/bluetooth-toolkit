package net.ljga.projects.apps.bttk.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.repository.DatabaseBluetoothDeviceRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseDataFrameRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattCharacteristicSettingsRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattScriptRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattServerRepository
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicSettingsRepository
import net.ljga.projects.apps.bttk.domain.repository.GattScriptRepository
import net.ljga.projects.apps.bttk.domain.repository.GattServerRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindBluetoothDeviceRepository(
        databaseBluetoothDeviceRepository: DatabaseBluetoothDeviceRepository
    ): BluetoothDeviceRepository

    @Singleton
    @Binds
    fun bindsDataFrameRepository(
        databaseDataFrameRepository: DatabaseDataFrameRepository
    ): DataFrameRepository

    @Singleton
    @Binds
    fun bindsGattCharacteristicSettingsRepository(
        databaseGattCharacteristicSettingsRepository: DatabaseGattCharacteristicSettingsRepository
    ): GattCharacteristicSettingsRepository

    @Singleton
    @Binds
    fun bindsGattScriptRepository(
        databaseGattScriptRepository: DatabaseGattScriptRepository
    ): GattScriptRepository

    @Singleton
    @Binds
    fun bindsGattServerRepository(
        databaseGattServerRepository: DatabaseGattServerRepository
    ): GattServerRepository
}
