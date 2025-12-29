package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gatt_servers")
data class GattServerEntity(
    @PrimaryKey val id: Int = 1,
    val servicesJson: String,
    val nextServiceIndex: Int = 0
)
