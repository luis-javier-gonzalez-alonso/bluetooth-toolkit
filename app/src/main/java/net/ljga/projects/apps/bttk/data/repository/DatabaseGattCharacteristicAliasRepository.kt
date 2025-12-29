package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicAliasDao
import net.ljga.projects.apps.bttk.data.database.entity.GattCharacteristicAliasEntity
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicAliasRepository
import javax.inject.Inject

class DatabaseGattCharacteristicAliasRepository @Inject constructor(
    private val gattCharacteristicAliasDao: GattCharacteristicAliasDao
) : GattCharacteristicAliasRepository {

    override val gattAliases: Flow<Map<String, String>> =
        gattCharacteristicAliasDao.getAllAliases().map { aliases ->
            aliases.associate { "${it.serviceUuid}-${it.characteristicUuid}" to it.alias }
        }

    override suspend fun saveAlias(serviceUuid: String, characteristicUuid: String, alias: String) {
        if (alias.isBlank()) {
            deleteAlias(serviceUuid, characteristicUuid)
        } else {
            gattCharacteristicAliasDao.insertAlias(
                GattCharacteristicAliasEntity(
                    serviceUuid,
                    characteristicUuid,
                    alias
                )
            )
        }
    }

    override suspend fun deleteAlias(serviceUuid: String, characteristicUuid: String) {
        gattCharacteristicAliasDao.deleteAlias(serviceUuid, characteristicUuid)
    }
}
