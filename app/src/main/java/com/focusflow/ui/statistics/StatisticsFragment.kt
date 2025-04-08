package com.focusflow.ui.statistics

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.focusflow.R
import com.focusflow.databinding.FragmentStatisticsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StatisticsFragment : Fragment() {

    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: StatisticsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatisticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observeViewModel()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun observeViewModel() {
        viewModel.statistics.observe(viewLifecycleOwner) { stats ->
            binding.textViewTotalFocusTime.text = stats.totalFocusTimeFormatted
            binding.textViewSessionsCompleted.text = stats.totalSessions.toString()
            
            val streakText = resources.getQuantityString(
                R.plurals.days_count,
                stats.currentStreak,
                stats.currentStreak
            )
            binding.textViewCurrentStreak.text = streakText
            
            // Handle chart data
            if (stats.dailyFocusData.isEmpty()) {
                binding.textViewNoData.visibility = View.VISIBLE
                binding.chartContainer.visibility = View.GONE
            } else {
                binding.textViewNoData.visibility = View.GONE
                binding.chartContainer.visibility = View.VISIBLE
                renderChart(stats.dailyFocusData)
            }
        }
    }

    private fun renderChart(dailyData: Map<String, Long>) {
        // In a real implementation, we would use a charting library like MPAndroidChart
        // For now, just show placeholder
        binding.chartContainer.removeAllViews()
        
        // Create a simple text view showing the data
        val chartText = dailyData.entries.joinToString("\n") { (date, minutes) ->
            "$date: ${minutes / 60000} minutes"
        }
        
        val textView = layoutInflater.inflate(
            R.layout.chart_placeholder, 
            binding.chartContainer, 
            false
        )
        binding.chartContainer.addView(textView)
    }
}
