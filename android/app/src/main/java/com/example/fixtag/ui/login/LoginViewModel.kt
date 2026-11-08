package com.example.fixtag.ui.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// LoginActivity's, shared with its fragments: it alone decides where to go once someone is signed in
class LoginViewModel(private val repository: Repository) : ViewModel() {

    // Sealed class representing the possible navigation states
    sealed class NavigationState {
        object Loading : NavigationState()
        object GoToLogin : NavigationState()
        object GoToPasswordChange : NavigationState()
        object GoToMain : NavigationState()
    }

    private val _navigationState = MutableStateFlow<NavigationState>(NavigationState.Loading)
    val navigationState: StateFlow<NavigationState> = _navigationState.asStateFlow()

    init {
        viewModelScope.launch {
            // restoreSession(), not repository.user: after process death Android can bring this
            // activity back by itself, before anything has read the session
            val user = repository.restoreSession()
            if (user == null) Log.d("LoginViewModel", "Could not find authentication details")
            show(user)
        }
    }

    // LoginFragment calls this after /login, PasswordChangeFragment after /me/password
    fun signedIn(user: User) = show(user)

    private fun show(user: User?) {
        _navigationState.value = when {
            user == null -> NavigationState.GoToLogin
            user.mustChangePassword -> NavigationState.GoToPasswordChange
            else -> NavigationState.GoToMain
        }
    }
}