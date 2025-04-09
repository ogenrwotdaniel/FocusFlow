package com.focusflow.analytics

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.ai.Insight
import com.focusflow.ai.RecommendationEngine
import com.focusflow.data.local.dao.SessionDao
import com.focusflow.data.model.ProductivityMetric
import com.focusflow.data.model.SessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

/**
 * ViewModel for the Analytics Dashboard screen
 * Handles data preparation and processing for visualization components
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val productivityInsights: ProductivityInsights,
    private val recommendationEngine: RecommendationEngine,
    private val sessionDao: SessionDao
) : ViewModel() {

    // Dashboard data state
    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Loading)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()
    
    // Weekly focus data for chart
    private val _weeklyFocusData = MutableStateFlow<Map<LocalDate, Int>>(emptyMap())
    val weeklyFocusData: StateFlow<Map<LocalDate, Int>> = _weeklyFocusData.asStateFlow()
    
    // Focus distribution data for pie chart
    private val _focusDistribution = MutableStateFlow<Map<String, Int>>(emptyMap())
    val focusDistribution: StateFlow<Map<String, Int>> = _focusDistribution.asStateFlow()
    
    // Optimal focus times
    private val _optimalFocusTimes = MutableStateFlow<List<OptimalFocusTime>>(emptyList())
    val optimalFocusTimes: StateFlow<List<OptimalFocusTime>> = _optimalFocusTimes.asStateFlow()
    
    // Interruption data for bar chart
    private val _interruptionData = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val interruptionData: StateFlow<Map<Int, Float>> = _interruptionData.asStateFlow()
    
    // Currently selected time period
    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.DAILY)
    val selectedTimePeriod: StateFlow<TimePeriod> = _selectedTimePeriod.asStateFlow()
    
    // Dashboard metrics for the selected time period
    private val _metrics = MutableStateFlow<ProductivityMetric?>(null)
    val metrics: StateFlow<ProductivityMetric?> = _metrics.asStateFlow()
    
    init {
        loadDashboardData()
    }
    
    /**
     * Load all data required for the analytics dashboard
     */
    fun loadDashboardData() {
        viewModelScope.launch {
            _dashboardState.value = DashboardState.Loading
            
            try {
                // Load productivity dashboard
                val dashboard = productivityInsights.getProductivityDashboard().first()
                
                // Update UI state
                _weeklyFocusData.value = getWeeklyFocusData()
                _focusDistribution.value = dashboard.focusDistribution
                _optimalFocusTimes.value = dashboard.optimalFocusTimes
                _interruptionData.value = getInterruptionsByHour()
                
                // Set initial metrics to daily
                updateTimePeriod(TimePeriod.DAILY)
                
                _dashboardState.value = DashboardState.Success(dashboard)
            } catch (e: Exception) {
                _dashboardState.value = DashboardState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    /**
     * Update the selected time period and associated metrics
     */
    fun updateTimePeriod(period: TimePeriod) {
        viewModelScope.launch {
            _selectedTimePeriod.value = period
            
            // Update metrics based on time period
            when (period) {
                TimePeriod.DAILY -> {
                    _metrics.value = productivityInsights.getDailyMetrics().first()
                }
                TimePeriod.WEEKLY -> {
                    _metrics.value = productivityInsights.getWeeklyMetrics().first()
                }
                TimePeriod.MONTHLY -> {
                    _metrics.value = productivityInsights.getMonthlyMetrics().first()
                }
            }
        }
    }
    
    /**
     * Schedule a focus session at the optimal time
     */
    fun scheduleOptimalFocusSession(optimalTime: OptimalFocusTime) {
        viewModelScope.launch {
            // Implementation would connect to scheduling system
            // This is a placeholder for the actual implementation
        }
    }
    
    /**
     * Apply an insight action
     */
    fun applyInsight(insight: Insight) {
        viewModelScope.launch {
            // Implementation would vary based on insight type
            // This is a placeholder for the actual implementation
        }
    }
    
    /**
     * Get focus data for the past week
     */
    private suspend fun getWeeklyFocusData(): Map<LocalDate, Int> {
        val today = LocalDate.now()
        val startDate = today.minusDays(6)
        
        val startTimeMs = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        val endTimeMs = today.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000
        
        val sessions = sessionDao.getSessionsInTimeRange(startTimeMs, endTimeMs).first()
        
        // Group sessions by day and sum up focus minutes
        return sessions.groupBy { session ->
            LocalDateTime.ofEpochSecond(session.startTimeMs / 1000, 0, ZoneOffset.UTC).toLocalDate()
        }.mapValues { (_, sessions) ->
            sessions.sumOf { it.durationMinutes }
        }
    }
    
    /**
     * Get average interruptions by hour of day
     */
    private suspend fun getInterruptionsByHour(): Map<Int, Float> {
        val sessions = sessionDao.getAllCompletedSessions().first()
        
        // Group sessions by hour
        return sessions.groupBy { session ->
            LocalDateTime.ofEpochSecond(session.startTimeMs / 1000, 0, ZoneOffset.UTC).hour
        }.mapValues { (_, sessions) ->
            sessions.map { it.interruptionCount.toFloat() }.average().toFloat()
        }
    }
}

/**
 * Represents the state of the analytics dashboard
 */
sealed class DashboardState {
    object Loading : DashboardState()
    data class Success(val dashboard: ProductivityDashboard) : DashboardState()
    data class Error(val message: String) : DashboardState()
}

/**
 * Time periods for analytics
 */
enum class TimePeriod {
    DAILY, WEEKLY, MONTHLY
}
