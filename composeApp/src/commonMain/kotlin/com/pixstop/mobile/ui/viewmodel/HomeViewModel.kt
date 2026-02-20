package com.pixstop.mobile.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.pixstop.mobile.data.model.User
import com.pixstop.mobile.data.repository.AuthRepository
import com.pixstop.mobile.data.repository.Result

/**
 * Estado da tela Home
 */
data class HomeUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isOfflineMode: Boolean = false,
    val isLoggedOut: Boolean = false,
    val error: String? = null
)

/**
 * ViewModel para a tela Home
 */
class HomeViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            when (val result = authRepository.fetchProfile()) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = result.data,
                        isOfflineMode = false
                    )
                }
                is Result.Error -> {
                    // Tenta usar cache
                    val cachedUser = authRepository.getCachedUser()
                    if (cachedUser != null) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            user = cachedUser,
                            isOfflineMode = true
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message
                        )
                    }
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            authRepository.logout()

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                isLoggedOut = true,
                user = null
            )
        }
    }

    fun refreshProfile() {
        loadUserData()
    }
}
