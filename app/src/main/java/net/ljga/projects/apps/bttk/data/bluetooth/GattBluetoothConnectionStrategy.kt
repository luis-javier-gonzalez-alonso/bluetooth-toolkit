package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class GattBluetoothConnectionStrategy(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionStrategy {
    override val name: String = "GATT"
    override val uuid: UUID? = null
    
    private var bluetoothGatt: BluetoothGatt? = null

    private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = callbackFlow {
        val device = bluetoothAdapter.getRemoteDevice(address)
        
        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
//                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
//                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                    close()
//                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.services.forEach { service ->
                        service.characteristics.forEach { characteristic ->
                            if (isNotifyable(characteristic)) {
                                enableNotification(gatt, characteristic)
                            }
                        }
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                val data = characteristic.value
                trySend(BluetoothDataPacket(data = data, source = "GATT: ${characteristic.uuid}"))
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                trySend(BluetoothDataPacket(data = value, source = "GATT: ${characteristic.uuid}"))
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            disconnectInternal()
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableNotification(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
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

    private fun isNotifyable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
               (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
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
