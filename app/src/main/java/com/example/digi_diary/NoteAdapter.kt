package com.example.digi_diary

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.digi_diary.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private var notes: List<Note> = emptyList(),
    private val onNoteClick: (Note) -> Unit = {},
    private val onDeleteClick: (Note) -> Unit = {}
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
    private val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    
    init {
        android.util.Log.d("NoteAdapter", "Adapter created with ${notes.size} notes")
        setHasStableIds(true)
        Log.d("NoteAdapter", "Stable IDs enabled: ${hasStableIds()}")
    }
    
    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById<TextView>(R.id.noteTitleText).apply {
            Log.d("NoteAdapter", "🔍 Found title view: $this")
        }
        private val contentText: TextView = itemView.findViewById<TextView>(R.id.noteContentText).apply {
            Log.d("NoteAdapter", "🔍 Found content view: $this")
        }
        private val dateText: TextView = itemView.findViewById<TextView>(R.id.noteDateText).apply {
            Log.d("NoteAdapter", "🔍 Found date view: $this")
        }
        private val editButton: View = itemView.findViewById<View>(R.id.editNoteButton).apply {
            Log.d("NoteAdapter", "🔍 Found edit button: $this")
        }
        private val deleteButton: View = itemView.findViewById<View>(R.id.deleteNoteButton).apply {
            Log.d("NoteAdapter", "🔍 Found delete button: $this")
        }

        fun bind(note: Note) {
            try {
                Log.d("NoteAdapter", "🔗 Binding note: ID=${note.id}, Title='${note.title.take(20)}...', Content length=${note.content.length}")
                
                // Log view hierarchy for debugging
                Log.d("NoteAdapter", "   Title view: ${titleText.javaClass.simpleName}@${Integer.toHexString(titleText.hashCode())}")
                Log.d("NoteAdapter", "   Content view: ${contentText.javaClass.simpleName}@${Integer.toHexString(contentText.hashCode())}")
                
                titleText.text = note.title.ifEmpty { "Untitled" }
                contentText.text = note.content
                
                val formattedDate = formatDate(note.date)
                dateText.text = formattedDate
                
                Log.d("NoteAdapter", "✅ Successfully bound - Title: '${titleText.text}', Date: $formattedDate")
                
                // Force layout to ensure views are measured
                itemView.post {
                    Log.d("NoteAdapter", "📏 Item dimensions - Width: ${itemView.width}px, Height: ${itemView.height}px")
                    Log.d("NoteAdapter", "   Title dimensions - Width: ${titleText.width}px, Height: ${titleText.height}px")
                }
            } catch (e: Exception) {
                Log.e("NoteAdapter", "❌ Error binding note ${note.id}: ${e.message}", e)
            }

            // Set click listener for the entire note item
            itemView.setOnClickListener {
                android.util.Log.d("NoteAdapter", "Note clicked: ${note.id}")
                onNoteClick(note)
            }
            
            // Set click listener for edit button
            editButton.setOnClickListener {
                android.util.Log.d("NoteAdapter", "Edit note clicked: ${note.id}")
                onNoteClick(note)
            }
            
            // Set click listener for delete button
            deleteButton.setOnClickListener {
                android.util.Log.d("NoteAdapter", "Delete note clicked: ${note.id}")
                onDeleteClick(note)
            }
        }
        
        private fun formatDate(date: Date): String {
            val now = Calendar.getInstance()
            val noteDate = Calendar.getInstance().apply { time = date }
            
            return when {
                isToday(now, noteDate) -> "Today, ${timeFormat.format(date)}"
                isYesterday(now, noteDate) -> "Yesterday, ${timeFormat.format(date)}"
                isThisWeek(now, noteDate) -> "${dayFormat.format(date)}, ${timeFormat.format(date)}"
                isThisYear(now, noteDate) -> "${dateFormat.format(date)}"
                else -> monthYearFormat.format(date)
            }
        }
        
        private fun isToday(now: Calendar, date: Calendar): Boolean {
            return now.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                   now.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
        }
        
        private fun isYesterday(now: Calendar, date: Calendar): Boolean {
            val yesterday = now.clone() as Calendar
            yesterday.add(Calendar.DAY_OF_YEAR, -1)
            return yesterday.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
                   yesterday.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
        }
        
        private fun isThisWeek(now: Calendar, date: Calendar): Boolean {
            val weekAgo = now.clone() as Calendar
            weekAgo.add(Calendar.DAY_OF_YEAR, -7)
            return !date.before(weekAgo) && date.before(now)
        }
        
        private fun isThisYear(now: Calendar, date: Calendar): Boolean {
            return now.get(Calendar.YEAR) == date.get(Calendar.YEAR)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        Log.d("NoteAdapter", "🆕 onCreateViewHolder called")
        val view = try {
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_note, parent, false)
        } catch (e: Exception) {
            Log.e("NoteAdapter", "❌ Error inflating item_note: ${e.message}")
            throw e
        }
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes.getOrNull(position)
        if (note == null) {
            Log.e("NoteAdapter", "❌ Note at position $position is null!")
            return
        }
        Log.d("NoteAdapter", "🔍 Binding note at position $position - ID: ${note.id}, Title: '${note.title}'")
        holder.bind(note)
    }

    override fun getItemCount(): Int {
        Log.d("NoteAdapter", "getItemCount() = ${notes.size}")
        return notes.size
    }
    
    override fun getItemId(position: Int): Long {
        return if (position in notes.indices) {
            val id = notes[position].id
            Log.d("NoteAdapter", "getItemId($position) = $id")
            id
        } else {
            Log.e("NoteAdapter", "❌ Invalid position $position for getItemId()")
            RecyclerView.NO_ID
        }
    }

    fun updateNotes(newNotes: List<Note>) {
        Log.d("NoteAdapter", "🔄 updateNotes() called with ${newNotes.size} notes")
        
        // Create a new list to ensure we don't have reference issues
        val updatedNotes = newNotes.sortedByDescending { it.date }
        
        // Check if the data has actually changed
        if (notes == updatedNotes) {
            Log.d("NoteAdapter", "No changes in notes, skipping update")
            return
        }
        
        // Log the differences
        val oldIds = notes.map { it.id }.toSet()
        val newIds = updatedNotes.map { it.id }.toSet()
        
        val added = newIds - oldIds
        val removed = oldIds - newIds
        val changed = updatedNotes.filter { note ->
            val oldNote = notes.find { it.id == note.id }
            oldNote != null && oldNote != note
        }
        
        Log.d("NoteAdapter", "📊 Changes - Added: ${added.size}, Removed: ${removed.size}, Changed: ${changed.size}")
        
        // Update the internal list
        notes = updatedNotes
        
        // Notify adapter of changes
        try {
            Log.d("NoteAdapter", "Notifying adapter of data changes...")
            
            // If the list was empty or is now empty, use notifyDataSetChanged for simplicity
            if (notes.isEmpty() || oldIds.isEmpty()) {
                Log.d("NoteAdapter", "Using notifyDataSetChanged()")
                notifyDataSetChanged()
            } else {
                // For small changes, use the appropriate notify methods
                if (added.isNotEmpty() || removed.isNotEmpty() || changed.isNotEmpty()) {
                    Log.d("NoteAdapter", "Using granular update notifications")
                    
                    // Calculate positions for added/removed items
                    val addedPositions = added.mapNotNull { id -> 
                        updatedNotes.indexOfFirst { it.id == id }.takeIf { it >= 0 }
                    }
                    val removedPositions = removed.mapNotNull { id ->
                        val oldIndex = notes.indexOfFirst { it.id == id }
                        if (oldIndex >= 0) oldIndex else null
                    }
                    
                    // Notify of changes
                    if (removedPositions.isNotEmpty()) {
                        removedPositions.sortedDescending().forEach { position ->
                            Log.d("NoteAdapter", "Removing item at position $position")
                            notifyItemRemoved(position)
                        }
                    }
                    
                    if (addedPositions.isNotEmpty()) {
                        addedPositions.sorted().forEach { position ->
                            Log.d("NoteAdapter", "Adding item at position $position")
                            notifyItemInserted(position)
                        }
                    }
                    
                    // Handle changed items
                    changed.forEach { updatedNote ->
                        val position = updatedNotes.indexOfFirst { it.id == updatedNote.id }
                        if (position >= 0) {
                            Log.d("NoteAdapter", "Updating item at position $position")
                            notifyItemChanged(position)
                        }
                    }
                } else {
                    // Fallback to full update if we can't determine changes
                    Log.d("NoteAdapter", "No changes detected, using notifyDataSetChanged")
                    notifyDataSetChanged()
                }
            }
            
            Log.d("NoteAdapter", "✅ Update complete. New item count: $itemCount")
            
        } catch (e: Exception) {
            Log.e("NoteAdapter", "❌ Error during update: ${e.message}", e)
            // Fallback to full update on error
            notifyDataSetChanged()
        }
    }
    
    private fun getItem(position: Int): Note? {
        return if (position in 0 until itemCount) {
            notes[position]
        } else {
            null
        }
    }
}
