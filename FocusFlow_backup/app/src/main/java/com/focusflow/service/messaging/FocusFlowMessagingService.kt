package com.focusflow.service.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.focusflow.FocusFlowApplication
import com.focusflow.R
import com.focusflow.data.remote.FirebaseRepository
import com.focusflow.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for handling push notifications.
 * This service handles new tokens and incoming messages for social features
 * and focus session reminders.
 */
@AndroidEntryPoint
class FocusFlowMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "FocusFlowMessaging"
    }

    @Inject
    lateinit var firebaseRepository: FirebaseRepository

    /**
     * Called when a new token is generated.
     * This token needs to be saved to the user's profile for sending messages.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "New FCM token: $token")
        saveFcmToken(token)
    }

    /**
     * Called when a message is received.
     * Handles both notification messages (which show up automatically) and
     * data messages (which we need to process ourselves).
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if the message contains data
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Check if the message contains a notification
        remoteMessage.notification?.let {
            Log.d(TAG, "Notification: ${it.title} - ${it.body}")
            sendNotification(it.title ?: getString(R.string.app_name), it.body ?: "")
        }
    }

    /**
     * Process data messages to update app state or show custom notifications.
     */
    private fun handleDataMessage(data: Map<String, String>) {
        when (data["type"]) {
            "social_invite" -> {
                // Show a notification for social invites
                val title = data["title"] ?: getString(R.string.new_invitation)
                val message = data["message"] ?: getString(R.string.you_have_been_invited)
                val sessionId = data["sessionId"]
                
                if (sessionId != null) {
                    sendNotification(title, message, sessionId)
                } else {
                    sendNotification(title, message)
                }
            }
            "focus_reminder" -> {
                // Show a notification for scheduled focus sessions
                val title = getString(R.string.focus_reminder)
                val message = data["message"] ?: getString(R.string.time_to_focus)
                sendNotification(title, message)
            }
            "analytics_update" -> {
                // Silently update analytics data
                val userId = data["userId"]
                if (userId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Update analytics data
                            val analyticsUpdate = data.filterKeys { 
                                it != "type" && it != "userId" 
                            }
                            if (analyticsUpdate.isNotEmpty()) {
                                firebaseRepository.updateUserAnalytics(userId, analyticsUpdate)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error updating analytics", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Save the FCM token to the user's profile.
     */
    private fun saveFcmToken(token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = firebaseRepository.getCurrentUser()
                user?.uid?.let { userId ->
                    firebaseRepository.updateUserSettings(userId, mapOf("fcmToken" to token))
                    Log.d(TAG, "FCM token saved for user ${user.uid}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save FCM token", e)
            }
        }
    }

    /**
     * Create and show a notification with the given title and message.
     */
    private fun sendNotification(title: String, message: String, sessionId: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            if (sessionId != null) {
                putExtra("sessionId", sessionId)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = FocusFlowApplication.SOCIAL_CHANNEL_ID
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // For Oreo and above, create the channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.social_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = getString(R.string.social_notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0, notificationBuilder.build())
    }
}
