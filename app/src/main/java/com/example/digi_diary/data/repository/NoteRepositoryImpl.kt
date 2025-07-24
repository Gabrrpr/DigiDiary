package com.example.digi_diary.data.repository

import android.util.Log
import com.example.digi_diary.data.Result
import com.example.digi_diary.data.local.NoteLocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlin.coroutines.CoroutineContext
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.data.remote.NoteRemoteDataSource
import com.example.digi_diary.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "NoteRepositoryImpl"

/**
 * Implementation of the NoteRepository interface that coordinates between local and remote data sources.
 * Handles data synchronization between Room and Firestore.
 */
@Singleton
class NoteRepositoryImpl @Inject constructor(
    private val localDataSource: NoteLocalDataSource,
    private val remoteDataSource: NoteRemoteDataSource,
    private val applicationScope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : NoteRepository, CoroutineScope by CoroutineScope(ioDispatcher) {

    override fun getNotes(userId: String): Flow<Result<List<Note>>> {
        // Return a flow that emits when local data changes
        return localDataSource.getNotes(userId)
            .map { notes ->
                Result.Success(notes.sortedByDescending { it.date })
            }
            .catch { e ->
                Log.e(TAG, "Error getting notes from local data source", e)
                // Return an empty list in case of error to keep the flow alive
                emit(Result.Success(emptyList()))
            }
            .flowOn(ioDispatcher)
    }

    override suspend fun getNoteById(noteId: Long, userId: String): Result<Note?> {
        return withContext(ioDispatcher) {
            try {
                val note = localDataSource.getNoteById(noteId, userId)
                Result.Success(note)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting note by ID: $noteId", e)
                Result.Error(e)
            }
        }
    }

    override suspend fun saveNote(note: Note): Result<Long> {
        Log.d(TAG, "Starting saveNote with note: ${note.title}")
        return withContext(ioDispatcher) {
            try {
                Log.d(TAG, "Saving note to local database")
                val localId = localDataSource.insertOrUpdateNote(note)
                Log.d(TAG, "Saved to local database with ID: $localId")
                
                // If this is a new note, update the ID from local database
                val noteToSave = if (note.id == 0L) {
                    Log.d(TAG, "New note detected, updating ID to: $localId")
                    note.copy(id = localId)
                } else {
                    Log.d(TAG, "Updating existing note with ID: ${note.id}")
                    note
                }
                
                // Save to remote in the background
                Log.d(TAG, "Starting background save to remote")
                launch {
                    try {
                        Log.d(TAG, "Attempting to save note to remote: ${noteToSave.id}")
                        when (val result = remoteDataSource.saveNote(noteToSave)) {
                            is Result.Success -> {
                                val savedNote = result.data
                                Log.d(TAG, "Successfully saved to remote, updating local with server data")
                                // Update local database with any server-generated fields
                                localDataSource.insertOrUpdateNote(savedNote)
                                Log.d(TAG, "Local database updated with remote changes")
                            }
                            is Result.Error -> {
                                Log.e(TAG, "Failed to save note to remote: ${result.exception.message}", result.exception)
                                // TODO: Queue for retry
                            }
                            else -> {
                                Log.w(TAG, "Unexpected result type when saving to remote")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in background save to remote", e)
                        // TODO: Queue for retry
                    }
                }
                
                // Return the local ID immediately
                Result.Success(localId)
            } catch (e: Exception) {
                Log.e(TAG, "Error saving note locally", e)
                Result.Error(e)
            }
        }
    }

    override suspend fun deleteNote(noteId: Long, userId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                // Delete from local database first for immediate UI update
                localDataSource.deleteNote(noteId, userId)
                
                // Delete from remote in the background
                launch {
                    try {
                        when (val result = remoteDataSource.deleteNote(noteId, userId)) {
                            is Result.Error -> {
                                Log.e(TAG, "Failed to delete note from remote: ${result.exception.message}")
                                // TODO: Queue for retry
                            }
                            else -> {}
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in background delete from remote", e)
                        // TODO: Queue for retry
                    }
                }
                
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting note locally", e)
                Result.Error(e)
            }
        }
    }

    override suspend fun deleteAllUserNotes(userId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                // Delete all user notes from local database
                localDataSource.deleteAllUserNotes(userId)
                
                // Note: We don't delete from remote here to allow for sync recovery
                Result.Success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting all user notes", e)
                Result.Error(e)
            }
        }
    }

    override suspend fun syncNotes(userId: String): Result<Unit> {
        return withContext(ioDispatcher) {
            try {
                // Get the last sync timestamp
                val lastSyncTimestamp = localDataSource.getLastSyncTimestamp(userId)
                
                // Get updated notes from remote
                when (val result = remoteDataSource.getNotes(userId, lastSyncTimestamp)) {
                    is Result.Success -> {
                        val remoteNotes = result.data
                        
                        // Save to local database
                        if (remoteNotes.isNotEmpty()) {
                            localDataSource.insertNotes(remoteNotes)
                        }
                        
                        // Update last sync timestamp
                        val serverTimestamp = remoteDataSource.getServerTimestamp()
                        localDataSource.updateLastSyncTimestamp(userId, serverTimestamp)
                        
                        Result.Success(Unit)
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error syncing notes from remote: ${result.exception.message}")
                        Result.Error(result.exception)
                    }
                    else -> Result.Success(Unit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during sync", e)
                Result.Error(e)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: NoteRepositoryImpl? = null

        fun getInstance(
            localDataSource: NoteLocalDataSource,
            remoteDataSource: NoteRemoteDataSource,
            applicationScope: CoroutineScope,
            ioDispatcher: CoroutineDispatcher
        ): NoteRepositoryImpl {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NoteRepositoryImpl(
                    localDataSource,
                    remoteDataSource,
                    applicationScope,
                    ioDispatcher
                ).also { INSTANCE = it }
            }
        }
    }
}
