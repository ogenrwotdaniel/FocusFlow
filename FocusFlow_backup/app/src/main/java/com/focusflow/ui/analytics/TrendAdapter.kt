package com.focusflow.ui.analytics

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.analytics.TrendAnalyzer
import java.text.NumberFormat
import kotlin.math.abs

/**
 * Adapter for displaying productivity trends in a RecyclerView
 */
class TrendAdapter : ListAdapter<TrendAnalyzer.ProductivityTrend, TrendAdapter.TrendViewHolder>(TrendDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrendViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.trend_card_item,
            parent,
            false
        )
        return TrendViewHolder(view)
    }

    override fun onBindViewHolder(holder: TrendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TrendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.trend_icon)
        private val title: TextView = itemView.findViewById(R.id.trend_title)
        private val value: TextView = itemView.findViewById(R.id.trend_value)
        private val period: TextView = itemView.findViewById(R.id.trend_period)
        private val description: TextView = itemView.findViewById(R.id.trend_description)

        fun bind(trend: TrendAnalyzer.ProductivityTrend) {
            val context = itemView.context
            
            // Set icon based on trend direction and type
            val iconResId = when {
                trend.isImproving -> R.drawable.ic_trending_up
                else -> R.drawable.ic_trending_down
            }
            icon.setImageResource(iconResId)
            
            // Set icon color based on trend direction
            val iconColor = if (trend.isImproving) {
                ContextCompat.getColor(context, R.color.positive_trend)
            } else {
                ContextCompat.getColor(context, R.color.negative_trend)
            }
            icon.setColorFilter(iconColor)
            
            // Set title based on trend type
            title.text = getTrendTitle(trend.trendType, context)
            
            // Format percentage change
            val absPercentChange = abs(trend.percentageChange)
            val percentFormat = NumberFormat.getPercentInstance()
            percentFormat.maximumFractionDigits = 1
            val formattedPercent = percentFormat.format(absPercentChange / 100)
            
            // Set value with appropriate sign
            val prefix = if (trend.isImproving) "+" else "-"
            value.text = "$prefix$formattedPercent"
            value.setTextColor(iconColor)
            
            // Set period text
            period.text = "Last ${trend.timePeriodDays} days"
            
            // Set description based on trend type and direction
            description.text = getDescriptionText(trend, context)
        }
        
        private fun getTrendTitle(trendType: TrendAnalyzer.TrendType, context: Context): String {
            return when (trendType) {
                TrendAnalyzer.TrendType.FOCUS_TIME -> context.getString(R.string.trend_focus_time)
                TrendAnalyzer.TrendType.COMPLETION_RATE -> context.getString(R.string.trend_completion_rate)
                TrendAnalyzer.TrendType.SESSIONS_COMPLETED -> context.getString(R.string.trend_sessions_completed)
                TrendAnalyzer.TrendType.OVERALL_PRODUCTIVITY -> context.getString(R.string.trend_overall_productivity)
            }
        }
        
        private fun getDescriptionText(trend: TrendAnalyzer.ProductivityTrend, context: Context): String {
            val direction = if (trend.isImproving) "increased" else "decreased"
            
            return when (trend.trendType) {
                TrendAnalyzer.TrendType.FOCUS_TIME -> {
                    if (trend.isImproving) {
                        context.getString(R.string.trend_focus_time_improving)
                    } else {
                        context.getString(R.string.trend_focus_time_declining)
                    }
                }
                TrendAnalyzer.TrendType.COMPLETION_RATE -> {
                    if (trend.isImproving) {
                        context.getString(R.string.trend_completion_rate_improving)
                    } else {
                        context.getString(R.string.trend_completion_rate_declining)
                    }
                }
                TrendAnalyzer.TrendType.SESSIONS_COMPLETED -> {
                    if (trend.isImproving) {
                        context.getString(R.string.trend_sessions_improving)
                    } else {
                        context.getString(R.string.trend_sessions_declining)
                    }
                }
                TrendAnalyzer.TrendType.OVERALL_PRODUCTIVITY -> {
                    if (trend.isImproving) {
                        context.getString(R.string.trend_productivity_improving)
                    } else {
                        context.getString(R.string.trend_productivity_declining)
                    }
                }
            }
        }
    }

    class TrendDiffCallback : DiffUtil.ItemCallback<TrendAnalyzer.ProductivityTrend>() {
        override fun areItemsTheSame(oldItem: TrendAnalyzer.ProductivityTrend, newItem: TrendAnalyzer.ProductivityTrend): Boolean {
            return oldItem.trendType == newItem.trendType
        }

        override fun areContentsTheSame(oldItem: TrendAnalyzer.ProductivityTrend, newItem: TrendAnalyzer.ProductivityTrend): Boolean {
            return oldItem == newItem
        }
    }
}
