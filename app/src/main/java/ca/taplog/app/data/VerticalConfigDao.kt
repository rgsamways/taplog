package ca.taplog.app.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "vertical_configs")
data class VerticalConfigEntity(
    @PrimaryKey val verticalCode: String,
    val configJson: String
)

@Dao
interface VerticalConfigDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VerticalConfigEntity)

    @Query("SELECT * FROM vertical_configs")
    suspend fun getAll(): List<VerticalConfigEntity>
}
