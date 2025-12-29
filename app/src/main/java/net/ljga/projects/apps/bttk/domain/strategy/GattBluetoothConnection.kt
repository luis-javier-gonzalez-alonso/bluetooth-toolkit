package net.ljga.projects.apps.bttk.domain.strategy

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.ljga.projects.apps.bttk.domain.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.utils.prettyName
import java.util.UUID

class GattBluetoothConnection(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : BluetoothConnection {
    override val type: BluetoothConnectionType = BluetoothConnectionType.GATT

    private val cccd: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var bluetoothGatt: BluetoothGatt? = null

    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = callbackFlow {
        val device = bluetoothAdapter.getRemoteDevice(address)

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    close()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
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
                    trySend(
                        BluetoothDataPacket(
                            format = DataFormat.GATT_STRUCTURE,
                            gattServices = services,
                            source = address
                        )
                    )
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    processData(characteristic, value)
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                processData(characteristic, value)
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
        }

        bluetoothGatt = device.connectGatt(context, false, gattCallback)

        awaitClose {
            disconnectInternal()
        }
    }

    override suspend fun disconnect() {
        disconnectInternal()
    }

    fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            bluetoothGatt?.readCharacteristic(characteristic)
        }
    }

    fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {
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

    fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
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

    fun readDescriptors(serviceUuid: String, characteristicUuid: String) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        characteristic?.descriptors?.forEach { descriptor ->
            bluetoothGatt?.readDescriptor(descriptor)
        }
    }

    private fun enableNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, true)
        val descriptor = characteristic.getDescriptor(cccd)
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

    private fun disableNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ) {
        gatt.setCharacteristicNotification(characteristic, false)
        val descriptor = characteristic.getDescriptor(cccd)
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

    private fun disconnectInternal() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
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
