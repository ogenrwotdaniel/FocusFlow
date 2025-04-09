package com.focusflow.analytics

import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Analyzes user productivity trends and patterns from historical data
 * to generate meaningful insights and recommendations.
 */
@Singleton
class TrendAnalyzer @Inject constructor(
    private val remoteConfigManager: RemoteConfigManager
) {
    /**
     * Represents a daily productivity metric
     */
    data class DailyProductivity(
        val date: LocalDate,
        val focusMinutes: Int,
        val sessionsCompleted: Int,
        val completionRate: Double
    )
    
    /**
     * Represents a productivity trend over time
     */
    data class ProductivityTrend(
        val trendType: TrendType,
        val metricName: String,
        val percentageChange: Double,
        val timePeriodDays: Int,
        val isImproving: Boolean
    )
    
    /**
     * Represents a productivity streak
     */
    data class ProductivityStreak(
        val streakType: StreakType,
        val currentStreakDays: Int,
        val bestStreakDays: Int,
        val isActive: Boolean
    )
    
    /**
     * Represents a productivity pattern
     */
    data class ProductivityPattern(
        val patternType: PatternType,
        val dayOfWeek: DayOfWeek? = null,
        val timeOfDay: LocalTime? = null,
        val metricName: String,
        val value: Double,
        val confidence: Double // 0.0 to 1.0
    )
    
    enum class TrendType {
        FOCUS_TIME,
        COMPLETION_RATE,
        SESSIONS_COMPLETED,
        OVERALL_PRODUCTIVITY
    }
    
    enum class StreakType {
        DAILY_GOAL_MET,
        COMPLETION_RATE_ABOVE_THRESHOLD,
        CONSECUTIVE_FOCUS_DAYS
    }
    
    enum class PatternType {
        MOST_PRODUCTIVE_DAY,
        MOST_PRODUCTIVE_TIME,
        LEAST_PRODUCTIVE_DAY,
        LONGEST_SESSIONS
    }
    
    /**
     * Calculate trends from a list of daily productivity data
     */
    fun calculateTrends(productivityData: List<DailyProductivity>): List<ProductivityTrend> {
        if (productivityData.size < 2) return emptyList()
        
        val lookbackDays = remoteConfigManager.getTrendsLookbackDays()
        val trends = mutableListOf<ProductivityTrend>()
        
        // Filter to only include data from the lookback period
        val filteredData = productivityData.filter { 
            it.date.isAfter(LocalDate.now().minusDays(lookbackDays.toLong())) 
        }
        
        if (filteredData.size < 2) return emptyList()
        
        // Calculate focus time trend
        trends.add(calculateFocusTimeTrend(filteredData, lookbackDays))
        
        // Calculate completion rate trend
        trends.add(calculateCompletionRateTrend(filteredData, lookbackDays))
        
        // Calculate sessions completed trend
        trends.add(calculateSessionsCompletedTrend(filteredData, lookbackDays))
        
        // Calculate overall productivity trend
        trends.add(calculateOverallProductivityTrend(filteredData, lookbackDays))
        
        return trends
    }
    
    /**
     * Calculate focus time trend
     */
    private fun calculateFocusTimeTrend(
        data: List<DailyProductivity>, 
        lookbackDays: Int
    ): ProductivityTrend {
        // Split data into two halves
        val midpoint = data.size / 2
        val firstHalf = data.subList(0, midpoint)
        val secondHalf = data.subList(midpoint, data.size)
        
        // Calculate averages
        val firstHalfAvg = firstHalf.map { it.focusMinutes }.average()
        val secondHalfAvg = secondHalf.map { it.focusMinutes }.average()
        
        // Calculate percentage change
        val percentChange = if (firstHalfAvg > 0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
        } else {
            0.0
        }
        
        return ProductivityTrend(
            trendType = TrendType.FOCUS_TIME,
            metricName = "focus minutes",
            percentageChange = percentChange,
            timePeriodDays = lookbackDays,
            isImproving = percentChange > 0
        )
    }
    
    /**
     * Calculate completion rate trend
     */
    private fun calculateCompletionRateTrend(
        data: List<DailyProductivity>, 
        lookbackDays: Int
    ): ProductivityTrend {
        // Split data into two halves
        val midpoint = data.size / 2
        val firstHalf = data.subList(0, midpoint)
        val secondHalf = data.subList(midpoint, data.size)
        
        // Calculate averages
        val firstHalfAvg = firstHalf.map { it.completionRate }.average()
        val secondHalfAvg = secondHalf.map { it.completionRate }.average()
        
        // Calculate percentage change
        val percentChange = if (firstHalfAvg > 0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
        } else {
            0.0
        }
        
        return ProductivityTrend(
            trendType = TrendType.COMPLETION_RATE,
            metricName = "completion rate",
            percentageChange = percentChange,
            timePeriodDays = lookbackDays,
            isImproving = percentChange > 0
        )
    }
    
    /**
     * Calculate sessions completed trend
     */
    private fun calculateSessionsCompletedTrend(
        data: List<DailyProductivity>, 
        lookbackDays: Int
    ): ProductivityTrend {
        // Split data into two halves
        val midpoint = data.size / 2
        val firstHalf = data.subList(0, midpoint)
        val secondHalf = data.subList(midpoint, data.size)
        
        // Calculate averages
        val firstHalfAvg = firstHalf.map { it.sessionsCompleted }.average()
        val secondHalfAvg = secondHalf.map { it.sessionsCompleted }.average()
        
        // Calculate percentage change
        val percentChange = if (firstHalfAvg > 0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
        } else {
            0.0
        }
        
        return ProductivityTrend(
            trendType = TrendType.SESSIONS_COMPLETED,
            metricName = "sessions completed",
            percentageChange = percentChange,
            timePeriodDays = lookbackDays,
            isImproving = percentChange > 0
        )
    }
    
    /**
     * Calculate overall productivity trend
     */
    private fun calculateOverallProductivityTrend(
        data: List<DailyProductivity>, 
        lookbackDays: Int
    ): ProductivityTrend {
        // Parse weights from remote config
        val weightsJson = remoteConfigManager.getProductivityScoreWeightsJson()
        val weights = try {
            val json = JSONObject(weightsJson)
            mapOf(
                "completion_rate" to json.optDouble("completion_rate", 0.5),
                "sessions_completed" to json.optDouble("sessions_completed", 0.3),
                "focus_duration" to json.optDouble("focus_duration", 0.2)
            )
        } catch (e: Exception) {
            mapOf(
                "completion_rate" to 0.5,
                "sessions_completed" to 0.3,
                "focus_duration" to 0.2
            )
        }
        
        // Calculate weighted productivity scores for each day
        val productivityScores = data.map { daily ->
            val completionRateScore = daily.completionRate * weights["completion_rate"]!!
            val sessionsScore = (daily.sessionsCompleted / 4.0) * weights["sessions_completed"]!! // Normalize to 0-1 assuming 4 sessions is "good"
            val focusScore = (daily.focusMinutes / 120.0) * weights["focus_duration"]!! // Normalize to 0-1 assuming 120 min is "good"
            
            // Combined score (0-100)
            (completionRateScore + sessionsScore + focusScore) * 100.0
        }
        
        // Split data into two halves
        val midpoint = productivityScores.size / 2
        val firstHalf = productivityScores.subList(0, midpoint)
        val secondHalf = productivityScores.subList(midpoint, productivityScores.size)
        
        // Calculate averages
        val firstHalfAvg = firstHalf.average()
        val secondHalfAvg = secondHalf.average()
        
        // Calculate percentage change
        val percentChange = if (firstHalfAvg > 0) {
            ((secondHalfAvg - firstHalfAvg) / firstHalfAvg) * 100.0
        } else {
            0.0
        }
        
        return ProductivityTrend(
            trendType = TrendType.OVERALL_PRODUCTIVITY,
            metricName = "overall productivity",
            percentageChange = percentChange,
            timePeriodDays = lookbackDays,
            isImproving = percentChange > 0
        )
    }
    
    /**
     * Find productivity patterns in user data
     */
    fun findPatterns(productivityData: List<DailyProductivity>): List<ProductivityPattern> {
        if (productivityData.isEmpty()) return emptyList()
        
        val patterns = mutableListOf<ProductivityPattern>()
        
        // Find most productive day of week
        patterns.add(findMostProductiveDayOfWeek(productivityData))
        
        // Additional patterns could be determined here...
        // - Most consistent day
        // - Best time of day (would require more granular data than daily)
        // - Longest session duration patterns
        
        return patterns
    }
    
    /**
     * Determine productivity streaks
     */
    fun calculateStreaks(productivityData: List<DailyProductivity>): ProductivityStreak {
        if (productivityData.isEmpty()) return ProductivityStreak(
            StreakType.DAILY_GOAL_MET, 0, 0, false
        )
        
        val sortedData = productivityData.sortedByDescending { it.date }
        val dailyGoalMinutes = remoteConfigManager.getDailyFocusGoalMinutes()
        
        var currentStreak = 0
        var bestStreak = 0
        var tempStreak = 0
        
        // Calculate current streak (consecutive days meeting goal)
        for (daily in sortedData) {
            if (daily.focusMinutes >= dailyGoalMinutes) {
                currentStreak++
                
                // Break if we've found a day that doesn't meet the goal
                if (daily.date != LocalDate.now().minusDays(currentStreak.toLong() - 1)) {
                    break
                }
            } else {
                break
            }
        }
        
        // Calculate best streak historically
        for (daily in sortedData.sortedBy { it.date }) {
            if (daily.focusMinutes >= dailyGoalMinutes) {
                tempStreak++
                if (tempStreak > bestStreak) {
                    bestStreak = tempStreak
                }
            } else {
                tempStreak = 0
            }
        }
        
        return ProductivityStreak(
            streakType = StreakType.DAILY_GOAL_MET,
            currentStreakDays = currentStreak,
            bestStreakDays = bestStreak,
            isActive = currentStreak > 0 && sortedData.firstOrNull()?.date == LocalDate.now()
        )
    }
    
    /**
     * Find the most productive day of the week based on historical data
     */
    private fun findMostProductiveDayOfWeek(productivityData: List<DailyProductivity>): ProductivityPattern {
        // Group data by day of week
        val dayData = productivityData.groupBy { it.date.dayOfWeek }
        
        // Calculate average productivity by day
        val dayProductivity = dayData.mapValues { (_, dailyList) ->
            // Calculate average focus minutes for this day
            dailyList.map { it.focusMinutes }.average()
        }
        
        // Find the most productive day
        val (mostProductiveDay, highestAvg) = dayProductivity.maxByOrNull { it.value } 
            ?: (DayOfWeek.MONDAY to 0.0)
        
        // Calculate confidence based on sample size and variance
        val confidence = calculateConfidence(dayData[mostProductiveDay] ?: emptyList())
        
        return ProductivityPattern(
            patternType = PatternType.MOST_PRODUCTIVE_DAY,
            dayOfWeek = mostProductiveDay,
            timeOfDay = null,
            metricName = "focus minutes",
            value = highestAvg,
            confidence = confidence
        )
    }
    
    /**
     * Calculate confidence score based on sample size and data consistency
     */
    private fun calculateConfidence(data: List<DailyProductivity>): Double {
        if (data.isEmpty()) return 0.0
        if (data.size == 1) return 0.5
        
        // More data points = higher confidence, up to a point
        val sampleSizeConfidence = minOf(data.size / 10.0, 0.7)
        
        // Calculate coefficient of variation (lower variance = higher consistency = higher confidence)
        val focusMinutes = data.map { it.focusMinutes }
        val mean = focusMinutes.average()
        val variance = focusMinutes.map { (it - mean) * (it - mean) }.sum() / focusMinutes.size
        val stdDev = Math.sqrt(variance)
        val cv = if (mean > 0) stdDev / mean else 1.0
        
        // Convert to a confidence score (0-0.3)
        val consistencyConfidence = 0.3 * (1.0 - minOf(cv, 1.0))
        
        // Combine the two confidence factors
        return sampleSizeConfidence + consistencyConfidence
    }
    
    /**
     * Generate human-readable insights from trend analysis
     */
    fun generateInsightsFromTrends(
        trends: List<ProductivityTrend>,
        patterns: List<ProductivityPattern>,
        streak: ProductivityStreak
    ): List<String> {
        val insights = mutableListOf<String>()
        
        // Process trend insights
        trends.forEach { trend ->
            val trendInsight = generateTrendInsight(trend)
            if (trendInsight.isNotEmpty()) {
                insights.add(trendInsight)
            }
        }
        
        // Process pattern insights
        patterns.forEach { pattern ->
            val patternInsight = generatePatternInsight(pattern)
            if (patternInsight.isNotEmpty()) {
                insights.add(patternInsight)
            }
        }
        
        // Process streak insights
        val streakInsight = generateStreakInsight(streak)
        if (streakInsight.isNotEmpty()) {
            insights.add(streakInsight)
        }
        
        return insights
    }
    
    /**
     * Generate an insight from a productivity trend
     */
    private fun generateTrendInsight(trend: ProductivityTrend): String {
        val formatter = DateTimeFormatter.ofPattern("MMM d")
        val startDate = LocalDate.now().minusDays(trend.timePeriodDays.toLong())
        val formattedStartDate = startDate.format(formatter)
        val formattedEndDate = LocalDate.now().format(formatter)
        
        val percentChange = String.format("%.1f", Math.abs(trend.percentageChange))
        val direction = if (trend.isImproving) "increased" else "decreased"
        val evaluation = if (trend.isImproving) {
            when {
                trend.percentageChange > 20 -> "significantly increased! Great job!"
                trend.percentageChange > 10 -> "shown good improvement. Keep it up!"
                else -> "slightly improved. You're on the right track."
            }
        } else {
            when {
                trend.percentageChange < -20 -> "significantly decreased. Consider what might be affecting your focus."
                trend.percentageChange < -10 -> "been trending down. Try to identify what changed."
                else -> "slightly decreased. This is a minor change and may not be significant."
            }
        }
        
        return when (trend.trendType) {
            TrendType.FOCUS_TIME -> "Since $formattedStartDate, your daily focus time has $evaluation Your total focus minutes have $direction by $percentChange% through $formattedEndDate."
            TrendType.COMPLETION_RATE -> "Your session completion rate has $evaluation The percentage of completed sessions has $direction by $percentChange% since $formattedStartDate."
            TrendType.SESSIONS_COMPLETED -> "The number of focus sessions you complete has $evaluation Your daily sessions have $direction by $percentChange% compared to earlier this month."
            TrendType.OVERALL_PRODUCTIVITY -> "Your overall productivity score has $evaluation Overall productivity has $direction by $percentChange% in the past ${trend.timePeriodDays} days."
        }
    }
    
    /**
     * Generate an insight from a productivity pattern
     */
    private fun generatePatternInsight(pattern: ProductivityPattern): String {
        // Skip low-confidence patterns
        if (pattern.confidence < 0.5) return ""
        
        return when (pattern.patternType) {
            PatternType.MOST_PRODUCTIVE_DAY -> {
                val dayName = pattern.dayOfWeek?.name?.toLowerCase()?.capitalize()
                val focusMinutes = pattern.value.toInt()
                
                if (pattern.confidence > 0.7) {
                    "$dayName is definitely your most productive day! You average $focusMinutes minutes of focus time on ${dayName}s. Consider scheduling your most important work on this day."
                } else {
                    "You seem to be most productive on ${dayName}s with an average of $focusMinutes focus minutes. This pattern is beginning to emerge in your data."
                }
            }
            PatternType.MOST_PRODUCTIVE_TIME -> {
                val timeDescription = pattern.timeOfDay?.let {
                    when (it.hour) {
                        in 5..11 -> "morning"
                        in 12..16 -> "afternoon"
                        in 17..21 -> "evening"
                        else -> "late night"
                    }
                } ?: "unknown time"
                
                "You appear to be most productive during the $timeDescription. Consider scheduling your most challenging tasks during this time."
            }
            PatternType.LEAST_PRODUCTIVE_DAY -> {
                val dayName = pattern.dayOfWeek?.name?.toLowerCase()?.capitalize()
                "Your focus tends to dip on ${dayName}s. Consider planning lighter workloads or more breaks on this day."
            }
            PatternType.LONGEST_SESSIONS -> {
                "Your longest focus sessions typically last around ${pattern.value.toInt()} minutes. This may be your optimal session length before needing a break."
            }
        }
    }
    
    /**
     * Generate an insight from a productivity streak
     */
    private fun generateStreakInsight(streak: ProductivityStreak): String {
        val goalMinutes = remoteConfigManager.getDailyFocusGoalMinutes()
        
        return when {
            streak.currentStreakDays > 0 && streak.isActive -> {
                when {
                    streak.currentStreakDays >= 30 -> "Incredible focus discipline! You've met your daily goal of $goalMinutes focus minutes for ${streak.currentStreakDays} days straight. This puts you in the top tier of focused individuals."
                    streak.currentStreakDays >= 14 -> "Outstanding streak! You've maintained your daily focus goal of $goalMinutes minutes for ${streak.currentStreakDays} consecutive days. Your consistency is building a powerful habit."
                    streak.currentStreakDays >= 7 -> "Great work! You've hit your daily focus goal of $goalMinutes minutes for ${streak.currentStreakDays} days in a row. You're building excellent momentum."
                    streak.currentStreakDays >= 3 -> "You're on a ${streak.currentStreakDays}-day streak of meeting your focus goal of $goalMinutes minutes. Keep it going!"
                    else -> "You've met your daily focus goal of $goalMinutes minutes. Now build on this success and extend your streak!"
                }
            }
            streak.bestStreakDays > 7 -> "Your best focus streak was ${streak.bestStreakDays} consecutive days meeting your goal of $goalMinutes minutes. Can you beat that record?"
            streak.bestStreakDays > 0 -> "Your longest streak so far is ${streak.bestStreakDays} days. Today is a great day to start a new streak!"
            else -> "Set a goal of $goalMinutes focused minutes each day and build your first streak!"
        }
    }
}
