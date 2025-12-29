package net.ljga.projects.apps.bttk.data.repository

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.database.entities.BluetoothScript

interface BluetoothScriptRepository {
    fun getAllScripts(): Flow<List<BluetoothScript>>
    suspend fun getScriptById(id: Int): BluetoothScript?
    suspend fun saveScript(script: BluetoothScript): Long
    suspend fun deleteScript(id: Int)
}
