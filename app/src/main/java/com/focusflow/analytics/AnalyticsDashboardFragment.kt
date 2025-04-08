package com.focusflow.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusflow.R
import com.focusflow.databinding.FragmentAnalyticsDashboardBinding
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Fragment that displays the analytics dashboard with productivity visualizations
 * Includes charts, insights, and actionable recommendations based on user data
 */
@AndroidEntryPoint
class AnalyticsDashboardFragment : Fragment() {

    private var _binding: FragmentAnalyticsDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsDashboardViewModel by viewModels()
    
    @Inject
    lateinit var progressVisualization: ProgressVisualization
    
    private val insightAdapter = InsightAdapter { insight -> 
        viewModel.applyInsight(insight)
    }
    
    private val optimalTimeAdapter = OptimalTimeAdapter { optimalTime ->
        viewModel.scheduleOptimalFocusSession(optimalTime)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerViews()
        setupTabLayout()
        setupFab()
        observeViewModel()
    }
    
    private fun setupRecyclerViews() {
        // Setup insights recycler view
        binding.rvInsights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = insightAdapter
        }
        
        // Setup optimal times recycler view
        binding.rvOptimalTimes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = optimalTimeAdapter
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> viewModel.updateTimePeriod(TimePeriod.DAILY)
                    1 -> viewModel.updateTimePeriod(TimePeriod.WEEKLY)
                    2 -> viewModel.updateTimePeriod(TimePeriod.MONTHLY)
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
    
    private fun setupFab() {
        binding.fabStartFocus.setOnClickListener {
            // Navigate to focus session fragment
            // Navigation would be implemented here
            Toast.makeText(requireContext(), "Starting focus session", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe dashboard state
            viewModel.dashboardState.collect { state ->
                when (state) {
                    is DashboardState.Loading -> showLoading()
                    is DashboardState.Success -> {
                        hideLoading()
                        updateProductivityScore(state.dashboard.productivityScore)
                        updateStreakDays(state.dashboard.streakDays)
                        insightAdapter.submitList(state.dashboard.topInsights)
                        optimalTimeAdapter.submitList(state.dashboard.optimalFocusTimes)
                    }
                    is DashboardState.Error -> {
                        hideLoading()
                        Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe weekly focus data
            viewModel.weeklyFocusData.collect { focusData ->
                if (focusData.isNotEmpty()) {
                    // Convert to format needed by the chart
                    progressVisualization.setupWeeklyFocusChart(binding.chartWeeklyFocus, focusData)
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe focus distribution data
            viewModel.focusDistribution.collect { distribution ->
                if (distribution.isNotEmpty()) {
                    progressVisualization.setupFocusDistributionChart(
                        binding.chartFocusDistribution, 
                        distribution
                    )
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe interruption data
            viewModel.interruptionData.collect { interruptionData ->
                if (interruptionData.isNotEmpty()) {
                    progressVisualization.setupInterruptionsChart(
                        binding.chartInterruptions,
                        interruptionData
                    )
                }
            }
        }
        
        viewLifecycleOwner.lifecycleScope.launch {
            // Observe metrics updates
            viewModel.metrics.collect { metrics ->
                metrics?.let { updateMetrics(it) }
            }
        }
    }
    
    private fun updateProductivityScore(score: Int) {
        binding.tvProductivityScore.text = score.toString()
        
        // Update color based on score
        val colorRes = when {
            score >= 80 -> R.color.colorSuccess
            score >= 60 -> R.color.colorPrimary
            score >= 40 -> R.color.colorWarning
            else -> R.color.colorError
        }
        binding.tvProductivityScore.setTextColor(requireContext().getColor(colorRes))
    }
    
    private fun updateStreakDays(days: Int) {
        binding.tvStreakDays.text = resources.getQuantityString(
            R.plurals.streak_days, 
            days, 
            days
        )
    }
    
    private fun updateMetrics(metrics: ProductivityMetric) {
        // Update focus time
        binding.tvFocusTimeToday.text = getString(
            R.string.focus_minutes_value, 
            metrics.totalFocusMinutes
        )
        
        // Update completion rate
        binding.tvCompletionRate.text = getString(
            R.string.percentage_value, 
            metrics.completionRate.toInt()
        )
    }
    
    private fun showLoading() {
        // Implement loading state
        // Could show a progress indicator or skeleton UI
    }
    
    private fun hideLoading() {
        // Hide loading state
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
