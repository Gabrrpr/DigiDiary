package com.example.digi_diary.data.repository

import com.example.digi_diary.data.Result
import com.example.digi_diary.data.model.Note
import kotlinx.coroutines.flow.Flow

/**
 * Interface defining the operations for the Note repository.
 * This will be implemented by NoteRepositoryImpl to handle both local (Room) and remote (Firestore) data sources.
 */
interface NoteRepository {
    // Get all notes for a user
    fun getNotes(userId: String): Flow<Result<List<Note>>>
    
    // Get a single note by ID
    suspend fun getNoteById(noteId: Long, userId: String): Result<Note?>
    
    // Save a note (create or update)
    suspend fun saveNote(note: Note): Result<Long>
    
    // Delete a note
    suspend fun deleteNote(noteId: Long, userId: String): Result<Unit>
    
    // Delete all notes for a user
    suspend fun deleteAllUserNotes(userId: String): Result<Unit>
    
    // Sync notes for a user (pull from remote)
    suspend fun syncNotes(userId: String): Result<Unit>
}
