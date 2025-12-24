package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

abstract class BaseGattBluetoothConnectionStrategy(
    protected val context: Context,
    protected val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionStrategy {

    protected var bluetoothGatt: BluetoothGatt? = null
    protected val handlers = mutableListOf<GattCharacteristicHandler>()

    protected val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = callbackFlow {
        val device = bluetoothAdapter.getRemoteDevice(address)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val current = BluetoothConnectionState.fromInt(status)
                if (current == BluetoothConnectionState.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (current == BluetoothConnectionState.STATE_DISCONNECTED) {
                    close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onGattServicesDiscovered(gatt, this@callbackFlow)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    processData(characteristic, characteristic.value)
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    processData(characteristic, value)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                processData(characteristic, characteristic.value)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                processData(characteristic, value)
            }

            private fun processData(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                handlers.forEach { handler ->
                    val packet = handler.handleData(characteristic, value)
                    if (packet != null) {
                        trySend(packet)
                    }
                }
                onDataReceived(characteristic, value, this@callbackFlow)
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            disconnectInternal()
        }
    }

    protected abstract fun onGattServicesDiscovered(gatt: BluetoothGatt, scope: ProducerScope<BluetoothDataPacket>)
    
    protected open fun onDataReceived(characteristic: BluetoothGattCharacteristic, value: ByteArray, scope: ProducerScope<BluetoothDataPacket>) {}

    @SuppressLint("MissingPermission")
    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            bluetoothGatt?.readCharacteristic(characteristic)
        }
    }

    @SuppressLint("MissingPermission")
    override fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            if (enable) {
                enableNotification(bluetoothGatt!!, characteristic)
            } else {
                disableNotification(bluetoothGatt!!, characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(
                    characteristic,
                    data,
                    if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                        BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    } else {
                        BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                    }
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                characteristic.writeType = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(characteristic)
            }
        }
    }

    @SuppressLint("MissingPermission")
    protected fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            val value = if (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    @SuppressLint("MissingPermission")
    protected fun disableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
        gatt.setCharacteristicNotification(characteristic, false)
        val descriptor = characteristic.getDescriptor(CCCD_UUID)
        if (descriptor != null) {
            val value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value)
            } else {
                @Suppress("DEPRECATION")
                descriptor.value = value
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(descriptor)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun disconnectInternal() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override suspend fun disconnect() {
        disconnectInternal()
    }
}
