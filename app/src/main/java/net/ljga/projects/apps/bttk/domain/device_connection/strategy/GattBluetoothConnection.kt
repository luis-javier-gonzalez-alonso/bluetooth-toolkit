package net.ljga.projects.apps.bttk.domain.device_connection.strategy

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.GlobalScope
import net.ljga.projects.apps.bttk.domain.device_connection.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ReadGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.WriteGattCharacteristicRequest
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ToggleNotificationRequest
import net.ljga.projects.apps.bttk.domain.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.utils.prettyCharacteristicName
import net.ljga.projects.apps.bttk.domain.utils.prettyName
import java.util.UUID

private const val TAG = "GattConnection"

@SuppressLint("MissingPermission")
class GattBluetoothConnection(
    private val bluetoothAdapter: BluetoothAdapter,
    private val context: Context
) : BluetoothConnection {
    override val type: BluetoothConnectionType = BluetoothConnectionType.GATT

    private val cccd: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
    private var bluetoothGatt: BluetoothGatt? = null
    private var isManualDisconnect = false

    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = callbackFlow {
        Log.i(TAG, "Initiating GATT connection to $address")
        val device = bluetoothAdapter.getRemoteDevice(address)
        var retryCount = 0
        val maxRetries = 5
        isManualDisconnect = false

        val gattCallback = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                val statusDescription = getGattStatusDescription(status)
                
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.e(TAG, "GATT Error: status=$status ($statusDescription), newState=$newState")
                    
                    if (!isManualDisconnect && retryCount < maxRetries) {
                        retryCount++
                        val backoffMillis = retryCount * 1000L
                        val retryMsg = "Connection error: $statusDescription. Retrying in ${backoffMillis/1000}s... ($retryCount/$maxRetries)"
                        Log.w(TAG, retryMsg)
                        trySend(BluetoothDataPacket(
                            source = "System",
                            text = retryMsg,
                            format = DataFormat.STRUCTURED
                        ))
                        
                        gatt.close()
                        
                        val callbackInstance = this
                        @Suppress("OPT_IN_USAGE")
                        GlobalScope.launch {
                            delay(backoffMillis)
                            if (!isManualDisconnect) {
                                bluetoothGatt = connectGattCompat(device, callbackInstance)
                            }
                        }
                        return
                    } else {
                        val failMsg = "Connection failed: $statusDescription after $retryCount retries."
                        Log.e(TAG, failMsg)
                        trySend(BluetoothDataPacket(source = "System", text = failMsg, format = DataFormat.STRUCTURED))
                    }
                }

                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.i(TAG, "GATT Connected to ${device.address}")
                    retryCount = 0
                    trySend(BluetoothDataPacket(
                        source = "System",
                        text = "Connected. Requesting MTU...",
                        format = DataFormat.STRUCTURED
                    ))
                    gatt.requestMtu(517)
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.i(TAG, "GATT Disconnected from ${device.address}")
                    trySend(BluetoothDataPacket(
                        source = "System",
                        text = if (isManualDisconnect) "Disconnected by user" else "Disconnected (Status: $statusDescription)",
                        format = DataFormat.STRUCTURED
                    ))
                    gatt.close()
                    if (gatt == bluetoothGatt) {
                        bluetoothGatt = null
                    }
                    if (!isManualDisconnect && status != BluetoothGatt.GATT_SUCCESS) {
                        close()
                    } else if (isManualDisconnect) {
                        close()
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                Log.i(TAG, "MTU changed to $mtu, status=$status")
                trySend(BluetoothDataPacket(
                    source = "System",
                    text = "MTU updated to $mtu. Discovering services...",
                    format = DataFormat.STRUCTURED
                ))
                gatt.discoverServices()
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "Services discovered for ${device.address}: ${gatt.services.size} services found")
                    trySend(BluetoothDataPacket(
                        source = "System",
                        text = "Services discovered successfully",
                        format = DataFormat.STRUCTURED
                    ))
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
                } else {
                    val errorMsg = "Service discovery failed (status: ${getGattStatusDescription(status)})"
                    Log.e(TAG, errorMsg)
                    trySend(BluetoothDataPacket(source = "System", text = errorMsg, format = DataFormat.STRUCTURED))
                }
            }

            override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d(TAG, "Characteristic read: ${characteristic.uuid} (${value.size} bytes)")
                    processData(characteristic, value, "Read")
                } else {
                    val errorMsg = "Failed to read characteristic ${characteristic.uuid.prettyCharacteristicName()}. Status: $status"
                    Log.w(TAG, errorMsg)
                    trySend(BluetoothDataPacket(source = "System", text = errorMsg, format = DataFormat.STRUCTURED))
                }
            }

            override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.v(TAG, "Characteristic write successful: ${characteristic.uuid}")
                } else {
                    val errorMsg = "Write failed for ${characteristic.uuid.prettyCharacteristicName()}. Status: $status"
                    Log.w(TAG, errorMsg)
                    trySend(BluetoothDataPacket(source = "System", text = errorMsg, format = DataFormat.STRUCTURED))
                }
            }

            override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
                Log.v(TAG, "Characteristic notification: ${characteristic.uuid} (${value.size} bytes)")
                processData(characteristic, value, "Notify")
            }

            override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
                val action = if (descriptor.uuid == cccd) "Notifications" else "Descriptor Write"
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.i(TAG, "$action enabled for ${descriptor.characteristic.uuid}")
                    trySend(BluetoothDataPacket(
                        source = "System",
                        text = "$action state updated for ${descriptor.characteristic.uuid.prettyCharacteristicName()}",
                        format = DataFormat.STRUCTURED
                    ))
                } else {
                    Log.e(TAG, "Failed to update $action. Status: $status")
                }
            }

            private fun processData(characteristic: BluetoothGattCharacteristic, value: ByteArray, type: String) {
                trySend(
                    BluetoothDataPacket(
                        data = value,
                        source = "$type: ${characteristic.prettyName()}...",
                        format = DataFormat.HEX_ASCII,
                        serviceUuid = characteristic.service.uuid.toString(),
                        characteristicUuid = characteristic.uuid.toString()
                    )
                )
            }
        }

        bluetoothGatt = connectGattCompat(device, gattCallback)

        awaitClose {
            Log.d(TAG, "Closing GATT connection flow for $address")
            disconnect()
        }
    }

    private fun connectGattCompat(device: BluetoothDevice, callback: BluetoothGattCallback): BluetoothGatt? {
        // Since minSdk is 23 (Marshmallow), Build.VERSION_CODES.M is always true.
        return device.connectGatt(context, false, callback, BluetoothDevice.TRANSPORT_LE)
    }

    override fun disconnect() {
        Log.i(TAG, "Disconnecting from GATT")
        isManualDisconnect = true
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    override fun process(request: ProcessRequest): BluetoothDataPacket? {
        when (request) {
            is ReadGattCharacteristicRequest -> {
                readCharacteristic(request.serviceUuid, request.characteristicUuid)
            }
            is WriteGattCharacteristicRequest -> {
                writeCharacteristic(request.serviceUuid, request.characteristicUuid, request.data)
                return BluetoothDataPacket(
                    data = request.data,
                    source = "Write: ${request.characteristicUuid.prettyCharacteristicName()}...",
                    format = DataFormat.HEX_ASCII,
                    serviceUuid = request.serviceUuid,
                    characteristicUuid = request.characteristicUuid
                )
            }
            is ToggleNotificationRequest -> {
                toggleNotifications(request.serviceUuid, request.characteristicUuid, request.enable)
            }
        }
        return null
    }

    private fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            Log.v(TAG, "Reading characteristic $characteristicUuid")
            gatt.readCharacteristic(characteristic)
        } else {
            Log.w(TAG, "Characteristic $characteristicUuid not found for reading")
        }
    }

    private fun writeCharacteristic(
        serviceUuid: String,
        characteristicUuid: String,
        data: ByteArray
    ) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        if (characteristic != null) {
            Log.v(TAG, "Writing ${data.size} bytes to $characteristicUuid")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
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
                gatt.writeCharacteristic(characteristic)
            }
        } else {
            Log.w(TAG, "Characteristic $characteristicUuid not found for writing")
        }
    }

    private fun toggleNotifications(serviceUuid: String, characteristicUuid: String, enable: Boolean) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(UUID.fromString(serviceUuid))
        val characteristic = service?.getCharacteristic(UUID.fromString(characteristicUuid))
        
        if (characteristic != null) {
            Log.i(TAG, "${if (enable) "Enabling" else "Disabling"} notifications for $characteristicUuid")
            gatt.setCharacteristicNotification(characteristic, enable)
            
            val descriptor = characteristic.getDescriptor(cccd)
            if (descriptor != null) {
                val value = when {
                    !enable -> BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0 -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0 -> BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else -> BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
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
    }

    private fun getGattStatusDescription(status: Int): String {
        return when (status) {
            BluetoothGatt.GATT_SUCCESS -> "Success"
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> "Read Not Permitted"
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> "Write Not Permitted"
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> "Insufficient Authentication"
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> "Request Not Supported"
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> "Insufficient Encryption"
            BluetoothGatt.GATT_INVALID_OFFSET -> "Invalid Offset"
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> "Invalid Attribute Length"
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> "Connection Congested"
            BluetoothGatt.GATT_FAILURE -> "GATT Failure"
            133 -> "GATT_ERROR (133)"
            8 -> "GATT_INSUF_AUTHORIZATION (8)"
            19 -> "GATT_CONN_TERMINATE_PEER_USER (19)"
            22 -> "GATT_CONN_TERMINATE_LOCAL_HOST (22)"
            62 -> "GATT_CONN_FAIL_ESTABLISH (62)"
            else -> "Unknown Error ($status)"
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
