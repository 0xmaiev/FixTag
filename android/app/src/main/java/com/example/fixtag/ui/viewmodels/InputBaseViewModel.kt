package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.MACHINE_ID_KEY
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

// Base generic abstract class for data creation results
abstract class CreateResult<T>(
    val success: T? = null,
    val error: Int? = null
)

// A form's errors, equal only to itself. StateFlow skips a value equal to the one it holds, and
// typing clears an error from its field, so a second submit with the same mistake would show nothing
abstract class FormState {
    final override fun equals(other: Any?) = this === other
    final override fun hashCode() = System.identityHashCode(this)
}

abstract class InputBaseViewModel(): ViewModel() {
    sealed interface State { data object Editing : State; data object Saving : State; data object Saved : State }

    val state = MutableStateFlow<State>(State.Editing)
    val uiState: StateFlow<State> = state.asStateFlow()

    // errors that belong to no field, for a toast. A Channel hands each one over once, where a
    // StateFlow would show it again after every rotation
    protected val failures = Channel<Int>(Channel.BUFFERED)
    val errors: Flow<Int> = failures.receiveAsFlow()
}