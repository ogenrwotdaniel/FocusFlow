package com.focusflow.ui.garden

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.focusflow.R
import com.focusflow.databinding.FragmentGardenBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class GardenFragment : Fragment() {

    private var _binding: FragmentGardenBinding? = null
    private val binding get() = _binding!!

    private val viewModel: GardenViewModel by viewModels()
    private lateinit var treeAdapter: TreeAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGardenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        treeAdapter = TreeAdapter()
        binding.recyclerViewTrees.apply {
            adapter = treeAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun observeViewModel() {
        viewModel.trees.observe(viewLifecycleOwner) { trees ->
            treeAdapter.submitList(trees)
            
            // Show empty view if there are no trees
            if (trees.isEmpty()) {
                binding.textViewEmptyGarden.visibility = View.VISIBLE
                binding.recyclerViewTrees.visibility = View.GONE
            } else {
                binding.textViewEmptyGarden.visibility = View.GONE
                binding.recyclerViewTrees.visibility = View.VISIBLE
            }
        }
        
        viewModel.gardenStats.observe(viewLifecycleOwner) { stats ->
            binding.textViewStats.text = getString(
                R.string.garden_stats,
                stats.totalTrees,
                stats.totalFocusTimeFormatted
            )
        }
    }
}
