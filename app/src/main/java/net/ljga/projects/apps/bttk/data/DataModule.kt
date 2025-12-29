package net.ljga.projects.apps.bttk.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.repository.DatabaseBluetoothDeviceRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseDataFrameRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattCharacteristicAliasRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattCharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattScriptRepository
import net.ljga.projects.apps.bttk.data.repository.DatabaseGattServerRepository
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicAliasRepository
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicParserRepository
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
    fun bindsGattCharacteristicAliasRepository(
        databaseGattCharacteristicAliasRepository: DatabaseGattCharacteristicAliasRepository
    ): GattCharacteristicAliasRepository

    @Singleton
    @Binds
    fun bindsGattCharacteristicParserRepository(
        databaseGattCharacteristicParserRepository: DatabaseGattCharacteristicParserRepository
    ): GattCharacteristicParserRepository

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
