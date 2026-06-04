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
import com.example.MovieApplication
import com.example.databinding.FragmentGenresBinding
import com.example.ui.viewmodel.GenreUiState
import com.example.ui.viewmodel.GenreViewModel
import kotlinx.coroutines.launch

class GenresFragment : Fragment() {

    private var _binding: FragmentGenresBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GenreViewModel
    private lateinit var adapter: GenresAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGenresBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Fetch application container
        val appContainer = (requireContext().applicationContext as MovieApplication).container

        // Instantiate ViewModel with its custom factory
        val factory = GenreViewModel.Factory(
            repository = appContainer.movieRepository,
            isApiKeyValid = appContainer.isApiKeyValid
        )
        viewModel = ViewModelProvider(this, factory)[GenreViewModel::class.java]

        // Setup Recycler View with 2 Column Grid
        adapter = GenresAdapter(emptyList()) { genre ->
            // Navigate to DiscoverFragment
            val discoverFragment = DiscoverFragment.newInstance(genre.id, genre.name)
            parentFragmentManager.beginTransaction()
                .replace(com.example.R.id.fragment_container, discoverFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.genresRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.genresRecyclerView.adapter = adapter

        // Setup Retry Trigger
        binding.retryButton.setOnClickListener {
            viewModel.loadGenres()
        }

        // Set up Flow/State collection
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is GenreUiState.Loading -> {
                            binding.loadingIndicator.visibility = View.VISIBLE
                            binding.genresRecyclerView.visibility = View.GONE
                            binding.errorLayout.visibility = View.GONE
                        }
                        is GenreUiState.Success -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.genresRecyclerView.visibility = View.VISIBLE
                            binding.errorLayout.visibility = View.GONE
                            adapter.updateData(state.genres)
                        }
                        is GenreUiState.Error -> {
                            binding.loadingIndicator.visibility = View.GONE
                            binding.genresRecyclerView.visibility = View.GONE
                            binding.errorLayout.visibility = View.VISIBLE
                            binding.errorMessage.text = state.message
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
