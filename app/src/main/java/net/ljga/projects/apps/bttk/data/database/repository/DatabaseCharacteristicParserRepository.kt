package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.dao.CharacteristicParserDao
import net.ljga.projects.apps.bttk.data.database.entities.CharacteristicParserConfig
import net.ljga.projects.apps.bttk.data.repository.CharacteristicParserRepository
import javax.inject.Inject

class DatabaseCharacteristicParserRepository @Inject constructor(
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