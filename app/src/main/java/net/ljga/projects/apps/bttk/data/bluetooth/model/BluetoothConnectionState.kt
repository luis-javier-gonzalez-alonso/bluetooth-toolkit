package net.ljga.projects.apps.bttk.data.bluetooth.model;

import android.bluetooth.BluetoothProfile

enum class BluetoothConnectionState(val state: Int) {
    STATE_DISCONNECTED(BluetoothProfile.STATE_DISCONNECTED),
    STATE_CONNECTING(BluetoothProfile.STATE_CONNECTING),
    STATE_CONNECTED(BluetoothProfile.STATE_CONNECTED),
    STATE_DISCONNECTING(BluetoothProfile.STATE_DISCONNECTING);

    companion object {
        fun fromInt(value: Int): BluetoothConnectionState? {
            return entries.find { it.state == value }
        }
    }
}
