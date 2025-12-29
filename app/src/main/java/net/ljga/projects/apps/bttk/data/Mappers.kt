package net.ljga.projects.apps.bttk.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.database.entities.*
import net.ljga.projects.apps.bttk.domain.model.*

// DataFrame Mappers
fun DataFrame.toDomain(): DataFrameDomain = DataFrameDomain(
    uid = uid,
    name = name,
    data = data
)

fun DataFrameDomain.toEntity(): DataFrame = DataFrame(
    name = name,
    data = data
).apply {
    uid = this@toEntity.uid
}

// BluetoothScript Mappers
fun BluetoothScript.toDomain(): BluetoothScriptDomain = BluetoothScriptDomain(
    id = id,
    name = name,
    operations = operations.map { it.toDomain() }
)

fun BluetoothScriptOperation.toDomain(): BluetoothScriptOperationDomain = BluetoothScriptOperationDomain(
    type = ScriptOperationTypeDomain.valueOf(type.name),
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    data = data,
    delayMs = delayMs
)

fun BluetoothScriptDomain.toEntity(): BluetoothScript = BluetoothScript(
    id = id,
    name = name,
    operations = operations.map { it.toEntity() }
)

fun BluetoothScriptOperationDomain.toEntity(): BluetoothScriptOperation = BluetoothScriptOperation(
    type = ScriptOperationType.valueOf(type.name),
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    data = data,
    delayMs = delayMs
)

// CharacteristicParserConfig Mappers
fun CharacteristicParserConfig.toDomain(): CharacteristicParserConfigDomain = CharacteristicParserConfigDomain(
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    fields = fields.map { it.toDomain() },
    template = template
)

fun ParserField.toDomain(): ParserFieldDomain = ParserFieldDomain(
    name = name,
    offset = offset,
    length = length,
    type = FieldTypeDomain.valueOf(type.name),
    endianness = EndiannessDomain.valueOf(endianness.name)
)

fun CharacteristicParserConfigDomain.toEntity(): CharacteristicParserConfig = CharacteristicParserConfig(
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    fields = fields.map { it.toEntity() },
    template = template
)

fun ParserFieldDomain.toEntity(): ParserField = ParserField(
    name = name,
    offset = offset,
    length = length,
    type = FieldType.valueOf(type.name),
    endianness = Endianness.valueOf(endianness.name)
)

// GattServerConfig Mappers
fun GattServerConfig.toDomain(): GattServerStateDomain {
    return try {
        Json.decodeFromString<GattServerStateDomain>(servicesJson)
    } catch (e: Exception) {
        try {
            val services = Json.decodeFromString<List<BluetoothServiceDomain>>(servicesJson)
            GattServerStateDomain(services, services.size)
        } catch (e2: Exception) {
            GattServerStateDomain(emptyList(), 0)
        }
    }
}

fun GattServerStateDomain.toEntity(): GattServerConfig = GattServerConfig(
    servicesJson = Json.encodeToString(this)
)

// SavedDevice Mappers
fun SavedDevice.toDomain(): BluetoothDeviceDomain {
    val services = servicesJson?.let {
        try {
            Json.decodeFromString<List<BluetoothServiceDomain>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()

    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isInRange = false,
        services = services
    )
}

fun BluetoothDeviceDomain.toEntity(): SavedDevice = SavedDevice(
    address = address,
    name = name,
    servicesJson = if (services.isNotEmpty()) Json.encodeToString(services) else null
)
