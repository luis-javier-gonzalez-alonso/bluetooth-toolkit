package net.ljga.projects.apps.bttk.domain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.domain.device_connection.DeviceConnectionController
import net.ljga.projects.apps.bttk.domain.gatt_server.GattServerController
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    @Singleton
    fun provideConnectionController(
        @ApplicationContext context: Context,
        bluetoothDeviceRepository: BluetoothDeviceRepository
    ): DeviceConnectionController {
        return DeviceConnectionController(context, bluetoothDeviceRepository)
    }

    @Provides
    @Singleton
    fun provideDeviceScanController(
        @ApplicationContext context: Context
    ): DeviceScanController {
        return DeviceScanController(context)
    }

    @Provides
    @Singleton
    fun provideGattServerController(
        @ApplicationContext context: Context
    ): GattServerController {
        return GattServerController(context)
    }
}