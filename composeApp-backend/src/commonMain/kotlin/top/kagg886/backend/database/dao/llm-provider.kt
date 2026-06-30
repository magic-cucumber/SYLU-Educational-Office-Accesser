package top.kagg886.backend.database.dao

import androidx.room3.Dao
import androidx.room3.Delete
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "llm-provider")
data class LLMProviderEntity(
    @PrimaryKey val uuid: String,
    val modelName: String,
    val modelKey: String,
    val baseUrl: String,
    val supportMultimodal: Boolean,
    val supportNativeJsonOutput: Boolean,
    val modelRemark: String,
    val modelDescription: String,
)

@Dao
interface LLMProviderDao {
    @Query("SELECT * FROM `llm-provider`")
    fun allFlow(): Flow<List<LLMProviderEntity>>

    @Query("SELECT * FROM `llm-provider`")
    suspend fun all(): List<LLMProviderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LLMProviderEntity)

    @Update
    suspend fun update(item: LLMProviderEntity)

    @Delete
    suspend fun delete(item: LLMProviderEntity)
}
