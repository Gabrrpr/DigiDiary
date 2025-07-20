package com.example.digi_diary.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.digi_diary.Login
import com.example.digi_diary.R
import com.google.android.material.button.MaterialButton

class HomePage : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        
        // Get user data from intent
        val username = intent.getStringExtra("USER_NAME") ?: "User"
        val email = intent.getStringExtra("USER_EMAIL") ?: ""
        
        // Update UI with user data
        val welcomeText = findViewById<TextView>(R.id.welcomeText)
        welcomeText.text = getString(R.string.welcome_user, username)
        
        val emailText = findViewById<TextView>(R.id.emailText)
        emailText.text = email
        
        // Set up logout button
        findViewById<MaterialButton>(R.id.logoutButton).setOnClickListener {
            // Clear any user session data if needed
            
            // Navigate back to login screen
            val intent = Intent(this, Login::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
            
            // Show logout message
            Toast.makeText(this, "Successfully logged out", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onBackPressed() {
        // Prevent going back to login screen using back button
        // Instead, minimize the app
        moveTaskToBack(true)
    }
}
