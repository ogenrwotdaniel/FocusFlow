package com.focusflow

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FocusFlowApplication : Application() {

    companion object {
        const val TIMER_CHANNEL_ID = "timer_channel"
        const val MOTIVATION_CHANNEL_ID = "motivation_channel"
        const val AUDIO_CHANNEL_ID = "audio_channel"
        const val SOCIAL_CHANNEL_ID = "social_channel"
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        firebaseAnalytics = Firebase.analytics
        
        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Timer channel for foreground service
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.timer_notification_channel_description)
                setShowBadge(false)
            }

            // Motivation channel for motivational messages
            val motivationChannel = NotificationChannel(
                MOTIVATION_CHANNEL_ID,
                getString(R.string.motivation_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.motivation_notification_channel_description)
            }
            
            // Audio service channel for background audio
            val audioChannel = NotificationChannel(
                AUDIO_CHANNEL_ID,
                getString(R.string.audio_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.audio_service_channel_description)
                setShowBadge(false)
                setSound(null, null) // No sound for audio service notifications
            }
            
            // Social notifications channel for group invites and updates
            val socialChannel = NotificationChannel(
                SOCIAL_CHANNEL_ID,
                getString(R.string.social_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.social_notification_channel_description)
            }

            // Register the channels with the system
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(timerChannel)
            notificationManager.createNotificationChannel(motivationChannel)
            notificationManager.createNotificationChannel(audioChannel)
            notificationManager.createNotificationChannel(socialChannel)
        }
    }
}
