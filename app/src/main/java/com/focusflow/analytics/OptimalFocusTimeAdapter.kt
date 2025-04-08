package com.focusflow.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.ui.analytics.OptimalFocusTimeUiModel

/**
 * Adapter for displaying optimal focus time recommendations in the analytics dashboard.
 * These recommendations are based on the user's historical productivity data from Firebase.
 */
class OptimalFocusTimeAdapter : 
    ListAdapter<OptimalFocusTimeUiModel, OptimalFocusTimeAdapter.OptimalTimeViewHolder>(OptimalTimeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OptimalTimeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_optimal_focus_time, parent, false)
        return OptimalTimeViewHolder(view)
    }

    override fun onBindViewHolder(holder: OptimalTimeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class OptimalTimeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        
        private val cvTimeCard: CardView = itemView.findViewById(R.id.cvTimeCard)
        private val tvDayName: TextView = itemView.findViewById(R.id.tvDayName)
        private val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        private val tvReason: TextView = itemView.findViewById(R.id.tvReason)
        
        fun bind(item: OptimalFocusTimeUiModel) {
            tvDayName.text = item.dayName
            tvTime.text = item.time
            tvReason.text = item.reason
            
            // Set different card colors for visual distinction
            val colorPosition = adapterPosition % 3
            val colorRes = when (colorPosition) {
                0 -> R.color.colorPrimary
                1 -> R.color.colorSecondary
                else -> R.color.colorTertiary
            }
            
            cvTimeCard.setCardBackgroundColor(itemView.context.getColor(colorRes))
        }
    }
}

/**
 * DiffUtil callback for optimal focus times
 */
class OptimalTimeDiffCallback : DiffUtil.ItemCallback<OptimalFocusTimeUiModel>() {
    override fun areItemsTheSame(oldItem: OptimalFocusTimeUiModel, newItem: OptimalFocusTimeUiModel): Boolean {
        return oldItem.dayName == newItem.dayName && oldItem.time == newItem.time
    }

    override fun areContentsTheSame(oldItem: OptimalFocusTimeUiModel, newItem: OptimalFocusTimeUiModel): Boolean {
        return oldItem == newItem
    }
}
