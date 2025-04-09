package com.focusflow.analytics

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.ai.Insight
import com.focusflow.ai.InsightType
import com.google.android.material.button.MaterialButton

/**
 * Adapter for displaying AI-generated productivity insights
 */
class InsightAdapter(private val onInsightActionClicked: (Insight) -> Unit) : 
    ListAdapter<Insight, InsightAdapter.InsightViewHolder>(InsightDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_insight, parent, false)
        return InsightViewHolder(view, onInsightActionClicked)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class InsightViewHolder(
        itemView: View,
        private val onInsightActionClicked: (Insight) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val ivInsightIcon: ImageView = itemView.findViewById(R.id.ivInsightIcon)
        private val tvInsightTitle: TextView = itemView.findViewById(R.id.tvInsightTitle)
        private val tvInsightDescription: TextView = itemView.findViewById(R.id.tvInsightDescription)
        private val btnInsightAction: MaterialButton = itemView.findViewById(R.id.btnInsightAction)
        
        fun bind(insight: Insight) {
            tvInsightTitle.text = insight.title
            tvInsightDescription.text = insight.description
            
            // Set the appropriate icon based on insight type
            val iconRes = when (insight.type) {
                InsightType.PATTERN_DETECTED -> R.drawable.ic_lightbulb
                InsightType.RECOMMENDATION -> R.drawable.ic_recommendation
                InsightType.ACHIEVEMENT -> R.drawable.ic_trophy
                InsightType.WARNING -> R.drawable.ic_warning
            }
            ivInsightIcon.setImageResource(iconRes)
            
            // Set button text and action
            btnInsightAction.text = insight.actionText ?: itemView.context.getString(R.string.apply_insight)
            btnInsightAction.visibility = if (insight.hasAction) View.VISIBLE else View.GONE
            
            btnInsightAction.setOnClickListener {
                onInsightActionClicked(insight)
            }
        }
    }

    class InsightDiffCallback : DiffUtil.ItemCallback<Insight>() {
        override fun areItemsTheSame(oldItem: Insight, newItem: Insight): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Insight, newItem: Insight): Boolean {
            return oldItem == newItem
        }
    }
}
