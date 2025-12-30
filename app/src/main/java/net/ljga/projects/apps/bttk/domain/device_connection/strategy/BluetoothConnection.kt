package net.ljga.projects.apps.bttk.domain.device_connection.strategy

import kotlinx.coroutines.flow.Flow
import net.ljga.projects.apps.bttk.domain.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.process.ProcessRequest

interface BluetoothConnection {
    val type: BluetoothConnectionType

    suspend fun connect(address: String): Flow<BluetoothDataPacket>
    fun disconnect()

    fun process(request: ProcessRequest): BluetoothDataPacket?
}
