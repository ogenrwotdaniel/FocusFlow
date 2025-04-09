package com.focusflow.ui.statistics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.data.repository.SessionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _statistics = MutableLiveData<StatisticsData>()
    val statistics: LiveData<StatisticsData> = _statistics

    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    init {
        loadStatistics()
    }

    private fun loadStatistics() {
        viewModelScope.launch {
            combine(
                sessionRepository.getTotalFocusTime(),
                sessionRepository.getTotalSessionsCount(),
                sessionRepository.getCurrentStreak(),
                sessionRepository.getDailyFocusTime()
            ) { totalTime, sessions, streak, dailyData ->
                StatisticsData(
                    totalFocusTimeMs = totalTime,
                    totalFocusTimeFormatted = formatTime(totalTime),
                    totalSessions = sessions,
                    currentStreak = streak,
                    dailyFocusData = dailyData.mapKeys { dateFormat.format(it.key) }
                )
            }.collect { stats ->
                _statistics.value = stats
            }
        }
    }

    private fun formatTime(timeMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m"
        }
    }
}

data class StatisticsData(
    val totalFocusTimeMs: Long,
    val totalFocusTimeFormatted: String,
    val totalSessions: Int,
    val currentStreak: Int,
    val dailyFocusData: Map<String, Long>
)
