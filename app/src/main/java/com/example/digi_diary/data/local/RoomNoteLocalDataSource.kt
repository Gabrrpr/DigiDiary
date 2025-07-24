package com.example.digi_diary.data.local

import android.content.Context
import android.util.Log
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Room implementation of the NoteLocalDataSource
 */
class RoomNoteLocalDataSource @Inject constructor(
    private val database: AppDatabase,
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher
) : NoteLocalDataSource {
    
    private val noteDao = database.noteDao()
    private val sharedPreferences by lazy {
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    }
    
    override fun getNotes(userId: String): Flow<List<Note>> {
        return noteDao.getUserNotes(userId)
            .flowOn(Dispatchers.IO)
    }
    
    override suspend fun getNoteById(noteId: Long, userId: String): Note? {
        return withContext(Dispatchers.IO) {
            try {
                noteDao.getUserNoteById(noteId, userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting note by ID: $noteId", e)
                null
            }
        }
    }
    
    override suspend fun insertOrUpdateNote(note: Note): Long {
        return withContext(Dispatchers.IO) {
            try {
                if (note.id == 0L) {
                    // Insert new note
                    noteDao.insert(note)
                } else {
                    // Update existing note
                    noteDao.update(note)
                    note.id // Return existing ID
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving note: ${note.id}", e)
                -1L
            }
        }
    }
    
    override suspend fun deleteNote(noteId: Long, userId: String) {
        withContext(Dispatchers.IO) {
            try {
                val note = noteDao.getUserNoteById(noteId, userId)
                if (note != null) {
                    noteDao.delete(note)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note: $noteId", e)
                throw e
            }
        }
    }
    
    override suspend fun deleteAllUserNotes(userId: String) {
        withContext(Dispatchers.IO) {
            try {
                noteDao.deleteAllUserNotes(userId)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all notes for user: $userId", e)
                throw e
            }
        }
    }
    
    override suspend fun insertNotes(notes: List<Note>) {
        withContext(Dispatchers.IO) {
            try {
                // Insert notes one by one since we don't have an insertAll method
                notes.forEach { note ->
                    noteDao.insert(note)
                }
                Log.d(TAG, "Successfully inserted ${notes.size} notes")
            } catch (e: Exception) {
                Log.e(TAG, "Error inserting notes batch", e)
                throw e
            }
        }
    }
    
    override suspend fun getLastSyncTimestamp(userId: String): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val key = "last_sync_$userId"
                if (sharedPreferences.contains(key)) {
                    sharedPreferences.getLong(key, 0).takeIf { it > 0 }
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting last sync timestamp", e)
                null
            }
        }
    }
    
    override suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                sharedPreferences.edit()
                    .putLong("last_sync_$userId", timestamp)
                    .apply()
            } catch (e: Exception) {
                Log.e(TAG, "Error updating last sync timestamp", e)
            }
        }
    }
    
    companion object {
        private const val TAG = "RoomNoteLocalDS"
        
        @Volatile
        private var INSTANCE: RoomNoteLocalDataSource? = null
        
        @Deprecated("Use Hilt for dependency injection instead")
        fun getInstance(context: Context, ioDispatcher: CoroutineDispatcher = Dispatchers.IO): RoomNoteLocalDataSource {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.getDatabase(context)
                RoomNoteLocalDataSource(database, context, ioDispatcher).also { INSTANCE = it }
            }
        }
    }
}
