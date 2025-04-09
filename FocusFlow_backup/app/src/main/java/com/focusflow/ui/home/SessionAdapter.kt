package com.focusflow.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.databinding.ItemSessionBinding
import com.focusflow.domain.model.Session
import com.focusflow.domain.model.SessionType
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class SessionAdapter : ListAdapter<Session, SessionAdapter.SessionViewHolder>(SessionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding = ItemSessionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SessionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class SessionViewHolder(private val binding: ItemSessionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        fun bind(session: Session) {
            val context = binding.root.context

            // Set session type icon and title
            when (session.type) {
                SessionType.FOCUS -> {
                    binding.imageViewSessionType.setImageResource(R.drawable.ic_focus)
                    binding.textViewSessionTitle.text = context.getString(R.string.focus_session)
                }
                SessionType.BREAK -> {
                    binding.imageViewSessionType.setImageResource(R.drawable.ic_break)
                    binding.textViewSessionTitle.text = context.getString(R.string.break_session)
                }
            }

            // Format and set date
            binding.textViewSessionDate.text = dateFormat.format(session.startTime)

            // Format and set duration
            val minutes = TimeUnit.MILLISECONDS.toMinutes(session.durationMs)
            binding.textViewSessionTime.text = context.getString(R.string.minutes_format, minutes)
        }
    }

    class SessionDiffCallback : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }
}
