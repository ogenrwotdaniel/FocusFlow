package com.focusflow.ui.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.analytics.TrendAnalyzer

/**
 * Adapter for displaying productivity streak in a RecyclerView
 */
class StreakCardAdapter(
    private val streak: TrendAnalyzer.ProductivityStreak?,
    private val goalMinutes: Int,
    private val onStreakActionClick: () -> Unit
) : RecyclerView.Adapter<StreakCardAdapter.StreakViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StreakViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.streak_card_item,
            parent,
            false
        )
        return StreakViewHolder(view, onStreakActionClick)
    }

    override fun onBindViewHolder(holder: StreakViewHolder, position: Int) {
        streak?.let { holder.bind(it, goalMinutes) }
    }

    override fun getItemCount(): Int = if (streak != null) 1 else 0

    class StreakViewHolder(
        itemView: View,
        private val onStreakActionClick: () -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val streakIcon: ImageView = itemView.findViewById(R.id.streak_icon)
        private val streakTitle: TextView = itemView.findViewById(R.id.streak_title)
        private val streakValue: TextView = itemView.findViewById(R.id.streak_value)
        private val streakLabel: TextView = itemView.findViewById(R.id.streak_label)
        private val bestStreakTitle: TextView = itemView.findViewById(R.id.best_streak_title)
        private val bestStreakValue: TextView = itemView.findViewById(R.id.best_streak_value)
        private val streakDescription: TextView = itemView.findViewById(R.id.streak_description)
        private val streakActionButton: Button = itemView.findViewById(R.id.streak_action_button)

        fun bind(streak: TrendAnalyzer.ProductivityStreak, goalMinutes: Int) {
            // Set streak value
            streakValue.text = streak.currentStreakDays.toString()
            
            // Adjust label for single day
            streakLabel.text = if (streak.currentStreakDays == 1) "DAY" else "DAYS"
            
            // Set best streak value
            bestStreakValue.text = "${streak.bestStreakDays} days"
            
            // Generate appropriate description based on streak
            streakDescription.text = generateDescription(streak, goalMinutes)
            
            // Set action button text and visibility
            setActionButton(streak)
            
            // Set icon appearance based on streak status
            setStreakIconAppearance(streak)
            
            // Set button click listener
            streakActionButton.setOnClickListener { onStreakActionClick() }
        }
        
        private fun generateDescription(streak: TrendAnalyzer.ProductivityStreak, goalMinutes: Int): String {
            val context = itemView.context
            
            return when {
                streak.currentStreakDays == 0 -> {
                    context.getString(R.string.streak_start_new, goalMinutes)
                }
                streak.currentStreakDays == 1 -> {
                    context.getString(R.string.streak_day_one, goalMinutes)
                }
                streak.currentStreakDays in 2..6 -> {
                    context.getString(R.string.streak_building, streak.currentStreakDays, goalMinutes)
                }
                streak.currentStreakDays in 7..13 -> {
                    context.getString(R.string.streak_week_milestone, streak.currentStreakDays)
                }
                streak.currentStreakDays in 14..29 -> {
                    context.getString(R.string.streak_two_week_milestone, streak.currentStreakDays)
                }
                else -> {
                    context.getString(R.string.streak_month_milestone, streak.currentStreakDays)
                }
            }
        }
        
        private fun setActionButton(streak: TrendAnalyzer.ProductivityStreak) {
            val context = itemView.context
            
            // Adjust button text based on streak status
            when {
                !streak.isActive && streak.currentStreakDays == 0 -> {
                    streakActionButton.text = context.getString(R.string.streak_action_start)
                }
                !streak.isActive && streak.currentStreakDays > 0 -> {
                    streakActionButton.text = context.getString(R.string.streak_action_resume)
                }
                streak.currentStreakDays > 0 && streak.currentStreakDays == streak.bestStreakDays -> {
                    streakActionButton.text = context.getString(R.string.streak_action_maintain)
                }
                else -> {
                    streakActionButton.text = context.getString(R.string.streak_action_set_goal)
                }
            }
        }
        
        private fun setStreakIconAppearance(streak: TrendAnalyzer.ProductivityStreak) {
            // Change icon color/appearance based on streak length
            when {
                !streak.isActive -> {
                    streakIcon.setImageResource(R.drawable.ic_hourglass_empty)
                }
                streak.currentStreakDays >= 30 -> {
                    streakIcon.setImageResource(R.drawable.ic_emoji_events)
                }
                streak.currentStreakDays >= 14 -> {
                    streakIcon.setImageResource(R.drawable.ic_local_fire_department_24)
                    // Apply animation or special effects for longer streaks if desired
                }
                else -> {
                    streakIcon.setImageResource(R.drawable.ic_local_fire_department_24)
                }
            }
        }
    }
}
