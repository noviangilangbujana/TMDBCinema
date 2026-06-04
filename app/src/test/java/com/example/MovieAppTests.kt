package com.example

import com.example.data.remote.*
import com.example.data.repository.MovieRepositoryImpl
import com.example.domain.model.Genre
import com.example.domain.model.Movie
import com.example.ui.viewmodel.DiscoverUiState
import com.example.ui.viewmodel.DiscoverViewModel
import com.example.ui.viewmodel.GenreUiState
import com.example.ui.viewmodel.GenreViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MovieAppTests {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ==========================================
    // 1. DATA/REMOTE LAYER TEST: MAPPING LOGIC
    // ==========================================
    @Test
    fun movieRepository_getGenres_success_mapsToDomain() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getGenres()

        assertTrue(result.isSuccess)
        val list = result.getOrThrow()
        assertEquals(2, list.size)
        assertEquals("Action", list[0].name)
        assertEquals(28, list[0].id)
    }

    @Test
    fun movieRepository_getGenres_failure_returnsException() = runTest {
        val fakeApi = FakeMovieApi(shouldThrow = true)
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.getGenres()

        assertTrue(result.isFailure)
        assertEquals("API Error simulated", result.exceptionOrNull()?.message)
    }

    @Test
    fun movieRepository_discoverMovies_success_mapsToDomain() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)

        val result = repository.discoverMovies(genreId = 28, page = 1)

        assertTrue(result.isSuccess)
        val movies = result.getOrThrow()
        assertEquals(1, movies.size)
        assertEquals("Fake Movie", movies[0].title)
        assertEquals(8.5, movies[0].voteAverage, 0.01)
    }

    // ==========================================
    // 2. VIEWMODEL LAYER TEST: GENRE VIEWMODEL
    // ==========================================
    @Test
    fun genreViewModel_onInit_success_emitsSuccessState() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)
        val viewModel = GenreViewModel(repository, isApiKeyValid = true)

        // Trigger coroutines
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GenreUiState.Success)
        val genres = (state as GenreUiState.Success).genres
        assertEquals(2, genres.size)
    }

    @Test
    fun genreViewModel_onInit_missingKey_emitsErrorState() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)
        val viewModel = GenreViewModel(repository, isApiKeyValid = false)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state is GenreUiState.Error)
        assertTrue((state as GenreUiState.Error).message.contains("Not set or invalid", ignoreCase = true))
    }

    // ==========================================
    // 3. VIEWMODEL LAYER TEST: DISCOVER VIEWMODEL (PAGINATION)
    // ==========================================
    @Test
    fun discoverViewModel_onInit_loadsFirstPage() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)
        val viewModel = DiscoverViewModel(repository, isApiKeyValid = true, genreId = 28)

        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.movies.size)
        assertEquals(2, state.currentPage) // Points to page to load next (1 + 1)
        assertFalse(state.isLoading)
        assertFalse(state.isPaginating)
    }

    @Test
    fun discoverViewModel_endlessScrolling_paginatesCorrectly() = runTest {
        val fakeApi = FakeMovieApi()
        val repository = MovieRepositoryImpl(fakeApi)
        val viewModel = DiscoverViewModel(repository, isApiKeyValid = true, genreId = 28)

        testScheduler.advanceUntilIdle()

        // Page 1 is loaded. Load page 2
        viewModel.loadMovies()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.movies.size) // Page 1 film + Page 2 film
        assertEquals(3, state.currentPage) // Points to next page (2 + 1)
    }
}

// ==========================================
// TEST UTILITIES, DUMMY DTOS AND FAKES
// ==========================================
class FakeMovieApi(private val shouldThrow: Boolean = false) : MovieApi {

    override suspend fun getGenres(): GenreListResponse {
        if (shouldThrow) throw Exception("API Error simulated")
        return GenreListResponse(
            genres = listOf(
                GenreDto(id = 28, name = "Action"),
                GenreDto(id = 35, name = "Comedy")
            )
        )
    }

    override suspend fun discoverMovies(genreIds: String, page: Int): MovieListResponse {
        if (shouldThrow) throw Exception("API Error simulated")
        return MovieListResponse(
            page = page,
            results = listOf(
                MovieDto(
                    id = 100 + page,
                    title = "Fake Movie",
                    overview = "Mock overview",
                    posterPath = "/image.jpg",
                    backdropPath = "/backdrop.jpg",
                    releaseDate = "2026-06-04",
                    voteAverage = 8.5,
                    voteCount = 120
                )
            ),
            totalPages = 5,
            totalResults = 50
        )
    }

    override suspend fun getMovieDetails(movieId: Int): MovieDto {
        if (shouldThrow) throw Exception("API Error simulated")
        return MovieDto(
            id = movieId,
            title = "Fake Detail Movie",
            overview = "Detail Overview",
            posterPath = "/img.jpg",
            backdropPath = "/back.jpg",
            releaseDate = "2026-06-04",
            voteAverage = 9.0,
            voteCount = 200
        )
    }

    override suspend fun getMovieReviews(movieId: Int, page: Int): ReviewListResponse {
        if (shouldThrow) throw Exception("API Error simulated")
        return ReviewListResponse(
            id = movieId,
            page = page,
            results = listOf(
                ReviewDto(
                    id = "rev_1",
                    author = "John Doe",
                    content = "Fantastic film!",
                    createdAt = "2026-06-04",
                    authorDetails = AuthorDetailsDto(rating = 10.0)
                )
            ),
            totalPages = 2,
            totalResults = 2
        )
    }

    override suspend fun getMovieVideos(movieId: Int): VideoListResponse {
        if (shouldThrow) throw Exception("API Error simulated")
        return VideoListResponse(
            id = movieId,
            results = listOf(
                VideoDto(
                    id = "v_1",
                    key = "12345",
                    name = "Official Trailer",
                    site = "YouTube",
                    type = "Trailer",
                    official = true
                )
            )
        )
    }
}
