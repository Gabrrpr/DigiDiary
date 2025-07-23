package com.example.digi_diary

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.digi_diary.databinding.ActivityForgotPassBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

private const val TAG = "ForgotPass"

class ForgotPass : AppCompatActivity() {
    private lateinit var binding: ActivityForgotPassBinding
    private lateinit var auth: FirebaseAuth
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
    
    private fun showSuccessDialog(title: String, message: String) {
        android.app.AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = Firebase.auth
        
        // Set up back button
        binding.backButton.setOnClickListener {
            onBackPressed()
        }

        // Clear any errors when text changes
        binding.emailEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                binding.emailLayout.error = null
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Handle reset password button click
        binding.updatePasswordButton.setOnClickListener {
            resetPassword()
        }
    }



    private fun validateInputs(): Boolean {
        val email = binding.emailEditText.text.toString().trim()

        if (email.isEmpty()) {
            binding.emailLayout.error = "Email is required"
            return false
        }
        
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.error = "Please enter a valid email"
            return false
        }

        return true
    }
    
    private fun redirectToLogin(message: String? = null) {
        val intent = Intent(this, Login::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            message?.let { putExtra("show_message", it) }
        }
        startActivity(intent)
        finish()
    }
    
    private fun handleError(error: String) {
        binding.updatePasswordButton.isEnabled = true
        binding.updatePasswordButton.text = getString(R.string.reset_password)
        
        val errorMsg = when {
            error.contains("user-not-found", ignoreCase = true) -> 
                "❌ No account found with this email."
            error.contains("user-disabled", ignoreCase = true) -> 
                "❌ This account has been disabled."
            error.contains("too-many-requests", ignoreCase = true) -> 
                "⚠️ Too many attempts. Please try again later."
            error.contains("network", ignoreCase = true) ->
                "🌐 Network error. Please check your connection and try again."
            else -> "❌ Error: ${error.take(100)}${if (error.length > 100) "..." else ""}"
        }
        
        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        Log.e("ForgotPass", "Error: $error")
    }
    
    private fun resetPassword() {
        if (!validateInputs()) return
        
        val email = binding.emailEditText.text.toString().trim()
        
        // Show loading state
        binding.updatePasswordButton.isEnabled = false
        binding.updatePasswordButton.text = "Sending reset link..."
        
        // Send a password reset email
        Firebase.auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                binding.updatePasswordButton.isEnabled = true
                binding.updatePasswordButton.text = getString(R.string.reset_password)
                
                if (task.isSuccessful) {
                    Log.d(TAG, "Password reset email sent")
                    showSuccessDialog(
                        "Reset Email Sent",
                        "We've sent a password reset link to $email. Please check your email and follow the instructions to set a new password."
                    )
                } else {
                    val error = task.exception?.message ?: "Failed to send password reset email"
                    handleError(error)
                }
            }
    }
}