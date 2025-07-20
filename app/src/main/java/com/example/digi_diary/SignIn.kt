package com.example.digi_diary

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.digi_diary.data.model.User
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SignIn : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    
    private val app by lazy { application as DigiDiaryApplication }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)
        
        // Set up window insets for edge-to-edge display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        passwordLayout = findViewById(R.id.passwordLayout)
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout)
        
        // Set up back button
        findViewById<ImageButton>(R.id.backButton).setOnClickListener {
            finish()
        }
        
        // Set up password toggles
        setupPasswordToggles()
        
        // Set up real-time password match validation
        setupPasswordMatchValidation()
        
        // Set up sign up button
        findViewById<MaterialButton>(R.id.SignInButton).setOnClickListener {
            if (validateInputs()) {
                registerUser()
            }
        }
    }
    
    private fun setupPasswordToggles() {
        // Password field toggle
        setupPasswordToggle(passwordLayout, passwordEditText)
        
        // Confirm password field toggle
        setupPasswordToggle(confirmPasswordLayout, confirmPasswordEditText)
    }
    
    private fun setupPasswordToggle(layout: TextInputLayout, editText: TextInputEditText) {
        layout.setEndIconOnClickListener {
            if (editText.transformationMethod == PasswordTransformationMethod.getInstance()) {
                layout.endIconDrawable = getDrawable(R.drawable.ic_visibility_off)
                editText.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                layout.endIconDrawable = getDrawable(R.drawable.ic_visibility)
                editText.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            // Move cursor to the end of the text
            editText.setSelection(editText.text?.length ?: 0)
        }
    }
    
    private fun setupPasswordMatchValidation() {
        val passwordTextWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePasswordMatch()
            }
        }
        
        passwordEditText.addTextChangedListener(passwordTextWatcher)
        confirmPasswordEditText.addTextChangedListener(passwordTextWatcher)
    }
    
    private fun validatePasswordMatch() {
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        
        if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords don't match"
        } else {
            confirmPasswordLayout.error = null
        }
    }
    
    private fun validateInputs(): Boolean {
        val email = emailEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        val confirmPassword = confirmPasswordEditText.text.toString()
        
        if (email.isEmpty()) {
            emailEditText.error = "Email is required"
            return false
        }
        
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.error = "Please enter a valid email"
            return false
        }
        
        if (username.isEmpty()) {
            usernameEditText.error = "Username is required"
            return false
        }
        
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            return false
        }
        
        if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            return false
        }
        
        if (confirmPassword.isEmpty()) {
            confirmPasswordLayout.error = "Please confirm your password"
            return false
        }
        
        if (password != confirmPassword) {
            confirmPasswordLayout.error = "Passwords don't match"
            return false
        }
        
        return true
    }
    
    private fun registerUser() {
        val email = emailEditText.text.toString().trim()
        val username = usernameEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        // Show loading state
        findViewById<MaterialButton>(R.id.SignInButton).isEnabled = false
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Check if email already exists
                val emailExists = withContext(Dispatchers.IO) {
                    app.userRepository.isEmailTaken(email)
                }
                
                if (emailExists) {
                    emailEditText.error = "Email already registered"
                    return@launch
                }
                
                // Check if username is already taken
                val usernameExists = withContext(Dispatchers.IO) {
                    app.userRepository.isUsernameTaken(username)
                }
                
                if (usernameExists) {
                    usernameEditText.error = "Username already taken"
                    return@launch
                }
                
                // Register the user
                val success = withContext(Dispatchers.IO) {
                    app.userRepository.registerUser(username, email, password)
                }
                
                if (success) {
                    Toast.makeText(
                        this@SignIn,
                        "Registration successful! Please login.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish() // Return to login screen
                } else {
                    Toast.makeText(
                        this@SignIn,
                        "Registration failed. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(
                    this@SignIn,
                    "An error occurred: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                // Re-enable the button
                findViewById<MaterialButton>(R.id.SignInButton).isEnabled = true
            }
        }
    }
}