package com.focusflow.ai

import android.content.Context
import com.focusflow.data.local.dao.SessionDao
import com.focusflow.data.model.FocusPattern
import com.focusflow.data.model.SessionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes user's focus session patterns to identify trends and optimal working conditions.
 * Uses historical session data to determine when the user is most productive.
 */
@Singleton
class FocusPatternAnalyzer @Inject constructor(
    private val sessionDao: SessionDao,
    private val context: Context
) {
    /**
     * Identifies the user's most productive times of day based on completed focus sessions.
     * @return Flow of time ranges when the user has highest focus scores
     */
    fun getOptimalFocusTimes(): Flow<List<TimeRange>> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            analyzeOptimalTimeRanges(sessions)
        }
    }
    
    /**
     * Identifies the optimal session duration based on the user's history of completed vs abandoned sessions.
     * @return Recommended focus session duration in minutes
     */
    fun getOptimalSessionDuration(): Flow<Int> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            calculateOptimalDuration(sessions)
        }
    }
    
    /**
     * Analyzes the relationship between background sounds and focus quality.
     * @return The audio track that correlates with highest focus scores
     */
    fun getMostEffectiveAudioTrack(): Flow<String?> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            findMostEffectiveAudio(sessions)
        }
    }
    
    /**
     * Generates a FocusPattern object that summarizes the user's productivity patterns.
     * This can be used by the RecommendationEngine to generate personalized suggestions.
     */
    fun generateUserFocusPattern(): Flow<FocusPattern> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            FocusPattern(
                optimalTimeRanges = analyzeOptimalTimeRanges(sessions),
                optimalDuration = calculateOptimalDuration(sessions),
                preferredAudioTrack = findMostEffectiveAudio(sessions),
                averageSessionsPerDay = calculateAverageSessionsPerDay(sessions),
                mostProductiveDay = findMostProductiveDay(sessions)
            )
        }
    }
    
    // Private analysis methods
    
    private fun analyzeOptimalTimeRanges(sessions: List<SessionData>): List<TimeRange> {
        // Group sessions by hour of day and calculate average productivity score
        val hourlyScores = sessions.groupBy { it.startTime.hour }
            .mapValues { (_, sessionsInHour) ->
                sessionsInHour.map { it.productivityScore }.average()
            }
        
        // Find the hours with above-average productivity
        val averageScore = hourlyScores.values.average()
        val productiveHours = hourlyScores.filter { it.value > averageScore }
            .keys.sorted()
        
        // Group consecutive productive hours into time ranges
        return groupConsecutiveHours(productiveHours)
    }
    
    private fun calculateOptimalDuration(sessions: List<SessionData>): Int {
        // Group sessions by duration and calculate completion rate and average productivity
        val durationEffectiveness = sessions.groupBy { it.durationMinutes }
            .mapValues { (_, sessionsWithDuration) ->
                val completionRate = sessionsWithDuration.count { it.completed } / 
                    sessionsWithDuration.size.toDouble()
                val avgProductivity = sessionsWithDuration.map { it.productivityScore }.average()
                completionRate * avgProductivity // Effectiveness score
            }
        
        // Return the duration with the highest effectiveness score
        return durationEffectiveness.maxByOrNull { it.value }?.key ?: 25 // Default to 25 if no data
    }
    
    private fun findMostEffectiveAudio(sessions: List<SessionData>): String? {
        if (sessions.isEmpty()) return null
        
        // Group sessions by audio track and calculate average productivity
        val audioEffectiveness = sessions.groupBy { it.audioTrackId }
            .mapValues { (_, sessionsWithTrack) ->
                sessionsWithTrack.map { it.productivityScore }.average()
            }
        
        // Return the track with highest average productivity
        return audioEffectiveness.maxByOrNull { it.value }?.key
    }
    
    private fun calculateAverageSessionsPerDay(sessions: List<SessionData>): Double {
        if (sessions.isEmpty()) return 0.0
        
        // Group sessions by date
        val sessionsByDate = sessions.groupBy { 
            LocalDate.ofEpochDay(it.startTime.toEpochDay())
        }
        
        // Calculate average sessions per active day
        return sessionsByDate.values.map { it.size }.average()
    }
    
    private fun findMostProductiveDay(sessions: List<SessionData>): Int {
        if (sessions.isEmpty()) return 1 // Monday as default
        
        // Group sessions by day of week and calculate average productivity
        val dayProductivity = sessions.groupBy { it.startTime.dayOfWeek.value }
            .mapValues { (_, sessionsOnDay) ->
                sessionsOnDay.map { it.productivityScore }.average()
            }
        
        // Return the day with highest productivity (1 = Monday, 7 = Sunday)
        return dayProductivity.maxByOrNull { it.value }?.key ?: 1
    }
    
    private fun groupConsecutiveHours(hours: List<Int>): List<TimeRange> {
        if (hours.isEmpty()) return emptyList()
        
        val ranges = mutableListOf<TimeRange>()
        var startHour = hours.first()
        var endHour = startHour
        
        for (i in 1 until hours.size) {
            if (hours[i] == endHour + 1) {
                // Continue the current range
                endHour = hours[i]
            } else {
                // End the current range and start a new one
                ranges.add(TimeRange(
                    start = LocalTime.of(startHour, 0),
                    end = LocalTime.of(endHour, 59)
                ))
                startHour = hours[i]
                endHour = startHour
            }
        }
        
        // Add the last range
        ranges.add(TimeRange(
            start = LocalTime.of(startHour, 0),
            end = LocalTime.of(endHour, 59)
        ))
        
        return ranges
    }
}

/**
 * Represents a time range during the day.
 */
data class TimeRange(
    val start: LocalTime,
    val end: LocalTime
)
