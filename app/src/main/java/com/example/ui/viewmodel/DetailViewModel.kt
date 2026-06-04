package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Movie
import com.example.domain.model.Review
import com.example.domain.model.Video
import com.example.domain.repository.MovieRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailUiState(
    val movie: Movie? = null,
    val isMovieLoading: Boolean = false,
    val movieError: String? = null,
    
    val trailerKey: String? = null,
    val videos: List<Video> = emptyList(),
    
    val reviews: List<Review> = emptyList(),
    val isReviewsLoading: Boolean = false,
    val isReviewsPaginating: Boolean = false,
    val reviewsPage: Int = 1,
    val endOfReviewsPageReached: Boolean = false,
    val reviewsError: String? = null
)

class DetailViewModel(
    private val repository: MovieRepository,
    private val isApiKeyValid: Boolean,
    private val movieId: Int
) : ViewModel() {

    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        loadMovieDetails()
        loadVideos()
        loadReviews(reset = true)
    }

    fun loadMovieDetails() {
        if (!isApiKeyValid) {
            _uiState.update { it.copy(movieError = "API Key error.") }
            return
        }

        _uiState.update { it.copy(isMovieLoading = true, movieError = null) }

        viewModelScope.launch {
            repository.getMovieDetails(movieId)
                .onSuccess { details ->
                    _uiState.update { it.copy(movie = details, isMovieLoading = false) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isMovieLoading = false, movieError = error.localizedMessage ?: "Failed to get movie details.") }
                }
        }
    }

    fun loadVideos() {
        if (!isApiKeyValid) return

        viewModelScope.launch {
            repository.getMovieVideos(movieId)
                .onSuccess { videoList ->
                    val trailer = videoList.firstOrNull { it.isYoutubeTrailer }
                        ?: videoList.firstOrNull { it.site.equals("YouTube", ignoreCase = true) }
                    
                    _uiState.update { current ->
                        current.copy(
                            videos = videoList,
                            trailerKey = trailer?.key
                        )
                    }
                }
                .onFailure {
                    // Ignore video errors silently in UI or keep empty trailer key
                }
        }
    }

    fun loadReviews(reset: Boolean = false) {
        if (!isApiKeyValid) return

        val pageToLoad = if (reset) 1 else _uiState.value.reviewsPage

        if (!reset && (_uiState.value.isReviewsLoading || _uiState.value.isReviewsPaginating || _uiState.value.endOfReviewsPageReached)) {
            return
        }

        _uiState.update {
            if (reset) {
                it.copy(isReviewsLoading = true, reviewsError = null, reviewsPage = 1, reviews = emptyList(), endOfReviewsPageReached = false)
            } else {
                it.copy(isReviewsPaginating = true)
            }
        }

        viewModelScope.launch {
            repository.getMovieReviews(movieId, pageToLoad)
                .onSuccess { newReviews ->
                    _uiState.update { current ->
                        val updatedList = if (reset) newReviews else current.reviews + newReviews
                        current.copy(
                            reviews = updatedList,
                            isReviewsLoading = false,
                            isReviewsPaginating = false,
                            reviewsPage = pageToLoad + 1,
                            endOfReviewsPageReached = newReviews.isEmpty()
                        )
                    }
                }
                .onFailure { failure ->
                    _uiState.update { current ->
                        current.copy(
                            isReviewsLoading = false,
                            isReviewsPaginating = false,
                            reviewsError = failure.localizedMessage ?: "Error loading reviews."
                        )
                    }
                }
        }
    }

    class Factory(
        private val repository: MovieRepository,
        private val isApiKeyValid: Boolean,
        private val movieId: Int
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DetailViewModel(repository, isApiKeyValid, movieId) as T
        }
    }
}
