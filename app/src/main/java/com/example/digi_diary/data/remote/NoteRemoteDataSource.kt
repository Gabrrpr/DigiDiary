package com.example.digi_diary.data.remote

import com.example.digi_diary.data.Result
import com.example.digi_diary.data.model.Note

/**
 * Remote data source interface for Firestore operations
 */
interface NoteRemoteDataSource {
    /**
     * Get all notes for a user from the remote data source
     * @param userId The ID of the user whose notes to fetch
     * @param lastSyncTimestamp Timestamp of the last sync (optional)
     * @return Result containing the list of notes or an error
     */
    suspend fun getNotes(userId: String, lastSyncTimestamp: Long? = null): Result<List<Note>>
    
    /**
     * Save a note to the remote data source
     * @param note The note to save
     * @return Result containing the saved note or an error
     */
    suspend fun saveNote(note: Note): Result<Note>
    
    /**
     * Delete a note from the remote data source
     * @param noteId The ID of the note to delete
     * @param userId The ID of the user who owns the note
     * @return Result indicating success or failure
     */
    suspend fun deleteNote(noteId: Long, userId: String): Result<Unit>
    
    /**
     * Get the server timestamp from Firestore
     * @return Current server timestamp in milliseconds
     */
    suspend fun getServerTimestamp(): Long
}
