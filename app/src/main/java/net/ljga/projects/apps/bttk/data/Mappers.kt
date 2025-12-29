package net.ljga.projects.apps.bttk.data

import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothScript
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothScriptOperation
import net.ljga.projects.apps.bttk.data.database.entity.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.database.entity.DataFrame
import net.ljga.projects.apps.bttk.data.database.entity.Endianness
import net.ljga.projects.apps.bttk.data.database.entity.FieldType
import net.ljga.projects.apps.bttk.data.database.entity.GattServerConfig
import net.ljga.projects.apps.bttk.data.database.entity.ParserField
import net.ljga.projects.apps.bttk.data.database.entity.SavedDevice
import net.ljga.projects.apps.bttk.data.database.entity.ScriptOperationType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptOperationDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import net.ljga.projects.apps.bttk.domain.model.EndiannessDomain
import net.ljga.projects.apps.bttk.domain.model.FieldTypeDomain
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain
import net.ljga.projects.apps.bttk.domain.model.ParserFieldDomain
import net.ljga.projects.apps.bttk.domain.model.ScriptOperationTypeDomain

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
    val services = Json.decodeFromString<List<BluetoothServiceDomain>>(this.servicesJson)
    return GattServerStateDomain(services, this.nextServiceIndex)
}

fun GattServerStateDomain.toEntity(): GattServerConfig = GattServerConfig(
    servicesJson = Json.encodeToString(this.services),
    nextServiceIndex = this.nextServiceIndex
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
