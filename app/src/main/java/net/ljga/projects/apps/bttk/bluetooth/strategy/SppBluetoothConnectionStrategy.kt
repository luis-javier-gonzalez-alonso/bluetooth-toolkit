package net.ljga.projects.apps.bttk.bluetooth.strategy

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.bluetooth.model.BluetoothProfile
import java.io.IOException
import java.util.UUID

class SppBluetoothConnectionStrategy(
    private val bluetoothAdapter: BluetoothAdapter,
    override val uuid: UUID = BluetoothProfile.SPP.uuid!!
) : BluetoothConnectionStrategy {
    override val name: String = "SPP"
    private var socket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = flow {
        val device = bluetoothAdapter.getRemoteDevice(address)
        socket = device.createRfcommSocketToServiceRecord(uuid)
        
        socket?.connect()
        
        val inputStream = socket?.inputStream ?: throw IOException("Input stream is null")
        val buffer = ByteArray(1024)
        
        while (true) {
            val bytesRead = try {
                inputStream.read(buffer)
            } catch (e: IOException) {
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

    override suspend fun disconnect() {
        try {
            socket?.close()
        } catch (e: IOException) {
            // Ignore
        }
        socket = null
    }

    override fun writeCharacteristic(serviceUuid: String, characteristicUuid: String, data: ByteArray) {
        try {
            socket?.outputStream?.write(data)
        } catch (e: IOException) {
            // Ignore
        }
    }
}
