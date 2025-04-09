package com.focusflow.ai.model

import android.content.Context
import android.util.Log
import com.focusflow.data.model.SessionData
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Machine learning model that predicts optimal focus patterns based on user data.
 * Uses TensorFlow Lite to handle on-device predictions for personalized recommendations.
 */
@Singleton
class FocusPatternPredictor @Inject constructor(
    private val context: Context
) {
    private var interpreter: Interpreter? = null
    private val modelName = "focus_pattern_model.tflite"
    private val inputSize = 12 // Number of input features
    private val outputSize = 4 // Number of output predictions

    init {
        try {
            // Check if model exists in assets
            if (assetExists(context, modelName)) {
                loadModelFromAssets()
            } else {
                Log.d(TAG, "Model file not found, will use heuristic predictions instead")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TensorFlow Lite model", e)
        }
    }

    /**
     * Predict the optimal focus time for the user based on their focus history
     * @param sessions List of previous focus sessions
     * @param currentTime Current time to use for prediction context
     * @return Predicted optimal focus duration in minutes
     */
    fun predictOptimalFocusDuration(sessions: List<SessionData>, currentTime: LocalDateTime): Int {
        if (sessions.isEmpty()) {
            return DEFAULT_FOCUS_DURATION
        }

        // If model is available, use ML prediction
        if (interpreter != null) {
            return predictWithModel(sessions, currentTime)
        }

        // Fallback to heuristic approach
        return predictWithHeuristic(sessions)
    }

    /**
     * Predict the optimal time of day for focusing
     * @param sessions List of previous focus sessions
     * @return Optimal hour of day (0-23)
     */
    fun predictOptimalTimeOfDay(sessions: List<SessionData>): Int {
        if (sessions.isEmpty()) {
            return DEFAULT_OPTIMAL_HOUR
        }

        // If model is available, use ML prediction
        if (interpreter != null) {
            val predictions = runInference(prepareFeatures(sessions))
            return predictions[1].toInt()
        }

        // Fallback to heuristic approach
        return findMostProductiveHour(sessions)
    }

    /**
     * Predict the likelihood of successfully completing a focus session at the current time
     * @param sessions List of previous focus sessions
     * @param currentTime Current time to make the prediction for
     * @return Probability of successful completion (0.0-1.0)
     */
    fun predictSessionSuccessProbability(
        sessions: List<SessionData>,
        currentTime: LocalDateTime
    ): Float {
        if (sessions.isEmpty()) {
            return DEFAULT_SUCCESS_PROBABILITY
        }

        // If model is available, use ML prediction
        if (interpreter != null) {
            val features = prepareFeatures(sessions, currentTime)
            val predictions = runInference(features)
            return predictions[2]
        }

        // Fallback to heuristic approach
        return calculateSuccessProbability(sessions, currentTime)
    }

    /**
     * Prepare input features for ML model
     */
    private fun prepareFeatures(
        sessions: List<SessionData>,
        currentTime: LocalDateTime = LocalDateTime.now()
    ): FloatArray {
        val features = FloatArray(inputSize)

        // Time context features
        features[0] = currentTime.hour.toFloat() / 23f // Normalize hour to 0-1
        features[1] = currentTime.minute.toFloat() / 59f // Normalize minute to 0-1
        features[2] = currentTime.dayOfWeek.value.toFloat() / 7f // Normalize day of week to 0-1
        
        // Recent session features
        val recentSessions = sessions.sortedByDescending { it.startTime }.take(5)
        
        // Average duration of recent sessions
        features[3] = recentSessions.map { it.durationMinutes }.average().toFloat() / 120f
        
        // Completion rate of recent sessions
        features[4] = recentSessions.count { it.completed }.toFloat() / recentSessions.size.coerceAtLeast(1)
        
        // Time since last session (normalized to days)
        val lastSessionTime = recentSessions.firstOrNull()?.startTime ?: currentTime.minusDays(7)
        val hoursSinceLastSession = java.time.Duration.between(lastSessionTime, currentTime).toHours()
        features[5] = (hoursSinceLastSession / 168f).coerceAtMost(1f) // Max 1 week
        
        // User historical features
        features[6] = sessions.count { it.completed }.toFloat() / sessions.size.coerceAtLeast(1)
        features[7] = sessions.map { it.productivityScore }.average().toFloat() / 10f
        
        // Session frequency
        val daysBetweenFirstAndLast = if (sessions.size >= 2) {
            val first = sessions.minByOrNull { it.startTime }!!.startTime
            val last = sessions.maxByOrNull { it.startTime }!!.startTime
            java.time.Duration.between(first, last).toDays().toFloat()
        } else 1f
        features[8] = (sessions.size / daysBetweenFirstAndLast.coerceAtLeast(1f)) / 10f
        
        // Interruption rate
        features[9] = sessions.map { it.interruptionCount }.average().toFloat() / 10f
        
        // Audio track consistency
        val mostUsedTrack = sessions.groupBy { it.audioTrackId }
            .maxByOrNull { it.value.size }?.key
        features[10] = sessions.count { it.audioTrackId == mostUsedTrack }.toFloat() / sessions.size
        
        // Historical success at this time of day
        val currentHour = currentTime.hour
        val sessionsInSameHour = sessions.filter { it.startTime.hour == currentHour }
        features[11] = sessionsInSameHour.count { it.completed }.toFloat() / 
                       sessionsInSameHour.size.coerceAtLeast(1)
        
        return features
    }

    /**
     * Run inference with TensorFlow Lite model
     */
    private fun runInference(inputFeatures: FloatArray): FloatArray {
        val interpreter = this.interpreter ?: return FloatArray(outputSize) { 0f }
        
        val inputBuffer = ByteBuffer.allocateDirect(inputSize * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
        
        for (feature in inputFeatures) {
            inputBuffer.putFloat(feature)
        }
        
        val outputBuffer = ByteBuffer.allocateDirect(outputSize * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
        
        interpreter.run(inputBuffer, outputBuffer)
        
        outputBuffer.rewind()
        val outputArray = FloatArray(outputSize)
        for (i in 0 until outputSize) {
            outputArray[i] = outputBuffer.getFloat()
        }
        
        return outputArray
    }

    /**
     * Use ML model to predict optimal focus duration
     */
    private fun predictWithModel(sessions: List<SessionData>, currentTime: LocalDateTime): Int {
        val features = prepareFeatures(sessions, currentTime)
        val predictions = runInference(features)
        
        // The first output is the predicted optimal duration in normalized form (0-1)
        // We denormalize it to minutes (15-120)
        val normalizedDuration = predictions[0]
        return (MIN_FOCUS_DURATION + normalizedDuration * (MAX_FOCUS_DURATION - MIN_FOCUS_DURATION)).toInt()
    }

    /**
     * Fallback heuristic prediction for optimal focus duration
     */
    private fun predictWithHeuristic(sessions: List<SessionData>): Int {
        if (sessions.isEmpty()) return DEFAULT_FOCUS_DURATION
        
        // Group sessions by duration
        val durationGroups = sessions.groupBy { it.durationMinutes }
        
        // For each duration, calculate success rate * productivity score
        val durationEffectiveness = durationGroups.mapValues { (_, sessionsWithDuration) ->
            val completionRate = sessionsWithDuration.count { it.completed } / 
                sessionsWithDuration.size.toDouble()
            val avgProductivity = sessionsWithDuration.map { it.productivityScore }.average()
            completionRate * avgProductivity // Effectiveness score
        }
        
        // Return the duration with highest effectiveness
        return durationEffectiveness.maxByOrNull { it.value }?.key ?: DEFAULT_FOCUS_DURATION
    }

    /**
     * Find the hour of day when the user is most productive
     */
    private fun findMostProductiveHour(sessions: List<SessionData>): Int {
        // Group sessions by hour
        val hourlyGroups = sessions.groupBy { it.startTime.hour }
        
        // Calculate productivity for each hour
        val hourlyProductivity = hourlyGroups.mapValues { (_, sessionsInHour) ->
            val completionRate = sessionsInHour.count { it.completed } / 
                sessionsInHour.size.toDouble()
            val avgProductivity = sessionsInHour.map { it.productivityScore }.average()
            completionRate * avgProductivity
        }
        
        // Return hour with highest productivity
        return hourlyProductivity.maxByOrNull { it.value }?.key ?: DEFAULT_OPTIMAL_HOUR
    }

    /**
     * Calculate the probability of successfully completing a session
     */
    private fun calculateSuccessProbability(
        sessions: List<SessionData>,
        currentTime: LocalDateTime
    ): Float {
        // Base probability
        var probability = 0.65f
        
        // Factor 1: Time of day
        val currentHour = currentTime.hour
        val sessionsInSameHour = sessions.filter { it.startTime.hour == currentHour }
        val hourSuccessRate = if (sessionsInSameHour.isNotEmpty()) {
            sessionsInSameHour.count { it.completed }.toFloat() / sessionsInSameHour.size
        } else 0.5f
        
        // Factor 2: Day of week
        val currentDay = currentTime.dayOfWeek.value
        val sessionsOnSameDay = sessions.filter { it.startTime.dayOfWeek.value == currentDay }
        val daySuccessRate = if (sessionsOnSameDay.isNotEmpty()) {
            sessionsOnSameDay.count { it.completed }.toFloat() / sessionsOnSameDay.size
        } else 0.5f
        
        // Factor 3: Recent success rate (last 5 sessions)
        val recentSessions = sessions.sortedByDescending { it.startTime }.take(5)
        val recentSuccessRate = if (recentSessions.isNotEmpty()) {
            recentSessions.count { it.completed }.toFloat() / recentSessions.size
        } else 0.5f
        
        // Weighted combination
        probability = (0.4f * hourSuccessRate) + (0.2f * daySuccessRate) + (0.4f * recentSuccessRate)
        
        return probability.coerceIn(0.1f, 0.95f)
    }

    /**
     * Load TensorFlow Lite model from assets
     */
    private fun loadModelFromAssets() {
        val assetManager = context.assets
        val modelBuffer = FileUtil.loadMappedFile(context, modelName)
        interpreter = Interpreter(modelBuffer)
    }

    /**
     * Check if an asset exists
     */
    private fun assetExists(context: Context, assetName: String): Boolean {
        return try {
            val assetList = context.assets.list("") ?: emptyArray()
            assetList.contains(assetName)
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "FocusPatternPredictor"
        private const val FLOAT_SIZE = 4 // Size of float in bytes
        private const val DEFAULT_FOCUS_DURATION = 25 // Default Pomodoro duration
        private const val DEFAULT_OPTIMAL_HOUR = 10 // 10 AM default most productive hour
        private const val DEFAULT_SUCCESS_PROBABILITY = 0.7f
        private const val MIN_FOCUS_DURATION = 15
        private const val MAX_FOCUS_DURATION = 120
    }
}
