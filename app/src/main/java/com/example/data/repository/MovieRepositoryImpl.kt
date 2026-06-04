package com.example.data.repository

import com.example.data.remote.MovieApi
import com.example.domain.model.Genre
import com.example.domain.model.Movie
import com.example.domain.model.Review
import com.example.domain.model.Video
import com.example.domain.repository.MovieRepository

class MovieRepositoryImpl(
    private val api: MovieApi
) : MovieRepository {

    override suspend fun getGenres(): Result<List<Genre>> {
        return runCatching {
            api.getGenres().genres.map {
                Genre(
                    id = it.id,
                    name = it.name
                )
            }
        }
    }

    override suspend fun discoverMovies(genreId: Int, page: Int): Result<List<Movie>> {
        return runCatching {
            api.discoverMovies(genreIds = genreId.toString(), page = page).results.map {
                Movie(
                    id = it.id,
                    title = it.title,
                    overview = it.overview ?: "",
                    posterPath = it.posterPath,
                    backdropPath = it.backdropPath,
                    releaseDate = it.releaseDate ?: "",
                    voteAverage = it.voteAverage ?: 0.0,
                    voteCount = it.voteCount ?: 0
                )
            }
        }
    }

    override suspend fun getMovieDetails(movieId: Int): Result<Movie> {
        return runCatching {
            val it = api.getMovieDetails(movieId)
            Movie(
                id = it.id,
                title = it.title,
                overview = it.overview ?: "",
                posterPath = it.posterPath,
                backdropPath = it.backdropPath,
                releaseDate = it.releaseDate ?: "",
                voteAverage = it.voteAverage ?: 0.0,
                voteCount = it.voteCount ?: 0
            )
        }
    }

    override suspend fun getMovieReviews(movieId: Int, page: Int): Result<List<Review>> {
        return runCatching {
            api.getMovieReviews(movieId, page).results.map {
                Review(
                    id = it.id,
                    author = it.author,
                    content = it.content,
                    createdAt = it.createdAt ?: "",
                    rating = it.authorDetails?.rating
                )
            }
        }
    }

    override suspend fun getMovieVideos(movieId: Int): Result<List<Video>> {
        return runCatching {
            api.getMovieVideos(movieId).results.map {
                Video(
                    id = it.id,
                    key = it.key,
                    name = it.name,
                    site = it.site,
                    type = it.type,
                    official = it.official ?: false
                )
            }
        }
    }
}
