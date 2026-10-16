package com.example.fixtag.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.fixtag.data.Repository
import com.example.fixtag.ui.viewmodels.IncidentDetailViewModel
import com.example.fixtag.ui.viewmodels.MachineDetailViewModel
import com.example.fixtag.ui.viewmodels.NewIncidentViewModel
import com.example.fixtag.ui.viewmodels.NewMachineViewModel
import com.example.fixtag.ui.viewmodels.NewOrganizationViewModel
import com.example.fixtag.ui.viewmodels.OrganizationsViewModel

class ItemViewModelFactory(
    private val repository: Repository,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        return when {
            modelClass.isAssignableFrom(IncidentDetailViewModel::class.java) -> {
                IncidentDetailViewModel(repository, extras.createSavedStateHandle()) as T
            }
            modelClass.isAssignableFrom(NewIncidentViewModel::class.java) -> {
                NewIncidentViewModel(repository, extras.createSavedStateHandle()) as T
            }
            modelClass.isAssignableFrom(MachineDetailViewModel::class.java) -> {
                MachineDetailViewModel(repository, extras.createSavedStateHandle()) as T
            }
            modelClass.isAssignableFrom(NewMachineViewModel::class.java) -> {
                NewMachineViewModel(repository, extras.createSavedStateHandle()) as T
            }
            modelClass.isAssignableFrom(NewOrganizationViewModel::class.java) -> {
                NewOrganizationViewModel(repository, extras.createSavedStateHandle()) as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}