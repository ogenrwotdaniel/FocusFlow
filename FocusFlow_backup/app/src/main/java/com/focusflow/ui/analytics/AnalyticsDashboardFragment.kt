package com.focusflow.ui.analytics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.focusflow.R
import com.focusflow.analytics.FocusTimeRecommendation
import com.focusflow.analytics.InsightAdapter
import com.focusflow.analytics.OptimalFocusTimeAdapter
import com.focusflow.databinding.FragmentAnalyticsDashboardBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import java.util.Locale

/**
 * Fragment that displays the analytics dashboard with productivity insights and visualizations.
 * This implements Firebase-backed analytics to help users understand their productivity patterns.
 */
@AndroidEntryPoint
class AnalyticsDashboardFragment : Fragment() {

    private var _binding: FragmentAnalyticsDashboardBinding? = null
    private val binding get() = _binding!!
    
    private val viewModel: AnalyticsDashboardViewModel by viewModels()
    
    private lateinit var insightAdapter: InsightAdapter
    private lateinit var optimalTimeAdapter: OptimalFocusTimeAdapter
    private lateinit var trendAdapter: TrendAdapter
    private lateinit var streakAdapter: StreakCardAdapter
    
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
        
        setupUI()
        observeViewModel()
        
        // Refresh analytics data
        viewModel.loadAnalyticsData()
    }
    
    private fun setupUI() {
        // Setup productivity insights adapter
        insightAdapter = InsightAdapter { insight ->
            viewModel.onInsightActionClicked(insight)
        }
        
        binding.rvProductivityInsights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = insightAdapter
        }
        
        // Setup optimal focus times adapter
        optimalTimeAdapter = OptimalFocusTimeAdapter()
        
        binding.rvOptimalFocusTimes.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = optimalTimeAdapter
        }
        
        // Setup trend adapter
        trendAdapter = TrendAdapter()
        
        binding.rvTrends.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = trendAdapter
        }
        
        // Setup streak adapter (initially empty, will be updated in observeViewModel)
        streakAdapter = StreakCardAdapter(null, 0) {
            // Handle streak action button click
            viewModel.refreshRemoteConfig()
        }
        
        binding.rvStreak.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = streakAdapter
        }
        
        // Setup swipe refresh
        binding.swipeRefreshLayout.setOnRefreshListener {
            viewModel.loadAnalyticsData()
        }
        
        // Setup retry button
        binding.btnRetry.setOnClickListener {
            viewModel.loadAnalyticsData()
        }
    }
    
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Observe productivity score
                launch {
                    viewModel.productivityScore.collect { score ->
                        updateProductivityScore(score)
                    }
                }
                
                // Observe insights
                launch {
                    viewModel.insights.collect { insights ->
                        insightAdapter.submitList(insights)
                    }
                }
                
                // Observe optimal focus times
                launch {
                    viewModel.optimalFocusTimes.collect { times ->
                        optimalTimeAdapter.submitList(times.map { mapToUiModel(it) })
                    }
                }
                
                // Observe productivity trends
                launch {
                    viewModel.productivityTrends.collect { trends ->
                        trendAdapter.submitList(trends)
                        
                        // Show or hide trends section based on data availability
                        val trendsParent = binding.rvTrends.parent.parent as View
                        trendsParent.visibility = if (trends.isNotEmpty()) View.VISIBLE else View.GONE
                    }
                }
                
                // Observe streak data
                launch {
                    viewModel.currentStreak.collect { streak ->
                        // Update streak days in header
                        streak?.let {
                            // Update main streak display if visible
                            binding.tvStreakDays.text = resources.getQuantityString(
                                R.plurals.streak_days, 
                                it.currentStreakDays,
                                it.currentStreakDays
                            )
                            
                            // Update streak card
                            viewModel.dailyFocusGoal.value.let { goal ->
                                // Create new adapter with updated data to force redraw
                                streakAdapter = StreakCardAdapter(streak, goal) {
                                    viewModel.refreshRemoteConfig()
                                }
                                binding.rvStreak.adapter = streakAdapter
                            }
                        } ?: run {
                            binding.tvStreakDays.text = resources.getString(R.string.no_streak) 
                        }
                    }
                }
                
                // Observe loading state
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefreshLayout.isRefreshing = isLoading
                        if (isLoading) {
                            binding.loadingStateGroup.visibility = View.VISIBLE
                            binding.errorStateGroup.visibility = View.GONE
                            binding.contentGroup.visibility = View.GONE
                        } else {
                            binding.loadingStateGroup.visibility = View.GONE
                        }
                    }
                }
                
                // Observe error state
                launch {
                    viewModel.error.collect { errorMessage ->
                        errorMessage?.let {
                            binding.errorStateGroup.visibility = View.VISIBLE
                            binding.contentGroup.visibility = View.GONE
                            binding.tvErrorMessage.text = it
                        } ?: run {
                            binding.errorStateGroup.visibility = View.GONE
                            binding.contentGroup.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
    }
    
    private fun updateProductivityScore(score: Int) {
        // Update score text
        binding.tvProductivityScore.text = score.toString()
        
        // Update gauge chart
        setupProductivityGaugeChart(score)
        
        // Update scorecard color based on score
        val colorRes = when {
            score >= 80 -> R.color.colorSuccess
            score >= 60 -> R.color.colorPrimary
            score >= 40 -> R.color.colorWarning
            else -> R.color.colorError
        }
        binding.cvProductivityScore.setCardBackgroundColor(resources.getColor(colorRes, null))
    }
    
    private fun setupProductivityGaugeChart(score: Int) {
        val entries = listOf(
            PieEntry(score.toFloat(), ""),
            PieEntry((100 - score).toFloat(), "") // Remaining
        )
        
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                resources.getColor(R.color.colorPrimary, null),
                resources.getColor(R.color.colorBackgroundLight, null)
            )
            setDrawValues(false)
            sliceSpace = 0f
        }
        
        val data = PieData(dataSet)
        
        binding.chartProductivity.apply {
            this.data = data
            description.isEnabled = false
            legend.isEnabled = false
            holeRadius = 80f
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setDrawCenterText(false)
            setDrawEntryLabels(false)
            setTouchEnabled(false)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }
    
    private fun mapToUiModel(recommendation: FocusTimeRecommendation): OptimalFocusTimeUiModel {
        return OptimalFocusTimeUiModel(
            dayName = recommendation.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            time = recommendation.time,
            reason = recommendation.reason
        )
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * UI model for optimal focus time
 */
data class OptimalFocusTimeUiModel(
    val dayName: String,
    val time: String,
    val reason: String
)
