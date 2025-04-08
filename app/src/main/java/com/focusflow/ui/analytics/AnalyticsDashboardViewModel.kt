package com.focusflow.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.ai.Insight
import com.focusflow.ai.InsightType
import com.focusflow.analytics.AnalyticsDashboardHelper
import com.focusflow.analytics.AnalyticsTracker
import com.focusflow.analytics.FocusTimeRecommendation
import com.focusflow.data.remote.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Analytics Dashboard that integrates with Firebase.
 * Manages the UI state and data fetch operations for productivity metrics.
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val analyticsHelper: AnalyticsDashboardHelper,
    private val analyticsTracker: AnalyticsTracker
) : ViewModel() {

    // Productivity score
    private val _productivityScore = MutableStateFlow(0)
    val productivityScore: StateFlow<Int> = _productivityScore.asStateFlow()
    
    // Productivity insights
    private val _insights = MutableStateFlow<List<Insight>>(emptyList())
    val insights: StateFlow<List<Insight>> = _insights.asStateFlow()
    
    // Optimal focus times
    private val _optimalFocusTimes = MutableStateFlow<List<FocusTimeRecommendation>>(emptyList())
    val optimalFocusTimes: StateFlow<List<FocusTimeRecommendation>> = _optimalFocusTimes.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load analytics data from Firebase
     */
    fun loadAnalyticsData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val currentUser = firebaseRepository.getCurrentUser()
                val userId = currentUser?.uid
                
                if (userId != null) {
                    // Load data in parallel
                    launch { 
                        val score = analyticsHelper.getProductivityScore(userId)
                        _productivityScore.value = score
                        
                        // Track analytics dashboard view
                        analyticsTracker.logViewedAnalytics(score)
                        analyticsTracker.logScreenView("analytics_dashboard", "AnalyticsDashboardFragment")
                    }
                    
                    launch {
                        val insightStrings = analyticsHelper.getProductivityInsights(userId)
                        _insights.value = mapInsights(insightStrings)
                    }
                    
                    launch {
                        val focusTimes = analyticsHelper.getOptimalFocusTimes(userId)
                        _optimalFocusTimes.value = focusTimes
                    }
                } else {
                    _error.value = "Please sign in to view your analytics"
                }
            } catch (e: Exception) {
                _error.value = "Failed to load analytics: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Handle user interaction with an insight action
     */
    fun onInsightActionClicked(insight: Insight) {
        viewModelScope.launch {
            // Record the interaction in Firebase
            val currentUser = firebaseRepository.getCurrentUser()
            currentUser?.uid?.let { userId ->
                firebaseRepository.recordInsightInteraction(userId, insight.id)
                
                // Log the analytics event
                analyticsTracker.logAppliedInsight(
                    insightId = insight.id,
                    insightType = insight.type.name
                )
                
                // If the insight is a recommendation, implement it automatically
                if (insight.type == InsightType.RECOMMENDATION) {
                    // Apply the recommendation settings
                    insight.metadata?.get("settingKey")?.let { key ->
                        insight.metadata["settingValue"]?.let { value ->
                            firebaseRepository.updateUserSettings(userId, mapOf(key to value))
                            
                            // Track settings changed via insight
                            analyticsTracker.logSettingsChanged(key, value)
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Schedule a focus session based on optimal time recommendation
     */
    fun scheduleFocusSession(recommendation: FocusTimeRecommendation) {
        viewModelScope.launch {
            val currentUser = firebaseRepository.getCurrentUser()
            currentUser?.uid?.let { userId ->
                // In a real implementation, this would schedule a focus session in the calendar
                // or set up a notification for the recommended time
                
                // Track that user scheduled a focus session
                analyticsTracker.logEvent(
                    AnalyticsTracker.Events.SCHEDULED_SESSION,
                    mapOf(
                        "day_of_week" to recommendation.dayOfWeek.name,
                        "time" to recommendation.time,
                        "reason" to recommendation.reason
                    )
                )
            }
        }
    }
    
    /**
     * Map string insights to the Insight model
     */
    private fun mapInsights(insightStrings: List<String>): List<Insight> {
        return insightStrings.mapIndexed { index, text ->
            Insight(
                id = "insight_$index",
                title = getInsightTitle(text),
                description = text,
                type = getInsightType(text),
                hasAction = hasAction(text),
                actionText = getActionText(text),
                metadata = getMetadata(text)
            )
        }
    }
    
    private fun getInsightTitle(text: String): String {
        // Extract a title from the insight text
        return when {
            text.contains("completion rate") -> "Completion Rate"
            text.contains("most productive day") -> "Productive Day"
            text.contains("most productive") -> "Productivity Peak"
            text.contains("trending up") -> "Trending Upward"
            text.contains("focus time has been") -> "Focus Trend"
            else -> "Productivity Insight"
        }
    }
    
    private fun getInsightType(text: String): InsightType {
        return when {
            text.contains("Great work") || text.contains("excellent") -> InsightType.ACHIEVEMENT
            text.contains("Try to") || text.contains("Consider") || text.contains("decreasing") -> InsightType.WARNING
            text.contains("Your most productive") -> InsightType.PATTERN_DETECTED
            else -> InsightType.RECOMMENDATION
        }
    }
    
    private fun hasAction(text: String): Boolean {
        return when {
            text.contains("Try to") || text.contains("Consider") -> true
            else -> false
        }
    }
    
    private fun getActionText(text: String): String? {
        return when {
            text.contains("Try to improve") -> "Set Reminder"
            text.contains("Consider adjusting") -> "View Schedule"
            else -> null
        }
    }
    
    private fun getMetadata(text: String): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        
        if (text.contains("most productive day is")) {
            val dayRegex = "most productive day is (\\w+)".toRegex()
            val matchResult = dayRegex.find(text)
            matchResult?.groupValues?.get(1)?.let {
                metadata["productiveDay"] = it
            }
        }
        
        if (text.contains("most productive during")) {
            val timeRegex = "most productive during the ([\\w\\s-()]+)".toRegex()
            val matchResult = timeRegex.find(text)
            matchResult?.groupValues?.get(1)?.let {
                metadata["productiveTime"] = it.trim()
            }
        }
        
        return metadata
    }
}
