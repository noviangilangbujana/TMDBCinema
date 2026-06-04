package com.example.ui.screens

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.databinding.ItemMovieBinding
import com.example.domain.model.Movie

class MoviesAdapter(
    private var movies: List<Movie>,
    private val onMovieClick: (Movie) -> Unit
) : RecyclerView.Adapter<MoviesAdapter.MovieViewHolder>() {

    fun updateData(newMovies: List<Movie>) {
        movies = newMovies
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MovieViewHolder {
        val binding = ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MovieViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MovieViewHolder, position: Int) {
        holder.bind(movies[position])
    }

    override fun getItemCount(): Int = movies.size

    inner class MovieViewHolder(private val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(movie: Movie) {
            binding.movieTitle.text = movie.title
            
            val year = if (movie.releaseDate.length >= 4) movie.releaseDate.substring(0, 4) else "N/A"
            binding.movieYear.text = year
            binding.movieRating.text = String.format("%.1f", movie.voteAverage)

            if (movie.fullPosterUrl != null) {
                binding.moviePoster.visibility = View.VISIBLE
                binding.noPosterText.visibility = View.GONE
                binding.moviePoster.load(movie.fullPosterUrl) {
                    crossfade(true)
                }
            } else {
                binding.moviePoster.visibility = View.GONE
                binding.noPosterText.visibility = View.VISIBLE
            }

            binding.root.setOnClickListener { onMovieClick(movie) }
        }
    }
}
