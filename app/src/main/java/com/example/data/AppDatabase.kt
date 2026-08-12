package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    @Query("SELECT * FROM shortcuts")
    fun getAllShortcuts(): Flow<List<ShortcutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutEntity)

    @Query("SELECT * FROM shortcuts WHERE id = :id")
    suspend fun getShortcut(id: Int): ShortcutEntity?
}

@Dao
interface ClipboardDao {
    @Query("SELECT * FROM clipboard ORDER BY timestamp DESC LIMIT 20")
    fun getClipboardHistory(): Flow<List<ClipboardEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertClipboardItem(item: ClipboardEntity)

    @Query("DELETE FROM clipboard")
    suspend fun clearHistory()
}

@Database(entities = [ShortcutEntity::class, ClipboardEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shortcutDao(): ShortcutDao
    abstract fun clipboardDao(): ClipboardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "orbs_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
