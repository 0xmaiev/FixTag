package com.example.fixtag.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.fixtag.R
import com.example.fixtag.data.Repository
import com.example.fixtag.data.model.Organization
import com.example.fixtag.data.model.User
import com.example.fixtag.ui.ORGANIZATION_ID_HANDLE
import com.example.fixtag.ui.ORGANIZATION_ID_KEY
import com.example.fixtag.ui.ORGANIZATION_NAME_HANDLE
import com.example.fixtag.ui.login.LoginFormState
import com.example.fixtag.ui.login.LoginResult
import kotlinx.coroutines.launch
import kotlin.uuid.Uuid

data class NewOrganizationFormState(
    val nameError: Int? = null,
    val isDataValid: Boolean = false
)

data class NewOrganizationFormResult(
    val success: Organization? = null,
    val error: Int? = null
)

class NewOrganizationViewModel(
    private val repository: Repository,
    private val savedStateHandle: SavedStateHandle
): InputBaseViewModel() {
    // one id per form, kept across rotation and process death
    private val organizationId: String = savedStateHandle[ORGANIZATION_ID_HANDLE]
        ?: Uuid.random().toString().also { savedStateHandle[ORGANIZATION_ID_HANDLE] = it }

    private val _form = MutableLiveData<NewOrganizationFormState>()
    val form: LiveData<NewOrganizationFormState> = _form

    private val _result = MutableLiveData<NewOrganizationFormResult>()
    val result: LiveData<NewOrganizationFormResult> = _result

    private var _name: MutableLiveData<String?> = savedStateHandle.getLiveData(ORGANIZATION_NAME_HANDLE, null)

    fun submit() {
        if (_name.value == null) return
        // State.* comes from `InputBaseViewModel`
        if (state.value != State.Editing) return // ignores double taps
        state.value = State.Saving
        viewModelScope.launch {
            val organization = Organization.create(
                Uuid.parse(organizationId),
                _name.value!!,
            )
            val saved = repository.createOrganization(organization)
            state.value = State.Saved
            _result.value = NewOrganizationFormResult(success = saved)
        }
    }

    fun formDataChanged(name: String) {
        // validate form data
        if (name.isEmpty()) {
            _form.value = NewOrganizationFormState(nameError = R.string.empty_name_error)
            return
        }
        // set value and update state
        _name.value = name
        _form.value = NewOrganizationFormState(isDataValid = true)
    }
}