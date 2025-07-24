package com.example.digi_diary

import android.app.Activity
import android.view.WindowManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.Toast
import com.google.android.material.snackbar.Snackbar
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.digi_diary.data.Result
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.data.repository.NoteRepository
import com.example.digi_diary.databinding.ActivityCreateBinding
import com.example.digi_diary.di.IoDispatcher
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class Create : AppCompatActivity() {
    /**
     * Navigate back to home screen
     */
    private fun navigateToHome() {
        try {
            val homeIntent = Intent(this, HomePage::class.java).apply {
                // Clear the back stack
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TASK)

                // Pass user data
                putExtra("USER_ID", userId)
                putExtra("USER_NAME", userName)
                putExtra("USER_EMAIL", userEmail)
                putExtra("refresh_notes", true)
            }

            startActivity(homeIntent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (e: Exception) {
            Log.e("CreateActivity", "Error in navigateToHome: ${e.message}", e)
            finish()
        }
    }
    private var isNoteModified = false

    // User data variables
    private var userName: String = ""
    private var userEmail: String = ""
    private var userId: String = ""
    private var currentUserId: String = ""
    


    private lateinit var binding: ActivityCreateBinding
    private lateinit var contentEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var saveButton: com.google.android.material.floatingactionbutton.FloatingActionButton
    private lateinit var pencilIcon: ImageView
    // Back button has been removed from the UI
    private lateinit var dateText: TextView

    // Bottom navigation is optional for this activity
    private var bottomNavigationView: BottomNavigationView? = null
    private var noteId: Long = -1
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        android.util.Log.d("CreateActivity", "onCreate started")

        try {
            enableEdgeToEdge()
            binding = ActivityCreateBinding.inflate(layoutInflater)
            setContentView(binding.root)
            android.util.Log.d("CreateActivity", "View binding and content view set")

            // Initialize views using binding after setContentView
            contentEditText = binding.contentEditText
            titleEditText = binding.titleEditText
            saveButton = binding.saveButton
            pencilIcon = binding.pencilIcon
            dateText = binding.dateText

            // Get user data from intent
            intent?.extras?.let { bundle ->
                userName = bundle.getString("USER_NAME", "")
                userEmail = bundle.getString("USER_EMAIL", "")
                userId = bundle.getString("USER_ID", "")
                currentUserId = userId // Set the currentUserId as well if needed
            }

            // Set current date before any view operations
            dateText.text = dateFormat.format(calendar.time)

            // Set up home navigation in the bottom navigation
            binding.bottomNavigationView.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        // Check for unsaved changes before navigating
                        if (isNoteModified && (titleEditText.text.toString().isNotEmpty() ||
                                contentEditText.text.toString().isNotEmpty())) {
                            showSaveDialog()
                        } else {
                            // If no unsaved changes, just navigate home
                            navigateToHome()
                        }
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    }

                    else -> false
                }
            }

            // Hide the profile button since it's not needed in Create activity
            binding.bottomNavigationView.menu.findItem(R.id.navigation_profile)?.isVisible = false

            // Set up text change listeners
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    isNoteModified = true
                    validateInput() // Update UI when text changes
                }

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    // No-op
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // No-op
                }
            }

            titleEditText.addTextChangedListener(textWatcher)
            contentEditText.addTextChangedListener(textWatcher)
            
            // Initial validation
            validateInput()

            // Get current user ID as String
            currentUserId = intent.getStringExtra("user_id") ?: ""
            if (currentUserId.isEmpty()) {
                Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            // Check if we're editing an existing note
            noteId = intent.getLongExtra("note_id", -1)
            val isEditing = noteId != -1L

            if (isEditing) {
                // Set title for editing
                binding.titleText.text = "Edit Note"

                // Load the existing note using the user ID
                val userId = intent.getStringExtra("user_id") ?: ""
                if (userId.isEmpty()) {
                    showErrorAndFinish("User not authenticated")
                    return
                }

                loadNote()
            }

            // Initialize empty state after all views are set up
            validateInput()

            // Set up save button click listener
            saveButton.setOnClickListener {
                try {
                    // Log the button click
                    android.util.Log.d("CreateActivity", "Save button clicked")
                    
                    // Disable the button to prevent multiple clicks
                    saveButton.isEnabled = false
                    
                    // Save the note and navigate home after successful save
                    saveNote {
                        // This callback runs after successful save
                        android.util.Log.d("CreateActivity", "Save completed, navigating home")
                        navigateToHome()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CreateActivity", "Error in save button click: ${e.message}", e)
                    saveButton.isEnabled = true // Re-enable button on error
                }
            }

            // Clear any existing text for new notes
            if (noteId == -1L) {
                titleEditText.setText("")
                contentEditText.setText("")
            } else {
                // Load existing note if we're editing
                loadNote()
            }

            // Set current date
            updateDateText()

            // Set up window insets
            ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }

        } catch (e: Exception) {
            android.util.Log.e("CreateActivity", "Error in onCreate: ${e.message}", e)
            showErrorAndFinish("Error initializing activity")
        }
    }

    private fun showErrorAndFinish(message: String) {
        android.util.Log.e("CreateActivity", message)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun validateInput() {
        try {
            val hasTitle = titleEditText.text.toString().trim().isNotEmpty()
            val hasContent = contentEditText.text.toString().trim().isNotEmpty()
            val hasChanges = hasTitle || hasContent

            // Log the current state for debugging
            android.util.Log.d("CreateActivity", "validateInput - hasTitle: $hasTitle, hasContent: $hasContent, hasChanges: $hasChanges")

            // Toggle save button and pencil icon visibility
            runOnUiThread {
                try {
                    // Always show save button if there are changes
                    saveButton.visibility = if (hasChanges) View.VISIBLE else View.VISIBLE
                    pencilIcon.visibility = View.INVISIBLE
                    saveButton.isEnabled = hasChanges
                    saveButton.alpha = if (hasChanges) 1.0f else 0.5f
                    saveButton.requestLayout()
                    
                    // Log the button state
                    android.util.Log.d("CreateActivity", "Button visibility: ${saveButton.visibility}, enabled: ${saveButton.isEnabled}")
                } catch (e: Exception) {
                    android.util.Log.e("CreateActivity", "Error updating UI in validateInput: ${e.message}", e)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CreateActivity", "Error in validateInput: ${e.message}", e)
        }
    }

    private fun saveNote(onSuccess: (() -> Unit)? = null) {
        // Only reset isNoteModified after successful save
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            android.util.Log.d("CreateActivity", "Save aborted: Note is empty")
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            saveButton.isEnabled = true
            return
        }
        
        // Show saving notification
        val savingToast = Toast.makeText(this, "Saving note...", Toast.LENGTH_SHORT)
        savingToast.setGravity(Gravity.CENTER, 0, 0)
        savingToast.show()

        android.util.Log.d(
            "CreateActivity",
            "Saving note - Title: '$title', Content length: ${content.length}"
        )
        android.util.Log.d("CreateActivity", "Current user ID: '$currentUserId'")
        android.util.Log.d("CreateActivity", "Save button enabled: ${saveButton.isEnabled}")

        // Disable the save button to prevent multiple clicks
        saveButton.isEnabled = false

        // Mark as not modified immediately to prevent race conditions
        isNoteModified = false

        lifecycleScope.launch(ioDispatcher) {
            try {
                val note = if (noteId == -1L) {
                    // Create new note
                    Note(
                        title = title,
                        content = content,
                        date = Date(),
                        userId = currentUserId
                    )
                } else {
                    // Update existing note
                    Note(
                        id = noteId,
                        title = title,
                        content = content,
                        date = calendar.time,
                        userId = currentUserId
                    )
                }

                // Save the note using the repository
                when (val saveResult = noteRepository.saveNote(note)) {
                    is Result.Success -> {
                        android.util.Log.d(
                            "CreateActivity",
                            "Note saved successfully with ID: ${saveResult.data}"
                        )

                        withContext(Dispatchers.Main) {
                            try {
                                // Update noteId if this was a new note
                                if (noteId == -1L) {
                                    noteId = saveResult.data ?: -1L
                                }

                                if (onSuccess != null) {
                                    // If callback is provided, just invoke it
                                    onSuccess()
                                } else {
                                    // If no callback, show success prompt and navigate
                                    showSaveSuccessPrompt { navigateToHome() }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "CreateActivity",
                                    "Error in save success handler",
                                    e
                                )
                                // If there was an error in the success handler, we still consider the save successful
                                // so don't set isNoteModified back to true
                            } finally {
                                saveButton.isEnabled = true
                            }
                        }
                    }

                    is Result.Error -> {
                        withContext(Dispatchers.Main) {
                            // If save failed, set isNoteModified back to true
                            isNoteModified = true
                            saveButton.isEnabled = true
                            android.util.Log.e(
                                "CreateActivity",
                                "Error saving note",
                                saveResult.exception
                            )
                            Toast.makeText(
                                this@Create,
                                "Error saving note: ${saveResult.exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    is Result.Loading -> {
                        // This case shouldn't normally happen with our current implementation
                        withContext(Dispatchers.Main) {
                            // Keep isNoteModified as is until we know the result
                            Toast.makeText(
                                this@Create,
                                "Saving note...",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // If we get an exception, assume the save failed
                    isNoteModified = true
                    saveButton.isEnabled = true
                    android.util.Log.e("CreateActivity", "Unexpected error saving note", e)
                    Toast.makeText(
                        this@Create,
                        "An error occurred: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun loadNote() {
        isNoteModified = false
        lifecycleScope.launch(ioDispatcher) {
            try {
                when (val result = noteRepository.getNoteById(noteId, currentUserId)) {
                    is Result.Success -> {
                        result.data?.let { note ->
                            withContext(Dispatchers.Main) {
                                titleEditText.setText(note.title)
                                contentEditText.setText(note.content)
                                updateDateText(note.date)
                                calendar.time = note.date
                            }
                        } ?: run {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@Create,
                                    "Note not found",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    }

                    is Result.Error -> {
                        android.util.Log.e(
                            "CreateActivity",
                            "Error loading note",
                            result.exception
                        )
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@Create,
                                "Error loading note: ${result.exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            finish()
                        }
                    }

                    else -> {}
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateActivity", "Unexpected error in loadNote", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@Create,
                        "Error: ${e.message}",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                    finish()
                }
            }
        }
    }

    private fun updateDateText(date: Date = Date()) {
        binding.dateText.text = dateFormat.format(date)
    }

    // Handle back button press
    override fun onBackPressed() {
        // Check if we're already in the process of saving
        if (!saveButton.isEnabled) {
            android.util.Log.d("CreateActivity", "Save in progress, ignoring back press")
            return
        }
        
        // Check for unsaved changes before navigating
        if (isNoteModified && (titleEditText.text.toString().isNotEmpty() ||
                contentEditText.text.toString().isNotEmpty())) {
            android.util.Log.d("CreateActivity", "Showing save dialog - isNoteModified: $isNoteModified")
            showSaveDialog()
        } else {
            // If no unsaved changes, navigate back
            android.util.Log.d("CreateActivity", "No unsaved changes, navigating back")
            super.onBackPressed()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun showSaveDialog() {
        android.util.Log.d("CreateActivity", "Showing save dialog")
        
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Save Changes?")
            .setMessage("You have unsaved changes. Do you want to save before leaving?")
            .setPositiveButton("Save") { dialog, _ ->
                // Save the note and navigate home after successful save
                saveButton.isEnabled = false
                saveNote {
                    // This callback runs after successful save
                    try {
                        dialog.dismiss()
                        navigateToHome()
                    } catch (e: Exception) {
                        Log.e("CreateActivity", "Error after save: ${e.message}", e)
                        navigateToHome()
                    }
                }
            }
            .setNegativeButton("Don't Save") { dialog, _ ->
                // User chose to discard changes
                try {
                    isNoteModified = false
                    dialog.dismiss()
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                } catch (e: Exception) {
                    Log.e("CreateActivity", "Error discarding changes: ${e.message}", e)
                    finish()
                }
            }
            .setNeutralButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .setOnDismissListener {
                // Re-enable the save button if dialog is dismissed
                saveButton.isEnabled = true
            }
            .create()
            
        dialog.setOnShowListener {
            // Set button colors when dialog is shown
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.md_theme_light_primary
                )
            )
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)?.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.md_theme_light_primary
                )
            )
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL)?.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.md_theme_light_primary
                )
            )
        }
        
        // Prevent dialog from being dismissed when clicking outside
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        
        try {
            dialog.show()
        } catch (e: Exception) {
            Log.e("CreateActivity", "Error showing save dialog: ${e.message}", e)
            // If we can't show the dialog, just navigate home
            navigateToHome()
        }
    }

    // Show a success prompt when note is saved
    private fun showSaveSuccessPrompt(navigateHome: () -> Unit) {
        try {
            // Inflate the custom toast layout
            val inflater = LayoutInflater.from(this)
            val layout = inflater.inflate(
                R.layout.custom_toast_layout,
                null
            )

            // Set the text
            val text = layout.findViewById<TextView>(R.id.text)
            text.text = "✓ Note saved successfully"

            // Create and show the toast
            val toast = Toast(this)
            toast.duration = Toast.LENGTH_SHORT
            toast.view = layout
            toast.setGravity(Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, 16)
            toast.show()

            // Navigate back after a short delay
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    try {
                        navigateHome()
                    } catch (e: Exception) {
                        Log.e("CreateActivity", "Error in navigateHome callback: ${e.message}", e)
                        // If navigation fails, just finish the activity
                        finish()
                    }
                },
                2000 // 2 seconds delay
            )
        } catch (e: Exception) {
            Log.e("CreateActivity", "Error showing save success", e)
            // If showing the toast fails, just navigate
            navigateHome()
        }
    }
}
