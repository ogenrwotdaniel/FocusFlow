package com.focusflow.data.model

import com.focusflow.ai.TimeRange
import java.time.DayOfWeek

/**
 * A data class representing a user's focus patterns and productivity trends.
 * This is used by the AI recommendation engine to generate personalized suggestions.
 */
data class FocusPattern(
    /**
     * The time ranges during the day when the user tends to be most productive
     */
    val optimalTimeRanges: List<TimeRange>,
    
    /**
     * The session duration (in minutes) that has resulted in the highest completion rate
     * and productivity for this user
     */
    val optimalDuration: Int,
    
    /**
     * The audio track that correlates with the highest productivity scores for this user
     */
    val preferredAudioTrack: String?,
    
    /**
     * The average number of focus sessions completed per active day
     */
    val averageSessionsPerDay: Double,
    
    /**
     * The day of the week when the user tends to be most productive (1 = Monday, 7 = Sunday)
     */
    val mostProductiveDay: Int
) {
    /**
     * Get the most productive day as a DayOfWeek enum value
     */
    fun getMostProductiveDayOfWeek(): DayOfWeek = DayOfWeek.of(mostProductiveDay)
    
    /**
     * Returns true if the pattern has enough data to make meaningful recommendations
     */
    fun hasEnoughData(): Boolean = optimalTimeRanges.isNotEmpty() && averageSessionsPerDay > 0
}

/**
 * Represents a focus session data point used for analysis and machine learning
 */
data class SessionData(
    val id: Long,
    val startTime: java.time.LocalDateTime,
    val durationMinutes: Int,
    val completed: Boolean,
    val audioTrackId: String?,
    val productivityScore: Double, // 0.0 to 10.0 rating (can be self-reported or calculated)
    val interruptionCount: Int = 0
)
