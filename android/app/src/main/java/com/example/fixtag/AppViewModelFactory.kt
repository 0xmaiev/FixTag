package com.example.fixtag

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fixtag.data.Repository
import com.example.fixtag.ui.viewmodels.IncidentsViewModel
import com.example.fixtag.ui.viewmodels.MachinesViewModel
import com.example.fixtag.ui.login.LoginViewModel
import com.example.fixtag.ui.viewmodels.OrganizationsViewModel

class AppViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel(repository) as T
            }
            modelClass.isAssignableFrom(MachinesViewModel::class.java) -> {
                MachinesViewModel(repository) as T
            }
            modelClass.isAssignableFrom(IncidentsViewModel::class.java) -> {
                IncidentsViewModel(repository) as T
            }
            modelClass.isAssignableFrom(OrganizationsViewModel::class.java) -> {
                OrganizationsViewModel(repository) as T
            }
            // Add any future ViewModels here if they also need the AppRepository
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}