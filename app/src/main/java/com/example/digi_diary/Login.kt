package com.example.digi_diary

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Login : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Set up window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, 0)  // Remove any padding that might be set
            insets
        }
        
        // Make the status bar light
        window.statusBarColor = getColor(android.R.color.white)
        window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or 
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        
        // Set up click listener for sign in button
        val signInButton = findViewById<MaterialButton>(R.id.SignInButton)
        signInButton.setOnClickListener {
            // Handle sign in logic here
            // For now, just show a toast
            Toast.makeText(this, "Sign in clicked", Toast.LENGTH_SHORT).show()
        }
        
        // Set up click listener for sign up text
        val signUpLink = findViewById<TextView>(R.id.signUpLink)
        signUpLink.setOnClickListener {
            // Navigate to SignIn activity
            val intent = Intent(this, com.example.digi_diary.SignIn::class.java)
            startActivity(intent)
            // Optional: Add animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}