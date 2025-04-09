package com.focusflow.service.timer

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.focusflow.FocusFlowApplication.Companion.TIMER_CHANNEL_ID
import com.focusflow.R
import com.focusflow.data.repository.SessionRepository
import com.focusflow.data.repository.TreeRepository
import com.focusflow.domain.model.*
import com.focusflow.service.audio.AudioService
import com.focusflow.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import kotlin.math.max

@AndroidEntryPoint
class TimerService : Service() {

    @Inject
    lateinit var sessionRepository: SessionRepository
    
    @Inject
    lateinit var treeRepository: TreeRepository
    
    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceRunning = false
    
    private var timerJob: Job? = null
    private var currentSessionId: Long = 0
    private var currentTreeId: Long = 0
    
    private val _timerInfo = MutableLiveData<TimerInfo>()
    val timerInfo: LiveData<TimerInfo> = _timerInfo
    
    private var startTimeMs: Long = 0
    private var pausedTimeMs: Long = 0
    private var totalPausedMs: Long = 0
    
    companion object {
        const val NOTIFICATION_ID = 1
        const val ACTION_START = "com.focusflow.action.START"
        const val ACTION_PAUSE = "com.focusflow.action.PAUSE"
        const val ACTION_RESUME = "com.focusflow.action.RESUME"
        const val ACTION_STOP = "com.focusflow.action.STOP"
        const val ACTION_SKIP_BREAK = "com.focusflow.action.SKIP_BREAK"
        const val EXTRA_SESSION_TYPE = "com.focusflow.extra.SESSION_TYPE"
        const val EXTRA_DURATION_MINUTES = "com.focusflow.extra.DURATION_MINUTES"
    }
    
    override fun onCreate() {
        super.onCreate()
        _timerInfo.value = TimerInfo()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val sessionType = intent.getStringExtra(EXTRA_SESSION_TYPE)?.let {
                    SessionType.valueOf(it)
                } ?: SessionType.FOCUS
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 25)
                
                startTimer(sessionType, durationMinutes)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
            ACTION_SKIP_BREAK -> skipBreak()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder {
        return binder
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        releaseWakeLock()
    }
    
    private fun startTimer(sessionType: SessionType, durationMinutes: Int) {
        if (isServiceRunning && timerInfo.value?.isActive() == true) {
            stopTimer()
        }
        
        isServiceRunning = true
        acquireWakeLock()
        
        val totalDurationMs = durationMinutes * 60 * 1000L
        val newTimerInfo = TimerInfo(
            sessionType = sessionType,
            state = TimerState.RUNNING,
            totalDurationMs = totalDurationMs,
            remainingTimeMs = totalDurationMs,
            elapsedTimeMs = 0,
            progressPercent = 0f
        )
        _timerInfo.value = newTimerInfo
        
        serviceScope.launch {
            // Create a new session in the database
            val session = if (sessionType == SessionType.FOCUS) {
                Session.createFocusSession(durationMinutes)
            } else {
                Session.createBreakSession(durationMinutes)
            }
            
            currentSessionId = sessionRepository.createSession(session)
            _timerInfo.value = _timerInfo.value?.copy(currentSessionId = currentSessionId)
            
            // If it's a focus session, create a tree
            if (sessionType == SessionType.FOCUS) {
                val tree = Tree(
                    treeType = TreeType.MAPLE, // Default type, could be based on user selection
                    growthStage = GrowthStage.SEED,
                    sessionId = currentSessionId
                )
                currentTreeId = treeRepository.createTree(tree)
                
                // Start audio for focus session
                startAudioService()
            }
            
            startTimeMs = System.currentTimeMillis()
            totalPausedMs = 0
            
            startForeground(NOTIFICATION_ID, createNotification(newTimerInfo))
            timerJob = startTimerJob()
            
            // Send motivation message
            if (sessionType == SessionType.FOCUS) {
                sendMotivationNotification(getString(R.string.focus_start_message))
            } else {
                sendMotivationNotification(getString(R.string.break_start_message))
                
                // Stop audio for break
                stopAudioService()
            }
        }
    }
    
