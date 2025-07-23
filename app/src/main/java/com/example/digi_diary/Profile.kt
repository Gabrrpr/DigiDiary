package com.example.digi_diary

import android.content.Intent
import android.os.Bundle
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class Profile : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Log received intent extras
        val intentExtras = intent?.extras?.keySet()?.joinToString() ?: "null"
        Log.d("Profile", "Received intent extras: $intentExtras")

        // Get user data from intent with fallback to shared preferences if needed
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        
        // Log all shared preferences for debugging
        val allPrefs = sharedPrefs.all
        Log.d("Profile", "All shared preferences: $allPrefs")
        
        // Get user data from intent first, then fall back to shared preferences
        val userName = intent?.getStringExtra("USER_NAME")
            ?: intent?.getStringExtra("user_name")
            ?: sharedPrefs.getString("user_name", null)
            ?: "User"
            
        val userEmail = intent?.getStringExtra("USER_EMAIL")
            ?: intent?.getStringExtra("user_email")
            ?: sharedPrefs.getString("user_email", null)
            ?: ""
            
        val userId = intent?.getStringExtra("user_id")
            ?: sharedPrefs.getString("user_id", null)
            ?: ""
            
        Log.d("Profile", "Displaying profile for - Name: '$userName', Email: '$userEmail', ID: '$userId'")
        
        // Save to shared preferences for future use
        with(sharedPrefs.edit()) {
            putString("user_name", userName)
            putString("user_email", userEmail)
            putString("user_id", userId)
            apply()
        }
        
        try {
            // Update UI with user data
            val userNameView = findViewById<TextView>(R.id.tvUserName)
            userNameView.text = userName
            // Add some styling to the username
            userNameView.setTextColor(getColor(R.color.username_color))
            userNameView.textSize = 24f
            userNameView.setTypeface(userNameView.typeface, Typeface.BOLD)
            
            val userEmailView = findViewById<TextView>(R.id.tvUserEmail)
            userEmailView.text = userEmail
            // Style the email differently
            userEmailView.setTextColor(getColor(android.R.color.darker_gray))
            userEmailView.textSize = 16f
            userEmailView.setCompoundDrawablesRelativeWithIntrinsicBounds(
                R.drawable.ic_email, 0, 0, 0
            )
            userEmailView.compoundDrawablePadding = 16
            
            Log.d("Profile", "Successfully updated UI with user data")
        } catch (e: Exception) {
            Log.e("Profile", "Error updating UI with user data", e)
            Toast.makeText(this, "Error loading profile data", Toast.LENGTH_SHORT).show()
        }

        // Set up back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        // Set up navigation
        setupBottomNavigation()

        // Set up profile options click listeners
        setupProfileOptions()
    }

    private fun setupBottomNavigation() {
        try {
            // Home button
            val navHome = findViewById<View>(R.id.nav_home)
            navHome?.setOnClickListener {
                // Just finish the current activity to go back to HomePage
                finish()
                // Add fade animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }

            // Profile button - already on profile, so just highlight it
            val navProfile = findViewById<View>(R.id.nav_profile)
            navProfile?.isSelected = true
            navProfile?.setOnClickListener {
                // Already on profile, do nothing
            }

            // FAB (Add new entry)
            val fab = findViewById<View>(R.id.fab)
            fab?.setOnClickListener {
                startActivity(Intent(this@Profile, Create::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Handle the error appropriately
        }
    }

    private fun setupProfileOptions() {
        // Change Password
        findViewById<View>(R.id.btnChangePassword).setOnClickListener {
            // TODO: Implement change password functionality
            showChangePasswordDialog()
        }

        // View Developers
        findViewById<View>(R.id.btnViewDevelopers).setOnClickListener {
            val intent = Intent(this, Developer::class.java)
            startActivity(intent)
        }

        // Logout
        findViewById<View>(R.id.btnLogout).setOnClickListener {
            showLogoutConfirmation()
        }
    }

    private fun showChangePasswordDialog() {
        // Navigate to ForgotPass activity for password change
        val intent = Intent(this, ForgotPass::class.java).apply {
            // Pass the current user's email for verification
            putExtra("email", findViewById<TextView>(R.id.tvUserEmail).text.toString())
            // Set a flag to indicate this is a password change flow
            putExtra("is_password_change", true)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                // Perform logout
                val intent = Intent(this, Login::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        // Update the selected state when returning to this activity
        findViewById<View>(R.id.nav_home).isSelected = false
        findViewById<View>(R.id.nav_profile).isSelected = true
    }
}