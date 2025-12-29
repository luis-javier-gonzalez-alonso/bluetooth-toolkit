package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.ljga.projects.apps.bttk.database.dao.BluetoothScriptDao
import net.ljga.projects.apps.bttk.domain.repository.BluetoothScriptRepository
import net.ljga.projects.apps.bttk.data.toDomain
import net.ljga.projects.apps.bttk.data.toEntity
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain
import javax.inject.Inject

class DatabaseBluetoothScriptRepository @Inject constructor(
    private val bluetoothScriptDao: BluetoothScriptDao
) : BluetoothScriptRepository {
    override fun getAllScripts(): Flow<List<BluetoothScriptDomain>> = 
        bluetoothScriptDao.getAllScripts().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getScriptById(id: Int): BluetoothScriptDomain? = 
        bluetoothScriptDao.getScriptById(id)?.toDomain()

    override suspend fun saveScript(script: BluetoothScriptDomain): Long = 
        bluetoothScriptDao.insertScript(script.toEntity())

    override suspend fun deleteScript(id: Int) = bluetoothScriptDao.deleteScript(id)
}
