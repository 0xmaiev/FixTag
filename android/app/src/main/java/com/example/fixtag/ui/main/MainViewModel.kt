package com.example.fixtag.ui.main

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.SyncScheduler
import com.example.fixtag.data.model.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

class MainViewModel(
    private val repository: Repository,
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    // Sealed class representing the possible navigation states
    sealed class NavigationState {
        object Loading : NavigationState()
        object GoToLogin : NavigationState()
        object GoToMachines : NavigationState()
        object GoToIncidents : NavigationState()
    }

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    private val _role = MutableStateFlow<UserRole?>(null)
    val role: StateFlow<UserRole?> = _role.asStateFlow()

    private val _organizationId = MutableStateFlow<Uuid?>(null)
    val organizationId: StateFlow<Uuid?> = _organizationId.asStateFlow()

    val pendingCount: StateFlow<Int> = repository.observePendingCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        checkUserStatus()
    }

    fun switchProfile() {
        viewModelScope.launch {
            repository.logout()
            _navigationState.value = NavigationState.GoToLogin
        }
    }

    private fun checkUserStatus() {
        viewModelScope.launch {
            val user = repository.restoreSession()
            if (user != null) {
                _role.value = user.role
                _organizationId.value = user.organizationId
                Log.d("MainViewModel", "Found authentication details for ${user.role} ${user.name}")
                // operators route to machines view
                if (user.role == UserRole.Operador) {
                    _navigationState.value = NavigationState.GoToMachines
                } else {
                    // technicians and assistance route to incidents view
                    _navigationState.value = NavigationState.GoToIncidents
                }
                syncScheduler.syncNow()
            } else {
                Log.d("MainViewModel", "Could not find authentication details")
                _navigationState.value = NavigationState.GoToLogin
            }
        }
    }
}