package com.focusflow.ui.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusflow.R
import com.focusflow.databinding.FragmentHomeBinding
import com.focusflow.service.timer.TimerService
import com.focusflow.ui.session.SessionFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var sessionsAdapter: SessionAdapter

    private var timerService: TimerService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as TimerService.TimerBinder
            timerService = binder.getService()
            bound = true

            // Check if a session is already running
            timerService?.timerInfo?.observe(viewLifecycleOwner) { timerInfo ->
                if (timerInfo.isActive()) {
                    navigateToSession()
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            bound = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    override fun onStart() {
        super.onStart()
        // Bind to TimerService
        Intent(requireContext(), TimerService::class.java).also { intent ->
            requireActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        // Unbind from TimerService
        if (bound) {
            requireActivity().unbindService(connection)
            bound = false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRecyclerView() {
        sessionsAdapter = SessionAdapter()
        binding.recyclerViewRecentSessions.apply {
            adapter = sessionsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun setupClickListeners() {
        binding.buttonStartFocus.setOnClickListener {
            startFocusSession()
        }
    }

    private fun observeViewModel() {
        viewModel.todayFocusTime.observe(viewLifecycleOwner) { focusTimeString ->
            binding.textViewTodayFocus.text = 
                getString(R.string.today_focus, focusTimeString)
        }

        viewModel.currentStreak.observe(viewLifecycleOwner) { streak ->
            binding.textViewStreak.text = 
                getString(R.string.current_streak, streak)
        }

        viewModel.recentSessions.observe(viewLifecycleOwner) { sessions ->
            sessionsAdapter.submitList(sessions)
        }

        viewModel.recentTrees.observe(viewLifecycleOwner) { trees ->
            updateGardenPreview(trees)
        }
    }

    private fun updateGardenPreview(trees: List<Any>) {
        binding.layoutGardenPreview.removeAllViews()
        
        if (trees.isEmpty()) {
            binding.textViewEmptyGarden.visibility = View.VISIBLE
            binding.layoutGardenPreview.addView(binding.textViewEmptyGarden)
        } else {
            binding.textViewEmptyGarden.visibility = View.GONE
            
            // In a real implementation, we would add tree ImageViews here
            // For now, just showing a placeholder message
            val treeCount = trees.size
            val placeholderView = layoutInflater.inflate(
                R.layout.item_tree_preview, 
                binding.layoutGardenPreview, 
                false
            )
            binding.layoutGardenPreview.addView(placeholderView)
        }
    }

    private fun startFocusSession() {
        viewModel.getFocusSessionSettings().observe(viewLifecycleOwner) { settings ->
            val intent = Intent(requireContext(), TimerService::class.java).apply {
                action = TimerService.ACTION_START
                putExtra(TimerService.EXTRA_SESSION_TYPE, "FOCUS")
                putExtra(TimerService.EXTRA_DURATION_MINUTES, settings.focusSessionMinutes)
            }
            requireActivity().startService(intent)
            
            navigateToSession()
        }
    }

    private fun navigateToSession() {
        findNavController().navigate(R.id.action_home_to_session)
    }
}
