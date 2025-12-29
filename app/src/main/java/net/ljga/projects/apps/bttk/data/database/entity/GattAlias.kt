package net.ljga.projects.apps.bttk.data.database.entity

import androidx.room.Entity

@Entity(primaryKeys = ["serviceUuid", "characteristicUuid"])
data class GattAlias(
    val serviceUuid: String,
    val characteristicUuid: String,
    val alias: String
)
