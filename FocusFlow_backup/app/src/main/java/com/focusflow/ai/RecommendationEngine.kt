package com.focusflow.ai

import com.focusflow.data.model.FocusPattern
import com.focusflow.data.preference.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates personalized recommendations for optimal focus sessions based on user patterns.
 * Uses the data from FocusPatternAnalyzer to create actionable suggestions.
 */
@Singleton
class RecommendationEngine @Inject constructor(
    private val focusPatternAnalyzer: FocusPatternAnalyzer,
    private val userPreferences: UserPreferences
) {
    /**
     * Generate a personalized recommendation for the current moment
     * @return A recommendation object containing actionable advice
     */
    fun getCurrentRecommendation(): Flow<Recommendation> {
        // Combine user preferences and focus pattern to generate recommendations
        return combine(
            userPreferences.userData,
            focusPatternAnalyzer.generateUserFocusPattern()
        ) { prefs, pattern ->
            generateContextAwareRecommendation(pattern, LocalDateTime.now())
        }
    }
    
    /**
     * Generate a recommendation for the best time to schedule a focus session
     * @return A recommendation for optimal future focus time
     */
    fun getNextOptimalFocusTime(): Flow<ScheduleRecommendation> {
        return focusPatternAnalyzer.generateUserFocusPattern().combine(
            focusPatternAnalyzer.getOptimalFocusTimes()
        ) { pattern, timeRanges ->
            suggestNextFocusTime(pattern, LocalDateTime.now(), timeRanges)
        }
    }
    
    /**
     * Get a recommended session setup based on user's historical data
     * @return Session parameters optimized for the user's productivity
     */
    fun getOptimalSessionSetup(): Flow<SessionSetupRecommendation> {
        return combine(
            focusPatternAnalyzer.getOptimalSessionDuration(),
            focusPatternAnalyzer.getMostEffectiveAudioTrack(),
            userPreferences.userData
        ) { optimalDuration, effectiveTrack, prefs ->
            SessionSetupRecommendation(
                focusDurationMinutes = optimalDuration,
                breakDurationMinutes = calculateOptimalBreak(optimalDuration),
                recommendedAudioTrack = effectiveTrack ?: prefs.audioTrack,
                suggestedVolume = prefs.volume, // Start with user's current preference
                explanation = generateSessionExplanation(optimalDuration, effectiveTrack)
            )
        }
    }
    
    /**
     * Get deep insights about the user's productivity patterns
     * @return A collection of insights derived from the user's focus data
     */
    fun getProductivityInsights(): Flow<List<Insight>> {
        return focusPatternAnalyzer.generateUserFocusPattern().map { pattern ->
            generateInsightsFromPattern(pattern)
        }
    }
    
    // Private helper methods
    
    private fun generateContextAwareRecommendation(pattern: FocusPattern, currentTime: LocalDateTime): Recommendation {
        // If we don't have enough data, provide a generic recommendation
        if (!pattern.hasEnoughData()) {
            return Recommendation(
                title = "Build Your Focus Habit",
                description = "Complete more focus sessions to get personalized recommendations.",
                actionable = "Try a 25-minute focus session now.",
                confidence = 0.5 // Medium confidence for generic advice
            )
        }
        
        // Check if current time is in one of the optimal time ranges
        val currentHour = currentTime.hour
        val isOptimalTime = pattern.optimalTimeRanges.any { 
            currentHour >= it.start.hour && currentHour <= it.end.hour 
        }
        
        // Check if it's the user's most productive day
        val isProductiveDay = currentTime.dayOfWeek.value == pattern.mostProductiveDay
        
        return when {
            isOptimalTime && isProductiveDay -> Recommendation(
                title = "Prime Focus Time",
                description = "This is one of your most productive times on your best day!",
                actionable = "Start a ${pattern.optimalDuration}-minute focus session to maximize productivity.",
                confidence = 0.9 // High confidence
            )
            isOptimalTime -> Recommendation(
                title = "Good Focus Window",
                description = "This time of day typically works well for your focus sessions.",
                actionable = "Consider a ${pattern.optimalDuration}-minute session now.",
                confidence = 0.75 // Above average confidence
            )
            isProductiveDay -> Recommendation(
                title = "Productive Day",
                description = "Today is typically one of your more productive days.",
                actionable = "Even though it's not your optimal time, a focus session could be effective.",
                confidence = 0.7 // Good confidence
            )
            else -> Recommendation(
                title = "Focus Opportunity",
                description = "While not your optimal time, any focused work builds the habit.",
                actionable = "Try a shorter session of ${pattern.optimalDuration - 5} minutes.",
                confidence = 0.6 // Moderate confidence
            )
        }
    }
    
    private fun suggestNextFocusTime(pattern: FocusPattern, currentTime: LocalDateTime, 
                                    timeRanges: List<TimeRange>): ScheduleRecommendation {
        if (timeRanges.isEmpty()) {
            return ScheduleRecommendation(
                suggestedTime = currentTime.plusHours(1).withMinute(0),
                confidence = 0.5,
                explanation = "We don't have enough data yet to determine your optimal focus time."
            )
        }
        
        // Find the next optimal time range
        val currentHour = currentTime.hour
        val currentMinute = currentTime.minute
        
        // First check if we're currently in an optimal range
        val currentRange = timeRanges.find { range ->
            currentHour >= range.start.hour && 
            currentHour <= range.end.hour && 
            !(currentHour == range.end.hour && currentMinute > range.end.minute)
        }
        
        // If we're in a range, suggest starting now
        if (currentRange != null) {
            return ScheduleRecommendation(
                suggestedTime = currentTime,
                confidence = 0.85,
                explanation = "You're currently in one of your optimal focus periods."
            )
        }
        
        // Otherwise find the next range
        val nextRange = timeRanges.find { range ->
            range.start.hour > currentHour || 
            (range.start.hour == currentHour && range.start.minute > currentMinute)
        } ?: timeRanges.first() // If no next range today, use the first range (for tomorrow)
        
        // Create a datetime for the suggestion
        val suggestedTime = if (nextRange.start.hour > currentHour ||
                            (nextRange.start.hour == currentHour && nextRange.start.minute > currentMinute)) {
            // Later today
            currentTime.withHour(nextRange.start.hour).withMinute(nextRange.start.minute)
        } else {
            // Tomorrow
            currentTime.plusDays(1).withHour(nextRange.start.hour).withMinute(nextRange.start.minute)
        }
        
        return ScheduleRecommendation(
            suggestedTime = suggestedTime,
            confidence = 0.8,
            explanation = "Based on your history, this is when you tend to be most focused."
        )
    }
    
    private fun calculateOptimalBreak(focusDuration: Int): Int {
        // A simple heuristic: longer focus sessions need proportionally longer breaks
        return when {
            focusDuration <= 25 -> 5
            focusDuration <= 45 -> 8
            focusDuration <= 60 -> 10
            else -> 15
        }
    }
    
    private fun generateSessionExplanation(duration: Int, audioTrack: String?): String {
        val trackExplanation = if (audioTrack != null) {
            "The '$audioTrack' background sound has historically boosted your focus."
        } else {
            "Try different background sounds to find what works best for you."
        }
        
        return "A $duration-minute session aligns with your completion patterns. $trackExplanation"
    }
    
    private fun generateInsightsFromPattern(pattern: FocusPattern): List<Insight> {
        val insights = mutableListOf<Insight>()
        
        // Only generate insights if we have enough data
        if (!pattern.hasEnoughData()) {
            insights.add(Insight(
                title = "Not Enough Data",
                description = "Complete more focus sessions to unlock personalized insights.",
                insightType = InsightType.GENERAL
            ))
            return insights
        }
        
        // Add time-based insight
        if (pattern.optimalTimeRanges.isNotEmpty()) {
            val timeRangesText = pattern.optimalTimeRanges.joinToString(", ") { 
                "${it.start.format(DateTimeFormatter.ofPattern("h:mm a"))} to " +
                "${it.end.format(DateTimeFormatter.ofPattern("h:mm a"))}" 
            }
            
            insights.add(Insight(
                title = "Your Productivity Peaks",
                description = "You tend to be most focused during: $timeRangesText",
                insightType = InsightType.TIME_PATTERN
            ))
        }
        
        // Add day-based insight
        val dayName = pattern.getMostProductiveDayOfWeek().name.lowercase().capitalize()
        insights.add(Insight(
            title = "$dayName Productivity",
            description = "Your focus sessions tend to be most effective on $dayName.",
            insightType = InsightType.DAY_PATTERN
        ))
        
        // Add duration insight
        insights.add(Insight(
            title = "Optimal Session Length",
            description = "You complete more sessions successfully when they're ${pattern.optimalDuration} minutes long.",
            insightType = InsightType.DURATION
        ))
        
        // Add audio insight if we have one
        pattern.preferredAudioTrack?.let { track ->
            insights.add(Insight(
                title = "Effective Background Sound",
                description = "The '$track' sound correlates with your most productive sessions.",
                insightType = InsightType.AUDIO
            ))
        }
        
        return insights
    }
}

/**
 * A recommendation for the user based on their focus patterns
 */
data class Recommendation(
    val title: String,
    val description: String,
    val actionable: String,
    val confidence: Double // 0.0 to 1.0 indicating ML confidence in this recommendation
)

/**
 * A recommendation for when to schedule a focus session
 */
data class ScheduleRecommendation(
    val suggestedTime: LocalDateTime,
    val confidence: Double,
    val explanation: String
)

/**
 * A recommendation for optimal session parameters
 */
data class SessionSetupRecommendation(
    val focusDurationMinutes: Int,
    val breakDurationMinutes: Int,
    val recommendedAudioTrack: String,
    val suggestedVolume: Int,
    val explanation: String
)

/**
 * An insight about the user's productivity patterns
 */
data class Insight(
    val title: String,
    val description: String,
    val insightType: InsightType
)

/**
 * Types of productivity insights
 */
enum class InsightType {
    TIME_PATTERN,
    DAY_PATTERN,
    DURATION,
    AUDIO,
    GENERAL
}
