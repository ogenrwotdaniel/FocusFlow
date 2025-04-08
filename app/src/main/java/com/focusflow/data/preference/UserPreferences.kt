package com.focusflow.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    // Preference keys
    companion object {
        private val KEY_FOCUS_MINUTES = intPreferencesKey("focus_minutes")
        private val KEY_SHORT_BREAK_MINUTES = intPreferencesKey("short_break_minutes")
        private val KEY_LONG_BREAK_MINUTES = intPreferencesKey("long_break_minutes")
        private val KEY_SESSIONS_BEFORE_LONG_BREAK = intPreferencesKey("sessions_before_long_break")
        private val KEY_AUTO_START_BREAKS = booleanPreferencesKey("auto_start_breaks")
        private val KEY_AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
        private val KEY_AUDIO_VOLUME = floatPreferencesKey("audio_volume")
        private val KEY_PLAY_BACKGROUND_AUDIO = booleanPreferencesKey("play_background_audio")
        private val KEY_AUDIO_TRACK = stringPreferencesKey("audio_track")
        private val KEY_TREE_TYPE = stringPreferencesKey("tree_type")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_VOLUME = intPreferencesKey("volume")
    }

    // Session settings
    fun getFocusSessionSettings(): LiveData<SessionSettings> {
        return dataStore.data.map { preferences ->
            SessionSettings(
                focusSessionMinutes = preferences[KEY_FOCUS_MINUTES] ?: 25,
                shortBreakMinutes = preferences[KEY_SHORT_BREAK_MINUTES] ?: 5,
                longBreakMinutes = preferences[KEY_LONG_BREAK_MINUTES] ?: 15,
                sessionsBeforeLongBreak = preferences[KEY_SESSIONS_BEFORE_LONG_BREAK] ?: 4
            )
        }.asLiveData()
    }

    suspend fun saveFocusMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_FOCUS_MINUTES] = minutes
        }
    }

    suspend fun saveShortBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SHORT_BREAK_MINUTES] = minutes
        }
    }

    suspend fun saveLongBreakMinutes(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LONG_BREAK_MINUTES] = minutes
        }
    }

    suspend fun saveSessionsBeforeLongBreak(sessions: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_SESSIONS_BEFORE_LONG_BREAK] = sessions
        }
    }

    // Auto-start settings
    fun getAutoStartSettings(): Flow<AutoStartSettings> {
        return dataStore.data.map { preferences ->
            AutoStartSettings(
                autoStartBreaks = preferences[KEY_AUTO_START_BREAKS] ?: true,
                autoStartFocus = preferences[KEY_AUTO_START_FOCUS] ?: false
            )
        }
    }

    suspend fun saveAutoStartBreaks(autoStart: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_BREAKS] = autoStart
        }
    }

    suspend fun saveAutoStartFocus(autoStart: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_AUTO_START_FOCUS] = autoStart
        }
    }

    // Audio settings
    fun getAudioVolume(): Flow<Float> {
        return dataStore.data.map { preferences ->
            preferences[KEY_AUDIO_VOLUME] ?: 0.5f
        }
    }

    suspend fun saveAudioVolume(volume: Float) {
        dataStore.edit { preferences ->
            preferences[KEY_AUDIO_VOLUME] = volume
        }
    }
    
    // Get volume as integer (0-100)
    fun getVolume(): Flow<Int> {
        return dataStore.data.map { preferences ->
            preferences[KEY_VOLUME] ?: 50
        }
    }
    
    // Save volume as integer (0-100)
    suspend fun updateVolume(volume: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_VOLUME] = volume.coerceIn(0, 100)
            // Also update the float version for backward compatibility
            preferences[KEY_AUDIO_VOLUME] = (volume.coerceIn(0, 100) / 100f)
        }
    }

    fun getPlayBackgroundAudio(): Flow<Boolean> {
        return dataStore.data.map { preferences ->
            preferences[KEY_PLAY_BACKGROUND_AUDIO] ?: true
        }
    }

    suspend fun savePlayBackgroundAudio(play: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_PLAY_BACKGROUND_AUDIO] = play
        }
    }

    fun getAudioTrack(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[KEY_AUDIO_TRACK] ?: "default"
        }
    }

    suspend fun saveAudioTrack(track: String) {
        dataStore.edit { preferences ->
            preferences[KEY_AUDIO_TRACK] = track
        }
    }

    // Tree type
    fun getTreeType(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[KEY_TREE_TYPE] ?: "oak"
        }
    }

    suspend fun saveTreeType(type: String) {
        dataStore.edit { preferences ->
            preferences[KEY_TREE_TYPE] = type
        }
    }

    // Dark mode
    fun getDarkMode(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[KEY_DARK_MODE] ?: "system"
        }
    }

    suspend fun saveDarkMode(mode: String) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_MODE] = mode
        }
    }

    // Get all user preferences as a single data object
    val userData: Flow<UserPreferencesData> = dataStore.data.map { preferences ->
        UserPreferencesData(
            // Timer settings
            focusSessionMinutes = preferences[KEY_FOCUS_MINUTES] ?: 25,
            shortBreakMinutes = preferences[KEY_SHORT_BREAK_MINUTES] ?: 5,
            longBreakMinutes = preferences[KEY_LONG_BREAK_MINUTES] ?: 15,
            sessionsBeforeLongBreak = preferences[KEY_SESSIONS_BEFORE_LONG_BREAK] ?: 4,
            
            // Auto-start settings
            autoStartBreaks = preferences[KEY_AUTO_START_BREAKS] ?: true,
            autoStartFocus = preferences[KEY_AUTO_START_FOCUS] ?: false,
            
            // Audio settings
            volume = preferences[KEY_VOLUME] ?: 50,
            audioVolume = preferences[KEY_AUDIO_VOLUME] ?: 0.5f,
            playBackgroundAudio = preferences[KEY_PLAY_BACKGROUND_AUDIO] ?: true,
            audioTrack = preferences[KEY_AUDIO_TRACK] ?: "nature",
            
            // Appearance settings
            treeType = preferences[KEY_TREE_TYPE] ?: "oak",
            darkMode = preferences[KEY_DARK_MODE] ?: "system"
        )
    }
}

data class SessionSettings(
    val focusSessionMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val sessionsBeforeLongBreak: Int
)

data class AutoStartSettings(
    val autoStartBreaks: Boolean,
    val autoStartFocus: Boolean
)

data class UserPreferencesData(
    val focusSessionMinutes: Int,
    val shortBreakMinutes: Int,
    val longBreakMinutes: Int,
    val sessionsBeforeLongBreak: Int,
    val autoStartBreaks: Boolean,
    val autoStartFocus: Boolean,
    val volume: Int,
    val audioVolume: Float,
    val playBackgroundAudio: Boolean,
    val audioTrack: String,
    val treeType: String,
    val darkMode: String
)
