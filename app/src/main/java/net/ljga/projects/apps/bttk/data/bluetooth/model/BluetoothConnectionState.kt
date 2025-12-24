package net.ljga.projects.apps.bttk.data.bluetooth.model;

enum class BluetoothConnectionState(val state: Int) {
    STATE_CONNECTED(0),
    STATE_DISCONNECTED(1);

    companion object {
        fun fromInt(newValue: Int): BluetoothConnectionState? {
            return entries.find { it.state == newValue }
        }
    }
}