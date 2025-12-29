package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.BluetoothScriptDomain

interface BluetoothScriptRepository {
    fun getAllScripts(): Flow<List<BluetoothScriptDomain>>
    suspend fun getScriptById(id: Int): BluetoothScriptDomain?
    suspend fun saveScript(script: BluetoothScriptDomain): Long
    suspend fun deleteScript(id: Int)
}
