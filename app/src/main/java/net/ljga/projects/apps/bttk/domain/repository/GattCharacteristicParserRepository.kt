package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain

interface GattCharacteristicParserRepository {
    fun getAllConfigs(): Flow<List<CharacteristicParserConfigDomain>>
    fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfigDomain?>
    suspend fun saveConfig(config: CharacteristicParserConfigDomain)
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}
