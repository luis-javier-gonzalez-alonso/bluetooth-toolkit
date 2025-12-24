package net.ljga.projects.apps.bttk.data.bluetooth.utils

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class DeviceFoundReceiver(
    private val onDeviceFound: (BluetoothDevice, Int?) -> Unit
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableExtra(
                BluetoothDevice.EXTRA_DEVICE,
                BluetoothDevice::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent?.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }

        when (intent?.action) {
            BluetoothDevice.ACTION_FOUND -> {
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                val rssiValue = if (rssi == Short.MIN_VALUE.toInt()) null else rssi
                device?.let { onDeviceFound(it, rssiValue) }
            }
            BluetoothDevice.ACTION_UUID -> {
                device?.let { onDeviceFound(it, null) }
            }
        }
    }
}