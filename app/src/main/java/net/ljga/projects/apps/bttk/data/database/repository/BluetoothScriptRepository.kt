package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.entity.BluetoothScript
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothScriptDao
import javax.inject.Inject

interface BluetoothScriptRepository {
    fun getAllScripts(): Flow<List<BluetoothScript>>
    suspend fun getScriptById(id: Int): BluetoothScript?
    suspend fun saveScript(script: BluetoothScript): Long
    suspend fun deleteScript(id: Int)
}

class DefaultBluetoothScriptRepository @Inject constructor(
    private val bluetoothScriptDao: BluetoothScriptDao
) : BluetoothScriptRepository {
    override fun getAllScripts(): Flow<List<BluetoothScript>> = bluetoothScriptDao.getAllScripts()
    
    override suspend fun getScriptById(id: Int): BluetoothScript? = bluetoothScriptDao.getScriptById(id)
    
    override suspend fun saveScript(script: BluetoothScript): Long = bluetoothScriptDao.insertScript(script)
    
    override suspend fun deleteScript(id: Int) = bluetoothScriptDao.deleteScript(id)
}
