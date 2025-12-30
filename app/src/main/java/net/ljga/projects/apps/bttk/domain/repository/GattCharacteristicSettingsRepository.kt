package net.ljga.projects.apps.bttk.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.device_connection.model.GattCharacteristicSettingsDomain

interface GattCharacteristicSettingsRepository {
    val allSettings: Flow<List<GattCharacteristicSettingsDomain>>
    suspend fun getSettings(serviceUuid: String, characteristicUuid: String): GattCharacteristicSettingsDomain?
    suspend fun saveSettings(settings: GattCharacteristicSettingsDomain)
    suspend fun deleteSettings(serviceUuid: String, characteristicUuid: String)
}
