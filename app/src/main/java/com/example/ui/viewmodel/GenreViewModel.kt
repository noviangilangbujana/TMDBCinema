package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Genre
import com.example.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GenreUiState {
    object Loading : GenreUiState
    data class Success(val genres: List<Genre>) : GenreUiState
    data class Error(val message: String) : GenreUiState
}

class GenreViewModel(
    private val repository: MovieRepository,
    private val isApiKeyValid: Boolean
) : ViewModel() {

    private val _uiState = MutableStateFlow<GenreUiState>(GenreUiState.Loading)
    val uiState: StateFlow<GenreUiState> = _uiState.asStateFlow()

    init {
        loadGenres()
    }

    fun loadGenres() {
        if (!isApiKeyValid) {
            _uiState.value = GenreUiState.Error("TMDb API Key is not set or invalid. Please configure TMDB_API_KEY in the AI Studio Secrets panel.")
            return
        }

        viewModelScope.launch {
            _uiState.value = GenreUiState.Loading
            repository.getGenres()
                .onSuccess { genres ->
                    if (genres.isEmpty()) {
                        _uiState.value = GenreUiState.Error("No genres returned from TMDb API.")
                    } else {
                        _uiState.value = GenreUiState.Success(genres)
                    }
                }
                .onFailure { error ->
                    _uiState.value = GenreUiState.Error(error.localizedMessage ?: "Failed to load genres. Please check your connection or API Key.")
                }
        }
    }

    class Factory(
        private val repository: MovieRepository,
        private val isApiKeyValid: Boolean
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return GenreViewModel(repository, isApiKeyValid) as T
        }
    }
}
