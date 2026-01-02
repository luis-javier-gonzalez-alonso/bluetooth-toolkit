package net.ljga.projects.apps.bttk.domain.device_connection.strategy

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import net.ljga.projects.apps.bttk.domain.device_connection.model.BluetoothConnectionType
import net.ljga.projects.apps.bttk.domain.device_connection.model.process.ProcessRequest
import net.ljga.projects.apps.bttk.domain.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.domain.model.DataFormat
import java.io.IOException

private const val TAG = "SppConnection"

class SppBluetoothConnection(
    private val bluetoothAdapter: BluetoothAdapter
) : BluetoothConnection {
    override val type: BluetoothConnectionType = BluetoothConnectionType.SPP

    private var socket: BluetoothSocket? = null
    private var isManualDisconnect = false

    override suspend fun connect(address: String): Flow<BluetoothDataPacket> = flow {
        val device = bluetoothAdapter.getRemoteDevice(address)
        var retryCount = 0
        val maxRetries = 3
        isManualDisconnect = false

        while (retryCount <= maxRetries && !isManualDisconnect) {
            try {
                Log.i(TAG, "Attempting SPP connection to $address (Attempt ${retryCount + 1})")
                socket = device.createRfcommSocketToServiceRecord(type.uuid)
                
                // On some devices, discovery might still be active, which slows down connection
                if (bluetoothAdapter.isDiscovering) {
                    bluetoothAdapter.cancelDiscovery()
                }

                socket?.connect()
                
                Log.i(TAG, "SPP Connected to $address")
                emit(
                    BluetoothDataPacket(
                        source = "System",
                        text = "Connected to ${device.name ?: address} (SPP)",
                        format = DataFormat.STRUCTURED
                    )
                )
                break // Success, exit retry loop
            } catch (e: IOException) {
                retryCount++
                val errorMsg = "SPP Connection failed: ${e.message}"
                Log.e(TAG, errorMsg)
                
                if (retryCount <= maxRetries && !isManualDisconnect) {
                    val backoff = retryCount * 2000L
                    emit(BluetoothDataPacket(source = "System", text = "$errorMsg. Retrying in ${backoff/1000}s...", format = DataFormat.STRUCTURED))
                    socket?.close()
                    delay(backoff)
                } else {
                    emit(BluetoothDataPacket(source = "System", text = "SPP connection failed after $maxRetries retries.", format = DataFormat.STRUCTURED))
                    throw e
                }
            }
        }
        
        val inputStream = socket?.inputStream ?: throw IOException("Input stream is null")
        val buffer = ByteArray(2048) // Increased buffer for SPP
        
        try {
            while (!isManualDisconnect) {
                val bytesRead = try {
                    inputStream.read(buffer)
                } catch (e: IOException) {
                    if (!isManualDisconnect) {
                        Log.e(TAG, "Input stream read error: ${e.message}")
                        emit(
                            BluetoothDataPacket(
                                source = "System",
                                text = "Connection lost: ${e.message}",
                                format = DataFormat.STRUCTURED
                            )
                        )
                    }
                    -1
                }
                
                if (bytesRead == -1) break
                
                emit(
                    BluetoothDataPacket(
                        data = buffer.copyOfRange(0, bytesRead),
                        source = device.name ?: address,
                        format = DataFormat.HEX_ASCII
                    )
                )
            }
        } finally {
            disconnect()
        }
    }.flowOn(Dispatchers.IO)

    override fun disconnect() {
        Log.i(TAG, "Closing SPP socket")
        isManualDisconnect = true
        try {
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }
        socket = null
    }

    override fun process(request: ProcessRequest): BluetoothDataPacket? {
        // SPP usually doesn't have structured requests like GATT, 
        // but write logic could be added here if needed.
        return null
    }
}
