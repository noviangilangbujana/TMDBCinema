package com.example.ui.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.MovieApplication
import com.example.databinding.FragmentDiscoverBinding
import com.example.ui.viewmodel.DiscoverViewModel
import kotlinx.coroutines.launch

class DiscoverFragment : Fragment() {

    private var _binding: FragmentDiscoverBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: DiscoverViewModel
    private lateinit var adapter: MoviesAdapter

    private var genreId: Int = 0
    private var genreName: String = ""

    companion object {
        private const val ARG_GENRE_ID = "genre_id"
        private const val ARG_GENRE_NAME = "genre_name"

        fun newInstance(genreId: Int, genreName: String): DiscoverFragment {
            val fragment = DiscoverFragment()
            val args = Bundle().apply {
                putInt(ARG_GENRE_ID, genreId)
                putString(ARG_GENRE_NAME, genreName)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            genreId = it.getInt(ARG_GENRE_ID)
            genreName = it.getString(ARG_GENRE_NAME, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Genre Title in the Custom Toolbar Header
        binding.genreTitle.text = genreName.uppercase()

        // Setup Back Action
        binding.backButton.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Fetch application container
        val appContainer = (requireContext().applicationContext as MovieApplication).container

        // Instantiate ViewModel
        val factory = DiscoverViewModel.Factory(
            repository = appContainer.movieRepository,
            isApiKeyValid = appContainer.isApiKeyValid,
            genreId = genreId
        )
        viewModel = ViewModelProvider(this, factory)[DiscoverViewModel::class.java]

        // Setup RecyclerView grid
        adapter = MoviesAdapter(emptyList()) { movie ->
            val detailFragment = MovieDetailFragment.newInstance(movie.id)
            parentFragmentManager.beginTransaction()
                .replace(com.example.R.id.fragment_container, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        binding.moviesRecyclerView.layoutManager = gridLayoutManager
        binding.moviesRecyclerView.adapter = adapter

        // Setup Endless/Pagination Scroll Listener
        binding.moviesRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = gridLayoutManager.itemCount
                val lastVisibleItemPosition = gridLayoutManager.findLastVisibleItemPosition()
                
                // Load more if scrolled close to reaching end of the list
                if (lastVisibleItemPosition >= totalItemCount - 4) {
                    viewModel.loadMovies(reset = false)
                }
            }
        })

        // Collect State Flow
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    // Set Loading states
                    if (state.isLoading) {
                        binding.loadingIndicator.visibility = View.VISIBLE
                        binding.moviesRecyclerView.visibility = View.GONE
                        binding.noDataLayout.visibility = View.GONE
                    } else {
                        binding.loadingIndicator.visibility = View.GONE
                        binding.moviesRecyclerView.visibility = View.VISIBLE
                        
                        // Set empty list state
                        if (state.movies.isEmpty() && state.error != null) {
                            binding.noDataLayout.visibility = View.VISIBLE
                        } else {
                            binding.noDataLayout.visibility = View.GONE
                            adapter.updateData(state.movies)
                        }
                    }

                    // Set scrolling pagination progress bar state
                    if (state.isPaginating) {
                        binding.paginationIndicator.visibility = View.VISIBLE
                    } else {
                        binding.paginationIndicator.visibility = View.GONE
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
