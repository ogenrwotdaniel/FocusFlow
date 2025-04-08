package com.focusflow.service.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.focusflow.service.timer.TimerService

/**
 * BroadcastReceiver to handle notification action buttons for the timer
 */
class TimerActionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        val timerServiceIntent = Intent(context, TimerService::class.java)
        
        when (intent.action) {
            "PAUSE_RESUME" -> {
                // Determine current timer state and toggle
                val actionIntent = Intent(context, TimerService::class.java)
                actionIntent.action = TimerService.ACTION_TOGGLE_PAUSE_RESUME
                context.startService(actionIntent)
            }
            "STOP" -> {
                // Stop the timer service
                val actionIntent = Intent(context, TimerService::class.java)
                actionIntent.action = TimerService.ACTION_STOP
                context.startService(actionIntent)
            }
        }
    }
}
