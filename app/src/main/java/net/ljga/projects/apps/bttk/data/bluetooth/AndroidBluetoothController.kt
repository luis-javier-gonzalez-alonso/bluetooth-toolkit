package net.ljga.projects.apps.bttk.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothProfile
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.DataFormat
import net.ljga.projects.apps.bttk.data.bluetooth.strategy.BluetoothConnectionStrategy
import net.ljga.projects.apps.bttk.data.bluetooth.strategy.GattBluetoothConnectionStrategy
import net.ljga.projects.apps.bttk.data.bluetooth.strategy.SppBluetoothConnectionStrategy
import net.ljga.projects.apps.bttk.data.bluetooth.utils.DeviceFoundReceiver
import net.ljga.projects.apps.bttk.data.bluetooth.utils.prettyCharacteristicName
import java.util.UUID
import javax.inject.Inject

@SuppressLint("MissingPermission")
class AndroidBluetoothController @Inject constructor(
    private val context: Context
) : BluetoothController {

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter by lazy {
        bluetoothManager?.adapter
    }

    override val localAddress: String?
        get() = if (hasPermission(getConnectPermission())) bluetoothAdapter?.address else null

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val scannedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _scannedDevices.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    override val pairedDevices: StateFlow<List<BluetoothDeviceDomain>>
        get() = _pairedDevices.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean>
        get() = _isConnected.asStateFlow()

    private val _connectedAddress = MutableStateFlow<String?>(null)
    override val connectedAddress: StateFlow<String?>
        get() = _connectedAddress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    override val isScanning: StateFlow<Boolean>
        get() = _isScanning.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    override val errors: Flow<String>
        get() = _errors.asSharedFlow()

    private val _incomingData = MutableStateFlow<List<BluetoothDataPacket>>(emptyList())
    override val incomingData: StateFlow<List<BluetoothDataPacket>>
        get() = _incomingData.asStateFlow()

    // GATT Server State
    private val _isGattServerRunning = MutableStateFlow(false)
    override val isGattServerRunning: StateFlow<Boolean> = _isGattServerRunning.asStateFlow()

    private val _gattServerServices = MutableStateFlow<List<BluetoothServiceDomain>>(emptyList())
    override val gattServerServices: StateFlow<List<BluetoothServiceDomain>> = _gattServerServices.asStateFlow()

    private val _gattServerLogs = MutableStateFlow<List<BluetoothDataPacket>>(emptyList())
    override val gattServerLogs: StateFlow<List<BluetoothDataPacket>> = _gattServerLogs.asStateFlow()

    private var gattServer: BluetoothGattServer? = null
    private var isAdvertising = false
    private var originalName: String? = null

    private val gattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState)
            Log.d("GattServer", "Connection state change: ${device?.address} status: $status newState: $newState")
            val stateStr = when(newState) {
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

        override fun onCharacteristicReadRequest(device: BluetoothDevice?, requestId: Int, offset: Int, characteristic: BluetoothGattCharacteristic?) {
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

        override fun onCharacteristicWriteRequest(device: BluetoothDevice?, requestId: Int, characteristic: BluetoothGattCharacteristic?, preparedWrite: Boolean, responseNeeded: Boolean, offset: Int, value: ByteArray?) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
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

    private fun emitIncomingData(packet: BluetoothDataPacket) {
        if (packet.format == DataFormat.GATT_STRUCTURE && packet.gattServices != null && packet.source != null) {
            updateDeviceServices(packet.source, packet.gattServices)
        }
        _incomingData.update { (it + packet).takeLast(100) }
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

    private var isReceiverRegistered = false
    private val deviceFoundReceiver = DeviceFoundReceiver { device, rssi ->
        val newDevice = device.toBluetoothDeviceDomain(isInRange = true, rssi = rssi)

        _scannedDevices.update { devices ->
            if (devices.any { it.address == newDevice.address }) {
                devices.map { if (it.address == newDevice.address) newDevice else it }
            } else {
                devices + newDevice
            }
        }

        _pairedDevices.update { devices ->
            devices.map {
                if (it.address == newDevice.address) it.copy(
                    isInRange = true,
                    rssi = rssi,
                    uuids = newDevice.uuids
                ) else it
            }
        }
    }

    private val scanStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> _isScanning.value = true
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> _isScanning.value = false
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> updatePairedDevices()
            }
        }
    }

    private var currentStrategy: BluetoothConnectionStrategy? = null
    private var currentAddress: String? = null
    private var connectionJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        updatePairedDevices()
        registerScanStateReceiver()
    }

    override fun startDiscovery(): Boolean {
        if (!hasPermission(getScanPermission())) {
            _errors.tryEmit("Missing scan permission")
            return false
        }

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            _errors.tryEmit("Bluetooth not available")
            return false
        }

        updatePairedDevices()

        _pairedDevices.update { devices -> devices.map { it.copy(isInRange = false, rssi = null) } }
        _scannedDevices.value = emptyList()
        
        registerReceiver()
        val started = bluetoothAdapter?.startDiscovery() == true
        if (started) {
            _isScanning.value = true
        }
        return started
    }

    override fun stopDiscovery() {
        if (!hasPermission(getScanPermission())) return
        bluetoothAdapter?.cancelDiscovery()
    }

    override fun connectToDevice(device: BluetoothDeviceDomain, profile: BluetoothProfile?) {
        if (_isConnected.value && _connectedAddress.value == device.address) {
            return
        }

        if (!hasPermission(getConnectPermission())) {
            _errors.tryEmit("Missing connect permission")
            return
        }

        val adapter = bluetoothAdapter ?: return
        
        val strategy: BluetoothConnectionStrategy = when (profile) {
            BluetoothProfile.SPP -> SppBluetoothConnectionStrategy(adapter)
            BluetoothProfile.GATT -> GattBluetoothConnectionStrategy(context, adapter)
            else -> {
                val supportsSpp = device.uuids.any { it.equals(BluetoothProfile.SPP.uuid.toString(), ignoreCase = true) }
                
                when {
                    supportsSpp -> SppBluetoothConnectionStrategy(adapter)
                    else -> GattBluetoothConnectionStrategy(context, adapter)
                }
            }
        }

        connectionJob?.cancel()
        connectionJob = scope.launch {
            try {
                stopDiscovery()
                currentStrategy = strategy
                currentAddress = device.address
                
                strategy.connect(device.address).collect { packet ->
                    _isConnected.value = true
                    _connectedAddress.value = device.address
                    emitIncomingData(packet)
                }
            } catch (e: Exception) {
                _errors.tryEmit("Connection failed: ${e.message}")
            } finally {
                disconnect()
            }
        }
    }

    private fun updateDeviceServices(address: String, services: List<BluetoothServiceDomain>) {
        _scannedDevices.update { devices ->
            devices.map { if (it.address == address) it.copy(services = services) else it }
        }
        _pairedDevices.update { devices ->
            devices.map { if (it.address == address) it.copy(services = services) else it }
        }
    }

    override fun disconnect() {
        connectionJob?.cancel()
        scope.launch {
            currentStrategy?.disconnect()
            currentStrategy = null
            currentAddress = null
            _isConnected.value = false
            _connectedAddress.value = null
            _incomingData.value = emptyList() // Clear logs on disconnect
        }
    }

    override fun pairDevice(address: String) {
        if (!hasPermission(getConnectPermission())) return
        bluetoothAdapter?.getRemoteDevice(address)?.createBond()
    }

    override fun forgetDevice(address: String) {
        if (!hasPermission(getConnectPermission())) return
        val device = bluetoothAdapter?.getRemoteDevice(address)
        try {
            device?.let {
                it.javaClass.getMethod("removeBond").invoke(it)
                updatePairedDevices()
            }
        } catch (e: Exception) {
            _errors.tryEmit("Failed to forget device")
        }
    }

    override fun checkReachability(address: String) {
        if (!hasPermission(getConnectPermission())) return
        registerReceiver()
        bluetoothAdapter?.getRemoteDevice(address)?.fetchUuidsWithSdp()
    }

    override fun refreshPairedDevices() {
        updatePairedDevices()
    }

    override fun readCharacteristic(serviceUuid: String, characteristicUuid: String) {
        currentStrategy?.readCharacteristic(serviceUuid, characteristicUuid)
    }

    override fun toggleNotification(serviceUuid: String, characteristicUuid: String, enable: Boolean) {
        currentStrategy?.toggleNotification(serviceUuid, characteristicUuid, enable)
    }

    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        currentStrategy?.writeCharacteristic(serviceUuid, characteristicUuid, data)
        emitIncomingData(
            BluetoothDataPacket(
                data = data,
                source = "Write: ${characteristicUuid.prettyCharacteristicName()}...",
                format = DataFormat.HEX_ASCII,
                serviceUuid = serviceUuid,
                characteristicUuid = characteristicUuid
            )
        )
    }

    override fun readDescriptors(serviceUuid: String, characteristicUuid: String) {
        currentStrategy?.readDescriptors(serviceUuid, characteristicUuid)
    }

    override fun emitPacket(packet: BluetoothDataPacket) {
        emitIncomingData(packet)
    }

    // GATT Server implementation
    override fun startGattServer(deviceName: String?) {
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

    override fun stopGattServer() {
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

    override fun addGattService(service: BluetoothServiceDomain) {
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

    override fun removeGattService(serviceUuid: String) {
        _gattServerServices.update { it.filter { service -> service.uuid != serviceUuid } }
        if (_isGattServerRunning.value) {
            gattServer?.getService(UUID.fromString(serviceUuid))?.let {
                gattServer?.removeService(it)
            }
        }
    }

    override fun clearGattServices() {
        _gattServerServices.value = emptyList()
        if (_isGattServerRunning.value) {
            gattServer?.clearServices()
        }
    }

    override fun updateGattService(service: BluetoothServiceDomain) {
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

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothDevice.ACTION_UUID)
            }
            context.registerReceiver(deviceFoundReceiver, filter)
            isReceiverRegistered = true
        }
    }

    private fun registerScanStateReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(scanStateReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(scanStateReceiver, filter)
        }
    }

    override fun release() {
        if (isReceiverRegistered) {
            try { context.unregisterReceiver(deviceFoundReceiver) } catch (e: Exception) {}
            isReceiverRegistered = false
        }
        try { context.unregisterReceiver(scanStateReceiver) } catch (e: Exception) {}
        disconnect()
        stopDiscovery()
        stopGattServer()
    }

    private fun updatePairedDevices() {
        if (!hasPermission(getConnectPermission())) return
        _pairedDevices.update { 
            bluetoothAdapter?.bondedDevices?.map { it.toBluetoothDeviceDomain() } ?: emptyList()
        }
    }

    private fun hasPermission(permission: String) = context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    private fun getScanPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
    private fun getConnectPermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_CONNECT else Manifest.permission.BLUETOOTH
    private fun getAdvertisePermission() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Manifest.permission.BLUETOOTH_ADVERTISE else Manifest.permission.BLUETOOTH

    private fun BluetoothDevice.toBluetoothDeviceDomain(
        isInRange: Boolean = false,
        rssi: Int? = null
    ) =
        BluetoothDeviceDomain(
            name = name,
            address = address,
            isInRange = isInRange,
            bondState = bondState,
            type = type,
            uuids = uuids?.map { it.toString() } ?: emptyList(),
            rssi = rssi
        )
}
