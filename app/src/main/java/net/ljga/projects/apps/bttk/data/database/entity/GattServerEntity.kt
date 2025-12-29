package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gatt_servers")
data class GattServerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val deviceName: String? = null,
    val servicesJson: String,
    val nextServiceIndex: Int = 0,
    val serviceIndicesJson: String = "{}",
    val serviceNextCharIndicesJson: String = "{}"
)
