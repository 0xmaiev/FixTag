package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

// Base generic abstract class for data creation results
abstract class CreateResult<T>(
    val success: T? = null,
    val error: Int? = null
)

abstract class InputBaseViewModel(): ViewModel() {
    sealed interface State { data object Editing : State; data object Saving : State; data object Saved : State }

    val state = MutableStateFlow<State>(State.Editing)
    val uiState: StateFlow<State> = state.asStateFlow()
}