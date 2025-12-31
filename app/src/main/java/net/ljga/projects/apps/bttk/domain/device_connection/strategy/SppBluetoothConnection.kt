package net.ljga.projects.apps.bttk.domain.device_connection.strategy

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.ljga.projects.apps.bttk.domain.device_connection.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import java.io.IOException

class SppBluetoothConnection(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnection {
    override val type: BluetoothConnectionType = BluetoothConnectionType.SPP

    private var socket: BluetoothSocket? = null

    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = flow {
        val device = bluetoothAdapter.getRemoteDevice(address)
        socket = device.createRfcommSocketToServiceRecord(type.uuid)
        
        try {
            socket?.connect()
            emit(
                BluetoothDataPacket(
                    source = "System",
                    text = "Connected to ${device.name ?: device.address}",
                    format = DataFormat.STRUCTURED
                )
            )
        } catch (e: IOException) {
            emit(
                BluetoothDataPacket(
                    source = "System",
                    text = "Failed to connect: ${e.message}",
                    format = DataFormat.STRUCTURED
                )
            )
            throw e
        }
        
        val inputStream = socket?.inputStream ?: throw IOException("Input stream is null")
        val buffer = ByteArray(1024)
        
        while (true) {
            val bytesRead = try {
                inputStream.read(buffer)
            } catch (e: IOException) {
                emit(
                    BluetoothDataPacket(
                        source = "System",
                        text = "Connection lost: ${e.message}",
                        format = DataFormat.STRUCTURED
                    )
                )
                -1
            }
            
            if (bytesRead == -1) break
            
            emit(
                BluetoothDataPacket(
                    data = buffer.copyOfRange(0, bytesRead),
                    source = device.name ?: device.address
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    override fun disconnect() {
        try {
            socket?.close()
        } catch (_: IOException) {
            // Ignore
        }
        socket = null
    }

    override fun process(request: ProcessRequest): BluetoothDataPacket? {
        return null
    }
}