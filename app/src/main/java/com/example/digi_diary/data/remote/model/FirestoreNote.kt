package com.example.digi_diary.data.remote.model

import com.example.digi_diary.data.model.Note
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Data class representing a note as stored in Firestore.
 * This is separate from the domain model to maintain separation of concerns.
 */
data class FirestoreNote(
    @DocumentId
    val id: String = "",
    val title: String = "",
    val content: String = "",
    val date: Date = Date(),
    val userId: String = "",
    val isTestNote: Boolean = false,
    @ServerTimestamp
    val updatedAt: Date? = null,
    val isDeleted: Boolean = false
) {
    /**
     * Convert to domain model
     */
    fun toNote(): Note = Note(
        id = id.toLongOrNull() ?: 0L,
        title = title,
        content = content,
        date = date,
        userId = userId,
        isTestNote = isTestNote
    )

    companion object {
        /**
         * Convert from domain model to Firestore model
         */
        fun fromNote(note: Note): FirestoreNote = FirestoreNote(
            id = note.id.toString(),
            title = note.title,
            content = note.content,
            date = note.date,
            userId = note.userId,
            isTestNote = note.isTestNote,
            updatedAt = Date() // Set to current time
        )
    }
}
