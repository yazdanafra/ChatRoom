package com.g2.chatroom.feature.settings

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow<SettingsState>(SettingsState.Nothing)
    val state = _state.asStateFlow()

    var userName = mutableStateOf(FirebaseAuth.getInstance().currentUser?.displayName ?: "")
        private set

    fun updateProfile(name: String) {
        _state.value = SettingsState.Loading
        val user = FirebaseAuth.getInstance().currentUser
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        user?.updateProfile(profileUpdates)
            ?.addOnCompleteListener {
                if (it.isSuccessful) {
                    userName.value = name
                    _state.value = SettingsState.Success
                } else {
                    _state.value = SettingsState.Error
                }
            }
    }

    fun updatePassword(password: String) {
        _state.value = SettingsState.Loading
        FirebaseAuth.getInstance().currentUser?.updatePassword(password)
            ?.addOnCompleteListener {
                _state.value = if (it.isSuccessful) SettingsState.Success else SettingsState.Error
            }
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()
    }
}

sealed class SettingsState {
    object Nothing : SettingsState()
    object Loading : SettingsState()
    object Success : SettingsState()
    object Error : SettingsState()
}
