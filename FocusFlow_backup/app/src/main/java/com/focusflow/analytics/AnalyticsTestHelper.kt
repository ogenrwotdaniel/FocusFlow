package com.focusflow.analytics

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for testing Firebase Analytics implementation.
 * This should only be used in debug builds.
 */
@Singleton
class AnalyticsTestHelper @Inject constructor(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics,
    private val analyticsTracker: AnalyticsTracker
) {
    companion object {
        private const val TAG = "AnalyticsTestHelper"
        private const val TEST_PREFIX = "test_"
    }

    /**
     * Send test events to verify analytics is working correctly
     * This should only be called from a debug build
     */
    fun sendTestEvents() {
        Log.d(TAG, "Sending test events to Firebase Analytics")
        
        // Test basic events
        analyticsTracker.logScreenView("${TEST_PREFIX}analytics_test", "AnalyticsTestHelper")
        
        // Test session events
        analyticsTracker.logSessionStarted(25, "${TEST_PREFIX}pomodoro")
        analyticsTracker.logSessionCompleted(25, 20, 0.8)
        
        // Test insight tracking
        analyticsTracker.logAppliedInsight(
            "${TEST_PREFIX}insight_1",
            "RECOMMENDATION"
        )
        
        // Test settings events
        analyticsTracker.logSettingsChanged(
            "${TEST_PREFIX}notification_enabled",
            "true"
        )
        
        // Test custom events
        analyticsTracker.logEvent(
            "${TEST_PREFIX}${AnalyticsTracker.Events.SCHEDULED_SESSION}",
            mapOf(
                "day_of_week" to "MONDAY",
                "time" to "09:00",
                "reason" to "Test reason"
            )
        )
        
        // Direct Firebase Analytics event for validation
        val params = Bundle().apply {
            putString("test_param_1", "test_value_1")
            putLong("test_param_2", 42)
        }
        firebaseAnalytics.logEvent("${TEST_PREFIX}direct_test_event", params)
        
        Log.d(TAG, "Test events sent successfully")
    }
    
    /**
     * Clear Firebase Analytics user properties for testing purposes
     * This should only be called from a debug build
     */
    fun resetAnalyticsData() {
        firebaseAnalytics.resetAnalyticsData()
        Log.d(TAG, "Analytics data reset")
    }
}
