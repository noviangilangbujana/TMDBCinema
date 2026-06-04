package com.example.domain.repository

import com.example.domain.model.Genre
import com.example.domain.model.Movie
import com.example.domain.model.Review
import com.example.domain.model.Video

interface MovieRepository {
    suspend fun getGenres(): Result<List<Genre>>
    
    suspend fun discoverMovies(genreId: Int, page: Int): Result<List<Movie>>
    
    suspend fun getMovieDetails(movieId: Int): Result<Movie>
    
    suspend fun getMovieReviews(movieId: Int, page: Int): Result<List<Review>>
    
    suspend fun getMovieVideos(movieId: Int): Result<List<Video>>
}
