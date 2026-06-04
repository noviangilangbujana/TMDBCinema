package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Movie
import com.example.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiscoverUiState(
    val movies: List<Movie> = emptyList(),
    val isLoading: Boolean = false,
    val isPaginating: Boolean = false,
    val error: String? = null,
    val currentPage: Int = 1,
    val endOfPageReached: Boolean = false
)

class DiscoverViewModel(
    private val repository: MovieRepository,
    private val isApiKeyValid: Boolean,
    private val genreId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(DiscoverUiState())
    val uiState: StateFlow<DiscoverUiState> = _uiState.asStateFlow()

    init {
        loadMovies(reset = true)
    }

    fun loadMovies(reset: Boolean = false) {
        if (!isApiKeyValid) {
            _uiState.update { it.copy(error = "TMDb API Key is not configured. Please input TMDB_API_KEY in the Secrets panel.") }
            return
        }

        val pageToLoad = if (reset) 1 else _uiState.value.currentPage

        // Avoid double loads
        if (!reset && (_uiState.value.isLoading || _uiState.value.isPaginating || _uiState.value.endOfPageReached)) {
            return
        }

        _uiState.update { 
            if (reset) {
                it.copy(isLoading = true, error = null, currentPage = 1, movies = emptyList(), endOfPageReached = false)
            } else {
                it.copy(isPaginating = true)
            }
        }

        viewModelScope.launch {
            repository.discoverMovies(genreId, pageToLoad)
                .onSuccess { newMovies ->
                    _uiState.update { current ->
                        val updatedList = if (reset) newMovies else current.movies + newMovies
                        current.copy(
                            movies = updatedList,
                            isLoading = false,
                            isPaginating = false,
                            currentPage = pageToLoad + 1,
                            endOfPageReached = newMovies.isEmpty()
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            isPaginating = false,
                            error = failure.localizedMessage ?: "Failed to discover movies."
                        )
                    }
                }
        }
    }

    class Factory(
        private val repository: MovieRepository,
        private val isApiKeyValid: Boolean,
        private val genreId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DiscoverViewModel(repository, isApiKeyValid, genreId) as T
        }
    }
}
