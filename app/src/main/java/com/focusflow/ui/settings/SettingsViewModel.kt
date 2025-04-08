package com.focusflow.ui.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.focusflow.data.preference.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    // Timer settings
    private val _focusMinutes = MutableLiveData<Int>()
    val focusMinutes: LiveData<Int> = _focusMinutes

    private val _shortBreakMinutes = MutableLiveData<Int>()
    val shortBreakMinutes: LiveData<Int> = _shortBreakMinutes

    private val _longBreakMinutes = MutableLiveData<Int>()
    val longBreakMinutes: LiveData<Int> = _longBreakMinutes

    private val _sessionsBeforeLongBreak = MutableLiveData<Int>()
    val sessionsBeforeLongBreak: LiveData<Int> = _sessionsBeforeLongBreak

    // Audio settings
    private val _playBackgroundAudio = MutableLiveData<Boolean>()
    val playBackgroundAudio: LiveData<Boolean> = _playBackgroundAudio

    private val _audioTrack = MutableLiveData<String>()
    val audioTrack: LiveData<String> = _audioTrack

    private val _volume = MutableLiveData<Float>()
    val volume: LiveData<Float> = _volume

    // Appearance settings
    private val _darkMode = MutableLiveData<String>()
    val darkMode: LiveData<String> = _darkMode

    private val _treeType = MutableLiveData<String>()
    val treeType: LiveData<String> = _treeType

    // Temporary settings before saving
    private var tempFocusMinutes = 25
    private var tempShortBreakMinutes = 5
    private var tempLongBreakMinutes = 15
    private var tempSessionsBeforeLongBreak = 4
    private var tempPlayBackgroundAudio = true
    private var tempAudioTrack = "binaural"
    private var tempVolume = 0.5f
    private var tempDarkMode = "system"
    private var tempTreeType = "oak"

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            userPreferences.getFocusSessionSettings().collect { settings ->
                tempFocusMinutes = settings.focusSessionMinutes
                tempShortBreakMinutes = settings.shortBreakMinutes
                tempLongBreakMinutes = settings.longBreakMinutes
                tempSessionsBeforeLongBreak = settings.sessionsBeforeLongBreak
                
                _focusMinutes.value = settings.focusSessionMinutes
                _shortBreakMinutes.value = settings.shortBreakMinutes
                _longBreakMinutes.value = settings.longBreakMinutes
                _sessionsBeforeLongBreak.value = settings.sessionsBeforeLongBreak
            }
            
            userPreferences.getPlayBackgroundAudio().collect { play ->
                tempPlayBackgroundAudio = play
                _playBackgroundAudio.value = play
            }
            
            userPreferences.getAudioTrack().collect { track ->
                tempAudioTrack = track
                _audioTrack.value = track
            }
            
            userPreferences.getAudioVolume().collect { level ->
                tempVolume = level
                _volume.value = level
            }
            
            userPreferences.getDarkMode().collect { mode ->
                tempDarkMode = mode
                _darkMode.value = mode
            }
            
            userPreferences.getTreeType().collect { type ->
                tempTreeType = type
                _treeType.value = type
            }
        }
    }

    // Timer settings setters
    fun setFocusMinutes(minutes: Int) {
        tempFocusMinutes = minutes
    }

    fun setShortBreakMinutes(minutes: Int) {
        tempShortBreakMinutes = minutes
    }

    fun setLongBreakMinutes(minutes: Int) {
        tempLongBreakMinutes = minutes
    }

    fun setSessionsBeforeLongBreak(sessions: Int) {
        tempSessionsBeforeLongBreak = sessions
    }

    // Audio settings setters
    fun setPlayBackgroundAudio(play: Boolean) {
        tempPlayBackgroundAudio = play
    }

    fun setAudioTrack(track: String) {
        tempAudioTrack = track
    }

    fun setVolume(volume: Float) {
        tempVolume = volume
    }

    // Appearance settings setters
    fun setDarkMode(mode: String) {
        tempDarkMode = mode
    }

    fun setTreeType(type: String) {
        tempTreeType = type
    }

    // Save all settings
    fun saveSettings() {
        viewModelScope.launch {
            userPreferences.saveFocusMinutes(tempFocusMinutes)
            userPreferences.saveShortBreakMinutes(tempShortBreakMinutes)
            userPreferences.saveLongBreakMinutes(tempLongBreakMinutes)
            userPreferences.saveSessionsBeforeLongBreak(tempSessionsBeforeLongBreak)
            userPreferences.savePlayBackgroundAudio(tempPlayBackgroundAudio)
            userPreferences.saveAudioTrack(tempAudioTrack)
            userPreferences.saveAudioVolume(tempVolume)
            userPreferences.saveDarkMode(tempDarkMode)
            userPreferences.saveTreeType(tempTreeType)
            
            // Update LiveData values
            _focusMinutes.value = tempFocusMinutes
            _shortBreakMinutes.value = tempShortBreakMinutes
            _longBreakMinutes.value = tempLongBreakMinutes
            _sessionsBeforeLongBreak.value = tempSessionsBeforeLongBreak
            _playBackgroundAudio.value = tempPlayBackgroundAudio
            _audioTrack.value = tempAudioTrack
            _volume.value = tempVolume
            _darkMode.value = tempDarkMode
            _treeType.value = tempTreeType
        }
    }
}
