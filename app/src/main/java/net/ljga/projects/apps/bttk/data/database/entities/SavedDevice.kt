package net.ljga.projects.apps.bttk.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_devices")
data class SavedDevice(
    @PrimaryKey val address: String,
    val name: String?,
    val servicesJson: String? = null
)

