package top.kagg886.backend.database.dao

import androidx.room3.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Entity(tableName = "llm-provider")
@Serializable
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
