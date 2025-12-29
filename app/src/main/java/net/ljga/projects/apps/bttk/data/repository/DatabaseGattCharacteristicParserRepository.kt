package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicParserDao
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.CharacteristicParserConfigDomain
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicParserRepository
import javax.inject.Inject

class DatabaseGattCharacteristicParserRepository @Inject constructor(
    private val gattCharacteristicParserDao: GattCharacteristicParserDao
) : GattCharacteristicParserRepository {
    override fun getAllConfigs(): Flow<List<CharacteristicParserConfigDomain>> = 
        gattCharacteristicParserDao.getAllConfigs().map { entities -> entities.map { it.toDomain() } }

    override fun getConfig(serviceUuid: String, characteristicUuid: String): Flow<CharacteristicParserConfigDomain?> =
        gattCharacteristicParserDao.getConfig(serviceUuid, characteristicUuid).map { it?.toDomain() }

    override suspend fun saveConfig(config: CharacteristicParserConfigDomain) {
        gattCharacteristicParserDao.insertConfig(config.toEntity())
    }

    override suspend fun deleteConfig(serviceUuid: String, characteristicUuid: String) {
        gattCharacteristicParserDao.deleteConfig(serviceUuid, characteristicUuid)
    }
}
