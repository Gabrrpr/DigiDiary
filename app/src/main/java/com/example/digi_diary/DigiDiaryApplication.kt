package com.example.digi_diary

import android.app.Application
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.repository.UserRepository

class DigiDiaryApplication : Application() {
    // Using by lazy so the database and repository are only created when needed
    val database by lazy { AppDatabase.getDatabase(this) }
    val userRepository by lazy { UserRepository(database) }
}
