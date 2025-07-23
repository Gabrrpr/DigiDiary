package com.example.digi_diary.data

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.digi_diary.data.dao.NoteDao
import com.example.digi_diary.data.dao.UserDao
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.data.model.User
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Note::class
    ], 
    version = 5, 
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao

    companion object {
        private const val TAG = "AppDatabase"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            Log.d(TAG, "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                try {
                    val dbFile = context.getDatabasePath("digi_diary_database")
                    Log.d(TAG, "Database path: ${dbFile.absolutePath}")
                    Log.d(TAG, "Database exists: ${dbFile.exists()}")
                    
                    val instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "digi_diary_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            Log.d(TAG, "New database created, version: ${db.version}")
                        }
                        
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            Log.d(TAG, "Database opened, version: ${db.version}")
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                    
                    // Log database info after creation
                    Log.d(TAG, "Database created/opened, version: ${instance.openHelper.readableDatabase.version}")
                    
                    // Log tables in the database
                    val cursor = instance.openHelper.readableDatabase.query(
                        "SELECT name FROM sqlite_master WHERE type='table'"
                    )
                    val tables = mutableListOf<String>()
                    while (cursor.moveToNext()) {
                        tables.add(cursor.getString(0))
                    }
                    cursor.close()
                    Log.d(TAG, "Tables in database: $tables")
                    
                    INSTANCE = instance
                    instance
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating database", e)
                    throw e
                }
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration from version 1 to 2 - Add userId column to notes table
                database.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT DEFAULT ''")
                
                // Set a default user ID for existing notes
                database.execSQL("UPDATE notes SET userId = 'unknown' WHERE userId IS NULL OR userId = ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Migration from version 2 to 3 - Create users table and update notes table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        email TEXT NOT NULL,
                        created_at INTEGER NOT NULL
                    )
                """.trimIndent())
                
                // Ensure userId column exists and is not null
                try {
                    // Check if userId column exists
                    val cursor = database.query("PRAGMA table_info(notes)")
                    var hasUserIdColumn = false
                    while (cursor.moveToNext()) {
                        if (cursor.getString(1) == "userId") {
                            hasUserIdColumn = true
                            break
                        }
                    }
                    cursor.close()
                    
                    if (!hasUserIdColumn) {
                        // Add userId column if it doesn't exist
                        database.execSQL("ALTER TABLE notes ADD COLUMN userId TEXT DEFAULT ''")
                    }
                    
                    // Ensure no null userIds
                    database.execSQL("UPDATE notes SET userId = 'unknown' WHERE userId IS NULL OR userId = ''")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during migration 2->3", e)
                }
            }
        }

        // Migration from version 3 to 4 - Add isTestNote column to notes table
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE notes ADD COLUMN isTestNote INTEGER NOT NULL DEFAULT 0")
                Log.d(TAG, "Migrated database from version 3 to 4: Added isTestNote column")
            }
        }
        
        // Migration from version 4 to 5 - Update user IDs for existing notes
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Get the current Firebase user ID if available
                val firebaseUser = FirebaseAuth.getInstance().currentUser
                val currentUserId = firebaseUser?.uid ?: "unknown"
                
                // Update all notes to have the current user's ID
                database.execSQL("""
                    UPDATE notes 
                    SET userId = ? 
                    WHERE userId = '' 
                       OR userId = 'unknown' 
                       OR userId IS NULL
                """.trimIndent(), arrayOf(currentUserId))
                
                Log.d(TAG, "Migration 4->5: Updated user IDs for existing notes to: $currentUserId")
            }
        }
    }
    
    /**
     * Updates all notes with the given user ID to a new user ID.
     * This is typically used after Firebase authentication to update legacy notes.
     *
     * @param userId The new user ID to set for matching notes
     */
    fun updateUserIdsForExistingNotes(userId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Starting user ID update for notes")
                val db = this@AppDatabase
                val count = db.noteDao().updateUserIds("unknown", userId)
                Log.d(TAG, "Updated $count notes with userId: $userId")
            } catch (e: Exception) {
                Log.e(TAG, "Error updating user IDs", e)
            }
        }
    }
}
