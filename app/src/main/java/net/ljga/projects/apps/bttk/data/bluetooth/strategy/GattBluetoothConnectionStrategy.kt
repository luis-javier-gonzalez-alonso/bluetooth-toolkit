package net.ljga.projects.apps.bttk.data.bluetooth.strategy

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Context
import kotlinx.coroutines.channels.ProducerScope
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothCharacteristicDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothDataPacket
import net.ljga.projects.apps.bttk.data.bluetooth.model.BluetoothServiceDomain
import net.ljga.projects.apps.bttk.data.bluetooth.model.DataFormat
import net.ljga.projects.apps.bttk.data.bluetooth.strategy.gatt.handler.BatteryCharacteristicHandler
import net.ljga.projects.apps.bttk.data.bluetooth.strategy.gatt.handler.DefaultGattCharacteristicHandler
import java.util.UUID

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
        val services = gatt.services.map { service ->
            BluetoothServiceDomain(
                uuid = service.uuid.toString(),
                characteristics = service.characteristics.map { characteristic ->
                    BluetoothCharacteristicDomain(
                        uuid = characteristic.uuid.toString(),
                        properties = getPropertiesList(characteristic.properties),
                        descriptors = characteristic.descriptors.map { it.uuid.toString() }
                    )
                }
            )
        }

        // Emit structured data for persistence and UI
        scope.trySend(
            BluetoothDataPacket(
                format = DataFormat.GATT_STRUCTURE,
                gattServices = services,
                source = "GATT Discovery"
            )
        )

        // Process handlers for specific logic
        gatt.services.forEach { service ->
            service.characteristics.forEach { characteristic ->
                // Check specific handlers
                handlers.forEach { handler ->
                    if (handler.serviceUuid == null || handler.serviceUuid == service.uuid) {
                        val discoveryPacket = handler.onServiceDiscovered(gatt, characteristic)
                        if (discoveryPacket != null) {
                            scope.trySend(discoveryPacket)
                        }
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

    private fun getPropertiesList(properties: Int): List<String> {
        val list = mutableListOf<String>()
        if (properties and BluetoothGattCharacteristic.PROPERTY_READ != 0) list.add("READ")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE != 0) list.add("WRITE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE != 0) list.add("WRITE_NO_RESPONSE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY != 0) list.add("NOTIFY")
        if (properties and BluetoothGattCharacteristic.PROPERTY_INDICATE != 0) list.add("INDICATE")
        if (properties and BluetoothGattCharacteristic.PROPERTY_BROADCAST != 0) list.add("BROADCAST")
        if (properties and BluetoothGattCharacteristic.PROPERTY_EXTENDED_PROPS != 0) list.add("EXTENDED_PROPS")
        if (properties and BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE != 0) list.add("SIGNED_WRITE")
        return list
    }
}
