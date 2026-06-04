package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.MovieApplication
import com.example.databinding.FragmentDetailBinding
import com.example.ui.viewmodel.DetailViewModel
import kotlinx.coroutines.launch

class MovieDetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DetailViewModel
    private lateinit var reviewsAdapter: ReviewsAdapter

    private var movieId: Int = 0

    companion object {
        private const val ARG_MOVIE_ID = "movie_id"

        fun newInstance(movieId: Int): MovieDetailFragment {
            val fragment = MovieDetailFragment()
            val args = Bundle().apply {
                putInt(ARG_MOVIE_ID, movieId)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            movieId = it.getInt(ARG_MOVIE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Back Click
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Fetch application container
        val appContainer = (requireContext().applicationContext as MovieApplication).container

        // Instantiate ViewModel
        val factory = DetailViewModel.Factory(
            repository = appContainer.movieRepository,
            isApiKeyValid = appContainer.isApiKeyValid,
            movieId = movieId
        )
        viewModel = ViewModelProvider(this, factory)[DetailViewModel::class.java]

        // Setup Reviews RecyclerView
        reviewsAdapter = ReviewsAdapter(emptyList())
        binding.reviewsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.reviewsRecyclerView.adapter = reviewsAdapter

        // Set up Flow/State Collection
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // 1. Movie details loading state
                    if (state.movie != null) {
                        val movie = state.movie
                        binding.toolbarTitle.text = movie.title
                        binding.movieDetailTitle.text = movie.title
                        binding.movieDetailRating.text = String.format("%.1f", movie.voteAverage)
                        binding.reviewsCount.text = " (${movie.voteCount} reviews)"
                        binding.movieDetailRelease.text = "Released: ${movie.releaseDate}"
                        binding.movieSynopsis.text = movie.overview.ifBlank { "No synopsis available for this film." }

                        // Load images via Coil
                        if (movie.fullBackdropUrl != null) {
                            binding.movieBackdrop.load(movie.fullBackdropUrl) {
                                crossfade(true)
                            }
                        }
                        if (movie.fullPosterUrl != null) {
                            binding.moviePoster.load(movie.fullPosterUrl) {
                                crossfade(true)
                            }
                        }
                    }

                    // 2. Playable Video details state
                    val trailerKey = state.trailerKey
                    if (!trailerKey.isNullOrBlank()) {
                        binding.trailerRatioContainer.visibility = View.VISIBLE
                        binding.watchYoutubeButton.visibility = View.VISIBLE
                        binding.trailerPlaceholder.visibility = View.GONE

                        // Load WebView if not already done
                        binding.trailerWebview.settings.javaScriptEnabled = true
                        binding.trailerWebview.settings.mediaPlaybackRequiresUserGesture = false
                        binding.trailerWebview.webChromeClient = WebChromeClient()
                        binding.trailerWebview.webViewClient = WebViewClient()
                        
                        val html = """
                            <html>
                            <body style="margin:0;padding:0;background-color:black;">
                            <iframe width="100%" height="100%" src="https://www.youtube.com/embed/$trailerKey?autoplay=0&rel=0" frameborder="0" allowfullscreen></iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        binding.trailerWebview.loadData(html, "text/html", "utf-8")

                        // Configure Watch on YouTube Button
                        binding.watchYoutubeButton.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$trailerKey"))
                            startActivity(intent)
                        }
                    } else {
                        binding.trailerRatioContainer.visibility = View.GONE
                        binding.watchYoutubeButton.visibility = View.GONE
                        binding.trailerPlaceholder.visibility = View.VISIBLE
                    }

                    // 3. User reviews state
                    if (state.isReviewsLoading) {
                        binding.reviewsLoader.visibility = View.VISIBLE
                        binding.reviewsRecyclerView.visibility = View.GONE
                        binding.noReviewsPlaceholder.visibility = View.GONE
                    } else {
                        binding.reviewsLoader.visibility = View.GONE
                        if (state.reviews.isEmpty()) {
                            binding.reviewsRecyclerView.visibility = View.GONE
                            binding.noReviewsPlaceholder.visibility = View.VISIBLE
                        } else {
                            binding.reviewsRecyclerView.visibility = View.VISIBLE
                            binding.noReviewsPlaceholder.visibility = View.GONE
                            reviewsAdapter.updateData(state.reviews)
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
