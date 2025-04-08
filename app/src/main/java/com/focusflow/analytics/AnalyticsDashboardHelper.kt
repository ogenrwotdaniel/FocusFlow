package com.focusflow.analytics

import com.focusflow.data.remote.FirebaseRepository
import com.focusflow.domain.model.FocusSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage analytics dashboard data with Firebase integration.
 * Provides methods to analyze focus sessions and generate productivity insights.
 */
@Singleton
class AnalyticsDashboardHelper @Inject constructor(
    private val firebaseRepository: FirebaseRepository
) {
    /**
     * Get user's productivity score based on focus sessions
     * @return productivity score (0-100)
     */
    suspend fun getProductivityScore(userId: String? = null): Int {
        val sessions = getUserSessions(userId)
        if (sessions.isEmpty()) return 0
        
        // Calculate score based on session completion rate and average focus time
        val completionRate = sessions.count { it.completed } / sessions.size.toDouble()
        val avgSessionLength = sessions.sumOf { it.actualFocusTimeMinutes } / sessions.size.toDouble()
        
        // Weight completion rate more heavily than session length
        val score = (completionRate * 0.7 + (avgSessionLength / 120) * 0.3) * 100
        
        // Store the score in Firebase for future reference
        userId?.let { uid ->
            firebaseRepository.updateUserStats(uid, mapOf("productivityScore" to score.toInt()))
        }
        
        return score.toInt().coerceIn(0, 100)
    }
    
    /**
     * Get productivity insights based on user's focus patterns
     * @return list of insight messages
     */
    suspend fun getProductivityInsights(userId: String? = null): List<String> {
        val sessions = getUserSessions(userId)
        if (sessions.isEmpty()) {
            return listOf("Complete your first focus session to get personalized insights!")
        }
        
        val insights = mutableListOf<String>()
        
        // Analyze completion rate
        val completionRate = sessions.count { it.completed } / sessions.size.toDouble()
        when {
            completionRate >= 0.9 -> insights.add("Great work! You have an excellent session completion rate of ${(completionRate * 100).toInt()}%")
            completionRate >= 0.7 -> insights.add("Your session completion rate is ${(completionRate * 100).toInt()}% - keep up the good work!")
            completionRate >= 0.5 -> insights.add("Try to improve your session completion rate of ${(completionRate * 100).toInt()}%")
            else -> insights.add("Focus on completing more sessions to improve your ${(completionRate * 100).toInt()}% completion rate")
        }
        
        // Analyze most productive day
        val productiveDays = sessions
            .groupBy { it.startTime.dayOfWeek }
            .mapValues { (_, sessions) -> sessions.sumOf { it.actualFocusTimeMinutes } }
        
        if (productiveDays.isNotEmpty()) {
            val mostProductiveDay = productiveDays.maxByOrNull { it.value }
            mostProductiveDay?.let {
                insights.add("Your most productive day is ${it.key.name.toLowerCase().capitalize()} with ${it.value} minutes of focus time")
            }
        }
        
        // Analyze most productive time of day
        val morningMinutes = sessions.filter { it.startTime.hour in 5..11 }.sumOf { it.actualFocusTimeMinutes }
        val afternoonMinutes = sessions.filter { it.startTime.hour in 12..17 }.sumOf { it.actualFocusTimeMinutes }
        val eveningMinutes = sessions.filter { it.startTime.hour in 18..23 }.sumOf { it.actualFocusTimeMinutes }
        val nightMinutes = sessions.filter { it.startTime.hour < 5 }.sumOf { it.actualFocusTimeMinutes }
        
        val maxMinutes = maxOf(morningMinutes, afternoonMinutes, eveningMinutes, nightMinutes)
        val productiveTimeOfDay = when (maxMinutes) {
            morningMinutes -> "morning (5AM-11AM)"
            afternoonMinutes -> "afternoon (12PM-5PM)"
            eveningMinutes -> "evening (6PM-11PM)"
            else -> "night (12AM-4AM)"
        }
        
        insights.add("You're most productive during the $productiveTimeOfDay")
        
        // Analyze focus trend
        val recentSessions = sessions.filter { it.startTime.isAfter(LocalDateTime.now().minusDays(14)) }
        if (recentSessions.size >= 3) {
            val oldestToNewest = recentSessions.sortedBy { it.startTime }
            val firstHalf = oldestToNewest.take(oldestToNewest.size / 2)
            val secondHalf = oldestToNewest.takeLast(oldestToNewest.size / 2)
            
            val firstHalfAvg = firstHalf.sumOf { it.actualFocusTimeMinutes } / firstHalf.size.toDouble()
            val secondHalfAvg = secondHalf.sumOf { it.actualFocusTimeMinutes } / secondHalf.size.toDouble()
            
            val percentChange = ((secondHalfAvg - firstHalfAvg) / firstHalfAvg * 100)
            
            when {
                percentChange >= 10 -> insights.add("Your focus time is trending up by ${percentChange.toInt()}% - excellent progress!")
                percentChange >= 5 -> insights.add("Your focus time is gradually improving")
                percentChange <= -10 -> insights.add("Your focus time has been decreasing lately. Consider adjusting your schedule.")
                percentChange <= -5 -> insights.add("Your focus time is slightly declining. Try to stay consistent.")
                else -> insights.add("Your focus time has been consistent recently")
            }
        }
        
        // Store insights in Firebase for future reference
        userId?.let { uid ->
            firebaseRepository.updateUserAnalytics(uid, mapOf(
                "lastUpdated" to LocalDateTime.now().toString(),
                "insights" to insights
            ))
        }
        
        return insights
    }
    
    /**
     * Get optimal focus times based on most productive sessions
     */
    suspend fun getOptimalFocusTimes(userId: String? = null): List<FocusTimeRecommendation> {
        val sessions = getUserSessions(userId)
        if (sessions.size < 3) {
            return listOf(
                FocusTimeRecommendation(DayOfWeek.MONDAY, "9:00 AM", "Try mornings to start your week"),
                FocusTimeRecommendation(DayOfWeek.WEDNESDAY, "2:00 PM", "Mid-week productivity boost"),
                FocusTimeRecommendation(DayOfWeek.SATURDAY, "10:00 AM", "Weekend focus session")
            )
        }
        
        // Find completed sessions with good focus time
        val productiveSessions = sessions.filter { 
            it.completed && it.actualFocusTimeMinutes >= 0.8 * it.plannedFocusTimeMinutes 
        }
        
        // Group by day of week
        val sessionsByDay = productiveSessions.groupBy { it.startTime.dayOfWeek }
        
        // For each day, find the most common hour to start sessions
        val recommendations = sessionsByDay.map { (day, daySessions) ->
            val hourCounts = daySessions.groupBy { it.startTime.hour }
                .mapValues { it.value.size }
            
            val bestHour = hourCounts.maxByOrNull { it.value }?.key ?: 9
            
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val timeString = LocalTime.of(bestHour, 0).format(formatter)
            
            val reason = when {
                daySessions.size >= 5 -> "Your most consistent productive day"
                hourCounts.values.max() >= 3 -> "You've had ${hourCounts.values.max()} successful sessions at this time"
                else -> "Based on your previous successful sessions"
            }
            
            FocusTimeRecommendation(day, timeString, reason)
        }
        
        // Ensure we have at least 3 recommendations
        val result = if (recommendations.size >= 3) {
            recommendations.sortedByDescending { 
                sessionsByDay[it.dayOfWeek]?.size ?: 0 
            }.take(3)
        } else {
            recommendations + listOf(
                FocusTimeRecommendation(DayOfWeek.MONDAY, "9:00 AM", "Monday morning focus to start the week"),
                FocusTimeRecommendation(DayOfWeek.WEDNESDAY, "2:00 PM", "Mid-week productivity boost"),
                FocusTimeRecommendation(DayOfWeek.SATURDAY, "10:00 AM", "Weekend focus session")
            ).filter { rec -> recommendations.none { it.dayOfWeek == rec.dayOfWeek } }
                .take(3 - recommendations.size)
        }
        
        // Store recommendations in Firebase
        userId?.let { uid ->
            val recommendationsMap = result.mapIndexed { index, rec ->
                "recommendation_$index" to mapOf(
                    "dayOfWeek" to rec.dayOfWeek.toString(),
                    "time" to rec.time,
                    "reason" to rec.reason
                )
            }.toMap()
            
            firebaseRepository.updateUserAnalytics(uid, mapOf("focusTimeRecommendations" to recommendationsMap))
        }
        
        return result
    }
    
    /**
     * Get visualization data for focus sessions over time
     */
    suspend fun getWeeklyFocusData(userId: String? = null, weeksBack: Int = 4): List<WeeklyFocusData> {
        val sessions = getUserSessions(userId)
        
        val startDate = LocalDate.now().minusWeeks(weeksBack.toLong())
        val weeklyData = mutableListOf<WeeklyFocusData>()
        
        for (weekOffset in 0 until weeksBack) {
            val weekStart = startDate.plusWeeks(weekOffset.toLong())
            val weekEnd = weekStart.plusDays(6)
            
            val weekSessions = sessions.filter { 
                val sessionDate = it.startTime.toLocalDate()
                sessionDate.isEqual(weekStart) || (sessionDate.isAfter(weekStart) && sessionDate.isBefore(weekEnd.plusDays(1)))
            }
            
            val totalMinutes = weekSessions.sumOf { it.actualFocusTimeMinutes }
            val completedSessions = weekSessions.count { it.completed }
            val totalSessions = weekSessions.size
            
            weeklyData.add(
                WeeklyFocusData(
                    weekStart = weekStart,
                    weekEnd = weekEnd,
                    totalFocusMinutes = totalMinutes,
                    totalSessions = totalSessions,
                    completedSessions = completedSessions
                )
            )
        }
        
        // Store in Firebase
        userId?.let { uid ->
            val weeklyDataMap = weeklyData.mapIndexed { index, data ->
                "week_$index" to mapOf(
                    "weekStart" to data.weekStart.toString(),
                    "weekEnd" to data.weekEnd.toString(),
                    "totalFocusMinutes" to data.totalFocusMinutes,
                    "totalSessions" to data.totalSessions,
                    "completedSessions" to data.completedSessions,
                    "completionRate" to data.getCompletionRate()
                )
            }.toMap()
            
            firebaseRepository.updateUserAnalytics(uid, mapOf("weeklyFocusData" to weeklyDataMap))
        }
        
        return weeklyData
    }
    
    /**
     * Get user's focus sessions from Firebase
     */
    private suspend fun getUserSessions(userId: String? = null): List<FocusSession> {
        val currentUserId = userId ?: firebaseRepository.getCurrentUser()?.uid
        return currentUserId?.let { uid ->
            firebaseRepository.getUserSessions(uid)
        } ?: emptyList()
    }
}

/**
 * Data class for focus time recommendations
 */
data class FocusTimeRecommendation(
    val dayOfWeek: DayOfWeek,
    val time: String,
    val reason: String
)

/**
 * Data class for weekly focus visualization
 */
data class WeeklyFocusData(
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val totalFocusMinutes: Int,
    val totalSessions: Int,
    val completedSessions: Int
) {
    fun getCompletionRate(): Double = 
        if (totalSessions > 0) completedSessions.toDouble() / totalSessions else 0.0
        
    fun getFormattedDateRange(): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d")
        return "${weekStart.format(formatter)} - ${weekEnd.format(formatter)}"
    }
}
