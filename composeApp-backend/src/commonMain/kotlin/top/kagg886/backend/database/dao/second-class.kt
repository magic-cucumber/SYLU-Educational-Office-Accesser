package top.kagg886.backend.database.dao

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.ForeignKey
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Transaction
import top.kagg886.eoa.second.SecondClassData
import top.kagg886.eoa.second.SecondClassDataSummary

@Entity(tableName = "second_class_summary")
data class SecondClassSummaryEntity(
    @PrimaryKey val id: String,
    val max: Double,
    val sortOrder: Int
)

@Entity(
    tableName = "second_class_data",
    foreignKeys = [
        ForeignKey(
            entity = SecondClassSummaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["summaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["summaryId"])
    ]
)
data class SecondClassDataEntity(
    @PrimaryKey(autoGenerate = true) val id: Long? = null,
    val summaryId: String,
    val sortOrder: Int,
    val name: String,
    val sponsor: String,
    val time: String,
    val actor: String,
    val people: Int,
    val score: Double
)

@Dao
abstract class SecondClassDao {
    @Query("DELETE FROM second_class_data")
    abstract suspend fun clearData()

    @Query("DELETE FROM second_class_summary")
    abstract suspend fun clearSummary()

    @Query("SELECT * FROM second_class_summary ORDER BY sortOrder ASC")
    abstract suspend fun allSummary(): List<SecondClassSummaryEntity>

    @Query("SELECT * FROM second_class_data WHERE summaryId = :summaryId ORDER BY sortOrder ASC")
    abstract suspend fun allData(summaryId: String): List<SecondClassDataEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(item: SecondClassSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(items: List<SecondClassDataEntity>)

    @Transaction
    open suspend fun clear() {
        clearData()
        clearSummary()
    }

    @Transaction
    open suspend fun all(): Map<SecondClassDataSummary, List<SecondClassData>> {
        val map = linkedMapOf<SecondClassDataSummary, List<SecondClassData>>()
        for (summary in allSummary()) {
            map[summary.toItem()] = allData(summary.id).map { it.toItem() }
        }
        return map
    }

    @Transaction
    open suspend fun replaceAll(items: Map<SecondClassDataSummary, List<SecondClassData>>) {
        clear()
        items.entries.forEachIndexed { index, (summary, data) ->
            insert(summary.toEntity(index))
            insertAll(
                data.mapIndexed { dataIndex, item ->
                    item.toEntity(summary.id, dataIndex)
                }
            )
        }
    }
}

fun SecondClassDataSummary.toEntity(sortOrder: Int) = SecondClassSummaryEntity(
    id = id,
    max = max,
    sortOrder = sortOrder
)

fun SecondClassSummaryEntity.toItem() = SecondClassDataSummary(
    id = id,
    max = max
)

fun SecondClassData.toEntity(summaryId: String, sortOrder: Int) = SecondClassDataEntity(
    summaryId = summaryId,
    sortOrder = sortOrder,
    name = name,
    sponsor = sponsor,
    time = time,
    actor = actor,
    people = people,
    score = score
)

fun SecondClassDataEntity.toItem() = SecondClassData(
    name = name,
    sponsor = sponsor,
    time = time,
    actor = actor,
    people = people,
    score = score
)
