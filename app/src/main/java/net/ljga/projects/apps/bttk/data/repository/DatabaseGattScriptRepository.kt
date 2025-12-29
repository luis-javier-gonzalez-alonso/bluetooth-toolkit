package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.data.database.dao.GattScriptDao
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import net.ljga.projects.apps.bttk.domain.repository.GattScriptRepository
import javax.inject.Inject

class DatabaseGattScriptRepository @Inject constructor(
    private val gattScriptDao: GattScriptDao
) : GattScriptRepository {
    override fun getAllScripts(): Flow<List<BluetoothScriptDomain>> = 
        gattScriptDao.getAllScripts().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getScriptById(id: Int): BluetoothScriptDomain? = 
        gattScriptDao.getScriptById(id)?.toDomain()

    override suspend fun saveScript(script: BluetoothScriptDomain): Long = 
        gattScriptDao.insertScript(script.toEntity())

    override suspend fun deleteScript(id: Int) = gattScriptDao.deleteScript(id)
}
