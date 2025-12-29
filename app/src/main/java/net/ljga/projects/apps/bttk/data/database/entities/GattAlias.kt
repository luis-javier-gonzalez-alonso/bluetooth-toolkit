package net.ljga.projects.apps.bttk.data.database.entities

import androidx.room.Entity

@Entity(primaryKeys = ["serviceUuid", "characteristicUuid"])
data class GattAlias(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String
)
