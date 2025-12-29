package net.ljga.projects.apps.bttk.data

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.data.database.repository.DatabaseBluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.database.repository.DatabaseCharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.database.repository.DatabaseDataFrameRepository
import net.ljga.projects.apps.bttk.data.database.repository.DatabaseGattServerRepository
import net.ljga.projects.apps.bttk.data.database.repository.DatabaseSavedDeviceRepository
import net.ljga.projects.apps.bttk.domain.repository.BluetoothScriptRepository
import net.ljga.projects.apps.bttk.domain.repository.CharacteristicParserRepository
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattServerRepository
import net.ljga.projects.apps.bttk.domain.repository.SavedDeviceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface DataModule {

    @Singleton
    @Binds
    fun bindsDataFrameRepository(
        dataFrameRepository: DatabaseDataFrameRepository
    ): DataFrameRepository

    @Singleton
    @Binds
    fun bindsSavedDeviceRepository(
        savedDeviceRepository: DatabaseSavedDeviceRepository
    ): SavedDeviceRepository

    @Singleton
    @Binds
    fun bindsCharacteristicParserRepository(
        characteristicParserRepository: DatabaseCharacteristicParserRepository
    ): CharacteristicParserRepository

    @Singleton
    @Binds
    fun bindsBluetoothScriptRepository(
        bluetoothScriptRepository: DatabaseBluetoothScriptRepository
    ): BluetoothScriptRepository

    @Singleton
    @Binds
    fun bindsGattServerRepository(
        gattServerRepository: DatabaseGattServerRepository
    ): GattServerRepository
}
