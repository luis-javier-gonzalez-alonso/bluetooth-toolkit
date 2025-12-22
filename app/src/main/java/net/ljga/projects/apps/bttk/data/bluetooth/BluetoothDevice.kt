package net.ljga.projects.apps.bttk.data.bluetooth

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val isInRange: Boolean = false
)
