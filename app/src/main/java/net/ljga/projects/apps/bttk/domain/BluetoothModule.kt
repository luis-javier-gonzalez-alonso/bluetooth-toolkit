package net.ljga.projects.apps.bttk.domain

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

//    @Provides
//    @Singleton
//    fun provideBluetoothController(@ApplicationContext context: Context): BluetoothController {
//        return AndroidBluetoothController(context)
//    }

    @Provides
    @Singleton
    fun provideConnectionController(
        @ApplicationContext context: Context,
        bluetoothDeviceRepository: BluetoothDeviceRepository
    ): ConnectionController {
        return ConnectionController(context, bluetoothDeviceRepository)
    }

    @Provides
    @Singleton
    fun provideScannerController(
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