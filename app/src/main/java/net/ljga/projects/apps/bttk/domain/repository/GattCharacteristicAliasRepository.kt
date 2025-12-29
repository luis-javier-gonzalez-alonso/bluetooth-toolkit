package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow

interface GattCharacteristicAliasRepository {
    val gattAliases: Flow<Map<String, String>>
    suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String)
    suspend fun deleteAlias(serviceUuid: String, characteristicUuid: String)
}
