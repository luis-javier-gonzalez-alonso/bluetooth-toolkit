package net.ljga.projects.apps.bttk.data.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import java.util.*

class GattBluetoothConnectionStrategy(
    context: Context,
    bluetoothAdapter: BluetoothAdapter
) : BaseGattBluetoothConnectionStrategy(context, bluetoothAdapter) {
    override val name: String = "GATT"
    override val uuid: UUID? = null

    init {
        // You can add default handlers here if desired
        handlers.add(BatteryCharacteristicHandler())
    }

    @SuppressLint("MissingPermission")
    override fun onGattServicesDiscovered(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                // Let handlers take action
                handlers.forEach { handler ->
                    if (handler.serviceUuid == null || handler.serviceUuid == service.uuid) {
                        handler.onServiceDiscovered(gatt, characteristic)
                    }
                }
                
                // Generic GATT behavior: notify for everything else not handled specifically if desired
                // Or just keep it extensible via handlers.
                if (handlers.none { it.characteristicUuid == characteristic.uuid }) {
                    if (isNotifyable(characteristic)) {
                        enableNotification(gatt, characteristic)
                    }
                }
            }
        }
    }

    override fun onDataReceived(characteristic: BluetoothGattCharacteristic, value: ByteArray) {
        // Generic logging for data not caught by specific handlers
        if (handlers.none { it.characteristicUuid == characteristic.uuid }) {
            // The base class could also handle this or we can let it be handled here
        }
    }

    private fun isNotifyable(characteristic: BluetoothGattCharacteristic): Boolean {
        return (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) ||
               (characteristic.properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0)
    }
}
