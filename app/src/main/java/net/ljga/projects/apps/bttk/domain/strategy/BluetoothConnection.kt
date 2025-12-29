package net.ljga.projects.apps.bttk.domain.strategy

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket

interface BluetoothConnection {
    val type: BluetoothConnectionType

    suspend fun connect(address: String): Flow<BluetoothDataPacket>
    suspend fun disconnect()
}
