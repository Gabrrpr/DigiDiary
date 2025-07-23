package com.example.digi_diary

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.databinding.ActivityCreateBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class Create : AppCompatActivity() {
    
    private lateinit var binding: ActivityCreateBinding
    private lateinit var database: AppDatabase
    private lateinit var contentEditText: EditText
    private lateinit var titleEditText: EditText
    private lateinit var saveButton: ImageButton
    private lateinit var pencilIcon: ImageView
    private lateinit var backButton: ImageButton
    private lateinit var dateText: TextView
    // Bottom navigation is optional for this activity
    private var bottomNavigationView: BottomNavigationView? = null
    private var noteId: Long = -1
    private var currentUserId: String = ""
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            android.util.Log.d("CreateActivity", "onCreate started")
            
            try {
                enableEdgeToEdge()
                binding = ActivityCreateBinding.inflate(layoutInflater)
                setContentView(binding.root)
                android.util.Log.d("CreateActivity", "View binding and content view set")
            } catch (e: Exception) {
                android.util.Log.e("CreateActivity", "Error setting up view: ${e.message}", e)
                throw e
            }

            // Initialize database
            try {
                database = AppDatabase.getDatabase(this)
                android.util.Log.d("CreateActivity", "Database initialized")
            } catch (e: Exception) {
                android.util.Log.e("CreateActivity", "Error initializing database: ${e.message}", e)
                showErrorAndFinish("Error initializing database")
                return
            }

            // Initialize views using binding after setContentView
            contentEditText = binding.contentEditText
            titleEditText = binding.titleEditText
            saveButton = binding.saveButton
            pencilIcon = binding.pencilIcon
            backButton = binding.backButton
            dateText = binding.dateText
            
            // Set current date before any view operations
            dateText.text = dateFormat.format(calendar.time)
            
            // Set up click listeners
            backButton.setOnClickListener {
                onBackPressed()
            }
            
            // Initialize and set up bottom navigation
            bottomNavigationView = binding.bottomNavigationView
            
            // Set the selected item to none since this is the Create activity
            bottomNavigationView?.selectedItemId = -1
            
            bottomNavigationView?.setOnItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.navigation_home -> {
                        // Navigate back to HomePage
                        finish()
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    }
                    R.id.navigation_profile -> {
                        // Navigate to Profile activity
                        val profileIntent = Intent(this@Create, Profile::class.java).apply {
                            // Pass the user data to Profile activity
                            val name = intent?.getStringExtra("USER_NAME") ?: "User"
                            val email = intent?.getStringExtra("USER_EMAIL") ?: ""
                            val userId = intent?.getStringExtra("user_id") ?: ""
                            
                            putExtra("USER_NAME", name)
                            putExtra("USER_EMAIL", email)
                            putExtra("user_id", userId)
                        }
                        startActivity(profileIntent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    }
                    else -> false
                }
            }
            
            // Set up text change listeners
            val textWatcher = object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    validateInput()
                }
            }
            
            titleEditText.addTextChangedListener(textWatcher)
            contentEditText.addTextChangedListener(textWatcher)
            
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
                
                lifecycleScope.launch {
                    try {
                        val note = withContext(Dispatchers.IO) {
                            database.noteDao().getUserNoteById(noteId, userId)
                        }
                        
                        withContext(Dispatchers.Main) {
                            if (note != null) {
                                binding.titleEditText.setText(note.title)
                                binding.contentEditText.setText(note.content)
                                binding.dateText.text = dateFormat.format(note.date)
                                calendar.time = note.date
                            } else {
                                showErrorAndFinish("Note not found")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("CreateActivity", "Error loading note", e)
                        showErrorAndFinish("Error loading note: ${e.message}")
                    }
                }
            }
            
            // Initialize empty state after all views are set up
            validateInput()
            
            saveButton.setOnClickListener {
                saveNote()
            }

            // Check if we're editing an existing note
            noteId = intent.getLongExtra("note_id", -1)
            if (noteId != -1L) {
                loadNote()
            } else {
                // Set default title for new notes
                titleEditText.setText("")
                contentEditText.setText("")
            }

            // Set current date
            updateDateText()

            // Set up bottom navigation if it exists
            bottomNavigationView?.setOnItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_profile -> {
                        // Navigate to profile if needed
                        // For now, just show a toast
                        Toast.makeText(this, "Profile will be implemented soon", Toast.LENGTH_SHORT).show()
                        true
                    }
                    else -> false
                }
            }
            
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
            
            // Show/hide save button based on input
            saveButton.visibility = if (hasTitle || hasContent) View.VISIBLE else View.GONE
            
            // Toggle pencil icon visibility
            pencilIcon.visibility = if (hasTitle || hasContent) View.GONE else View.VISIBLE
        } catch (e: Exception) {
            android.util.Log.e("CreateActivity", "Error in validateInput: ${e.message}", e)
        }
    }

    private fun saveNote() {
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()
        
        if (title.isEmpty() && content.isEmpty()) {
            Toast.makeText(this, "Note cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        android.util.Log.d("CreateActivity", "Saving note - Title: '$title', Content length: ${content.length}")
        android.util.Log.d("CreateActivity", "Database path: ${getDatabasePath("digi_diary_database").absolutePath}")
        android.util.Log.d("CreateActivity", "Current user ID: '$currentUserId'")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Log database info before saving
                val dbFile = getDatabasePath("digi_diary_database")
                android.util.Log.d("CreateActivity", "Database exists: ${dbFile.exists()}, size: ${if (dbFile.exists()) dbFile.length() else 0} bytes")
                
                if (noteId == -1L) {
                    // Create new note
                    val note = Note(
                        title = title,
                        content = content,
                        date = Date(),
                        userId = currentUserId
                    )
                    android.util.Log.d("CreateActivity", "Inserting new note: $note")
                    val newId = database.noteDao().insert(note)
                    android.util.Log.d("CreateActivity", "Note saved with ID: $newId")
                    
                    // Verify the note was saved
                    val savedNote = database.noteDao().getUserNoteById(newId, currentUserId)
                    android.util.Log.d("CreateActivity", "Retrieved saved note: $savedNote")
                } else {
                    // Update existing note
                    android.util.Log.d("CreateActivity", "Updating existing note with ID: $noteId")
                    val existingNote = database.noteDao().getUserNoteById(noteId, currentUserId)
                    if (existingNote != null) {
                        val updatedNote = existingNote.copy(
                            title = title,
                            content = content,
                            date = Date()
                        )
                        android.util.Log.d("CreateActivity", "Updating note: $updatedNote")
                        database.noteDao().update(updatedNote)
                        android.util.Log.d("CreateActivity", "Note updated with ID: $noteId")
                    } else {
                        android.util.Log.e("CreateActivity", "Note with ID $noteId not found for user $currentUserId")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@Create, "Note not found", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        return@launch
                    }
                }
                
                // Log all notes after saving
                val allNotes = database.noteDao().getAllNotesForDebug()
                android.util.Log.d("CreateActivity", "Total notes in database after save: ${allNotes.size}")
                allNotes.forEachIndexed { index, note ->
                    android.util.Log.d("CreateActivity", "Note $index - ID: ${note.id}, Title: '${note.title}', UserID: '${note.userId}'")
                }
                
                withContext(Dispatchers.Main) {
                    android.util.Log.d("CreateActivity", "Note saved successfully")
                    val resultIntent = Intent().apply {
                        putExtra("note_saved", true)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateActivity", "Error saving note", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Create, "Error saving note: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun loadNote() {
        lifecycleScope.launch {
            try {
                val note = database.noteDao().getUserNoteById(noteId, currentUserId)
                withContext(Dispatchers.Main) {
                    if (note != null) {
                        titleEditText.setText(note.title)
                        contentEditText.setText(note.content)
                        updateDateText(note.date)
                    } else {
                        Toast.makeText(this@Create, "Note not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateActivity", "Error loading note", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@Create, "Error loading note: ${e.message}", Toast.LENGTH_SHORT).show()
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
        if (titleEditText.text.toString().isNotEmpty() || contentEditText.text.toString().isNotEmpty()) {
            // Show save dialog if there are unsaved changes
            showSaveDialog()
        } else {
            super.onBackPressed()
        }
    }
    
    private fun showSaveDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.unsaved_changes))
            .setMessage(getString(R.string.save_changes_question))
            .setPositiveButton(R.string.save) { _, _ -> saveNote() }
            .setNegativeButton(R.string.discard) { _, _ -> finish() }
            .setNeutralButton(R.string.cancel, null)
            .show()
    }
}