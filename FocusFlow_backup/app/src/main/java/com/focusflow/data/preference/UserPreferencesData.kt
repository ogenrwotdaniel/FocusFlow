package com.focusflow.data.preference

/**
 * Data class to represent all user preferences in a single object
 * Used for passing around complete user preferences
 */
data class UserPreferencesData(
    // Timer settings
    val focusSessionMinutes: Int = 25,
    val shortBreakMinutes: Int = 5,
    val longBreakMinutes: Int = 15,
    val sessionsBeforeLongBreak: Int = 4,
    
    // Auto-start settings
    val autoStartBreaks: Boolean = true,
    val autoStartFocus: Boolean = false,
    
    // Audio settings
    val volume: Int = 50,
    val audioVolume: Float = 0.5f,
    val playBackgroundAudio: Boolean = true,
    val audioTrack: String = "nature",
    
    // Appearance settings
    val treeType: String = "oak",
    val darkMode: String = "system"
)
