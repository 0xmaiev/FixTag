package com.example.fixtag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fixtag.data.Repository
import com.example.fixtag.ui.fragments.IncidentDetailViewModel
import com.example.fixtag.ui.fragments.MachineDetailViewModel
import com.example.fixtag.ui.login.LoginViewModel
import kotlin.uuid.Uuid

class ItemViewModelFactory(
    private val repository: Repository,
    private val id: Uuid
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(IncidentDetailViewModel::class.java) -> {
                IncidentDetailViewModel(repository, id) as T
            }
            modelClass.isAssignableFrom(MachineDetailViewModel::class.java) -> {
                MachineDetailViewModel(repository, id) as T
            }
            // Add any future ViewModels here if they also need the AppRepository
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}