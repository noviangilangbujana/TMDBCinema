package com.example.domain.model

data class Genre(
    val id: Int,
    val name: String
)

data class Movie(
    val id: Int,
    val title: String,
    val overview: String,
    val posterPath: String?,
    val backdropPath: String?,
    val releaseDate: String,
    val voteAverage: Double,
    val voteCount: Int
) {
    val fullPosterUrl: String?
        get() = if (posterPath != null) "https://image.tmdb.org/t/p/w500$posterPath" else null

    val fullBackdropUrl: String?
        get() = if (backdropPath != null) "https://image.tmdb.org/t/p/w780$backdropPath" else null
}

data class Review(
    val id: String,
    val author: String,
    val content: String,
    val createdAt: String,
    val rating: Double?
)

data class Video(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val official: Boolean
) {
    val isYoutubeTrailer: Boolean
        get() = site.equals("YouTube", ignoreCase = true) && type.equals("Trailer", ignoreCase = true)
}
