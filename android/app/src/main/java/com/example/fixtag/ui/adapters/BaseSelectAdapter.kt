package com.example.fixtag.ui.adapters

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

abstract class BaseSelectAdapter<T, VH : RecyclerView.ViewHolder>(
    diffCallback: DiffUtil.ItemCallback<T>,
    private val itemIdProvider: (T) -> Any, // Extract unique ID from any data type
    private val onItemSelected: (T?) -> Unit // Callback to notify your Dialog/Fragment
) : ListAdapter<T, VH>(diffCallback) {

    private var selectedItemId: Any? = null

    // Call this if you want to set an initial selection when data loads
    fun setInitialSelection(id: Any?) {
        selectedItemId = id
    }

    // Helper for your ViewHolders to check state
    protected fun isItemSelected(item: T): Boolean {
        return itemIdProvider(item) == selectedItemId
    }

    // Call this inside your concrete ViewHolders' click listeners
    protected fun handleItemClick(item: T) {
        val clickedId = itemIdProvider(item)
        val previousSelectedId = selectedItemId

        if (selectedItemId == clickedId) {
            selectedItemId = null // Optional: deselect if clicked again
            onItemSelected(null)
        } else {
            selectedItemId = clickedId
            onItemSelected(item)
        }

        // Find and refresh stream items safely by their IDs
        val prevPosition = currentList.indexOfFirst { itemIdProvider(it) == previousSelectedId }
        val currentPosition = currentList.indexOfFirst { itemIdProvider(it) == clickedId }

        if (prevPosition != -1) notifyItemChanged(prevPosition)
        if (currentPosition != -1) notifyItemChanged(currentPosition)
    }
}