    private fun pauseTimer() {
        if (timerInfo.value?.isRunning() == true) {
            timerJob?.cancel()
            pausedTimeMs = System.currentTimeMillis()
            
            val updatedInfo = timerInfo.value?.copy(state = TimerState.PAUSED)
            _timerInfo.value = updatedInfo
            
            updateNotification(updatedInfo)
            
            // Pause audio
            pauseAudioService()
        }
    }
    
    private fun resumeTimer() {
        val currentInfo = timerInfo.value ?: return
        
        if (currentInfo.isPaused()) {
            val currentPauseDuration = System.currentTimeMillis() - pausedTimeMs
            totalPausedMs += currentPauseDuration
            
            val updatedInfo = currentInfo.copy(state = TimerState.RUNNING)
            _timerInfo.value = updatedInfo
            
            updateNotification(updatedInfo)
            
            timerJob = startTimerJob()
            
            // Resume audio if it's a focus session
            if (currentInfo.isFocusSession()) {
                resumeAudioService()
            }
        }
    }
    
    private fun stopTimer() {
        serviceScope.launch {
            val currentInfo = timerInfo.value ?: return@launch
            timerJob?.cancel()
            
            // Update session in database
            val session = sessionRepository.getSessionById(currentSessionId)
            session?.let {
                val isCompleted = false // Session was manually stopped
                val endTime = Date()
                
                val updatedSession = it.copy(
                    endTime = endTime,
                    isCompleted = isCompleted
                )
                
                sessionRepository.updateSession(updatedSession)
                
                // Update tree if focus session
                if (currentInfo.isFocusSession()) {
                    treeRepository.getTreeById(currentTreeId)?.let { tree ->
                        val updatedTree = tree.withered() // Mark tree as withered since session was abandoned
                        treeRepository.updateTree(updatedTree)
                    }
                    
                    // Stop audio
                    stopAudioService()
                }
            }
            
            val updatedInfo = TimerInfo()
            _timerInfo.value = updatedInfo
            
            stopForeground(true)
            stopSelf()
            isServiceRunning = false
            releaseWakeLock()
        }
    }
    
    private fun skipBreak() {
        if (timerInfo.value?.isBreakSession() == true) {
            serviceScope.launch {
                // Mark break session as completed
                val session = sessionRepository.getSessionById(currentSessionId)
                session?.let {
                    val updatedSession = it.copy(
                        endTime = Date(),
                        isCompleted = true
                    )
                    sessionRepository.updateSession(updatedSession)
                }
                
                stopTimer()
            }
        }
    }
    
    private fun startTimerJob(): Job = serviceScope.launch {
        while (isActive) {
            val currentTimeMs = System.currentTimeMillis()
            val elapsedTimeMs = currentTimeMs - startTimeMs - totalPausedMs
            val currentInfo = timerInfo.value ?: continue
            
            val remainingTimeMs = max(0, currentInfo.totalDurationMs - elapsedTimeMs)
            val progressPercent = (elapsedTimeMs.toFloat() / currentInfo.totalDurationMs).coerceIn(0f, 1f)
            
            val updatedInfo = currentInfo.copy(
                remainingTimeMs = remainingTimeMs,
                elapsedTimeMs = elapsedTimeMs,
                progressPercent = progressPercent
            )
            _timerInfo.value = updatedInfo
            
            updateNotification(updatedInfo)
            
            // Update tree growth for focus sessions
            if (currentInfo.isFocusSession()) {
                updateTreeGrowth(progressPercent)
            }
            
            // Check for session completion
            if (remainingTimeMs <= 0) {
                completeSession()
                break
            }
            
            // Check for break ending soon notification
            if (currentInfo.isBreakSession() && remainingTimeMs <= 30000 && remainingTimeMs > 29000) {
                sendMotivationNotification(getString(R.string.break_ending_soon))
            }
            
            delay(1000)
        }
    }
    
