package net.ljga.projects.apps.bttk.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class DataFrame(
    val name: String,
    val data: ByteArray
) {
    @PrimaryKey(autoGenerate = true)
    var uid: Int = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataFrame

        if (name != other.name) return false
        if (!data.contentEquals(other.data)) return false
        if (uid != other.uid) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + uid
        return result
    }
}

