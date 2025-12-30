package net.ljga.projects.apps.bttk.data

import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothDeviceEntity
import net.ljga.projects.apps.bttk.data.database.entity.DataFrameEntity
import net.ljga.projects.apps.bttk.data.database.entity.Endianness
import net.ljga.projects.apps.bttk.data.database.entity.FieldType
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicSettingsEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptEntity
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptOperation
import net.ljga.projects.apps.bttk.data.database.entity.GattScriptOperationType
import net.ljga.projects.apps.bttk.data.database.entity.GattServerEntity
import net.ljga.projects.apps.bttk.data.database.entity.ParserField
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptOperationDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.domain.model.DataFrameDomain
import net.ljga.projects.apps.bttk.domain.model.EndiannessDomain
import net.ljga.projects.apps.bttk.domain.model.FieldTypeDomain
import net.ljga.projects.apps.bttk.domain.model.GattCharacteristicSettingsDomain
import net.ljga.projects.apps.bttk.domain.model.GattServerStateDomain
import net.ljga.projects.apps.bttk.domain.model.ParserFieldDomain
import net.ljga.projects.apps.bttk.domain.model.ScriptOperationTypeDomain

// DataFrame Mappers
fun DataFrameEntity.toDomain(): DataFrameDomain = DataFrameDomain(
    uid = uid,
    name = name,
    data = data
)

fun DataFrameDomain.toEntity(): DataFrameEntity = DataFrameEntity(
    name = name,
    data = data
).apply {
    uid = this@toEntity.uid
}

// BluetoothScript Mappers
fun GattScriptEntity.toDomain(): BluetoothScriptDomain = BluetoothScriptDomain(
    id = id,
    name = name,
    operations = operations.map { it.toDomain() }
)

fun GattScriptOperation.toDomain(): BluetoothScriptOperationDomain = BluetoothScriptOperationDomain(
    type = ScriptOperationTypeDomain.valueOf(type.name),
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    data = data,
    delayMs = delayMs
)

fun BluetoothScriptDomain.toEntity(): GattScriptEntity = GattScriptEntity(
    id = id,
    name = name,
    operations = operations.map { it.toEntity() }
)

fun BluetoothScriptOperationDomain.toEntity(): GattScriptOperation = GattScriptOperation(
    type = GattScriptOperationType.valueOf(type.name),
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
    data = data,
    delayMs = delayMs
)

// GattCharacteristicSettings Mappers
fun GattCharacteristicSettingsEntity.toDomain(): GattCharacteristicSettingsDomain =
    GattCharacteristicSettingsDomain(
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
        alias = alias,
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

fun GattCharacteristicSettingsDomain.toEntity(): GattCharacteristicSettingsEntity =
    GattCharacteristicSettingsEntity(
    serviceUuid = serviceUuid,
    characteristicUuid = characteristicUuid,
        alias = alias,
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
fun GattServerEntity.toDomain(): GattServerStateDomain = GattServerStateDomain(
    id = id,
    name = name,
    services = Json.decodeFromString(servicesJson),
    nextServiceIndex = nextServiceIndex,
    serviceIndices = Json.decodeFromString(serviceIndicesJson),
    serviceNextCharIndices = Json.decodeFromString(serviceNextCharIndicesJson),
    deviceName = deviceName
)

fun GattServerStateDomain.toEntity(): GattServerEntity = GattServerEntity(
    id = id,
    name = name,
    deviceName = deviceName,
    servicesJson = Json.encodeToString(services),
    nextServiceIndex = nextServiceIndex,
    serviceIndicesJson = Json.encodeToString(serviceIndices),
    serviceNextCharIndicesJson = Json.encodeToString(serviceNextCharIndices)
)

// SavedDevice Mappers
fun BluetoothDeviceEntity.toDomain(): BluetoothDeviceDomain {
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

fun BluetoothDeviceDomain.toEntity(): BluetoothDeviceEntity = BluetoothDeviceEntity(
    address = address,
    name = name,
    servicesJson = if (services.isNotEmpty()) Json.encodeToString(services) else null
)
