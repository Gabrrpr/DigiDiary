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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import android.util.Log

class Login : AppCompatActivity() {
    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailLayout: TextInputLayout
    private lateinit var passwordLayout: TextInputLayout
    private val app by lazy { application as DigiDiaryApplication }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        
        // Ensure the window has focus
        window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        
        // Check for password changed flag
        if (intent?.getBooleanExtra("PASSWORD_CHANGED", false) == true) {
            // Clear any cached credentials
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
            
            // Show success message - this will be overridden by the show_message extra if present
            Toast.makeText(
                this,
                "Your password has been updated successfully. Please sign in again with your new password.",
                Toast.LENGTH_LONG
            ).show()
        }
        
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
            
        // Check for success message from password change
        intent.getStringExtra("show_message")?.let { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
        
        // Initialize views
        emailEditText = findViewById(R.id.emailEditText)
        emailLayout = findViewById(R.id.emailLayout)
        passwordEditText = findViewById(R.id.passwordEditText)
        passwordLayout = findViewById(R.id.passwordLayout)
        
        // Ensure EditText fields are focusable and clickable
        emailEditText.isFocusable = true
        emailEditText.isFocusableInTouchMode = true
        emailEditText.isClickable = true
        
        passwordEditText.isFocusable = true
        passwordEditText.isFocusableInTouchMode = true
        passwordEditText.isClickable = true
        
        // Request focus on email field
        emailEditText.requestFocus()
        
        // Show keyboard
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
        imm.showSoftInput(emailEditText, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        
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
                // Handle any errors
            }
        }
        
        // Also make the entire "Don't have an account?" text clickable
        val signUpText = findViewById<TextView>(R.id.signUpText)
        signUpText.setOnClickListener {
            signUpLink.performClick()
        }
        
        // Handle Forgot Password click
        val forgotPasswordText = findViewById<TextView>(R.id.forgotPasswordText)
        forgotPasswordText.setOnClickListener {
            try {
                val intent = Intent(this, ForgotPass::class.java).apply {
                    // Explicitly set is_password_change to false for forgot password flow
                    putExtra("is_password_change", false)
                }
                startActivity(intent)
                // Add fade animation
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            } catch (e: Exception) {
                Log.e("Login", "Error navigating to ForgotPass", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun validateInputs(): Boolean {
        val identifier = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        var isValid = true
        
        // Reset error states
        emailLayout.error = null
        passwordLayout.error = null
        
        // Validate identifier (username or email)
        if (identifier.isEmpty()) {
            emailLayout.error = "Username or email is required"
            isValid = false
        }
        
        // Validate password
        if (password.isEmpty()) {
            passwordLayout.error = "Password is required"
            isValid = false
        } else if (password.length < 6) {
            passwordLayout.error = "Password must be at least 6 characters"
            isValid = false
        }
        
        return isValid
    }
    
    private fun loginUser() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()
        
        Log.d("Login", "Attempting to login with email: $email")
        
        // Show loading state
        val signInButton = findViewById<MaterialButton>(R.id.SignInButton)
        signInButton.isEnabled = false
        signInButton.text = getString(R.string.signing_in)
        
        // Sign in with Firebase Auth
        Firebase.auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                try {
                    if (task.isSuccessful) {
                        Log.d("Login", "Firebase auth successful")
                        // Sign in success, update UI with the signed-in user's information
                        val user = Firebase.auth.currentUser
                        if (user != null) {
                            Log.d("Login", "User object is not null, UID: ${user.uid}")
                            // Login successful, navigate to home page
                            val username = user.email?.substringBefore("@") ?: "User"
                            Log.d("Login", "Preparing to navigate to HomePage")
                            try {
                                Log.d("Login", "Creating intent for HomePage with UID: ${user.uid}")
                                val intent = Intent(this@Login, com.example.digi_diary.HomePage::class.java).apply {
                                    user.email?.let { email ->
                                        putExtra("USER_EMAIL", email)
                                    }
                                    putExtra("USER_NAME", username)
                                    putExtra("user_id", user.uid)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                                }
                                Log.d("Login", "Intent extras: ${intent.extras}")
                                Log.d("Login", "Target activity: ${intent.component?.className}")
                                Log.d("Login", "Starting HomePage activity")
                                startActivity(intent)
                                finish()
                            } catch (e: Exception) {
                                Log.e("Login", "Error starting HomePage activity", e)
                                runOnUiThread {
                                    Toast.makeText(
                                        this@Login,
                                        "Error navigating to home screen: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    signInButton.isEnabled = true
                                    signInButton.text = getString(R.string.sign_in)
                                }
                            }
                        } else {
                            Log.e("Login", "User object is null after successful login")
                            runOnUiThread {
                                Toast.makeText(
                                    this@Login,
                                    "User authentication failed. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                signInButton.isEnabled = true
                                signInButton.text = getString(R.string.sign_in)
                            }
                        }
                    } else {
                        // If sign in fails, display a message to the user.
                        val error = task.exception?.message ?: "Authentication failed"
                        Log.e("Login", "signInWithEmail:failure", task.exception)
                        
                        val errorMessage = when {
                            error.contains("wrong-password", ignoreCase = true) -> 
                                "❌ Incorrect password. Please try again."
                            error.contains("user-not-found", ignoreCase = true) -> 
                                "❌ No account found with this email. Please check your email or sign up."
                            error.contains("invalid-email", ignoreCase = true) ->
                                "❌ Invalid email format. Please enter a valid email address."
                            error.contains("user-disabled", ignoreCase = true) ->
                                "❌ This account has been disabled. Please contact support."
                            error.contains("network", ignoreCase = true) ->
                                "🌐 Network error. Please check your internet connection and try again."
                            else -> "❌ Authentication failed: $error"
                        }
                        
                        runOnUiThread {
                            passwordLayout.error = " "
                            Toast.makeText(
                                this@Login,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                            
                            // Re-enable the button
                            signInButton.isEnabled = true
                            signInButton.text = getString(R.string.sign_in)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Login", "Error in login process", e)
                    runOnUiThread {
                        Toast.makeText(
                            this@Login,
                            "An unexpected error occurred: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        signInButton.isEnabled = true
                        signInButton.text = getString(R.string.sign_in)
                    }
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