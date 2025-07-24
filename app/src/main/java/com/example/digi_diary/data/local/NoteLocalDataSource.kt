package com.example.digi_diary.data.local

import com.example.digi_diary.data.Result
import com.example.digi_diary.data.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Local data source interface for Room database operations
 */
interface NoteLocalDataSource {
    // Get all notes for a user
    fun getNotes(userId: String): Flow<List<Note>>
    
    // Get a single note by ID
    suspend fun getNoteById(noteId: Long, userId: String): Note?
    
    // Insert or update a note
    suspend fun insertOrUpdateNote(note: Note): Long
    
    // Delete a note
    suspend fun deleteNote(noteId: Long, userId: String)
    
    // Delete all notes for a user
    suspend fun deleteAllUserNotes(userId: String)
    
    // Insert multiple notes
    suspend fun insertNotes(notes: List<Note>)
    
    // Get the last sync timestamp for a user
    suspend fun getLastSyncTimestamp(userId: String): Long?
    
    // Update the last sync timestamp for a user
    suspend fun updateLastSyncTimestamp(userId: String, timestamp: Long)
}
