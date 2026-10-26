package com.example.fixtag.ui.main


import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fixtag.data.Repository
import com.example.fixtag.data.SyncScheduler
import com.example.fixtag.data.model.AuthDao

/**
 * ViewModel provider factory to instantiate MainViewModel.
 * Required given MainViewModel has a non-empty constructor
 */
class MainViewModelFactory(
    private val repository: Repository,
    private val syncScheduler: SyncScheduler
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(repository, syncScheduler) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}