package com.example.digi_diary

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Login : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    
    private val app by lazy { application as DigiDiaryApplication }
    
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
            
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordLayout = findViewById(R.id.passwordLayout)
        
        // Set up password toggle
        setupPasswordToggle()
        
        // Handle Sign In button click
        val signInButton = findViewById<MaterialButton>(R.id.SignInButton)
        signInButton.setOnClickListener {
            if (validateInputs()) {
                loginUser()
            }
        }
        
        // Handle Sign Up link click - using the clickable TextView
        val signUpLink = findViewById<TextView>(R.id.signUpLink)
        signUpLink.setOnClickListener {
            try {
                val intent = Intent(this, com.example.digi_diary.SignIn::class.java)
                startActivity(intent)
                // Add fade animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        // Also make the entire "Don't have an account?" text clickable
        val signUpText = findViewById<TextView>(R.id.signUpText)
        signUpText.setOnClickListener {
            signUpLink.performClick()
        }
    }
    
    private fun validateInputs(): Boolean {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            return false
        }
        
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            return false
        }
        
        return true
    }
    
    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        // Show loading state
        val signInButton = findViewById<MaterialButton>(R.id.SignInButton)
        signInButton.isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Attempt to authenticate user
                val user = withContext(Dispatchers.IO) {
                    app.userRepository.loginUser(email, password)
                }
                
                if (user != null) {
                    // Login successful, navigate to home page
                    val intent = Intent(this@Login, com.example.digi_diary.ui.home.HomePage::class.java).apply {
                        putExtra("USER_EMAIL", user.email)
                        putExtra("USER_NAME", user.username)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                } else {
                    // Login failed
                    Toast.makeText(
                        this@Login,
                        "Invalid email or password",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@Login,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Re-enable the button
                signInButton.isEnabled = true
            }
        }
    }
    
    private fun setupPasswordToggle() {
        passwordLayout.setEndIconOnClickListener {
            if (passwordEditText.transformationMethod == PasswordTransformationMethod.getInstance()) {
                // Show password
                passwordEditText.transformationMethod = HideReturnsTransformationMethod.getInstance()
                passwordLayout.endIconDrawable = getDrawable(R.drawable.ic_visibility)
            } else {
                // Hide password
                passwordEditText.transformationMethod = PasswordTransformationMethod.getInstance()
                passwordLayout.endIconDrawable = getDrawable(R.drawable.ic_visibility_off)
            }
            // Move cursor to the end of the text
            passwordEditText.setSelection(passwordEditText.text?.length ?: 0)
        }
    }
}