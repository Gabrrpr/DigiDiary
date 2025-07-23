package com.example.digi_diary

import android.content.Intent
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log

class SignIn : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var usernameEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var confirmPasswordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private lateinit var confirmPasswordLayout: TextInputLayout
    private val app by lazy { application as com.example.digi_diary.DigiDiaryApplication }
    
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
        emailLayout = findViewById(R.id.emailLayout)
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
    
    private fun navigateToHome(user: FirebaseUser?) {
        if (user != null) {
            val intent = Intent(this, HomePage::class.java).apply {
                user.email?.let { putExtra("USER_EMAIL", it) }
                putExtra("USER_NAME", user.displayName ?: user.email?.substringBefore("@") ?: "User")
                putExtra("user_id", user.uid)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        } else {
            // If user is null, go back to login
            finish()
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
        val signUpButton = findViewById<MaterialButton>(R.id.SignInButton)
        signUpButton.isEnabled = false
        signUpButton.text = getString(R.string.creating_account)
        
        // Create user with Firebase Auth
        Firebase.auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                try {
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("SignIn", "createUserWithEmail:success")
                        val user = Firebase.auth.currentUser
                        
                        // Update user profile with display name (username)
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(username)
                            .build()
                            
                        user?.updateProfile(profileUpdates)?.addOnCompleteListener { profileTask ->
                            if (profileTask.isSuccessful) {
                                Log.d("SignIn", "User profile updated with username")
                            } else {
                                Log.w("SignIn", "Failed to update user profile", profileTask.exception)
                            }
                            navigateToHome(user)
                        }
                    } else {
                        // If sign up fails, display a message to the user.
                        val error = task.exception?.message ?: "Registration failed"
                        Log.e("SignIn", "createUserWithEmail:failure", task.exception)
                        
                        val errorMessage = when {
                            error.contains("email-already-in-use", ignoreCase = true) -> 
                                "❌ An account already exists with this email. Please log in instead."
                            error.contains("invalid-email", ignoreCase = true) ->
                                "❌ Invalid email format. Please enter a valid email address."
                            error.contains("weak-password", ignoreCase = true) ->
                                "❌ Password is too weak. Please choose a stronger password."
                            error.contains("network", ignoreCase = true) ->
                                "🌐 Network error. Please check your internet connection and try again."
                            else -> "❌ Registration failed: ${error.take(100)}${if (error.length > 100) "..." else ""}"
                        }
                        
                        emailLayout.error = " " // Set error to show the error icon
                        Toast.makeText(
                            this@SignIn,
                            errorMessage,
                            Toast.LENGTH_LONG
                        ).show()
                        
                        signUpButton.isEnabled = true
                        signUpButton.text = getString(R.string.sign_up)
                    }
                } catch (e: Exception) {
                    Log.e("SignIn", "Error during registration", e)
                    Toast.makeText(
                        this@SignIn,
                        "❌ An error occurred: ${e.message ?: "Unknown error"}",
                        Toast.LENGTH_LONG
                    ).show()
                    signUpButton.isEnabled = true
                    signUpButton.text = getString(R.string.sign_up)
                }
            }
    }
}