package com.example.digi_diary.ui

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Item decoration that adds spacing between items in the RecyclerView.
 * @param spacing The spacing in pixels to be applied between items
 */
class NoteItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        
        val position = parent.getChildAdapterPosition(view)
        val itemCount = parent.adapter?.itemCount ?: 0
        
        // Skip if this is a header or footer view
        if (position == RecyclerView.NO_POSITION) return
        
        // Add top margin only for the first item to avoid double spacing between items
        if (position == 0) {
            outRect.top = spacing
        }
        
        // Add bottom margin for all items
        outRect.bottom = spacing
        
        // Add left and right margins
        outRect.left = spacing
        outRect.right = spacing
        
        // Log the decoration for debugging
        if (android.util.Log.isLoggable("NoteItemDecoration", android.util.Log.DEBUG)) {
            android.util.Log.d(
                "NoteItemDecoration",
                "Item $position - top: ${outRect.top}, bottom: ${outRect.bottom}, " +
                "left: ${outRect.left}, right: ${outRect.right}"
            )
        }
    }
}
