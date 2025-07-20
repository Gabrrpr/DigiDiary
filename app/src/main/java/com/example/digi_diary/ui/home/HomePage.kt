package com.example.digi_diary.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.digi_diary.Create
import com.example.digi_diary.Login
import com.example.digi_diary.R
import com.example.digi_diary.data.repository.UserRepository
import com.example.digi_diary.data.local.AppDatabase
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomePage : AppCompatActivity() {
    
    private var selectedMood: CardView? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        
        // Get user data from intent
        val username = intent.getStringExtra("USER_NAME") ?: "User"
        
        // Update UI with user data
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        val fullText = "Hello, $username!"
        val spannable = android.text.SpannableString(fullText)
        // Set "Hello, " in black
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
            0, 7, // "Hello, " is 7 characters
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Set up bottom navigation
        setupBottomNavigation()
        
        // Set up debug button
        setupDebugButton()
        // Set username in green
        spannable.setSpan(
            android.text.style.ForegroundColorSpan(android.graphics.Color.parseColor("#6B7158")),
            7, fullText.length,
            android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        welcomeText.text = spannable
        
        // Set current date
        val dateText = findViewById<TextView>(R.id.dateText)
        val dateFormat = SimpleDateFormat("EEEE, dd MMM", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        dateText.text = currentDate
        
        // Set up FAB click listener
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            // Navigate to Create activity
            val intent = Intent(this, Create::class.java)
            startActivity(intent)
            // Add fade animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
    
    override fun onBackPressed() {
        // Prevent going back to login screen using back button
        // Instead, minimize the app
        moveTaskToBack(true)
    }
    
    private fun setupDebugButton() {
        val debugButton = findViewById<MaterialButton>(R.id.debugButton)
        debugButton.setOnClickListener {
            // Get database instance
            val db = AppDatabase.getDatabase(applicationContext)
            val userRepository = UserRepository(db.userDao())
            
            // Launch coroutine to fetch users
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val users = userRepository.getAllUsersForDebug()
                    Log.d("UserRecords", "=== USER RECORDS (${users.size} total) ===")
                    users.forEach { user ->
                        Log.d("UserRecords", "ID: ${user.id}")
                        Log.d("UserRecords", "  Username: ${user.username}")
                        Log.d("UserRecords", "  Email: ${user.email}")
                        Log.d("UserRecords", "  Created: ${Date(user.createdAt)}")
                        Log.d("UserRecords", "  ----------------------------")
                    }
                    
                    runOnUiThread {
                        Toast.makeText(
                            this@HomePage,
                            "Check Logcat with tag 'UserRecords' for user data",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } catch (e: Exception) {
                    Log.e("UserRecords", "Error fetching users", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@HomePage,
                            "Error fetching users: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }
    
    private fun setupBottomNavigation() {
        // Your existing bottom navigation setup code
    }
}
