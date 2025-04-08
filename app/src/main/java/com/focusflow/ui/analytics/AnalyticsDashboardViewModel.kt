package com.focusflow.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.ai.Insight
import com.focusflow.ai.InsightType
import com.focusflow.analytics.AnalyticsDashboardHelper
import com.focusflow.analytics.AnalyticsTracker
import com.focusflow.analytics.FocusTimeRecommendation
import com.focusflow.analytics.RemoteConfigManager
import com.focusflow.analytics.TrendAnalyzer
import com.focusflow.data.remote.FirebaseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the Analytics Dashboard that integrates with Firebase.
 * Manages the UI state and data fetch operations for productivity metrics.
 */
@HiltViewModel
class AnalyticsDashboardViewModel @Inject constructor(
    private val firebaseRepository: FirebaseRepository,
    private val analyticsHelper: AnalyticsDashboardHelper,
    private val analyticsTracker: AnalyticsTracker,
    private val remoteConfigManager: RemoteConfigManager,
    private val trendAnalyzer: TrendAnalyzer
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
    
    // Productivity trends
    private val _productivityTrends = MutableStateFlow<List<TrendAnalyzer.ProductivityTrend>>(emptyList())
    val productivityTrends: StateFlow<List<TrendAnalyzer.ProductivityTrend>> = _productivityTrends.asStateFlow()
    
    // Productivity streak
    private val _currentStreak = MutableStateFlow<TrendAnalyzer.ProductivityStreak?>(null)
    val currentStreak: StateFlow<TrendAnalyzer.ProductivityStreak?> = _currentStreak.asStateFlow()
    
    // Daily goal
    private val _dailyFocusGoal = MutableStateFlow(0)
    val dailyFocusGoal: StateFlow<Int> = _dailyFocusGoal.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        // Initialize Remote Config
        remoteConfigManager.initialize()
        
        // Load default daily goal
        _dailyFocusGoal.value = remoteConfigManager.getDailyFocusGoalMinutes()
    }
    
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
                    
                    // Load daily productivity data for trend analysis
                    launch {
                        val productivityData = analyticsHelper.getDailyProductivityData(userId)
                        
                        // Only run trend analysis if enabled in remote config
                        if (remoteConfigManager.isFeatureEnabled(RemoteConfigManager.KEY_ENABLE_TREND_ANALYSIS) && 
                            productivityData.isNotEmpty()) {
                            
                            // Convert to TrendAnalyzer format
                            val trendData = productivityData.map { daily ->
                                TrendAnalyzer.DailyProductivity(
                                    date = LocalDate.parse(daily.date),
                                    focusMinutes = daily.focusMinutes,
                                    sessionsCompleted = daily.sessionsCompleted,
                                    completionRate = daily.completionRate
                                )
                            }
                            
                            // Calculate trends
                            val trends = trendAnalyzer.calculateTrends(trendData)
                            _productivityTrends.value = trends
                            
                            // Find patterns
                            val patterns = trendAnalyzer.findPatterns(trendData)
                            
                            // Calculate streaks
                            val streak = trendAnalyzer.calculateStreaks(trendData)
                            _currentStreak.value = streak
                            
                            // Generate insights from trends
                            val trendInsights = trendAnalyzer.generateInsightsFromTrends(
                                trends, patterns, streak
                            )
                            
                            // Combine with other insights
                            val baseInsights = analyticsHelper.getProductivityInsights(userId)
                            val combinedInsights = (baseInsights + trendInsights).distinct()
                            
                            // Map to insight objects
                            _insights.value = mapInsights(combinedInsights)
                            
                            // Log that we're providing advanced insights
                            analyticsTracker.logEvent(
                                "advanced_insights_provided",
                                mapOf(
                                    "insight_count" to combinedInsights.size,
                                    "trend_based_count" to trendInsights.size,
                                    "has_streak" to (streak.currentStreakDays > 0)
                                )
                            )
                        } else {
                            // Fall back to basic insights if trend analysis is disabled
                            val insightStrings = analyticsHelper.getProductivityInsights(userId)
                            _insights.value = mapInsights(insightStrings)
                        }
                    }
                    
                    // Only load focus time predictions if enabled
                    if (remoteConfigManager.isFeatureEnabled(RemoteConfigManager.KEY_ENABLE_FOCUS_PREDICTIONS)) {
                        launch {
                            val focusTimes = analyticsHelper.getOptimalFocusTimes(userId)
                            _optimalFocusTimes.value = focusTimes
                        }
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
                
                // Handle different action types
                if (insight.actionText == "Set Reminder") {
                    // TODO: Open reminder creation dialog
                } else if (insight.actionText == "View Schedule") {
                    // TODO: Navigate to schedule screen
                } else if (insight.actionText?.contains("Adjust") == true) {
                    // Parse setting to adjust from insight text
                    val settingRegex = "Adjust ([\\w\\s]+)".toRegex()
                    val matchResult = settingRegex.find(insight.actionText)
                    matchResult?.groupValues?.get(1)?.let { setting ->
                        // Determine key and value to update
                        val (key, value) = when {
                            setting.contains("notification") -> "notifications_enabled" to "true"
                            setting.contains("session length") -> "focus_session_minutes" to "25"
                            setting.contains("break") -> "short_break_minutes" to "5"
                            else -> "" to ""
                        }
                        
                        if (key.isNotEmpty()) {
                            // Update the user setting in Firebase
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
     * Manually check for updated remote configuration
     */
    fun refreshRemoteConfig() {
        remoteConfigManager.fetchAndActivate()
        _dailyFocusGoal.value = remoteConfigManager.getDailyFocusGoalMinutes()
        
        // Log analytics event for manual refresh
        analyticsTracker.logEvent(
            "refreshed_remote_config", 
            mapOf("source" to "analytics_dashboard")
        )
        
        // Reload analytics to reflect any config changes
        loadAnalyticsData()
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
            text.contains("trending up") || text.contains("increased by") -> "Trending Upward"
            text.contains("trending down") || text.contains("decreased by") -> "Trending Downward"
            text.contains("focus time has been") -> "Focus Trend"
            text.contains("streak") -> "Focus Streak"
            text.contains("daily goal") -> "Daily Goal"
            else -> "Productivity Insight"
        }
    }
    
    private fun getInsightType(text: String): InsightType {
        return when {
            text.contains("Great work") || text.contains("excellent") || 
            text.contains("streak") || text.contains("increased by") -> InsightType.ACHIEVEMENT
            text.contains("Try to") || text.contains("Consider") || 
            text.contains("decreasing") || text.contains("decreased by") -> InsightType.WARNING
            text.contains("Your most productive") || text.contains("pattern") -> InsightType.PATTERN_DETECTED
            else -> InsightType.RECOMMENDATION
        }
    }
    
    private fun hasAction(text: String): Boolean {
        return when {
            text.contains("Try to") || text.contains("Consider") || 
            text.contains("Can you beat") || text.contains("Set a goal") -> true
            else -> false
        }
    }
    
    private fun getActionText(text: String): String? {
        return when {
            text.contains("Try to improve") -> "Set Reminder"
            text.contains("Consider adjusting") -> "View Schedule"
            text.contains("Can you beat that record") -> "Set Streak Goal"
            text.contains("Set a goal") -> "Set Daily Goal"
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
        
        if (text.contains("streak")) {
            val streakRegex = "(\\d+)[-\\s]day streak".toRegex()
            val matchResult = streakRegex.find(text)
            matchResult?.groupValues?.get(1)?.let {
                metadata["streakDays"] = it
            }
        }
        
        if (text.contains("goal of")) {
            val goalRegex = "goal of (\\d+)".toRegex()
            val matchResult = goalRegex.find(text)
            matchResult?.groupValues?.get(1)?.let {
                metadata["goalMinutes"] = it
            }
        }
        
        return metadata
    }
}
