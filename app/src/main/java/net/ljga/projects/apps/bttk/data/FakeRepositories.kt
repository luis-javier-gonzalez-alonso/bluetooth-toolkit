package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.repository.BluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.repository.CharacteristicParserRepository
import net.ljga.projects.apps.bttk.data.repository.DataFrameRepository
import net.ljga.projects.apps.bttk.data.repository.GattServerRepository
import net.ljga.projects.apps.bttk.data.repository.GattServerStateData
import net.ljga.projects.apps.bttk.data.repository.SavedDeviceRepository
import net.ljga.projects.apps.bttk.database.entities.BluetoothScript
import net.ljga.projects.apps.bttk.database.entities.BluetoothScriptOperation
import net.ljga.projects.apps.bttk.database.entities.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.database.entities.DataFrame
import net.ljga.projects.apps.bttk.database.entities.Endianness
import net.ljga.projects.apps.bttk.database.entities.FieldType
import net.ljga.projects.apps.bttk.database.entities.ParserField
import net.ljga.projects.apps.bttk.database.entities.ScriptOperationType
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeDataFrameRepository @Inject constructor() : DataFrameRepository {
    private val _dataFrames = MutableStateFlow(fakeDataFrames)
    override val dataFrames: Flow<List<DataFrame>> = _dataFrames.asStateFlow()

    private var nextId = fakeDataFrames.maxOfOrNull { it.uid }?.plus(1) ?: 1

    override suspend fun add(name: String, data: ByteArray) {
        val newFrame = DataFrame(name, data).apply { uid = nextId++ }
        _dataFrames.value = _dataFrames.value + newFrame
    }

    override suspend fun remove(uid: Int) {
        _dataFrames.value = _dataFrames.value.filter { it.uid != uid }
    }
}

@Singleton
class FakeBluetoothScriptRepository @Inject constructor() : BluetoothScriptRepository {
    private val _scripts = MutableStateFlow(fakeScripts)
    override fun getAllScripts(): Flow<List<BluetoothScript>> = _scripts.asStateFlow()

    override suspend fun getScriptById(id: Int): BluetoothScript? {
        return _scripts.value.find { it.id == id }
    }

    override suspend fun saveScript(script: BluetoothScript): Long {
        val existing = _scripts.value.find { it.id == script.id }
        if (existing != null) {
            _scripts.value = _scripts.value.map { if (it.id == script.id) script else it }
            return script.id.toLong()
        } else {
            val newId = (_scripts.value.maxOfOrNull { it.id } ?: 0) + 1
            val newScript = script.copy(id = newId)
            _scripts.value = _scripts.value + newScript
            return newId.toLong()
        }
    }

    override suspend fun deleteScript(id: Int) {
        _scripts.value = _scripts.value.filter { it.id != id }
    }
}

@Singleton
class FakeCharacteristicParserRepository @Inject constructor() : CharacteristicParserRepository {
    private val _configs = MutableStateFlow(fakeParserConfigs)
    override fun getAllConfigs(): Flow<List<CharacteristicParserConfig>> = _configs.asStateFlow()

    override fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfig?> {
        return _configs.map { list ->
            list.find { it.serviceUuid == serviceUuid && it.characteristicUuid == characteristicUuid }
        }
    }

    override suspend fun saveConfig(config: CharacteristicParserConfig) {
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
    override val config: Flow<GattServerStateData> = _config.asStateFlow()

    override suspend fun saveConfig(
        services: List<BluetoothServiceDomain>,
        nextServiceIndex: Int,
        serviceIndices: Map<String, Int>,
        serviceNextCharIndices: Map<String, Int>,
        deviceName: String?
    ) {
        _config.value = GattServerStateData(
            services = services,
            nextServiceIndex = nextServiceIndex,
            serviceIndices = serviceIndices,
            serviceNextCharIndices = serviceNextCharIndices,
            deviceName = deviceName
        )
    }
}

@Singleton
class FakeSavedDeviceRepository @Inject constructor() : SavedDeviceRepository {
    private val _savedDevices = MutableStateFlow(fakeSavedDevices)
    private val _gattAliases = MutableStateFlow(fakeAliases)

    override val savedDevices: Flow<List<BluetoothDeviceDomain>> = _savedDevices.asStateFlow()
    override val gattAliases: Flow<Map<String, String>> = _gattAliases.asStateFlow()

    override suspend fun saveDevice(device: BluetoothDeviceDomain) {
        val existing = _savedDevices.value.find { it.address == device.address }
        if (existing != null) {
            _savedDevices.value = _savedDevices.value.map { if (it.address == device.address) device else it }
        } else {
            _savedDevices.value = _savedDevices.value + device
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
        _gattAliases.value = _gattAliases.value + (key to alias)
    }
}

private val fakeDataFrames = listOf(
    DataFrame(name = "Battery Level", data = byteArrayOf(0x64)).apply { uid = 1 },
    DataFrame(name = "Heart Rate", data = byteArrayOf(0x00, 0x4B)).apply { uid = 2 },
    DataFrame(name = "Temperature", data = byteArrayOf(0x0A, 0x09)).apply { uid = 3 }
)

private val fakeScripts = listOf(
    BluetoothScript(
        id = 1,
        name = "Read Device Info",
        operations = listOf(
            BluetoothScriptOperation(ScriptOperationType.READ, "180A", "2A29"),
            BluetoothScriptOperation(ScriptOperationType.DELAY, delayMs = 500),
            BluetoothScriptOperation(ScriptOperationType.READ, "180A", "2A24")
        )
    ),
    BluetoothScript(
        id = 2,
        name = "Toggle LED",
        operations = listOf(
            BluetoothScriptOperation(ScriptOperationType.WRITE, "FF01", "FF02", data = byteArrayOf(0x01)),
            BluetoothScriptOperation(ScriptOperationType.DELAY, delayMs = 1000),
            BluetoothScriptOperation(ScriptOperationType.WRITE, "FF01", "FF02", data = byteArrayOf(0x00))
        )
    )
)

private val fakeParserConfigs = listOf(
    CharacteristicParserConfig(
        serviceUuid = "1809",
        characteristicUuid = "2A1C",
        fields = listOf(
            ParserField("Temperature", 1, 2, FieldType.I16, Endianness.LITTLE_ENDIAN)
        ),
        template = "Temperature: {Temperature} °C"
    )
)

private val fakeGattServerState = GattServerStateData(
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
    deviceName = "BT Toolkit Fake"
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
