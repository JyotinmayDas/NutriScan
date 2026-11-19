package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.FoodDiaryEntry
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_diary_entries")
data class LocalDiaryEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val foodName: String,
    val quantity: Double,
    val calories: Int,
    val protein: Double,
    val carbohydrates: Double,
    val fat: Double,
    val fiber: Double,
    val sugar: Double,
    val mealType: String,
    val dateKey: String,
    val source: String,
    val confidence: Double,
    val notes: String,
    val updatedAtMillis: Long = System.currentTimeMillis()
) {
    fun toDomain(): FoodDiaryEntry = FoodDiaryEntry(
        id = id,
        userId = userId,
        foodName = foodName,
        quantity = quantity,
        calories = calories,
        protein = protein,
        carbohydrates = carbohydrates,
        fat = fat,
        fiber = fiber,
        sugar = sugar,
        mealType = mealType,
        dateKey = dateKey,
        source = source,
        confidence = confidence,
        notes = notes
    )

    companion object {
        fun fromDomain(entry: FoodDiaryEntry): LocalDiaryEntity = LocalDiaryEntity(
            id = entry.id.ifBlank { "local_${System.currentTimeMillis()}" },
            userId = entry.userId,
            foodName = entry.foodName,
            quantity = entry.quantity,
            calories = entry.calories,
            protein = entry.protein,
            carbohydrates = entry.carbohydrates,
            fat = entry.fat,
            fiber = entry.fiber,
            sugar = entry.sugar,
            mealType = entry.mealType,
            dateKey = entry.dateKey,
            source = entry.source,
            confidence = entry.confidence,
            notes = entry.notes
        )
    }
}

@Dao
interface LocalDiaryDao {
    @Query("SELECT * FROM local_diary_entries WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    fun observeCachedEntries(userId: String): Flow<List<LocalDiaryEntity>>

    @Query("SELECT * FROM local_diary_entries WHERE userId = :userId ORDER BY updatedAtMillis DESC")
    suspend fun getCachedEntries(userId: String): List<LocalDiaryEntity>

    @Query("SELECT * FROM local_diary_entries WHERE userId = :userId ORDER BY updatedAtMillis DESC LIMIT :limit")
    suspend fun getRecentCachedScans(userId: String, limit: Int): List<LocalDiaryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntries(entries: List<LocalDiaryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntry(entry: LocalDiaryEntity)

    @Query("DELETE FROM local_diary_entries WHERE id = :id")
    suspend fun deleteEntryById(id: String)
}

@Database(entities = [LocalDiaryEntity::class], version = 1, exportSchema = false)
abstract class NutriScanDatabase : RoomDatabase() {
    abstract fun localDiaryDao(): LocalDiaryDao

    companion object {
        @Volatile
        private var INSTANCE: NutriScanDatabase? = null

        fun getInstance(context: Context): NutriScanDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriScanDatabase::class.java,
                    "nutriscan_offline_cache.db"
                ).fallbackToDestructiveMigration(true).build().also { INSTANCE = it }
            }
        }
    }
}
