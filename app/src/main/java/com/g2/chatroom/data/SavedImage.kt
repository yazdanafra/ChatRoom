import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_images")
data class SavedImage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val originalUrl: String,
    val localPath: String,
    val fileName: String,
    val savedAt: Long = System.currentTimeMillis(),
    val messageId: String? = null
)