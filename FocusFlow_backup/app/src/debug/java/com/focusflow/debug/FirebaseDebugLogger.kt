package com.focusflow.debug

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Debug utility for logging Firebase Analytics events during development.
 * This class is only included in debug builds.
 */
@Singleton
class FirebaseDebugLogger @Inject constructor(
    private val context: Context
) {
    private val eventCounter = AtomicInteger(0)
    
    companion object {
        private const val TAG = "FirebaseDebugLogger"
        
        /**
         * Enable Firebase Analytics debug mode via ADB:
         * adb shell setprop debug.firebase.analytics.app com.focusflow
         *
         * View logs in real-time:
         * adb shell "logcat -v time | grep -i -e FirebaseAnalytics -e FA"
         */
        fun printSetupInstructions() {
            Log.i(TAG, "To enable Firebase Analytics debug mode, run:")
            Log.i(TAG, "adb shell setprop debug.firebase.analytics.app com.focusflow")
            Log.i(TAG, "To view analytics events in real-time, run:")
            Log.i(TAG, "adb shell \"logcat -v time | grep -i -e FirebaseAnalytics -e FA\"")
        }
    }

    init {
        // Print setup instructions on initialization
        printSetupInstructions()
    }
    
    /**
     * Log a Firebase Analytics event for debugging purposes
     */
    fun logEvent(firebaseAnalytics: FirebaseAnalytics, name: String, params: Bundle?) {
        val count = eventCounter.incrementAndGet()
        
        Log.d(TAG, "[$count] Logging event: $name")
        if (params != null && params.size() > 0) {
            params.keySet().forEach { key -> 
                Log.d(TAG, "  - $key: ${params.get(key)}")
            }
        }
        
        // Log that we received the event through a toast for immediate visual feedback
        Toast.makeText(
            context, 
            "Analytics event sent: $name", 
            Toast.LENGTH_SHORT
        ).show()
    }
    
    /**
     * Register a global Firebase Analytics event listener for debugging
     */
    fun registerGlobalEventListener(firebaseAnalytics: FirebaseAnalytics) {
        Log.d(TAG, "Registering global Firebase Analytics event listener")
        
        // Note: This analytical event listener is a debugging feature
        // and should only be used in development builds
        firebaseAnalytics.setAnalyticsCollectionEnabled(true)
        
        // This feature logs all events that go through Firebase Analytics to logcat
        Log.d(TAG, "Firebase Analytics debugging is now active")
    }
}
