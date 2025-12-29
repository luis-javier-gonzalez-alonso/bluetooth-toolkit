package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity

@Entity(
    tableName = "gatt_characteristic_alias",
    primaryKeys = ["serviceUuid", "characteristicUuid"]
)
data class GattCharacteristicAliasEntity(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String
)
