package com.example.fixtag.ui.main

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.Result
import com.example.fixtag.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: Repository) : ViewModel() {

    // Sealed class representing the possible navigation states
    sealed class NavigationState {
        object Loading : NavigationState()
        object GoToLogin : NavigationState()
        object GoToMachines : NavigationState()
        object GoToIncidents : NavigationState()
    }

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _machinesResult = MutableLiveData<MachinesResult>()
    val machinesResult: LiveData<MachinesResult> = _machinesResult

    private val _incidentsResult = MutableLiveData<IncidentsResult>()
    val incidentsResult: LiveData<IncidentsResult> = _incidentsResult

    init {
        checkUserStatus()
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            val user = repository.restoreSession()
            if (user != null) {
                Log.d("MainViewModel", "Found authentication details for ${user.role} ${user.name}")
                // operators route to machines view
                if (user.role == UserRole.Operator) {
                    _navigationState.value = NavigationState.GoToMachines
                } else {
                    // technicians and assistance route to incidents view
                    _navigationState.value = NavigationState.GoToIncidents
                }
                getMachinesInfo()
            } else {
                Log.d("MainViewModel", "Could not find authentication details")
                _navigationState.value = NavigationState.GoToLogin
            }
        }
    }

    private fun getMachinesInfo() {
        viewModelScope.launch {
            val result = repository.refreshMachines()
            if (result is Result.Success) {
                Log.d("MainViewModel", "Successfully fetched ${result.data.size} machines")
                _machinesResult.value = MachinesResult(success = result.data)
            } else {
                Log.d("MainViewModel", "Failed to fetch machines")
                _machinesResult.value = MachinesResult(error = R.string.machines_failed)
            }
        }
        getIncidents()
    }

    private fun getIncidents() {
        viewModelScope.launch {
            val result = repository.getIncidents()
            if (result is Result.Success) {
                Log.d("MainViewModel", "Successfully fetched ${result.data.size} incidents")
                _incidentsResult.value = IncidentsResult(success = result.data)
            } else {
                Log.d("MainViewModel", "Failed to fetch incidents")
                _incidentsResult.value = IncidentsResult(error = R.string.incidents_failed)
            }
        }
    }
}