package com.example.aplicativopesoplanta.data

import androidx.room.*
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "samplings")
data class SamplingEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val block: String,
    val date: Long,
    val weight: Double,
    val rootSystem: String,
    val fusarium: Boolean,
    val meristem: Boolean,
    val findings: String, // Comma-separated values
    val observations: String
)

@Entity(tableName = "cached_blocks")
data class CachedBlockEntity(
    @PrimaryKey val bloque: String
)

@Dao
interface SamplingDao {
    @Query("SELECT * FROM samplings ORDER BY date DESC")
    fun getAllSamplings(): Flow<List<SamplingEntity>>

    @Insert
    suspend fun insertSampling(sampling: SamplingEntity): Long

    @Delete
    suspend fun deleteSampling(sampling: SamplingEntity): Int

    @Query("DELETE FROM samplings")
    suspend fun deleteAll()

    // Cache methods
    @Query("SELECT bloque FROM cached_blocks")
    fun getCachedBlocks(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<CachedBlockEntity>)

    @Query("DELETE FROM cached_blocks")
    suspend fun clearCachedBlocks()
}

@Database(entities = [SamplingEntity::class, CachedBlockEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun samplingDao(): SamplingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "sampling_database"
                )
                .fallbackToDestructiveMigration() // Simple for this use case
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
