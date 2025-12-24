package net.ljga.projects.apps.bttk.data.local.database

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

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

@Dao
interface DataFrameDao {
    @Query("SELECT * FROM dataframe ORDER BY name ASC")
    fun getDataFrames(): Flow<List<DataFrame>>

    @Insert
    suspend fun insertDataFrame(item: DataFrame)

    @Query("DELETE FROM dataframe WHERE uid = :uid")
    suspend fun deleteDataFrame(uid: Int)
}
