package com.example.digi_diary.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.digi_diary.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY date DESC")
    fun getUserNotes(userId: String): Flow<List<Note>>
    
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY date DESC")
    suspend fun getUserNotesSync(userId: String): List<Note>

    @Query("DELETE FROM notes WHERE isTestNote = 1")
    suspend fun deleteTestNotes()

    @Query("SELECT * FROM notes WHERE id = :noteId AND userId = :userId LIMIT 1")
    suspend fun getUserNoteById(noteId: Long, userId: String): Note?
    
    // Debug method to get all notes regardless of user
    @Query("SELECT * FROM notes")
    suspend fun getAllNotesForDebug(): List<Note>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Delete
    suspend fun delete(note: Note)

    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllUserNotes(userId: String)
    
    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()
    
    // Debug methods
    @Query("SELECT COUNT(*) FROM notes")
    suspend fun getNoteCount(): Int
    
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId")
    suspend fun getUserNotesCount(userId: String): Int
    
    @Query("UPDATE notes SET userId = :newUserId WHERE userId = :oldUserId")
    suspend fun updateUserIds(oldUserId: String, newUserId: String): Int
}
