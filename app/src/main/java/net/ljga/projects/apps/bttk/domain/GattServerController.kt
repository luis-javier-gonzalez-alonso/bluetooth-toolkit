package net.ljga.projects.apps.bttk.domain

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import net.ljga.projects.apps.bttk.domain.utils.prettyCharacteristicName
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class GattServerController @Inject constructor(
    private val context: Context
) {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    private val _isGattServerRunning = MutableStateFlow(false)
    val isGattServerRunning: StateFlow<Boolean> = _isGattServerRunning.asStateFlow()

    private val _gattServerServices = MutableStateFlow<List<BluetoothServiceDomain>>(emptyList())
    val gattServerServices: StateFlow<List<BluetoothServiceDomain>> =
        _gattServerServices.asStateFlow()

    private val _gattServerLogs = MutableStateFlow<List<BluetoothDataPacket>>(emptyList())
    val gattServerLogs: StateFlow<List<BluetoothDataPacket>> = _gattServerLogs.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private var gattServer: BluetoothGattServer? = null
    private var isAdvertising = false
    private var originalName: String? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d(
                "GattServer",
                "Connection state change: ${device?.address} status: $status newState: $newState"
            )
            val stateStr = when (newState) {
                0 -> "Disconnected"
                2 -> "Connected"
                else -> "State $newState"
            }
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = device?.address ?: "Unknown",
                    format = DataFormat.STRUCTURED,
                    text = "Device $stateStr (status: $status)"
                )
            )
        }

        override fun onServiceAdded(status: Int, service: BluetoothGattService?) {
            super.onServiceAdded(status, service)
            val text = if (status == BluetoothGatt.GATT_SUCCESS) {
                "Service added successfully: ${service?.uuid}"
            } else {
                "Failed to add service: ${service?.uuid}, status: $status"
            }
            Log.d("GattServer", text)
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "GATT Server",
                    format = DataFormat.STRUCTURED,
                    text = text
                )
            )
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
            @Suppress("DEPRECATION")
            val valBytes = characteristic?.value ?: byteArrayOf()
            emitGattLog(
                BluetoothDataPacket(
                    data = valBytes,
                    source = device?.address ?: "Unknown",
                    format = DataFormat.HEX_ASCII,
                    text = "Read Request: ${characteristic?.uuid?.prettyCharacteristicName() ?: "Unknown"}",
                    serviceUuid = characteristic?.service?.uuid.toString(),
                    characteristicUuid = characteristic?.uuid.toString()
                )
            )
            gattServer?.sendResponse(device, requestId, 0, offset, valBytes)
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onCharacteristicWriteRequest(
                device,
                requestId,
                characteristic,
                preparedWrite,
                responseNeeded,
                offset,
                value
            )
            if (value != null) {
                @Suppress("DEPRECATION")
                characteristic?.value = value
                emitGattLog(
                    BluetoothDataPacket(
                        data = value,
                        source = device?.address ?: "Unknown",
                        format = DataFormat.HEX_ASCII,
                        text = "Write Request: ${characteristic?.uuid?.prettyCharacteristicName() ?: "Unknown"}",
                        serviceUuid = characteristic?.service?.uuid.toString(),
                        characteristicUuid = characteristic?.uuid.toString()
                    )
                )
            }
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, 0, offset, value)
            }
        }
    }

    private fun emitGattLog(packet: BluetoothDataPacket) {
        _gattServerLogs.update { (it + packet).takeLast(100) }
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            super.onStartSuccess(settingsInEffect)
            isAdvertising = true
            Log.d("GattServer", "Advertising started successfully")
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "GATT Server",
                    format = DataFormat.STRUCTURED,
                    text = "Advertising started successfully"
                )
            )
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            isAdvertising = false
            val errorMsg = when (errorCode) {
                ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising failed: Already started"
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising failed: Data too large"
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Advertising failed: Feature unsupported"
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Advertising failed: Internal error"
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Advertising failed: Too many advertisers"
                else -> "Advertising failed with error code: $errorCode"
            }
            _errors.tryEmit(errorMsg)
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "GATT Server",
                    format = DataFormat.STRUCTURED,
                    text = errorMsg
                )
            )
        }
    }

    fun startGattServer(deviceName: String?) {
        if (!hasPermission(getConnectPermission()) || !hasPermission(getAdvertisePermission())) {
            _errors.tryEmit("Missing permissions for GATT Server")
            return
        }

        if (gattServer != null) return

        if (deviceName != null) {
            originalName = bluetoothAdapter?.name
            bluetoothAdapter?.name = deviceName
        }

        gattServer = bluetoothManager?.openGattServer(context, gattServerCallback)
        if (gattServer == null) {
            val errorMsg = "Failed to open GATT Server"
            _errors.tryEmit(errorMsg)
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "GATT Server",
                    format = DataFormat.STRUCTURED,
                    text = errorMsg
                )
            )
            return
        }

        _gattServerLogs.value = emptyList() // Clear logs on start
        emitGattLog(
            BluetoothDataPacket(
                data = byteArrayOf(),
                source = "GATT Server",
                format = DataFormat.STRUCTURED,
                text = "Server started${deviceName?.let { " as $it" } ?: ""}"
            )
        )

        // Add configured services
        _gattServerServices.value.forEach { serviceDomain ->
            val gattService = BluetoothGattService(
                UUID.fromString(serviceDomain.uuid),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            serviceDomain.characteristics.forEach { charDomain ->
                val gattChar = BluetoothGattCharacteristic(
                    UUID.fromString(charDomain.uuid),
                    charDomain.propertyInts,
                    charDomain.permissionInts
                )
                charDomain.initialValue?.let { hexValue ->
                    try {
                        val bytes = hexValue.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        @Suppress("DEPRECATION")
                        gattChar.value = bytes
                    } catch (e: Exception) {
                        Log.e("GattServer", "Failed to set initial value for ${charDomain.uuid}", e)
                    }
                }
                gattService.addCharacteristic(gattChar)
            }
            gattServer?.addService(gattService)
        }

        startAdvertising()
        _isGattServerRunning.value = true
    }

    private fun startAdvertising() {
        val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser ?: run {
            val errorMsg = "BLE Advertising not supported"
            _errors.tryEmit(errorMsg)
            emitGattLog(
                BluetoothDataPacket(
                    data = byteArrayOf(),
                    source = "GATT Server",
                    format = DataFormat.STRUCTURED,
                    text = errorMsg
                )
            )
            return
        }

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()

        // Split data between Advertisement and Scan Response to avoid 31-byte limit
        val dataBuilder = AdvertiseData.Builder()
            .setIncludeTxPowerLevel(true)

        _gattServerServices.value.firstOrNull()?.let {
            dataBuilder.addServiceUuid(ParcelUuid(UUID.fromString(it.uuid)))
        }

        val scanResponseBuilder = AdvertiseData.Builder()
            .setIncludeDeviceName(true)

        val data = dataBuilder.build()
        val scanResponse = scanResponseBuilder.build()

        Log.d("GattServer", "Starting advertising...")
        advertiser.startAdvertising(settings, data, scanResponse, advertiseCallback)

        emitGattLog(
            BluetoothDataPacket(
                data = byteArrayOf(),
                source = "GATT Server",
                format = DataFormat.STRUCTURED,
                text = "Starting advertising..."
            )
        )
    }

    fun stopGattServer() {
        if (isAdvertising) {
            bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
            isAdvertising = false
        }
        gattServer?.close()
        gattServer = null
        _isGattServerRunning.value = false

        emitGattLog(
            BluetoothDataPacket(
                data = byteArrayOf(),
                source = "GATT Server",
                format = DataFormat.STRUCTURED,
                text = "Server stopped"
            )
        )
        // Note: we don't clear logs here so they can be reviewed after stopping,
        // they are cleared on next start.

        originalName?.let {
            bluetoothAdapter?.name = it
            originalName = null
        }
    }

    fun addGattService(service: BluetoothServiceDomain) {
        _gattServerServices.update { current ->
            if (current.any { it.uuid == service.uuid }) {
                current.map { if (it.uuid == service.uuid) service else it }
            } else {
                current + service
            }
        }
        if (_isGattServerRunning.value) {
            val gattService = BluetoothGattService(
                UUID.fromString(service.uuid),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            service.characteristics.forEach { charDomain ->
                val gattChar = BluetoothGattCharacteristic(
                    UUID.fromString(charDomain.uuid),
                    charDomain.propertyInts,
                    charDomain.permissionInts
                )
                charDomain.initialValue?.let { hexValue ->
                    try {
                        val bytes = hexValue.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        @Suppress("DEPRECATION")
                        gattChar.value = bytes
                    } catch (e: Exception) {
                        Log.e("GattServer", "Failed to set initial value for ${charDomain.uuid}", e)
                    }
                }
                gattService.addCharacteristic(gattChar)
            }
            gattServer?.addService(gattService)
        }
    }

    fun removeGattService(serviceUuid: String) {
        _gattServerServices.update { it.filter { service -> service.uuid != serviceUuid } }
        if (_isGattServerRunning.value) {
            gattServer?.getService(UUID.fromString(serviceUuid))?.let {
                gattServer?.removeService(it)
            }
        }
    }

    fun clearGattServices() {
        _gattServerServices.value = emptyList()
        if (_isGattServerRunning.value) {
            gattServer?.clearServices()
        }
    }

    fun updateGattService(service: BluetoothServiceDomain) {
        _gattServerServices.update { current ->
            current.map { if (it.uuid == service.uuid) service else it }
        }
        if (_isGattServerRunning.value) {
            gattServer?.getService(UUID.fromString(service.uuid))?.let {
                gattServer?.removeService(it)
            }
            val gattService = BluetoothGattService(
                UUID.fromString(service.uuid),
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            service.characteristics.forEach { charDomain ->
                val gattChar = BluetoothGattCharacteristic(
                    UUID.fromString(charDomain.uuid),
                    charDomain.propertyInts,
                    charDomain.permissionInts
                )
                charDomain.initialValue?.let { hexValue ->
                    try {
                        val bytes = hexValue.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                        @Suppress("DEPRECATION")
                        gattChar.value = bytes
                    } catch (e: Exception) {
                        Log.e("GattServer", "Failed to set initial value for ${charDomain.uuid}", e)
                    }
                }
                gattService.addCharacteristic(gattChar)
            }
            gattServer?.addService(gattService)
        }
    }

    private fun hasPermission(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    private fun getConnectPermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH

    private fun getAdvertisePermission() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_ADVERTISE else Manifest.permission.BLUETOOTH
}
