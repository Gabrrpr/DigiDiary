package com.example.digi_diary.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val title: String,
    val content: String,
    val date: Date,
    val userId: String,
    val isTestNote: Boolean = false
)
