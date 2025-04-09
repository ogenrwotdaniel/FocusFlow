package com.focusflow.test

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.focusflow.R
import com.focusflow.analytics.DailyProductivity
import com.focusflow.analytics.RemoteConfigManager
import com.focusflow.analytics.TrendAnalyzer
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

/**
 * Simple test activity to demonstrate trend analysis features without needing a full build
 */
@AndroidEntryPoint
class AnalyticsTestActivity : AppCompatActivity() {

    @Inject
    lateinit var remoteConfigManager: RemoteConfigManager
    
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics
    
    private lateinit var trendAnalyzer: TrendAnalyzer
    private lateinit var resultTextView: TextView
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics_test)
        
        resultTextView = findViewById(R.id.tvTestResults)
        
        trendAnalyzer = TrendAnalyzer(remoteConfigManager)
        
        // Set up test buttons
        findViewById<Button>(R.id.btnTestTrends).setOnClickListener {
            testTrendAnalysis()
        }
        
        findViewById<Button>(R.id.btnTestStreaks).setOnClickListener {
            testStreakDetection()
        }
        
        findViewById<Button>(R.id.btnTestRemoteConfig).setOnClickListener {
            testRemoteConfig()
        }
    }
    
    private fun testTrendAnalysis() {
        // Generate sample data
        val sampleData = generateSampleProductivityData()
        
        // Analyze trends
        val trends = trendAnalyzer.analyzeTrends(sampleData)
        
        // Display results
        val resultsBuilder = StringBuilder()
        resultsBuilder.append("Trend Analysis Results:\n\n")
        
        trends.forEach { trend ->
            resultsBuilder.append("Type: ${trend.type}\n")
            resultsBuilder.append("Title: ${trend.title}\n")
            resultsBuilder.append("Description: ${trend.description}\n")
            resultsBuilder.append("Value Change: ${trend.valueChange}\n\n")
        }
        
        resultTextView.text = resultsBuilder.toString()
        
        // Log to Firebase
        firebaseAnalytics.logEvent("test_trend_analysis", null)
    }
    
    private fun testStreakDetection() {
        // Generate sample data
        val sampleData = generateStreakData()
        
        // Detect streaks
        val streak = trendAnalyzer.detectProductivityStreak(sampleData)
        
        // Display results
        val resultsBuilder = StringBuilder()
        resultsBuilder.append("Streak Detection Results:\n\n")
        
        if (streak != null) {
            resultsBuilder.append("Current Streak: ${streak.currentStreakDays} days\n")
            resultsBuilder.append("Longest Streak: ${streak.longestStreakDays} days\n")
            resultsBuilder.append("Is Active: ${streak.isActive}\n")
            resultsBuilder.append("Last Active Date: ${streak.lastActiveDate}\n")
        } else {
            resultsBuilder.append("No streak detected.")
        }
        
        resultTextView.text = resultsBuilder.toString()
        
        // Log to Firebase
        firebaseAnalytics.logEvent("test_streak_detection", null)
    }
    
    private fun testRemoteConfig() {
        remoteConfigManager.fetchAndActivate { success ->
            if (success) {
                val resultsBuilder = StringBuilder()
                resultsBuilder.append("Remote Config Values:\n\n")
                
                resultsBuilder.append("Trend Analysis Enabled: ${remoteConfigManager.isTrendAnalysisEnabled()}\n")
                resultsBuilder.append("Daily Focus Goal: ${remoteConfigManager.getDailyFocusGoalMinutes()} minutes\n")
                resultsBuilder.append("Trend Lookback Period: ${remoteConfigManager.getTrendLookbackDays()} days\n")
                
                resultTextView.text = resultsBuilder.toString()
            } else {
                resultTextView.text = "Failed to fetch remote config values"
            }
        }
    }
    
    private fun generateSampleProductivityData(): List<DailyProductivity> {
        val today = LocalDate.now()
        return listOf(
            DailyProductivity(today.minusDays(7), 45, 3, 0.8f),
            DailyProductivity(today.minusDays(6), 50, 2, 0.85f),
            DailyProductivity(today.minusDays(5), 55, 1, 0.9f),
            DailyProductivity(today.minusDays(4), 60, 1, 0.95f),
            DailyProductivity(today.minusDays(3), 65, 0, 1.0f),
            DailyProductivity(today.minusDays(2), 60, 1, 0.9f),
            DailyProductivity(today.minusDays(1), 70, 0, 1.0f),
            DailyProductivity(today, 75, 0, 1.0f)
        )
    }
    
    private fun generateStreakData(): List<DailyProductivity> {
        val today = LocalDate.now()
        return listOf(
            DailyProductivity(today.minusDays(10), 30, 5, 0.6f),
            DailyProductivity(today.minusDays(9), 0, 0, 0f), // Break in streak
            DailyProductivity(today.minusDays(8), 0, 0, 0f), // Break in streak
            DailyProductivity(today.minusDays(7), 40, 3, 0.7f),
            DailyProductivity(today.minusDays(6), 45, 2, 0.8f),
            DailyProductivity(today.minusDays(5), 50, 1, 0.85f),
            DailyProductivity(today.minusDays(4), 55, 1, 0.9f),
            DailyProductivity(today.minusDays(3), 60, 0, 1.0f),
            DailyProductivity(today.minusDays(2), 65, 0, 1.0f),
            DailyProductivity(today.minusDays(1), 70, 0, 1.0f),
            DailyProductivity(today, 75, 0, 1.0f)
        )
    }
}
