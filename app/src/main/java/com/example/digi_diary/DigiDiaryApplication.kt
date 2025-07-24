package com.example.digi_diary

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.repository.UserRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DigiDiaryApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory
    
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    // Using by lazy so the database and repository are only created when needed
    val database by lazy { 
        Log.d("DigiDiaryApp", "Initializing database...")
        AppDatabase.getDatabase(this) 
    }
    
    val userRepository by lazy { 
        Log.d("DigiDiaryApp", "Initializing user repository...")
        UserRepository(database) 
    }
    
    override fun onCreate() {
        super.onCreate()
        Log.d("DigiDiaryApp", "Application onCreate started")
        
        try {
            // Initialize Firebase
            Log.d("DigiDiaryApp", "Initializing Firebase...")
            
            // Check if Firebase is already initialized
            if (FirebaseApp.getApps(this).isEmpty()) {
                Log.d("DigiDiaryApp", "Firebase not initialized, initializing now...")
                FirebaseApp.initializeApp(this)
                Firebase.initialize(this)
                Log.d("DigiDiaryApp", "Firebase initialized successfully")
            } else {
                Log.d("DigiDiaryApp", "Firebase already initialized")
            }
            
            // Force initialize database to catch any early initialization errors
            database
            
        } catch (e: Exception) {
            Log.e("DigiDiaryApp", "Error during initialization", e)
            throw RuntimeException("Failed to initialize application", e)
        }
        
        Log.d("DigiDiaryApp", "Application onCreate completed")
    }
}
