package com.example.digi_diary.data.remote

import android.util.Log
import com.example.digi_diary.data.Result
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.data.remote.model.FirestoreNote
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "FirestoreNoteDS"
private const val NOTES_COLLECTION = "notes"

/**
 * Firestore implementation of the NoteRemoteDataSource interface.
 * Handles all remote data operations for notes.
 */
@Singleton
class FirestoreNoteRemoteDataSource @Inject constructor() : NoteRemoteDataSource {

    private val firestore: FirebaseFirestore = Firebase.firestore
    
    override suspend fun getNotes(userId: String, lastSyncTimestamp: Long?): Result<List<Note>> {
        return try {
            // Start with a base query for the user's notes
            var query = firestore.collection(NOTES_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isDeleted", false)
            
            // If we have a last sync timestamp, only get updated notes
            lastSyncTimestamp?.let { timestamp ->
                val syncDate = Date(timestamp)
                query = query.whereGreaterThanOrEqualTo("updatedAt", syncDate)
            }
            
            // Execute the query
            val snapshot = query.get().await()
            
            // Convert Firestore documents to domain models
            val notes = snapshot.documents.mapNotNull { document ->
                try {
                    val firestoreNote = document.toObject(FirestoreNote::class.java)
                    firestoreNote?.toNote()
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing note ${document.id}", e)
                    null
                }
            }
            
            Result.Success(notes)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notes from Firestore", e)
            Result.Error(e)
        }
    }
    
    override suspend fun saveNote(note: Note): Result<Note> {
        return try {
            val firestoreNote = FirestoreNote.fromNote(note)
            val noteRef = if (note.id != 0L) {
                // Update existing note
                firestore.collection(NOTES_COLLECTION)
                    .document(note.id.toString())
            } else {
                // Create new note with auto-generated ID
                firestore.collection(NOTES_COLLECTION).document()
            }
            
            // Convert to map to include server timestamp
            val noteData = hashMapOf(
                "title" to firestoreNote.title,
                "content" to firestoreNote.content,
                "date" to firestoreNote.date,
                "userId" to firestoreNote.userId,
                "isTestNote" to firestoreNote.isTestNote,
                "updatedAt" to FieldValue.serverTimestamp(),
                "isDeleted" to false
            )
            
            // Set the document data
            noteRef.set(noteData).await()
            
            // Get the saved note with updated fields (like server timestamp)
            val savedDoc = noteRef.get().await()
            val savedNote = savedDoc.toObject(FirestoreNote::class.java)
                ?: throw Exception("Failed to parse saved note")
            
            // Convert back to domain model
            val resultNote = savedNote.toNote().copy(id = savedDoc.id.toLongOrNull() ?: 0L)
            Result.Success(resultNote)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving note to Firestore", e)
            Result.Error(e)
        }
    }
    
    override suspend fun deleteNote(noteId: Long, userId: String): Result<Unit> {
        return try {
            // Instead of deleting, we'll mark as deleted with a server timestamp
            firestore.collection(NOTES_COLLECTION)
                .document(noteId.toString())
                .update(
                    mapOf(
                        "isDeleted" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
                
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting note from Firestore", e)
            Result.Error(e)
        }
    }
    
    override suspend fun getServerTimestamp(): Long {
        return try {
            // Get server timestamp by writing and immediately reading a document
            val docRef = firestore.collection("server_timestamps").document()
            val timestamp = hashMapOf(
                "timestamp" to FieldValue.serverTimestamp()
            )
            
            // Write the document to get server timestamp
            docRef.set(timestamp).await()
            
            // Read it back to get the actual timestamp
            val doc = docRef.get().await()
            val serverTimestamp = doc.getTimestamp("timestamp")?.toDate()?.time
                ?: System.currentTimeMillis()
                
            // Clean up the temporary document
            docRef.delete().await()
            
            serverTimestamp
        } catch (e: Exception) {
            Log.e(TAG, "Error getting server timestamp", e)
            System.currentTimeMillis() // Fallback to local time
        }
    }
    
    companion object {
        // Add this import at the top of the file if needed
        // import com.google.firebase.firestore.FieldValue
        
        // This is a workaround for the missing FieldValue import in the code above
        // The actual FieldValue class is already imported by the Firestore SDK
        private object FieldValue {
            fun serverTimestamp() = com.google.firebase.firestore.FieldValue.serverTimestamp()
        }
    }
}
