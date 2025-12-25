package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gatt_server_config")
data class GattServerConfig(
    @PrimaryKey val id: Int = 1,
    val servicesJson: String,
    val nextServiceIndex: Int = 0
)