    private suspend fun completeSession() {
        val currentInfo = timerInfo.value ?: return
        
        // Update session in database
        val session = sessionRepository.getSessionById(currentSessionId)
        session?.let {
            val updatedSession = it.copy(
                endTime = Date(),
                isCompleted = true
            )
            sessionRepository.updateSession(updatedSession)
            
            // Update tree if focus session
            if (currentInfo.isFocusSession()) {
                treeRepository.getTreeById(currentTreeId)?.let { tree ->
                    val updatedTree = tree.completed()
                    treeRepository.updateTree(updatedTree)
                }
                
                // Stop audio
                stopAudioService()
            }
        }
        
        val updatedInfo = currentInfo.copy(
            state = TimerState.FINISHED,
            remainingTimeMs = 0,
            progressPercent = 1f
        )
        _timerInfo.value = updatedInfo
        
        updateNotification(updatedInfo)
        
        // Send completion notification
        sendSessionCompleteNotification()
        
        // Stop service
        stopForeground(true)
        stopSelf()
        isServiceRunning = false
        releaseWakeLock()
    }
    
    private suspend fun updateTreeGrowth(progressPercent: Float) {
        treeRepository.getTreeById(currentTreeId)?.let { tree ->
            val updatedTree = tree.withUpdatedGrowthStage(progressPercent)
            if (updatedTree.growthStage != tree.growthStage) {
                treeRepository.updateTree(updatedTree)
            }
        }
    }
    
    private fun createNotification(timerInfo: TimerInfo?): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val pauseResumeAction = if (timerInfo?.isRunning() == true) {
            NotificationCompat.Action(
                R.drawable.ic_pause,
                getString(R.string.pause),
                PendingIntent.getService(
                    this, 1,
                    Intent(this, TimerService::class.java).setAction(ACTION_PAUSE),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else {
            NotificationCompat.Action(
                R.drawable.ic_resume,
                getString(R.string.resume),
                PendingIntent.getService(
                    this, 2,
                    Intent(this, TimerService::class.java).setAction(ACTION_RESUME),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        }
        
        val stopAction = NotificationCompat.Action(
            R.drawable.ic_stop,
            getString(R.string.stop),
            PendingIntent.getService(
                this, 3,
                Intent(this, TimerService::class.java).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        
        val skipAction = if (timerInfo?.isBreakSession() == true) {
            NotificationCompat.Action(
                R.drawable.ic_skip,
                getString(R.string.skip_break),
                PendingIntent.getService(
                    this, 4,
                    Intent(this, TimerService::class.java).setAction(ACTION_SKIP_BREAK),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
        } else null
        
        val minutes = (timerInfo?.remainingTimeMs ?: 0) / 1000 / 60
        val seconds = ((timerInfo?.remainingTimeMs ?: 0) / 1000) % 60
        val timeString = String.format("%02d:%02d", minutes, seconds)
        
        val sessionLabel = if (timerInfo?.isFocusSession() == true) {
            getString(R.string.focus_session)
        } else {
            getString(R.string.break_session)
        }
        
        val builder = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle(sessionLabel)
            .setContentText(timeString)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(pauseResumeAction)
            .addAction(stopAction)
        
        skipAction?.let { builder.addAction(it) }
        
        return builder.build()
    }
    
    private fun updateNotification(timerInfo: TimerInfo?) {
        val notification = createNotification(timerInfo)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun sendMotivationNotification(message: String) {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 10, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_motivation)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(2, notification)
    }
    
    private fun sendSessionCompleteNotification() {
        val currentInfo = timerInfo.value ?: return
        
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 11, notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val durationMinutes = (currentInfo.totalDurationMs / 1000 / 60).toInt()
        val message = if (currentInfo.isFocusSession()) {
            getString(R.string.focus_complete_message, durationMinutes)
        } else {
            getString(R.string.session_complete)
        }
        
        val notification = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setContentTitle(getString(R.string.congratulations))
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_success)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(3, notification)
    }
    
    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FocusFlow::TimerWakeLock"
            )
            wakeLock?.acquire()
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
            }
            wakeLock = null
        }
    }
    
    private fun startAudioService() {
        Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_START
        }.also {
            startService(it)
        }
    }
    
    private fun stopAudioService() {
        Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_STOP
        }.also {
            startService(it)
        }
    }
    
    private fun pauseAudioService() {
        Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_PAUSE
        }.also {
            startService(it)
        }
    }
    
    private fun resumeAudioService() {
        Intent(this, AudioService::class.java).apply {
            action = AudioService.ACTION_RESUME
        }.also {
            startService(it)
        }
    }
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
}
