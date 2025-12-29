package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.database.entities.CharacteristicParserConfig

interface CharacteristicParserRepository {
    fun getAllConfigs(): Flow<List<CharacteristicParserConfig>>
    fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfig?>
    suspend fun saveConfig(config: CharacteristicParserConfig)
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}
