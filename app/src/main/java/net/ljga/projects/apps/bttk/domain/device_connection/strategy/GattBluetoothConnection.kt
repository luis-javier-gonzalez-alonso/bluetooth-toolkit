package net.ljga.projects.apps.bttk.domain.device_connection.strategy

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import net.ljga.projects.apps.bttk.domain.device_connection.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ReadGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.WriteGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.utils.prettyCharacteristicName
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
        var retryCount = 0
        val maxRetries = 10

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d("GattConnection", "onConnectionStateChange: status=$status, newState=$newState")
                
                if (status != BluetoothGatt.GATT_SUCCESS && retryCount < maxRetries) {
                    retryCount++
                    Log.w("GattConnection", "Encountered status 133, retrying... ($retryCount/$maxRetries)")
                    gatt.close()

                    // Re-attempt connection with a slight delay
                    val retryGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        device.connectGatt(context, false, this, BluetoothDevice.TRANSPORT_LE)
                    } else {
                        device.connectGatt(context, false, this)
                    }
                    bluetoothGatt = retryGatt
                    return
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    retryCount = 0
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    gatt.close()
                    if (gatt == bluetoothGatt) {
                        bluetoothGatt = null
                    }
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

        bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } else {
            device.connectGatt(context, false, gattCallback)
        }

        awaitClose {
            disconnect()
        }
    }

    override fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun process(request: ProcessRequest): BluetoothDataPacket? {
        if (request is ReadGattCharacteristicRequest) {
            readCharacteristic(request.serviceUuid, request.characteristicUuid)
            return null
        } else if (request is WriteGattCharacteristicRequest) {
            writeCharacteristic(request.serviceUuid, request.characteristicUuid, request.data)
            return BluetoothDataPacket(
                data = request.data,
                source = "Write: ${request.characteristicUuid.prettyCharacteristicName()}...",
                format = DataFormat.HEX_ASCII,
                serviceUuid = request.serviceUuid,
                characteristicUuid = request.characteristicUuid
            )
        }
        return null
    }

    private fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val service = bluetoothGatt?.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            bluetoothGatt?.readCharacteristic(characteristic)
        }
    }

    private fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    ) {
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