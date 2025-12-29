package net.ljga.projects.apps.bttk.data.database.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.data.database.dao.BluetoothScriptDao
import net.ljga.projects.apps.bttk.data.database.entities.BluetoothScript
import net.ljga.projects.apps.bttk.data.repository.BluetoothScriptRepository
import javax.inject.Inject

class DatabaseBluetoothScriptRepository @Inject constructor(
    private val bluetoothScriptDao: BluetoothScriptDao
) : BluetoothScriptRepository {
    override fun getAllScripts(): Flow<List<BluetoothScript>> = bluetoothScriptDao.getAllScripts()

    override suspend fun getScriptById(id: Int): BluetoothScript? = bluetoothScriptDao.getScriptById(id)

    override suspend fun saveScript(script: BluetoothScript): Long = bluetoothScriptDao.insertScript(script)

    override suspend fun deleteScript(id: Int) = bluetoothScriptDao.deleteScript(id)
}