package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.GattCharacteristicSettingsDao
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.GattCharacteristicSettingsDomain
import net.ljga.projects.apps.bttk.domain.repository.GattCharacteristicSettingsRepository
import javax.inject.Inject

class DatabaseGattCharacteristicSettingsRepository @Inject constructor(
    private val dao: GattCharacteristicSettingsDao
) : GattCharacteristicSettingsRepository {
    override val allSettings: Flow<List<GattCharacteristicSettingsDomain>> = 
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSettings(serviceUuid: String, characteristicUuid: String): GattCharacteristicSettingsDomain? {
        return dao.get(serviceUuid, characteristicUuid)?.toDomain()
    }

    override suspend fun saveSettings(settings: GattCharacteristicSettingsDomain) {
        dao.insert(settings.toEntity())
    }

    override suspend fun deleteSettings(serviceUuid: String, characteristicUuid: String) {
        dao.delete(serviceUuid, characteristicUuid)
    }
}
