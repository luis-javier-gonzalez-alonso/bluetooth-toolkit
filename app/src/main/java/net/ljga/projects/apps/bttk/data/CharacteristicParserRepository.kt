package net.ljga.projects.apps.bttk.data

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.local.database.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.local.database.CharacteristicParserDao
import javax.inject.Inject

interface CharacteristicParserRepository {
    fun getAllConfigs(): Flow<List<CharacteristicParserConfig>>
    fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfig?>
    suspend fun saveConfig(config: CharacteristicParserConfig)
    suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String)
}

class DefaultCharacteristicParserRepository @Inject constructor(
    private val characteristicParserDao: CharacteristicParserDao
) : CharacteristicParserRepository {
    override fun getAllConfigs(): Flow<List<CharacteristicParserConfig>> = characteristicParserDao.getAllConfigs()
    
    override fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfig?> = 
        characteristicParserDao.getConfig(serviceUuid, characteristicUuid)

    override suspend fun saveConfig(config: CharacteristicParserConfig) {
        characteristicParserDao.insertConfig(config)
    }

    override suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String) {
        characteristicParserDao.deleteConfig(serviceUuid, characteristicUuid)
    }
}
