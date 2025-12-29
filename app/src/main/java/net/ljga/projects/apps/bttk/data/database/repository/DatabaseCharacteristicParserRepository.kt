package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.CharacteristicParserDao
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain
import net.ljga.projects.apps.bttk.domain.repository.CharacteristicParserRepository
import javax.inject.Inject

class DatabaseCharacteristicParserRepository @Inject constructor(
    private val characteristicParserDao: CharacteristicParserDao
) : CharacteristicParserRepository {
    override fun getAllConfigs(): Flow<List<CharacteristicParserConfigDomain>> = 
        characteristicParserDao.getAllConfigs().map { entities -> entities.map { it.toDomain() } }

    override fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfigDomain?> =
        characteristicParserDao.getConfig(serviceUuid, characteristicUuid).map { it?.toDomain() }

    override suspend fun saveConfig(config: CharacteristicParserConfigDomain) {
        characteristicParserDao.insertConfig(config.toEntity())
    }

    override suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String) {
        characteristicParserDao.deleteConfig(serviceUuid, characteristicUuid)
    }
}
