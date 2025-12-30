package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.domain.model.*
import net.ljga.projects.apps.bttk.domain.repository.GattScriptRepository
import net.ljga.projects.apps.bttk.domain.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.domain.repository.GattServerRepository
import net.ljga.projects.apps.bttk.domain.repository.BluetoothDeviceRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDataFrameRepository @Inject constructor() : DataFrameRepository {
    private val _dataFrames = MutableStateFlow(fakeDataFrames)
    override val dataFrames: Flow<List<DataFrameDomain>> = _dataFrames.asStateFlow()

    private var nextId = fakeDataFrames.maxOfOrNull { it.uid }?.plus(1) ?: 1

    override suspend fun add(name: String, data: ByteArray) {
        val newFrame = DataFrameDomain(nextId++, name, data)
        _dataFrames.value += newFrame
    }

    override suspend fun remove(uid: Int) {
        _dataFrames.value = _dataFrames.value.filter { it.uid != uid }
    }
}

@Singleton
class FakeGattScriptRepository @Inject constructor() : GattScriptRepository {
    private val _scripts = MutableStateFlow(fakeScripts)
    override fun getAllScripts(): Flow<List<BluetoothScriptDomain>> = _scripts.asStateFlow()

    override suspend fun getScriptById(id: Int): BluetoothScriptDomain? {
        return _scripts.value.find { it.id == id }
    }

    override suspend fun saveScript(script: BluetoothScriptDomain): Long {
        val existing = _scripts.value.find { it.id == script.id }
        if (existing != null) {
            _scripts.value = _scripts.value.map { if (it.id == script.id) script else it }
            return script.id.toLong()
        } else {
            val newId = (_scripts.value.maxOfOrNull { it.id } ?: 0) + 1
            val newScript = script.copy(id = newId)
            _scripts.value += newScript
            return newId.toLong()
        }
    }

    override suspend fun deleteScript(id: Int) {
        _scripts.value = _scripts.value.filter { it.id != id }
    }
}

@Singleton
class FakeGattCharacteristicParserRepository @Inject constructor() : GattCharacteristicParserRepository {
    private val _configs = MutableStateFlow(fakeParserConfigs)
    override fun getAllConfigs(): Flow<List<CharacteristicParserConfigDomain>> = _configs.asStateFlow()

    override fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfigDomain?> {
        return _configs.map { list ->
            list.find { it.serviceUuid == serviceUuid && it.characteristicUuid == characteristicUuid }
        }
    }

    override suspend fun saveConfig(config: CharacteristicParserConfigDomain) {
        val filtered = _configs.value.filterNot {
            it.serviceUuid == config.serviceUuid && it.characteristicUuid == config.characteristicUuid
        }
        _configs.value = filtered + config
    }

    override suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String) {
        _configs.value = _configs.value.filterNot {
            it.serviceUuid == serviceUuid && it.characteristicUuid == characteristicUuid
        }
    }
}

@Singleton
class FakeGattServerRepository @Inject constructor() : GattServerRepository {
    private val _config = MutableStateFlow(fakeGattServerState)
    override val config: Flow<GattServerStateDomain> = _config.asStateFlow()

    override suspend fun saveConfig(state: GattServerStateDomain) {
        _config.value = state
    }
}

@Singleton
class FakeBluetoothDeviceRepository @Inject constructor() : BluetoothDeviceRepository {
    private val _savedDevices = MutableStateFlow(fakeSavedDevices)
    private val _gattAliases = MutableStateFlow(fakeAliases)

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> = _savedDevices.asStateFlow()
    override val gattAliases: Flow<Map<String, String>> = _gattAliases.asStateFlow()

    override suspend fun saveDevice(device: BluetoothDeviceDomain) {
        val existing = _savedDevices.value.find { it.address == device.address }
        if (existing != null) {
            _savedDevices.value = _savedDevices.value.map { if (it.address == device.address) device else it }
        } else {
            _savedDevices.value += device
        }
    }

    override suspend fun updateServices(address: String, services: List<BluetoothServiceDomain>) {
        _savedDevices.value = _savedDevices.value.map {
            if (it.address == address) it.copy(services = services) else it
        }
    }

    override suspend fun forgetDevice(address: String) {
        _savedDevices.value = _savedDevices.value.filter { it.address != address }
    }

    override suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        val key = "$serviceUuid-$characteristicUuid"
        _gattAliases.value += (key to alias)
    }
}

private val fakeDataFrames = listOf(
    DataFrameDomain(uid = 1, name = "Battery Level", data = byteArrayOf(0x64)),
    DataFrameDomain(uid = 2, name = "Heart Rate", data = byteArrayOf(0x00, 0x4B)),
    DataFrameDomain(uid = 3, name = "Temperature", data = byteArrayOf(0x0A, 0x09))
)

private val fakeScripts = listOf(
    BluetoothScriptDomain(
        id = 1,
        name = "Read Device Info",
        operations = listOf(
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.READ, "180A", "2A29"),
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.DELAY, delayMs = 500),
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.READ, "180A", "2A24")
        )
    ),
    BluetoothScriptDomain(
        id = 2,
        name = "Toggle LED",
        operations = listOf(
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.WRITE, "FF01", "FF02", data = byteArrayOf(0x01)),
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.DELAY, delayMs = 1000),
            BluetoothScriptOperationDomain(ScriptOperationTypeDomain.WRITE, "FF01", "FF02", data = byteArrayOf(0x00))
        )
    )
)

private val fakeParserConfigs = listOf(
    CharacteristicParserConfigDomain(
        serviceUuid = "1809",
        characteristicUuid = "2A1C",
        fields = listOf(
            ParserFieldDomain("Temperature", 1, 2, FieldTypeDomain.I16, EndiannessDomain.LITTLE_ENDIAN)
        ),
        template = "Temperature: {Temperature} °C"
    )
)

private val fakeGattServerState = GattServerStateDomain(
    services = listOf(
        BluetoothServiceDomain(
            uuid = "1800",
            characteristics = listOf(
                BluetoothCharacteristicDomain("2A00", listOf("READ"), initialValue = "Fake Device"),
                BluetoothCharacteristicDomain("2A01", listOf("READ"), initialValue = "0x0000")
            )
        )
    ),
    nextServiceIndex = 1,
    serviceIndices = mapOf("1800" to 0),
    serviceNextCharIndices = mapOf("1800" to 2),
    deviceName = "BT Toolkit Fake",
    name = "Fake Device"
)

private val fakeSavedDevices = listOf(
    BluetoothDeviceDomain(
        name = "Heart Rate Monitor",
        address = "AA:BB:CC:DD:EE:FF",
        isInRange = true,
        services = listOf(
            BluetoothServiceDomain(
                uuid = "180D",
                characteristics = listOf(
                    BluetoothCharacteristicDomain("2A37", listOf("NOTIFY"))
                )
            )
        )
    ),
    BluetoothDeviceDomain(
        name = "Smart Bulb",
        address = "11:22:33:44:55:66",
        isInRange = false
    )
)

private val fakeAliases = mapOf(
    "180D-2A37" to "HR Measurement"
)
