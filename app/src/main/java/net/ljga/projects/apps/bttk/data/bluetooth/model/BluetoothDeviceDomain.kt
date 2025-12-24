package net.ljga.projects.apps.bttk.data.bluetooth.model

data class BluetoothDeviceDomain(
    val name: String?,
    val address: String,
    val isInRange: Boolean = false,
    val bondState: Int = 10, // BluetoothDevice.BOND_NONE
    val type: Int = 0,      // BluetoothDevice.DEVICE_TYPE_UNKNOWN
    val uuids: List<String> = emptyList(),
    val rssi: Int? = null,
    val services: List<BluetoothServiceDomain> = emptyList()
)