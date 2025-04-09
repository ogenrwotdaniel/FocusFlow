package com.focusflow.service.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.focusflow.R
import com.focusflow.domain.model.SessionType
import com.focusflow.ui.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        const val TIMER_CHANNEL_ID = "timer_notifications"
        const val SESSION_NOTIFICATION_ID = 1001
        const val TIMER_NOTIFICATION_ID = 1002
        const val MOTIVATION_NOTIFICATION_ID = 1003
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create timer notification channel
            val timerChannel = NotificationChannel(
                TIMER_CHANNEL_ID,
                context.getString(R.string.timer_notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.timer_notification_channel_description)
            }

            // Register the channel with the system
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(timerChannel)
        }
    }

    fun createTimerNotification(
        sessionType: SessionType,
        title: String,
        message: String,
        timeRemaining: String,
        progress: Int
    ): NotificationCompat.Builder {
        // Create intent for notification tap action
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine icon based on session type
        val icon = if (sessionType == SessionType.FOCUS) {
            R.drawable.ic_focus
        } else {
            R.drawable.ic_break
        }

        return NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(icon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(100, progress, false)
            .addAction(
                R.drawable.ic_pause,
                context.getString(R.string.pause_resume),
                createServicePendingIntent("PAUSE_RESUME")
            )
            .addAction(
                R.drawable.ic_stop,
                context.getString(R.string.stop),
                createServicePendingIntent("STOP")
            )
    }

    fun createCompletionNotification(sessionType: SessionType, duration: Int): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            1, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (sessionType == SessionType.FOCUS) {
            context.getString(R.string.focus_session)
        } else {
            context.getString(R.string.break_session)
        }

        val message = if (sessionType == SessionType.FOCUS) {
            context.resources.getQuantityString(R.plurals.minutes, duration, duration)
        } else {
            context.getString(R.string.session_complete)
        }

        return NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
    }

    fun createMotivationalNotification(message: String): NotificationCompat.Builder {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            2, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, TIMER_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_focus)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
    }

    fun showTimerNotification(notification: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(TIMER_NOTIFICATION_ID, notification.build())
    }

    fun showCompletionNotification(notification: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(SESSION_NOTIFICATION_ID, notification.build())
    }

    fun showMotivationalNotification(notification: NotificationCompat.Builder) {
        NotificationManagerCompat.from(context).notify(MOTIVATION_NOTIFICATION_ID, notification.build())
    }

    private fun createServicePendingIntent(action: String): PendingIntent {
        val intent = Intent(context, TimerActionReceiver::class.java).apply {
            this.action = action
        }
        
        return PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
