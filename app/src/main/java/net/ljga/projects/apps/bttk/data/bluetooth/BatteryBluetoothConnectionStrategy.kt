package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.*

class BatteryBluetoothConnectionStrategy(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionStrategy {
    override val name: String = "Battery Service"
    override val uuid: UUID = BluetoothProfile.BATTERY.uuid!!
    
    private var bluetoothGatt: BluetoothGatt? = null

    private val BATTERY_LEVEL_CHARACTERISTIC_UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")
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
                    val service = gatt.getService(uuid)
                    val characteristic = service?.getCharacteristic(BATTERY_LEVEL_CHARACTERISTIC_UUID)
                    
                    if (characteristic != null) {
                        // 1. Initial read
                        gatt.readCharacteristic(characteristic)
                        
                        // 2. Enable notifications if supported
                        if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                            gatt.setCharacteristicNotification(characteristic, true)
                            characteristic.getDescriptor(CCCD_UUID)?.let { descriptor ->
                                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                                gatt.writeDescriptor(descriptor)
                            }
                        }
                    } else {
                        trySend(BluetoothDataPacket(data = "Battery characteristic not found".toByteArray(), source = "Battery"))
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    emitBatteryLevel(characteristic)
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    emitBatteryLevel(value)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
                emitBatteryLevel(characteristic)
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                emitBatteryLevel(value)
            }

            private fun emitBatteryLevel(characteristic: BluetoothGattCharacteristic) {
                val value = characteristic.value
                emitBatteryLevel(value)
            }

            private fun emitBatteryLevel(value: ByteArray) {
                if (value.isNotEmpty()) {
                    val level = value[0].toInt() and 0xFF
                    trySend(BluetoothDataPacket(
                        data = "Battery Level: $level%".toByteArray(),
                        source = "Battery"
                    ))
                }
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            disconnectInternal()
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
