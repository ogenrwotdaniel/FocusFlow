package com.focusflow.analytics

import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for tracking events in Firebase Analytics.
 * Provides standardized tracking methods for common events across the app.
 */
@Singleton
class AnalyticsTracker @Inject constructor() {

    private val firebaseAnalytics = Firebase.analytics

    /**
     * Custom events for the app
     */
    object Events {
        // Session events
        const val SESSION_STARTED = "focus_session_started"
        const val SESSION_COMPLETED = "focus_session_completed"
        const val SESSION_CANCELED = "focus_session_canceled"
        const val SESSION_PAUSED = "focus_session_paused"
        const val SESSION_RESUMED = "focus_session_resumed"
        
        // Social events
        const val JOINED_GROUP = "joined_group_session"
        const val CREATED_GROUP = "created_group_session"
        const val SENT_INVITATION = "sent_group_invitation"
        
        // Analytics events
        const val VIEWED_ANALYTICS = "viewed_analytics_dashboard"
        const val APPLIED_INSIGHT = "applied_insight_recommendation"
        const val SCHEDULED_SESSION = "scheduled_focus_session"
        
        // Feature usage events
        const val AUDIO_PLAYED = "audio_background_played"
        const val SETTINGS_CHANGED = "settings_changed"
    }
    
    /**
     * Parameter keys for events
     */
    object Params {
        const val SESSION_LENGTH = "session_length_minutes"
        const val SESSION_TYPE = "session_type"
        const val COMPLETION_RATE = "completion_rate"
        const val FOCUS_TIME = "focus_time_minutes"
        const val AUDIO_TRACK = "audio_track"
        const val GROUP_SIZE = "group_size"
        const val INSIGHT_TYPE = "insight_type"
        const val INSIGHT_ID = "insight_id"
        const val SETTING_KEY = "setting_key"
        const val SETTING_VALUE = "setting_value"
        const val PRODUCTIVITY_SCORE = "productivity_score"
    }
    
    /**
     * Log any custom event with parameters
     */
    fun logEvent(eventName: String, params: Map<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putInt(key, value)
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Float -> bundle.putFloat(key, value)
                is Boolean -> bundle.putBoolean(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }
    
    /**
     * Log a focus session started event
     */
    fun logSessionStarted(sessionLengthMinutes: Int, sessionType: String) {
        firebaseAnalytics.logEvent(Events.SESSION_STARTED) {
            param(Params.SESSION_LENGTH, sessionLengthMinutes.toLong())
            param(Params.SESSION_TYPE, sessionType)
        }
    }
    
    /**
     * Log a focus session completed event
     */
    fun logSessionCompleted(
        sessionLengthMinutes: Int, 
        actualFocusTimeMinutes: Int,
        completionRate: Double
    ) {
        firebaseAnalytics.logEvent(Events.SESSION_COMPLETED) {
            param(Params.SESSION_LENGTH, sessionLengthMinutes.toLong())
            param(Params.FOCUS_TIME, actualFocusTimeMinutes.toLong())
            param(Params.COMPLETION_RATE, completionRate)
        }
    }
    
    /**
     * Log a focus session canceled event
     */
    fun logSessionCanceled(
        sessionLengthMinutes: Int,
        actualFocusTimeMinutes: Int,
        completionRate: Double
    ) {
        firebaseAnalytics.logEvent(Events.SESSION_CANCELED) {
            param(Params.SESSION_LENGTH, sessionLengthMinutes.toLong())
            param(Params.FOCUS_TIME, actualFocusTimeMinutes.toLong())
            param(Params.COMPLETION_RATE, completionRate)
        }
    }
    
    /**
     * Log when user views the analytics dashboard
     */
    fun logViewedAnalytics(productivityScore: Int) {
        firebaseAnalytics.logEvent(Events.VIEWED_ANALYTICS) {
            param(Params.PRODUCTIVITY_SCORE, productivityScore.toLong())
        }
    }
    
    /**
     * Log when user applies an insight recommendation
     */
    fun logAppliedInsight(insightId: String, insightType: String) {
        firebaseAnalytics.logEvent(Events.APPLIED_INSIGHT) {
            param(Params.INSIGHT_ID, insightId)
            param(Params.INSIGHT_TYPE, insightType)
        }
    }
    
    /**
     * Log when settings are changed
     */
    fun logSettingsChanged(settingKey: String, settingValue: String) {
        firebaseAnalytics.logEvent(Events.SETTINGS_CHANGED) {
            param(Params.SETTING_KEY, settingKey)
            param(Params.SETTING_VALUE, settingValue)
        }
    }
    
    /**
     * Track screen views
     */
    fun logScreenView(screenName: String, screenClass: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, screenName)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, screenClass)
        }
    }
}
