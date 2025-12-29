package net.ljga.projects.apps.bttk.domain.strategy

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.ljga.projects.apps.bttk.domain.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionState
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.utils.prettyName
import java.util.UUID

class GattBluetoothConnectionStrategy(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnectionStrategy {
    override val name: String = "GATT"
    override val uuid: UUID? = null

    protected var bluetoothGatt: BluetoothGatt? = null

    protected val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = callbackFlow {
        val device = bluetoothAdapter.getRemoteDevice(address)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val current = BluetoothConnectionState.fromInt(newState)
                if (current == BluetoothConnectionState.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (current == BluetoothConnectionState.STATE_DISCONNECTED) {
                    close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    onGattServicesDiscovered(gatt, this@callbackFlow, address)
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

            @Deprecated("Deprecated in Java")
            override fun onDescriptorRead(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    processDescriptorData(descriptor, descriptor.value)
                }
            }

            override fun onDescriptorRead(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int,
                value: ByteArray
            ) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    processDescriptorData(descriptor, value)
                }
            }

            private fun processData(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                trySend(
                    BluetoothDataPacket(
                        data = value,
                        source = "Read: ${characteristic.prettyName()}...",
                        format = DataFormat.HEX_ASCII,
                        serviceUuid = characteristic.service.uuid.toString(),
                        characteristicUuid = characteristic.uuid.toString()
                    )
                )
            }

            private fun processDescriptorData(
                descriptor: BluetoothGattDescriptor,
                value: ByteArray
            ) {
                trySend(
                    BluetoothDataPacket(
                        data = value,
                        source = "Descriptor: ${descriptor.uuid}...",
                        format = DataFormat.HEX_ASCII,
                        serviceUuid = descriptor.characteristic.service.uuid.toString(),
                        characteristicUuid = descriptor.characteristic.uuid.toString()
                    )
                )
            }
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            disconnectInternal()
        }
    }

    @SuppressLint("MissingPermission")
    fun onGattServicesDiscovered(gatt: BluetoothGatt, scope: ProducerScope<BluetoothDataPacket>, address: String) {
        val services = gatt.services.map { service ->
            BluetoothServiceDomain(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    BluetoothCharacteristicDomain(
                        uuid = characteristic.uuid.toString(),
                        properties = getPropertiesList(characteristic.properties),
                        descriptors = characteristic.descriptors.map { it.uuid.toString() }
                    )
                }
            )
        }

        // Emit structured data for persistence and UI
        scope.trySend(
            BluetoothDataPacket(
                format = DataFormat.GATT_STRUCTURE,
                gattServices = services,
                source = address
            )
        )
    }

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
    override fun readDescriptors(serviceUuid: String, characteristicUuid: String) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        characteristic?.descriptors?.forEach { descriptor ->
            bluetoothGatt?.readDescriptor(descriptor)
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

    private fun getPropertiesList(properties: Int): List<String> {
        val list = mutableListOf<String>()
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) list.add("READ")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) list.add("WRITE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) list.add("WRITE_NO_RESPONSE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) list.add("NOTIFY")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) list.add("INDICATE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) list.add("BROADCAST")
        if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) list.add("EXTENDED_PROPS")
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) list.add("SIGNED_WRITE")
        return list
    }
}
