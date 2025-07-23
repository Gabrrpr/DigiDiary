package com.example.digi_diary

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.digi_diary.R
import com.example.digi_diary.data.AppDatabase
import com.example.digi_diary.data.model.Note
import com.example.digi_diary.databinding.ActivityHomePageBinding
import com.example.digi_diary.ui.NoteItemDecoration
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HomePage : AppCompatActivity() {
    companion object {
        private const val TAG = "HomePage"
        private const val REQUEST_CODE_EDIT_NOTE = 1001
        private const val REQUEST_CODE_CREATE_NOTE = 1002
    }

    private lateinit var binding: ActivityHomePageBinding
    private lateinit var database: AppDatabase
    private val notesList = mutableListOf<Note>()
    private var currentUserId: String = ""
    private lateinit var noteAdapter: NoteAdapter

    private fun createNoteAdapter(): NoteAdapter {
        return NoteAdapter(
            notes = ArrayList(),
            onNoteClick = { note ->
                Log.d(TAG, "Note clicked - ID: ${note.id}")
                val intent = Intent(this, Create::class.java).apply {
                    putExtra("note_id", note.id)
                    putExtra("is_editing", true)
                    putExtra("user_id", currentUserId)
                    intent?.getStringExtra("USER_NAME")?.let { putExtra("USER_NAME", it) }
                    intent?.getStringExtra("USER_EMAIL")?.let { putExtra("USER_EMAIL", it) }
                }
                startActivityForResult(intent, REQUEST_CODE_EDIT_NOTE)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            },
            onDeleteClick = { note ->
                Log.d(TAG, "Delete clicked for note: ${note.id}")
                showDeleteConfirmationDialog(note)
            }
        )
    }

    // Track loading state
    private var isLoading = false
    private var startTime: Long = 0
    
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "📝 onResume() - Refreshing notes")
        loadNotes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "🚀 onCreate() called with intent: $intent")

        // Log all intent extras for debugging
        intent?.extras?.keySet()?.forEach { key ->
            Log.d(TAG, "   Intent extra - $key: ${intent.extras?.get(key)}")
        }

        // Initialize loading state
        isLoading = true
        startTime = System.currentTimeMillis()

        // Initialize database
        database = AppDatabase.getDatabase(this)

        // Get current user ID from intent or shared preferences
        currentUserId = intent.getStringExtra("user_id") ?: ""

        if (currentUserId.isEmpty()) {
            Log.d(TAG, "⚠️ No user_id in intent, checking shared preferences")
            // Try to get from shared preferences as fallback
            val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
            currentUserId = sharedPref.getString("user_id", "") ?: ""
            Log.d(TAG, "📱 User ID from shared prefs: '$currentUserId'")
        } else {
            Log.d(TAG, "✅ Got user_id from intent: '$currentUserId'")
            // Save to shared preferences for future use
            getSharedPreferences("user_prefs", MODE_PRIVATE).edit()
                .putString("user_id", currentUserId)
                .apply()
        }

        if (currentUserId.isEmpty()) {
            val errorMsg = "❌ No user ID found in intent or shared prefs, cannot proceed"
            Log.e(TAG, errorMsg)
            showError("User not authenticated")
            finish()
            return
        }

        Log.d(TAG, "🚀 Activity created for user: $currentUserId")

        // Set initial UI state
        binding.loadingProgressBar.visibility = View.VISIBLE
        binding.emptyStateContainer.visibility = View.GONE
        binding.notesRecyclerView.visibility = View.GONE

        // Initialize all components
        Log.d(TAG, "🔄 Initializing components in onCreate")
        initializeComponents()
        
        // Load notes for the current user
        Log.d(TAG, "📝 Loading notes for user: $currentUserId")
        loadNotes()

        // Get user data from intent with fallback to shared preferences
        val sharedPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val userName = intent.getStringExtra("USER_NAME") ?: sharedPrefs.getString("user_name", "User") ?: "User"

        // Store user data in shared preferences for future use
        with(sharedPrefs.edit()) {
            putString("user_name", userName)
            intent.getStringExtra("USER_EMAIL")?.let { email ->
                putString("user_email", email)
            }
            apply()
        }

        // Create a styled welcome message with colored username
        val welcomeText = "Hello, "
        val fullText = SpannableString("$welcomeText$userName")

        // Set style for the username part
        val usernameStart = welcomeText.length
        val usernameEnd = fullText.length
        val usernameColor = getColor(R.color.username_color)

        fullText.setSpan(
            ForegroundColorSpan(usernameColor),
            usernameStart,
            usernameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        fullText.setSpan(
            StyleSpan(Typeface.BOLD),
            usernameStart,
            usernameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        fullText.setSpan(
            RelativeSizeSpan(1.2f),
            usernameStart,
            usernameEnd,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        binding.welcomeText.text = fullText

        // Set current date
        val currentDate = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date())
        binding.dateText.text = currentDate

        // Set up FAB click listener
        binding.fab.setOnClickListener {
            if (currentUserId.isEmpty()) {
                showError("User not authenticated")
                return@setOnClickListener
            }

            val intent = Intent(this@HomePage, Create::class.java).apply {
                putExtra("user_id", currentUserId)
                // Pass user data to pre-fill in Create activity if needed
                putExtra("USER_NAME", this@HomePage.intent.getStringExtra("USER_NAME") ?: "")
                putExtra("USER_EMAIL", this@HomePage.intent.getStringExtra("USER_EMAIL") ?: "")
            }
            startActivityForResult(intent, REQUEST_CODE_CREATE_NOTE)
            // Add fade animation
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        // Set up bottom navigation
        setupBottomNavigation()
    }
    
    private fun showError(message: String) {
        Log.e(TAG, "Showing error: $message")
        runOnUiThread {
            try {
                Toast.makeText(this@HomePage, message, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing error toast", e)
            }
        }
    }
    
    private fun showErrorState(errorMessage: String? = null) {
        Log.e("HomePage", "Showing error state: $errorMessage")
        runOnUiThread {
            try {
                binding.loadingProgressBar.visibility = View.GONE
                binding.emptyStateContainer.visibility = View.VISIBLE
                binding.notesRecyclerView.visibility = View.VISIBLE // Keep RecyclerView visible to maintain layout stability
                
                errorMessage?.let {
                    Toast.makeText(this@HomePage, it, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e("HomePage", "Error in showErrorState", e)
            }
        }
    }
    

    


    
    private fun showLoading() {
        updateUiState(
            showLoading = true,
            showEmptyState = false,
            showRecyclerView = false
        )
    }
    
    private fun updateUiState(
        showLoading: Boolean = false,
        showEmptyState: Boolean = false,
        showRecyclerView: Boolean = false,
        errorMessage: String? = null
    ) {
        if (isFinishing || isDestroyed) return
        
        runOnUiThread {
            try {
                binding.loadingProgressBar.visibility = if (showLoading) View.VISIBLE else View.GONE
                binding.emptyStateContainer.visibility = if (showEmptyState) View.VISIBLE else View.GONE
                binding.notesRecyclerView.visibility = if (showRecyclerView) View.VISIBLE else View.GONE
                
                errorMessage?.let { showError(it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating UI state", e)
            }
        }
    }
    
    private fun showDeleteConfirmationDialog(note: Note) {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note?")
                .setPositiveButton("Delete") { _, _ -> 
                    Log.d(TAG, "User confirmed deletion of note: ${note.id}")
                    deleteNote(note) 
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    Log.d(TAG, "User cancelled note deletion")
                    dialog.dismiss()
                }
                .setOnDismissListener {
                    Log.d(TAG, "Delete dialog dismissed")
                }
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing delete confirmation dialog", e)
            showError("Error preparing delete confirmation")
        }
    }
    
    private fun deleteNote(note: Note) {
        Log.d(TAG, "Deleting note: ${note.id}")
        
        // Show loading state
        showLoading()
        
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Delete the note from the database
                database.noteDao().delete(note)
                
                // Update the UI on the main thread
                withContext(Dispatchers.Main) {
                    // Remove the note from the local list
                    val removed = notesList.removeAll { it.id == note.id }
                    Log.d(TAG, "Note ${if (removed) "removed from" else "not found in"} local list")
                    
                    // Reload notes to ensure UI is in sync with the database
                    loadNotes()
                    
                    // Show success message
                    showError("Note deleted")
                    
                    Log.d(TAG, "✅ Successfully deleted note: ${note.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error deleting note: ${note.id}", e)
                
                withContext(Dispatchers.Main) {
                    showError("Failed to delete note: ${e.message}")
                    
                    // Try to recover by reloading the notes
                    loadNotes()
                }
            } finally {
                // Ensure we're not in a loading state
                withContext(Dispatchers.Main) {
                    if (notesList.isEmpty()) {
                        showEmptyState()
                    } else {
                        showNotesList(notesList)
                    }
                }
            }
        }
    }
    
    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.navigation_home

        binding.bottomNavigationView.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    binding.notesRecyclerView.smoothScrollToPosition(0)
                    true
                }
                R.id.navigation_profile -> {
                    try {
                        val sharedPref = getSharedPreferences("user_prefs", MODE_PRIVATE)
                        val userName = intent.getStringExtra("USER_NAME") ?: sharedPref.getString("USER_NAME", "") ?: ""
                        val userEmail = intent.getStringExtra("USER_EMAIL") ?: sharedPref.getString("USER_EMAIL", "") ?: ""

                        val intent = Intent(this, Profile::class.java).apply {
                            putExtra("USER_NAME", userName)
                            putExtra("user_name", userName)
                            putExtra("USER_EMAIL", userEmail)
                            putExtra("user_email", userEmail)
                            putExtra("user_id", currentUserId)
                            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                        }
                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        true
                    } catch (e: Exception) {
                        Log.e(TAG, "Error navigating to Profile", e)
                        showError("Error: ${e.message}")
                        false
                    }
                }
                else -> false
            }
        }
    }
    
    private fun updateNotesList(notes: List<Note>) {
        runOnUiThread {
            try {
                // Check if activity is still valid
                if (isFinishing || isDestroyed) {
                    Log.w(TAG, "⚠️ Activity is finishing or destroyed, skipping UI update")
                    return@runOnUiThread
                }

                // Ensure binding is initialized
                if (!::binding.isInitialized) {
                    Log.e(TAG, "❌ Binding not initialized in updateNotesList")
                    showErrorState("UI not properly initialized")
                    return@runOnUiThread
                }

                // Ensure RecyclerView exists
                if (binding.notesRecyclerView == null) {
                    Log.e(TAG, "❌ RecyclerView is null in updateNotesList")
                    showErrorState("Failed to initialize notes display")
                    return@runOnUiThread
                }

                // Ensure adapter is initialized
                if (!::noteAdapter.isInitialized) {
                    Log.w(
                        TAG,
                        "⚠️ NoteAdapter not initialized in updateNotesList, creating new one"
                    )
                    try {
                        noteAdapter = createNoteAdapter()
                        binding.notesRecyclerView.adapter = noteAdapter
                        Log.d(TAG, "✅ Created and set new NoteAdapter")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to create NoteAdapter", e)
                        showErrorState("Failed to initialize notes display")
                        return@runOnUiThread
                    }
                }

                // Update the notes list with the new data
                notesList.clear()
                notesList.addAll(notes)
                
                Log.d(TAG, "📝 Updated notes list with ${notes.size} items")

                // Update the adapter with the new notes
                noteAdapter.updateNotes(ArrayList(notes))

                // Notify adapter of data change
                noteAdapter.notifyDataSetChanged()

                Log.d(TAG, "✅ Successfully updated notes list with ${notes.size} items")

                // Show the notes list and hide loading/error states
                showNotesList(notes)

                // Force a layout pass to ensure RecyclerView updates
                binding.notesRecyclerView.post {
                    binding.notesRecyclerView.invalidateItemDecorations()
                }

                // Update UI based on whether we have notes or not
                if (notesList.isEmpty()) {
                    Log.d(TAG, "📭 No notes found, showing empty state")
                    showEmptyState()
                } else {
                    Log.d(TAG, "📚 Showing ${notesList.size} notes in the list")
                    showNotesList(notesList)

                    // Log the first item's position for debugging
                    val layoutManager =
                        binding.notesRecyclerView.layoutManager as? LinearLayoutManager
                    layoutManager?.let { manager ->
                        val firstVisible = manager.findFirstVisibleItemPosition()
                        val lastVisible = manager.findLastVisibleItemPosition()
                        Log.d(TAG, "👀 Visible items: $firstVisible to $lastVisible")
                    }
                }
            } catch (e: Exception) {
                val errorMsg = "❌ Error in updateNotesList: ${e.message}"
                Log.e(TAG, errorMsg, e)
                showErrorState("Error updating notes: ${e.message}")
            }
        }
    }

    private fun setupRecyclerView() {
        try {
            Log.d(TAG, "🔄 Setting up RecyclerView...")

            // 0. Check if binding is initialized
            if (!::binding.isInitialized) {
                Log.e(TAG, "❌ Binding not initialized in setupRecyclerView")
                return
            }

            // 1. Verify RecyclerView exists in the layout
            if (binding.notesRecyclerView == null) {
                Log.e(TAG, "❌ RecyclerView not found in layout")
                return
            }

            // 2. Set layout manager
            val layoutManager = LinearLayoutManager(this@HomePage).apply {
                Log.d(TAG, "📏 Initialized LinearLayoutManager")
            }
            binding.notesRecyclerView.layoutManager = layoutManager

            // 3. Set adapter (make sure it's initialized)
            if (!::noteAdapter.isInitialized) {
                Log.w(TAG, "⚠️ NoteAdapter not initialized in setupRecyclerView, creating new one")
                createNoteAdapter()

                if (!::noteAdapter.isInitialized) {
                    Log.e(TAG, "❌ Failed to initialize NoteAdapter")
                    showErrorState("Failed to initialize notes display")
                    return
                }
            }

            // 4. Set the adapter on RecyclerView
            binding.notesRecyclerView.adapter = noteAdapter
            Log.d(TAG, "✅ RecyclerView adapter set with ${notesList.size} notes")

            // 5. Add item decoration for spacing between items
            try {
                val spacingInPixels = resources.getDimensionPixelSize(R.dimen.note_item_spacing)
                if (binding.notesRecyclerView.itemDecorationCount == 0) {
                    binding.notesRecyclerView.addItemDecoration(NoteItemDecoration(spacingInPixels))
                    Log.d(TAG, "✅ Added item decoration with spacing: ${spacingInPixels}px")
                } else {
                    Log.d(TAG, "ℹ️ Item decoration already added")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error setting up item decoration", e)
                // Non-fatal, continue without decoration
            }

            // 6. Verify adapter data
            if (noteAdapter.itemCount == 0) {
                Log.d(TAG, "ℹ️ Adapter has no items, showing empty state")
                showEmptyState()
            } else {
                Log.d(TAG, "✅ RecyclerView setup complete with ${noteAdapter.itemCount} items")
                showNotesList(notesList)
            }

        } catch (e: Exception) {
            val errorMsg = "❌ Critical error in setupRecyclerView: ${e.message}"
            Log.e(TAG, errorMsg, e)
            showErrorState("Failed to set up notes display")
            // Don't rethrow to prevent app crash, but log the error
        }
    }

    private fun initializeComponents() {
        try {
            Log.d(TAG, "🔄 Initializing components...")

            // 1. First, create the adapter
            noteAdapter = createNoteAdapter()
            Log.d(TAG, "✅ NoteAdapter created: $noteAdapter")

            // 2. Set up RecyclerView with the adapter
            setupRecyclerView()
            Log.d(TAG, "✅ RecyclerView setup complete")

            // 3. Verify RecyclerView and Adapter
            if (binding.notesRecyclerView.adapter == null) {
                Log.e(TAG, "❌ RecyclerView adapter is still null after setup!")
                // Emergency fallback - set adapter directly
                binding.notesRecyclerView.adapter = noteAdapter
                Log.d(TAG, "⚠️ Manually set adapter on RecyclerView")
            }

            // 4. Load notes after a short delay to ensure UI is ready
            binding.notesRecyclerView.postDelayed({
                try {
                    Log.d(TAG, "⏳ Delayed loadNotes starting...")
                    loadNotes()
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in delayed loadNotes", e)
                    showErrorState("Error loading notes")
                }
            }, 100L) // 100ms delay
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing components", e)
            showErrorState("Error initializing app")
        }
    }

    private fun loadNotes() {
        Log.d(TAG, "🚀 Starting to load notes for user: '$currentUserId'")

        // Check if activity is still valid
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "⚠️ Activity is finishing or destroyed, skipping load")
            return
        }

        // Show loading state
        showLoading()

        // Ensure RecyclerView and adapter are properly initialized
        if (!::noteAdapter.isInitialized) {
            Log.d(TAG, "ℹ️ Initializing NoteAdapter")
            noteAdapter = createNoteAdapter()
            
            if (binding.notesRecyclerView.adapter == null) {
                Log.d(TAG, "ℹ️ Setting up RecyclerView")
                setupRecyclerView()
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                Log.d(TAG, "🔍 Querying database for user: '$currentUserId'")

                // Get notes for current user
                val userNotes = database.noteDao().getUserNotesSync(currentUserId)
                Log.d(TAG, "✅ Retrieved ${userNotes.size} notes for user '$currentUserId'")

                withContext(Dispatchers.Main) {
                    if (isFinishing || isDestroyed) return@withContext
                    
                    if (userNotes.isEmpty()) {
                        Log.d(TAG, "ℹ️ No notes found for user")
                        showEmptyState()
                    } else {
                        Log.d(TAG, "📝 Updating UI with ${userNotes.size} notes")
                        updateNotesList(userNotes)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading notes", e)
                showErrorState("Failed to load notes: ${e.message}")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_CREATE_NOTE || requestCode == REQUEST_CODE_EDIT_NOTE) {
            loadNotes()
        }
    }

    private fun showEmptyState() {
        Log.d(TAG, "🔄 [showEmptyState] Showing empty state")

        // Check if activity is still valid
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "⚠️ [showEmptyState] Activity is finishing or destroyed, skipping")
            return
        }

        // Ensure we're on the UI thread
        runOnUiThread {
            try {
                // 1. Verify binding is initialized
                if (!::binding.isInitialized) {
                    Log.e(TAG, "❌ [showEmptyState] Binding not initialized")
                    return@runOnUiThread
                }

                // 2. Log the current state of views
                Log.d(
                    TAG, "📊 [showEmptyState] Current state - " +
                            "Loading: ${binding.loadingProgressBar.visibility == View.VISIBLE}, " +
                            "RecyclerView: ${binding.notesRecyclerView.visibility == View.VISIBLE}, " +
                            "EmptyState: ${binding.emptyStateContainer.visibility == View.VISIBLE}"
                )

                // 3. Hide loading and RecyclerView
                binding.loadingProgressBar.visibility = View.GONE
                binding.notesRecyclerView.visibility = View.GONE

                // 4. Set empty state message
                try {
                    binding.emptyStateText.text = "No notes yet"
                    Log.d(TAG, "ℹ️ [showEmptyState] Set empty state text")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [showEmptyState] Failed to set empty state text", e)
                    try {
                        binding.emptyStateText.text = "No notes yet"
                    } catch (e2: Exception) {
                        Log.e(
                            TAG,
                            "❌ [showEmptyState] Critical error setting empty state text",
                            e2
                        )
                    }
                }

                // 5. Show empty state with animation
                try {
                    binding.emptyStateContainer.alpha = 0f
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.emptyStateContainer.animate()
                        .alpha(1f)
                        .setDuration(300)
                        .setListener(object : AnimatorListenerAdapter() {
                            override fun onAnimationEnd(animation: Animator) {
                                super.onAnimationEnd(animation)
                                Log.d(TAG, "✅ [showEmptyState] Empty state animation completed")
                            }
                        })
                    Log.d(TAG, "🎬 [showEmptyState] Started empty state animation")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ [showEmptyState] Error during empty state animation", e)
                    // Fallback to immediate visibility without animation
                    binding.emptyStateContainer.visibility = View.VISIBLE
                    binding.emptyStateContainer.alpha = 1f
                }

                // 6. Log the empty state
                Log.d(TAG, "✅ [showEmptyState] Empty state shown successfully")

            } catch (e: Exception) {
                Log.e(TAG, "❌ [showEmptyState] Critical error showing empty state", e)

                // Fallback to basic visibility change if animation fails
                try {
                    if (::binding.isInitialized) {
                        binding.loadingProgressBar.visibility = View.GONE
                        binding.notesRecyclerView.visibility = View.GONE
                        binding.emptyStateContainer.visibility = View.VISIBLE
                        Log.w(TAG, "⚠️ [showEmptyState] Used fallback to show empty state")
                    }
                } catch (e2: Exception) {
                    Log.e(TAG, "❌ [showEmptyState] Critical error in fallback", e2)
                    // At this point, we've tried everything we can
                }
            }
        }
    }

    private fun showNotesList(notes: List<Note>) {
        // Ensure we're on the main thread for UI updates
        if (isFinishing || isDestroyed) {
            Log.w(TAG, "Activity is finishing or destroyed, skipping UI update")
            return
        }

        try {
            // Ensure binding is initialized
            if (!::binding.isInitialized) {
                Log.e(TAG, "❌ Binding not initialized in showNotesList")
                return
            }

            // Ensure RecyclerView is properly initialized
            if (binding.notesRecyclerView == null) {
                Log.e(TAG, "❌ RecyclerView is null in showNotesList")
                showErrorState("Failed to initialize notes display")
                return
            }

            // Ensure adapter is initialized
            if (!::noteAdapter.isInitialized) {
                Log.w(TAG, "⚠️ NoteAdapter not initialized in showNotesList, initializing...")
                setupRecyclerView()
                return // setupRecyclerView will call loadNotes again
            }

            // Update UI on the main thread
            runOnUiThread {
                try {
                    Log.d(TAG, "📊 Displaying ${notesList.size} notes")

                    // Log detailed note information for debugging
                    if (notesList.isNotEmpty()) {
                        Log.d(TAG, "📝 Notes to display:")
                        notesList.forEachIndexed { index: Int, note: Note ->
                            Log.d(
                                TAG, "   Note[$index] - ID: ${note.id}, " +
                                        "Title: '${note.title.take(30)}${if (note.title.length > 30) "..." else ""}', " +
                                        "UserId: '${note.userId}', Date: ${note.date}"
                            )
                        }
                    } else {
                        Log.d(TAG, "ℹ️ No notes to display, showing empty state")
                        showEmptyState()
                        return@runOnUiThread
                    }

                    // Update the adapter with the latest notes
                    try {
                        noteAdapter.updateNotes(ArrayList(notesList))
                        Log.d(TAG, "✅ Updated adapter with ${notesList.size} notes")
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Failed to update adapter with notes", e)
                        showErrorState("Failed to display notes")
                        return@runOnUiThread
                    }

                    // Make sure RecyclerView is visible and loading/empty states are hidden
                    binding.loadingProgressBar.visibility = View.GONE
                    binding.emptyStateContainer.visibility = View.GONE
                    binding.notesRecyclerView.visibility = View.VISIBLE

                    // Request a layout pass after a short delay
                    binding.notesRecyclerView.postDelayed({
                        try {
                            // Invalidate decorations and request layout
                            binding.notesRecyclerView.invalidateItemDecorations()
                            binding.notesRecyclerView.requestLayout()

                            // Scroll to top to ensure visibility
                            if (notesList.isNotEmpty()) {
                                binding.notesRecyclerView.scrollToPosition(0)
                                Log.d(TAG, "🔝 Scrolled to top of notes list")
                            }

                            // Log the current state of the RecyclerView
                            val adapter = binding.notesRecyclerView.adapter
                            val layoutManager = binding.notesRecyclerView.layoutManager
                            val itemCount = adapter?.itemCount ?: 0

                            Log.d(
                                TAG, "📊 RecyclerView State - " +
                                        "Adapter: ${adapter?.javaClass?.simpleName ?: "null"}, " +
                                        "LayoutManager: ${layoutManager?.javaClass?.simpleName ?: "null"}, " +
                                        "ItemCount: $itemCount, " +
                                        "HasFixedSize: ${binding.notesRecyclerView.hasFixedSize()}"
                            )

                            // Additional debug for layout manager
                            layoutManager?.let { lm ->
                                val firstVisible =
                                    (lm as? LinearLayoutManager)?.findFirstVisibleItemPosition()
                                        ?: -1
                                val lastVisible =
                                    (lm as? LinearLayoutManager)?.findLastVisibleItemPosition()
                                        ?: -1
                                val total = lm.itemCount
                                Log.d(
                                    TAG,
                                    "   Visible items: $firstVisible to $lastVisible of $total"
                                )
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error in RecyclerView post", e)
                        }
                    }, 50) // Small delay to ensure the view is laid out

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Error in showNotesList UI update", e)
                    showErrorState("Failed to update UI")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in showNotesList", e)
            showErrorState("Failed to display notes")
        }
    }

    private suspend fun logDatabaseInfo() = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.readableDatabase

            // Log database version
            val version = db.version
            Log.d("HomePage", "Database version: $version")

            // Log tables
            val cursor = db.query("SELECT name FROM sqlite_master WHERE type='table'")
            val tables = mutableListOf<String>()
            while (cursor.moveToNext()) {
                tables.add(cursor.getString(0) ?: "unknown_table")
            }
            cursor.close()
            Log.d("HomePage", "Database tables: ${tables.joinToString()}")

            // Only proceed if notes table exists
            if (tables.contains("notes")) {
                // Log notes table schema
                val notesCursor = db.query("PRAGMA table_info(notes)")
                val columns = mutableListOf<String>()
                while (notesCursor.moveToNext()) {
                    val name = notesCursor.getString(1) ?: "unknown"
                    val type = notesCursor.getString(2) ?: "unknown"
                    columns.add("$name ($type)")
                }
                notesCursor.close()
                Log.d("HomePage", "Notes table columns: ${columns.joinToString()}")

                // Log number of notes per user
                val userNotesCursor = db.query(
                    "SELECT userId, COUNT(*) as count FROM notes GROUP BY userId"
                )
                while (userNotesCursor.moveToNext()) {
                    val userId = userNotesCursor.getString(0) ?: "null"
                    val count = userNotesCursor.getInt(1)
                    Log.d("HomePage", "User $userId has $count notes")
                }
                userNotesCursor.close()

                // Log first few notes for each user
                val sampleCursor = db.query(
                    "SELECT id, title, userId FROM notes ORDER BY id DESC LIMIT 10"
                )
                while (sampleCursor.moveToNext()) {
                    val id = sampleCursor.getLong(0)
                    val title = sampleCursor.getString(1) ?: "null"
                    val userId = sampleCursor.getString(2) ?: "null"
                    Log.d(
                        "HomePage",
                        "Sample note - ID: $id, Title: '$title', UserID: '$userId'"
                    )
                }
                sampleCursor.close()
            } else {
                Log.d("HomePage", "Notes table does not exist yet")
            }
        } catch (e: Exception) {
            Log.e("HomePage", "Error logging database info", e)
        }
    }
}
