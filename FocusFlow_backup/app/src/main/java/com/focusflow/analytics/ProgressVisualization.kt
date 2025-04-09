package com.focusflow.analytics

import android.content.Context
import android.graphics.Color
import com.focusflow.R
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provides data visualization tools for productivity analytics.
 * Creates charts and visual representations of focus session data based on cognitive science principles.
 */
@Singleton
class ProgressVisualization @Inject constructor(
    private val context: Context,
    private val productivityInsights: ProductivityInsights
) {

    /**
     * Set up the weekly focus chart to visualize daily focus minutes
     * @param chart The LineChart to configure
     * @param focusData Map of days to focus minutes
     */
    fun setupWeeklyFocusChart(chart: LineChart, focusData: Map<LocalDate, Int>) {
        // Create entries
        val entries = mutableListOf<Entry>()
        val xAxisLabels = mutableListOf<String>()
        
        // Get last 7 days
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val dayOfWeek = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
            val focusMinutes = focusData[date] ?: 0
            
            entries.add(Entry(6 - i.toFloat(), focusMinutes.toFloat()))
            xAxisLabels.add(dayOfWeek)
        }
        
        // Create dataset
        val dataSet = LineDataSet(entries, "Focus Minutes")
        
        // Style the dataset
        dataSet.apply {
            color = context.getColor(R.color.colorPrimary)
            setCircleColor(context.getColor(R.color.colorAccent))
            lineWidth = 2f
            circleRadius = 4f
            setDrawFilled(true)
            fillColor = context.getColor(R.color.colorPrimaryLight)
            fillAlpha = 100
            mode = LineDataSet.Mode.CUBIC_BEZIER
            valueTextSize = 10f
        }
        
        // Setup the chart
        chart.apply {
            data = LineData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            
            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(xAxisLabels)
            }
            
            // Y-axis
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
            }
            
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            
            animateY(1000)
            invalidate()
        }
    }
    
    /**
     * Set up a productivity score gauge chart to visualize overall productivity
     * @param chart The PieChart to configure as a gauge
     * @param score The productivity score (0-100)
     */
    fun setupProductivityGauge(chart: PieChart, score: Int) {
        // Create a semi-circle gauge with the score
        val scoreValue = score.toFloat()
        val remainingValue = 100f - scoreValue
        
        val entries = listOf(
            PieEntry(scoreValue, ""),
            PieEntry(remainingValue, "")
        )
        
        val dataSet = PieDataSet(entries, "").apply {
            // Color gradient based on score
            colors = listOf(
                getScoreColor(score),
                Color.LTGRAY
            )
            setDrawValues(false)
        }
        
        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            legend.isEnabled = false
            holeRadius = 80f
            transparentCircleRadius = 0f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            
            // Make it a half-circle
            maxAngle = 180f
            rotationAngle = 180f
            setRotationEnabled(false)
            
            // Center text
            setDrawCenterText(true)
            centerText = "$score"
            setCenterTextSize(36f)
            setCenterTextColor(getScoreColor(score))
            
            // Disable interaction
            setTouchEnabled(false)
            
            animateY(1000)
            invalidate()
        }
    }
    
    /**
     * Set up a focus distribution chart to show when the user focuses most
     * @param chart The PieChart to configure
     * @param distribution Map of time slots to session counts
     */
    fun setupFocusDistributionChart(chart: PieChart, distribution: Map<String, Int>) {
        // Filter to only include slots with sessions
        val activeDistribution = distribution.filter { it.value > 0 }
        
        if (activeDistribution.isEmpty()) {
            // No data case
            chart.setNoDataText("Complete focus sessions to see your distribution")
            return
        }
        
        // Create entries
        val entries = activeDistribution.map { (label, count) ->
            PieEntry(count.toFloat(), getShortTimeLabel(label))
        }
        
        // Create dataset
        val dataSet = PieDataSet(entries, "Focus Distribution").apply {
            colors = ColorTemplate.MATERIAL_COLORS.toList()
            valueTextSize = 12f
            valueFormatter = PercentFormatter(chart)
            valueTextColor = Color.WHITE
            sliceSpace = 2f
        }
        
        // Setup the chart
        chart.apply {
            data = PieData(dataSet)
            description.isEnabled = false
            isDrawHoleEnabled = true
            holeRadius = 40f
            transparentCircleRadius = 45f
            setHoleColor(Color.WHITE)
            
            legend.isEnabled = true
            setDrawEntryLabels(false)
            
            setUsePercentValues(true)
            
            animateY(1000)
            invalidate()
        }
    }
    
    /**
     * Set up an interruptions bar chart to show when the user experiences the most interruptions
     * @param chart The BarChart to configure
     * @param interruptionsByHour Map of hours to interruption counts
     */
    fun setupInterruptionsChart(chart: BarChart, interruptionsByHour: Map<Int, Float>) {
        // Create entries for each hour with data
        val entries = mutableListOf<BarEntry>()
        val xAxisLabels = mutableListOf<String>()
        
        // Get formatted hours
        val hourFormatter = DateTimeFormatter.ofPattern("h a")
        for (hour in interruptionsByHour.keys.sorted()) {
            val time = LocalDate.now().atStartOfDay().plusHours(hour.toLong())
            xAxisLabels.add(hourFormatter.format(time))
            entries.add(BarEntry(hour.toFloat(), interruptionsByHour[hour] ?: 0f))
        }
        
        // Create dataset
        val dataSet = BarDataSet(entries, "Average Interruptions").apply {
            color = context.getColor(R.color.colorAccent)
            valueTextSize = 10f
        }
        
        // Setup the chart
        chart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            
            // X-axis
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                valueFormatter = IndexAxisValueFormatter(xAxisLabels)
                labelCount = entries.size
                granularity = 1f
            }
            
            // Y-axis
            axisRight.isEnabled = false
            axisLeft.apply {
                setDrawGridLines(false)
                axisMinimum = 0f
                granularity = 1f
            }
            
            setFitBars(true)
            animateY(1000)
            invalidate()
        }
    }
    
    /**
     * Set up a productivity heatmap to show productivity by day and time
     * Implemented as a grid of colored cells
     * @param productivity 2D array with productivity scores
     * @return List of color values for the grid cells
     */
    fun createProductivityHeatmap(productivity: Array<Array<Float>>): List<Int> {
        val colors = mutableListOf<Int>()
        
        // Convert productivity scores to colors
        for (dayRow in productivity) {
            for (hourScore in dayRow) {
                colors.add(getHeatmapColor(hourScore))
            }
        }
        
        return colors
    }
    
    /**
     * Get a color based on a productivity score
     */
    private fun getScoreColor(score: Int): Int {
        return when {
            score >= 80 -> context.getColor(R.color.colorSuccess)
            score >= 60 -> context.getColor(R.color.colorPrimary)
            score >= 40 -> context.getColor(R.color.colorWarning)
            else -> context.getColor(R.color.colorError)
        }
    }
    
    /**
     * Get a color for heatmap based on productivity score
     */
    private fun getHeatmapColor(score: Float): Int {
        return when {
            score >= 8.0 -> Color.parseColor("#1a9850") // Dark green
            score >= 6.0 -> Color.parseColor("#66bd63") // Medium green
            score >= 4.0 -> Color.parseColor("#a6d96a") // Light green
            score >= 2.0 -> Color.parseColor("#fee08b") // Yellow
            score > 0.0 -> Color.parseColor("#fdae61") // Orange
            else -> Color.parseColor("#eeeeee") // Gray for no data
        }
    }
    
    /**
     * Get a shortened version of a time slot label for the pie chart
     */
    private fun getShortTimeLabel(label: String): String {
        return when (label) {
            "Early Morning (5-8 AM)" -> "5-8 AM"
            "Morning (8-11 AM)" -> "8-11 AM"
            "Midday (11 AM-2 PM)" -> "11-2 PM"
            "Afternoon (2-5 PM)" -> "2-5 PM"
            "Evening (5-8 PM)" -> "5-8 PM"
            "Night (8-11 PM)" -> "8-11 PM"
            "Late Night (11 PM-5 AM)" -> "11PM-5AM"
            else -> label
        }
    }
    
    /**
     * Get labels for days of the week
     */
    fun getWeekdayLabels(): List<String> {
        return DayOfWeek.values().map { 
            it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) 
        }
    }
    
    /**
     * Get labels for hours of the day
     */
    fun getHourLabels(): List<String> {
        val labels = mutableListOf<String>()
        val formatter = DateTimeFormatter.ofPattern("h a")
        
        for (hour in 0..23 step 3) {
            val time = LocalDate.now().atStartOfDay().plusHours(hour.toLong())
            labels.add(formatter.format(time))
        }
        
        return labels
    }
}
