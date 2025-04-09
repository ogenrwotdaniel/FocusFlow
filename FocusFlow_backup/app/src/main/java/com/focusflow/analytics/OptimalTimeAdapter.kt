package com.focusflow.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator

/**
 * Adapter for displaying optimal focus time recommendations
 */
class OptimalTimeAdapter(private val onScheduleClicked: (OptimalFocusTime) -> Unit) : 
    ListAdapter<OptimalFocusTime, OptimalTimeAdapter.OptimalTimeViewHolder>(OptimalTimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptimalTimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_optimal_time, parent, false)
        return OptimalTimeViewHolder(view, onScheduleClicked)
    }

    override fun onBindViewHolder(holder: OptimalTimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OptimalTimeViewHolder(
        itemView: View,
        private val onScheduleClicked: (OptimalFocusTime) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val tvTimeSlot: TextView = itemView.findViewById(R.id.tvTimeSlot)
        private val progressProductivity: LinearProgressIndicator = itemView.findViewById(R.id.progressProductivity)
        private val tvProductivityValue: TextView = itemView.findViewById(R.id.tvProductivityValue)
        private val tvOptimalDescription: TextView = itemView.findViewById(R.id.tvOptimalDescription)
        private val btnScheduleTime: MaterialButton = itemView.findViewById(R.id.btnScheduleTime)
        
        fun bind(optimalTime: OptimalFocusTime) {
            tvTimeSlot.text = optimalTime.formattedTime
            
            // Set productivity score (0-10 scale)
            val productivityScore = optimalTime.productivityScore
            tvProductivityValue.text = String.format("%.1f", productivityScore)
            
            // Update progress indicator (0-100 scale)
            progressProductivity.progress = (productivityScore * 10).toInt()
            
            // Update color based on score
            val colorRes = when {
                productivityScore >= 8.0f -> R.color.colorSuccess
                productivityScore >= 6.0f -> R.color.colorPrimary
                productivityScore >= 4.0f -> R.color.colorWarning
                else -> R.color.colorError
            }
            progressProductivity.setIndicatorColor(itemView.context.getColor(colorRes))
            
            // Set description text based on score
            val descriptionRes = when {
                productivityScore >= 8.0f -> R.string.optimal_time_excellent
                productivityScore >= 6.0f -> R.string.optimal_time_good
                else -> R.string.optimal_time_moderate
            }
            tvOptimalDescription.setText(descriptionRes)
            
            // Set button action
            btnScheduleTime.setOnClickListener {
                onScheduleClicked(optimalTime)
            }
        }
    }

    class OptimalTimeDiffCallback : DiffUtil.ItemCallback<OptimalFocusTime>() {
        override fun areItemsTheSame(oldItem: OptimalFocusTime, newItem: OptimalFocusTime): Boolean {
            return oldItem.hour == newItem.hour
        }

        override fun areContentsTheSame(oldItem: OptimalFocusTime, newItem: OptimalFocusTime): Boolean {
            return oldItem == newItem
        }
    }
}
