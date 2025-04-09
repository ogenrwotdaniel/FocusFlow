package com.focusflow.analytics

import android.util.Log
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Firebase Remote Config operations for analytics feature flags
 * and settings that can be dynamically configured without app updates.
 */
@Singleton
class RemoteConfigManager @Inject constructor() {
    
    private val remoteConfig = FirebaseRemoteConfig.getInstance()
    
    companion object {
        // Feature toggle keys
        const val KEY_ENABLE_TREND_ANALYSIS = "enable_trend_analysis"
        const val KEY_ENABLE_FOCUS_PREDICTIONS = "enable_focus_predictions"
        const val KEY_ENABLE_ADVANCED_INSIGHTS = "enable_advanced_insights"
        const val KEY_ENABLE_DASHBOARD_SHARING = "enable_dashboard_sharing"
        
        // Configuration keys
        const val KEY_DAILY_FOCUS_GOAL_MINUTES = "daily_focus_goal_minutes"
        const val KEY_DAILY_GOAL_MINUTES = "daily_goal_minutes" // Legacy key, use KEY_DAILY_FOCUS_GOAL_MINUTES instead
        const val KEY_TRENDS_LOOKBACK_DAYS = "trends_lookback_days"
        const val KEY_PRODUCTIVITY_SCORE_WEIGHTS = "productivity_score_weights"
        const val KEY_INSIGHT_COUNT_LIMIT = "insight_count_limit"
        const val KEY_FOCUSED_TIME_DISPLAY_TYPE = "focused_time_display_type"
        
        // Default values
        private const val DEFAULT_TREND_ANALYSIS_ENABLED = true
        private const val DEFAULT_FOCUS_PREDICTIONS_ENABLED = true
        private const val DEFAULT_ADVANCED_INSIGHTS_ENABLED = true
        private const val DEFAULT_DASHBOARD_SHARING_ENABLED = false
        private const val DEFAULT_DAILY_FOCUS_GOAL_MINUTES = 120
        private const val DEFAULT_TRENDS_LOOKBACK_DAYS = 14
        private const val DEFAULT_PRODUCTIVITY_SCORE_WEIGHTS = """
            {
                "completion_rate": 0.5,
                "sessions_completed": 0.3,
                "focus_duration": 0.2
            }
        """
        private const val DEFAULT_INSIGHT_COUNT_LIMIT = 5
        private const val DEFAULT_FOCUSED_TIME_DISPLAY_TYPE = "minutes"
        
        // Minimum fetch interval for development vs production
        private const val DEV_FETCH_INTERVAL_SECONDS = 0L // Immediate for dev
        private const val PROD_FETCH_INTERVAL_SECONDS = 3600L // 1 hour for prod
    }
    
    /**
     * Initialize the Remote Config with default values and fetch settings
     */
    fun initialize() {
        // Set default values
        val defaults = mapOf(
            KEY_ENABLE_TREND_ANALYSIS to DEFAULT_TREND_ANALYSIS_ENABLED,
            KEY_ENABLE_FOCUS_PREDICTIONS to DEFAULT_FOCUS_PREDICTIONS_ENABLED,
            KEY_ENABLE_ADVANCED_INSIGHTS to DEFAULT_ADVANCED_INSIGHTS_ENABLED,
            KEY_ENABLE_DASHBOARD_SHARING to DEFAULT_DASHBOARD_SHARING_ENABLED,
            KEY_DAILY_FOCUS_GOAL_MINUTES to DEFAULT_DAILY_FOCUS_GOAL_MINUTES,
            KEY_DAILY_GOAL_MINUTES to DEFAULT_DAILY_FOCUS_GOAL_MINUTES, // Legacy support
            KEY_TRENDS_LOOKBACK_DAYS to DEFAULT_TRENDS_LOOKBACK_DAYS,
            KEY_PRODUCTIVITY_SCORE_WEIGHTS to DEFAULT_PRODUCTIVITY_SCORE_WEIGHTS,
            KEY_INSIGHT_COUNT_LIMIT to DEFAULT_INSIGHT_COUNT_LIMIT,
            KEY_FOCUSED_TIME_DISPLAY_TYPE to DEFAULT_FOCUSED_TIME_DISPLAY_TYPE
        )
        remoteConfig.setDefaultsAsync(defaults)
        
        // Configure fetching settings
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(
                if (isDebugBuild()) DEV_FETCH_INTERVAL_SECONDS else PROD_FETCH_INTERVAL_SECONDS
            )
            .build()
        remoteConfig.setConfigSettingsAsync(configSettings)
        
        // Fetch and activate on startup
        fetchAndActivate()
    }
    
    /**
     * Fetch the latest config values from Firebase and activate them
     */
    fun fetchAndActivate() {
        remoteConfig.fetchAndActivate()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("RemoteConfigManager", "Remote config fetched and activated")
                } else {
                    Log.e("RemoteConfigManager", "Failed to fetch remote config", task.exception)
                }
            }
    }
    
    /**
     * Check if a feature is enabled in remote config
     */
    fun isFeatureEnabled(key: String): Boolean {
        return remoteConfig.getBoolean(key)
    }
    
    /**
     * Get the daily focus goal in minutes from remote config
     */
    fun getDailyFocusGoalMinutes(): Int {
        // Try the new key first, fall back to legacy key if needed
        val value = remoteConfig.getLong(KEY_DAILY_FOCUS_GOAL_MINUTES)
        return if (value > 0) {
            value.toInt()
        } else {
            remoteConfig.getLong(KEY_DAILY_GOAL_MINUTES).toInt()
        }
    }
    
    /**
     * Get the number of days to look back for trend analysis
     */
    fun getTrendsLookbackDays(): Int {
        return remoteConfig.getLong(KEY_TRENDS_LOOKBACK_DAYS).toInt()
    }
    
    /**
     * Get the productivity score weights JSON configuration
     */
    fun getProductivityScoreWeightsJson(): String {
        return remoteConfig.getString(KEY_PRODUCTIVITY_SCORE_WEIGHTS)
    }
    
    /**
     * Get the maximum number of insights to show at once
     */
    fun getInsightCountLimit(): Int {
        return remoteConfig.getLong(KEY_INSIGHT_COUNT_LIMIT).toInt()
    }
    
    /**
     * Get the display type for focused time (minutes, hours, etc.)
     */
    fun getFocusedTimeDisplayType(): String {
        return remoteConfig.getString(KEY_FOCUSED_TIME_DISPLAY_TYPE)
    }
    
    /**
     * Check if we're running in a debug build
     */
    private fun isDebugBuild(): Boolean {
        return try {
            Class.forName("com.focusflow.BuildConfig")
                .getField("DEBUG")
                .getBoolean(null)
        } catch (e: Exception) {
            // Default to false for safety if we can't determine
            false
        }
    }
}
