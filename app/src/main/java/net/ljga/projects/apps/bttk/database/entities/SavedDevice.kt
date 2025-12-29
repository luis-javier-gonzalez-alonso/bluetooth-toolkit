package net.ljga.projects.apps.bttk.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.json.Json
import net.ljga.projects.apps.bttk.domain.model.BluetoothDeviceDomain
import net.ljga.projects.apps.bttk.domain.model.BluetoothServiceDomain

@Entity(tableName = "saved_devices")
data class SavedDevice(
    @PrimaryKey val address: String,
    val name: String?,
    val servicesJson: String? = null
)

fun SavedDevice.toDomain(): BluetoothDeviceDomain {
    val services = servicesJson?.let {
        try {
            Json.decodeFromString<List<BluetoothServiceDomain>>(it)
        } catch (e: Exception) {
            emptyList()
        }
    } ?: emptyList()

    return BluetoothDeviceDomain(
        name = name,
        address = address,
        isInRange = false,
        services = services
    )
}
