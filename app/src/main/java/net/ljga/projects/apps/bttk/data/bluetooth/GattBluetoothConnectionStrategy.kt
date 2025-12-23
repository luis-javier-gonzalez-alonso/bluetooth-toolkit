package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import kotlinx.coroutines.channels.ProducerScope
import java.util.*

class GattBluetoothConnectionStrategy(
    context: Context,
    bluetoothAdapter: BluetoothAdapter
) : BaseGattBluetoothConnectionStrategy(context, bluetoothAdapter) {
    override val name: String = "GATT"
    override val uuid: UUID? = null

    private val defaultHandler = DefaultGattCharacteristicHandler()

    init {
        handlers.add(BatteryCharacteristicHandler())
    }

    @SuppressLint("MissingPermission")
    override fun onGattServicesDiscovered(gatt: BluetoothGatt, scope: ProducerScope<BluetoothDataPacket>) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                var handled = false
                
                // 1. Check specific handlers
                handlers.forEach { handler ->
                    if (handler.serviceUuid == null || handler.serviceUuid == service.uuid) {
                        val discoveryPacket = handler.onServiceDiscovered(gatt, characteristic)
                        if (discoveryPacket != null) {
                            scope.trySend(discoveryPacket)
                        }
                        if (handler.characteristicUuid == characteristic.uuid) {
                            handled = true
                        }
                    }
                }
                
                // 2. Fallback to default handler for logging and notifications
                if (!handled) {
                    val discoveryPacket = defaultHandler.onServiceDiscovered(gatt, characteristic)
                    if (discoveryPacket != null) {
                        scope.trySend(discoveryPacket)
                    }
                    
                    if (isNotifyable(characteristic)) {
                        enableNotification(gatt, characteristic)
                    }
                }
            }
        }
    }

    override fun onDataReceived(characteristic: BluetoothGattCharacteristic, value: ByteArray, scope: ProducerScope<BluetoothDataPacket>) {
        // If no specific handler took interest in this data, the default handler logs it
        if (handlers.none { it.characteristicUuid == characteristic.uuid }) {
            val packet = defaultHandler.handleData(characteristic, value)
            if (packet != null) {
                scope.trySend(packet)
            }
        }
    }

    private fun isNotifyable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
               (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
    }
}
