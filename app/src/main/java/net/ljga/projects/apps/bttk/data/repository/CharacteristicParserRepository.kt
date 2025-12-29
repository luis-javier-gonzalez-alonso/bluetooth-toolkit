package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain

interface CharacteristicParserRepository {
    fun getAllConfigs(): Flow<List<CharacteristicParserConfigDomain>>
    fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfigDomain?>
    suspend fun saveConfig(config: CharacteristicParserConfigDomain)
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}
