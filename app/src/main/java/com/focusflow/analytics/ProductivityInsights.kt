package com.focusflow.analytics

import com.focusflow.ai.Insight
import com.focusflow.ai.InsightType
import com.focusflow.ai.RecommendationEngine
import com.focusflow.data.local.dao.SessionDao
import com.focusflow.data.model.FocusPattern
import com.focusflow.data.model.ProductivityMetric
import com.focusflow.data.model.SessionData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Provides detailed productivity analytics and insights based on the user's focus data.
 * Leverages both statistical analysis and machine learning predictions to generate
 * actionable insights and visualizations.
 */
@Singleton
class ProductivityInsights @Inject constructor(
    private val sessionDao: SessionDao,
    private val recommendationEngine: RecommendationEngine
) {
    /**
     * Get a comprehensive dashboard of productivity metrics over different time periods
     */
    fun getProductivityDashboard(): Flow<ProductivityDashboard> {
        return combine(
            getDailyMetrics(),
            getWeeklyMetrics(),
            getMonthlyMetrics(),
            recommendationEngine.getProductivityInsights()
        ) { daily, weekly, monthly, insights ->
            ProductivityDashboard(
                dailyMetrics = daily,
                weeklyMetrics = weekly,
                monthlyMetrics = monthly,
                topInsights = insights.take(3),
                streakDays = calculateCurrentStreak(),
                productivityScore = calculateProductivityScore(daily, weekly),
                focusDistribution = calculateFocusDistribution(),
                optimalFocusTimes = getOptimalFocusTimes()
            )
        }
    }
    
    /**
     * Get detailed statistics for the current day
     */
    fun getDailyMetrics(): Flow<ProductivityMetric> {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay()
        val endOfDay = today.plusDays(1).atStartOfDay().minusNanos(1)
        
        return getMetricsForPeriod(startOfDay, endOfDay, "Today")
    }
    
    /**
     * Get detailed statistics for the current week
     */
    fun getWeeklyMetrics(): Flow<ProductivityMetric> {
        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).atStartOfDay()
        val endOfWeek = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
            .plusDays(1).atStartOfDay().minusNanos(1)
        
        return getMetricsForPeriod(startOfWeek, endOfWeek, "This Week")
    }
    
    /**
     * Get detailed statistics for the current month
     */
    fun getMonthlyMetrics(): Flow<ProductivityMetric> {
        val today = LocalDate.now()
        val startOfMonth = today.withDayOfMonth(1).atStartOfDay()
        val endOfMonth = today.withDayOfMonth(today.lengthOfMonth())
            .plusDays(1).atStartOfDay().minusNanos(1)
        
        return getMetricsForPeriod(startOfMonth, endOfMonth, "This Month")
    }
    
    /**
     * Get detailed productivity metrics for a specific time period
     */
    private fun getMetricsForPeriod(
        startDateTime: LocalDateTime,
        endDateTime: LocalDateTime,
        periodLabel: String
    ): Flow<ProductivityMetric> {
        return sessionDao.getSessionsInTimeRange(
            startTimeMs = startDateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000,
            endTimeMs = endDateTime.toEpochSecond(java.time.ZoneOffset.UTC) * 1000
        ).map { sessions ->
            val totalSessions = sessions.size
            val completedSessions = sessions.count { it.completed }
            val totalFocusMinutes = sessions.sumOf { it.durationMinutes }
            val avgProductivity = if (sessions.isNotEmpty()) {
                sessions.map { it.productivityRating }.average().toFloat()
            } else 0f
            
            ProductivityMetric(
                label = periodLabel,
                totalSessions = totalSessions,
                completedSessions = completedSessions,
                completionRate = if (totalSessions > 0) {
                    (completedSessions.toFloat() / totalSessions) * 100
                } else 0f,
                totalFocusMinutes = totalFocusMinutes,
                averageSessionLength = if (totalSessions > 0) {
                    totalFocusMinutes / totalSessions
                } else 0,
                productivityRating = avgProductivity,
                mostProductiveTime = findMostProductiveTime(sessions),
                leastInterruptedTime = findLeastInterruptedTime(sessions)
            )
        }
    }
    
    /**
     * Calculate the current streak of consecutive days with completed focus sessions
     */
    private fun calculateCurrentStreak(): Flow<Int> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            var currentStreak = 0
            val today = LocalDate.now()
            
            // Group sessions by date
            val sessionsByDate = sessions.groupBy { 
                LocalDateTime.ofEpochSecond(it.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC)
                    .toLocalDate()
            }
            
            // Check consecutive days
            var currentDate = today
            while (true) {
                val hasSessionOnDate = sessionsByDate[currentDate]?.any { it.completed } == true
                if (hasSessionOnDate) {
                    currentStreak++
                    currentDate = currentDate.minusDays(1)
                } else {
                    break
                }
            }
            
            currentStreak
        }
    }
    
    /**
     * Calculate overall productivity score based on multiple metrics
     */
    private fun calculateProductivityScore(
        dailyMetrics: ProductivityMetric,
        weeklyMetrics: ProductivityMetric
    ): Int {
        // Base score starts at 50
        var score = 50
        
        // Daily completion rate contributes up to 15 points
        score += (dailyMetrics.completionRate / 100 * 15).roundToInt()
        
        // Weekly completion rate contributes up to 10 points
        score += (weeklyMetrics.completionRate / 100 * 10).roundToInt()
        
        // Daily productivity rating contributes up to 15 points
        score += (dailyMetrics.productivityRating / 10 * 15).roundToInt()
        
        // Daily focus minutes contribute up to 10 points (max of 120 minutes)
        score += (minOf(dailyMetrics.totalFocusMinutes, 120) / 120.0 * 10).roundToInt()
        
        return score.coerceIn(0, 100)
    }
    
    /**
     * Calculate the distribution of focus sessions across different times of day
     */
    private fun calculateFocusDistribution(): Flow<Map<String, Int>> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            val timeSlots = listOf(
                "Early Morning (5-8 AM)",
                "Morning (8-11 AM)",
                "Midday (11 AM-2 PM)",
                "Afternoon (2-5 PM)",
                "Evening (5-8 PM)",
                "Night (8-11 PM)",
                "Late Night (11 PM-5 AM)"
            )
            
            val distributionMap = mutableMapOf<String, Int>()
            timeSlots.forEach { slot -> distributionMap[slot] = 0 }
            
            sessions.forEach { session ->
                val hour = LocalDateTime.ofEpochSecond(
                    session.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC
                ).hour
                
                val slot = when (hour) {
                    in 5..7 -> "Early Morning (5-8 AM)"
                    in 8..10 -> "Morning (8-11 AM)"
                    in 11..13 -> "Midday (11 AM-2 PM)"
                    in 14..16 -> "Afternoon (2-5 PM)"
                    in 17..19 -> "Evening (5-8 PM)"
                    in 20..22 -> "Night (8-11 PM)"
                    else -> "Late Night (11 PM-5 AM)"
                }
                
                distributionMap[slot] = distributionMap[slot]!! + 1
            }
            
            distributionMap
        }
    }
    
    /**
     * Get the optimal focus times based on historical data
     */
    private fun getOptimalFocusTimes(): Flow<List<OptimalFocusTime>> {
        return sessionDao.getAllCompletedSessions().map { sessions ->
            // Group sessions by hour and calculate productivity
            val hourlyProductivity = (0..23).associateWith { hour ->
                val sessionsInHour = sessions.filter { 
                    LocalDateTime.ofEpochSecond(it.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC).hour == hour
                }
                
                if (sessionsInHour.isEmpty()) {
                    0.0
                } else {
                    val completionRate = sessionsInHour.count { it.completed }.toDouble() / sessionsInHour.size
                    val avgProductivity = sessionsInHour.map { it.productivityRating }.average()
                    completionRate * avgProductivity
                }
            }
            
            // Get the top 3 most productive hours
            hourlyProductivity.entries
                .sortedByDescending { it.value }
                .take(3)
                .map { (hour, score) ->
                    val formatter = DateTimeFormatter.ofPattern("h a")
                    val time = LocalDateTime.now().withHour(hour).withMinute(0)
                    OptimalFocusTime(
                        hour = hour,
                        formattedTime = time.format(formatter),
                        productivityScore = (score * 10).toFloat().coerceIn(0f, 10f)
                    )
                }
        }
    }
    
    /**
     * Find the most productive time of day based on session data
     */
    private fun findMostProductiveTime(sessions: List<SessionEntity>): String {
        if (sessions.isEmpty()) return "Not enough data"
        
        // Group sessions by hour and calculate productivity
        val hourlyProductivity = (0..23).associateWith { hour ->
            val sessionsInHour = sessions.filter { 
                LocalDateTime.ofEpochSecond(it.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC).hour == hour
            }
            
            if (sessionsInHour.isEmpty()) {
                0.0
            } else {
                val completionRate = sessionsInHour.count { it.completed }.toDouble() / sessionsInHour.size
                val avgProductivity = sessionsInHour.map { it.productivityRating }.average()
                completionRate * avgProductivity
            }
        }
        
        // Get the most productive hour
        val bestHour = hourlyProductivity.maxByOrNull { it.value }?.key ?: return "Not enough data"
        
        // Format the time
        val formatter = DateTimeFormatter.ofPattern("h a")
        return LocalDateTime.now().withHour(bestHour).withMinute(0).format(formatter)
    }
    
    /**
     * Find the time with the fewest interruptions
     */
    private fun findLeastInterruptedTime(sessions: List<SessionEntity>): String {
        if (sessions.isEmpty()) return "Not enough data"
        
        // Group sessions by hour and calculate average interruptions
        val hourlyInterruptions = (0..23).associateWith { hour ->
            val sessionsInHour = sessions.filter { 
                LocalDateTime.ofEpochSecond(it.startTimeMs / 1000, 0, java.time.ZoneOffset.UTC).hour == hour
            }
            
            if (sessionsInHour.isEmpty()) {
                Double.MAX_VALUE // High value so empty slots aren't chosen
            } else {
                sessionsInHour.map { it.interruptionCount }.average()
            }
        }
        
        // Get the hour with fewest interruptions
        val bestHour = hourlyInterruptions.minByOrNull { it.value }?.key ?: return "Not enough data"
        
        // Format the time
        val formatter = DateTimeFormatter.ofPattern("h a")
        return LocalDateTime.now().withHour(bestHour).withMinute(0).format(formatter)
    }
}

/**
 * Comprehensive dashboard of productivity metrics and insights
 */
data class ProductivityDashboard(
    val dailyMetrics: ProductivityMetric,
    val weeklyMetrics: ProductivityMetric,
    val monthlyMetrics: ProductivityMetric,
    val topInsights: List<Insight>,
    val streakDays: Int,
    val productivityScore: Int,
    val focusDistribution: Map<String, Int>,
    val optimalFocusTimes: List<OptimalFocusTime>
)

/**
 * Represents an optimal time for focusing
 */
data class OptimalFocusTime(
    val hour: Int,
    val formattedTime: String,
    val productivityScore: Float
)
