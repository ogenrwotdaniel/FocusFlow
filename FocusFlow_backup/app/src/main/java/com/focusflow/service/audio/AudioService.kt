package com.focusflow.service.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.focusflow.FocusFlowApplication
import com.focusflow.R
import com.focusflow.data.model.AudioTracks
import com.focusflow.data.preference.UserPreferences
import com.focusflow.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AudioService : Service() {

    companion object {
        const val NOTIFICATION_ID = 100
        const val ACTION_START = "com.focusflow.audio.START"
        const val ACTION_PAUSE = "com.focusflow.audio.PAUSE"
        const val ACTION_RESUME = "com.focusflow.audio.RESUME"
        const val ACTION_STOP = "com.focusflow.audio.STOP"
        const val ACTION_SET_VOLUME = "com.focusflow.audio.SET_VOLUME"
        const val ACTION_SET_TRACK = "com.focusflow.audio.SET_TRACK"
        const val EXTRA_VOLUME = "com.focusflow.audio.VOLUME"
        const val EXTRA_TRACK_ID = "com.focusflow.audio.TRACK_ID"
    }

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var dataStore: DataStore<Preferences>

    private val binder = AudioBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume: Int = 50
    private var isPlaying = false
    private var currentTrackId: String = AudioTracks.NATURE
    private var currentTrackResId: Int = R.raw.nature_sounds
    
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Audio focus listener
    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Lost focus for an unbounded amount of time: pause playback and release media player
                pauseAudio()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus temporarily, pause playback but keep media player
                pauseAudio()
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Resume audio playback
                if (isPlaying) {
                    resumeAudio()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        createNotificationChannel()
        
        // Load user preferences
        serviceScope.launch {
            val prefs = userPreferences.userData.first()
            currentVolume = prefs.volume
            currentTrackId = prefs.audioTrack
            
            // Set up the initial audio track resource
            val track = AudioTracks.getById(currentTrackId)
            currentTrackResId = track.resourceId
            
            if (prefs.playBackgroundAudio) {
                initializeMediaPlayer()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startAudio()
            ACTION_PAUSE -> pauseAudio()
            ACTION_RESUME -> resumeAudio()
            ACTION_STOP -> stopAudio()
            ACTION_SET_VOLUME -> {
                val volume = intent.getIntExtra(EXTRA_VOLUME, currentVolume)
                setVolume(volume)
            }
            ACTION_SET_TRACK -> {
                val trackId = intent.getStringExtra(EXTRA_TRACK_ID) ?: currentTrackId
                serviceScope.launch {
                    userPreferences.saveAudioTrack(trackId)
                    val track = AudioTracks.getById(trackId)
                    setAudioTrack(track.resourceId)
                }
            }
        }
        
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        releaseMediaPlayer()
        releaseWakeLock()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                FocusFlowApplication.AUDIO_CHANNEL_ID,
                getString(R.string.audio_service_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.audio_service_channel_description)
                setShowBadge(false)
                setSound(null, null)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startAudio() {
        if (mediaPlayer == null) {
            initializeMediaPlayer()
        }
        
        if (requestAudioFocus()) {
            mediaPlayer?.start()
            isPlaying = true
            acquireWakeLock()
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    private fun initializeMediaPlayer() {
        if (mediaPlayer == null) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    
                    // Set the audio track based on user preference
                    setDataSource(resources.openRawResourceFd(currentTrackResId))
                    setVolume(currentVolume / 100f, currentVolume / 100f)
                    isLooping = true
                    prepareAsync()
                    
                    setOnPreparedListener {
                        // Only start playing if we have audio focus
                        if (requestAudioFocus()) {
                            start()
                            isPlaying = true
                            acquireWakeLock()
                            startForeground(NOTIFICATION_ID, createNotification())
                        }
                    }
                    
                    setOnErrorListener { _, what, extra ->
                        // Log error and reset media player
                        stopAudio()
                        true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                stopSelf()
            }
        }
    }

    private fun pauseAudio() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                isPlaying = false
                updateNotification()
            }
        }
    }

    private fun resumeAudio() {
        mediaPlayer?.let {
            if (!it.isPlaying && requestAudioFocus()) {
                it.start()
                isPlaying = true
                updateNotification()
            }
        }
    }

    private fun stopAudio() {
        releaseMediaPlayer()
        releaseWakeLock()
        stopForeground(true)
        stopSelf()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
                isPlaying = false
            }
            it.release()
            mediaPlayer = null
        }
        
        // Abandon audio focus
        abandonAudioFocus()
    }
    
    /**
     * Set the audio track to play
     * @param resourceId Resource ID of the audio track
     */
    fun setAudioTrack(resourceId: Int) {
        // Save the current resource ID
        currentTrackResId = resourceId
        
        // If currently playing, need to recreate media player with new track
        val wasPlaying = mediaPlayer?.isPlaying == true
        
        // Release current media player
        releaseMediaPlayer()
        
        // Create new media player with new track
        mediaPlayer = MediaPlayer().apply {
            setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            
            try {
                setDataSource(resources.openRawResourceFd(resourceId))
                setVolume(currentVolume / 100f, currentVolume / 100f)
                isLooping = true
                prepareAsync()
                
                setOnPreparedListener {
                    // Only restart if it was playing before
                    if (wasPlaying && requestAudioFocus()) {
                        start()
                        isPlaying = true
                        acquireWakeLock()
                        updateNotification()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        if (wasPlaying) {
            // Start foreground service if it was already playing
            startForeground(NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * Set the volume level for audio playback
     * @param volume Volume level (0-100)
     */
    fun setVolume(volume: Int) {
        currentVolume = volume.coerceIn(0, 100)
        mediaPlayer?.setVolume(currentVolume / 100f, currentVolume / 100f)
        
        // Save volume preference
        serviceScope.launch {
            userPreferences.updateVolume(currentVolume)
        }
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            
            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // Create pause/resume action
        val actionIntent = Intent(this, AudioService::class.java).apply {
            action = if (isPlaying) ACTION_PAUSE else ACTION_RESUME
        }
        val actionPendingIntent = PendingIntent.getService(
            this, 1, actionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val actionIcon = if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        val actionTitle = if (isPlaying) getString(R.string.pause) else getString(R.string.resume)
        
        return NotificationCompat.Builder(this, FocusFlowApplication.AUDIO_CHANNEL_ID)
            .setContentTitle(getString(R.string.focus_music))
            .setContentText(getString(R.string.playing_focus_music))
            .setSmallIcon(R.drawable.ic_music)
            .setContentIntent(pendingIntent)
            .addAction(actionIcon, actionTitle, actionPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "FocusFlow::AudioWakeLock"
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

    inner class AudioBinder : Binder() {
        fun getService(): AudioService = this@AudioService
    }
}
