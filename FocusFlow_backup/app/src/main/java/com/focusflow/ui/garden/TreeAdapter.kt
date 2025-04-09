package com.focusflow.ui.garden

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.focusflow.R
import com.focusflow.databinding.ItemTreeBinding
import com.focusflow.domain.model.Tree
import java.text.SimpleDateFormat
import java.util.Locale

class TreeAdapter : ListAdapter<Tree, TreeAdapter.TreeViewHolder>(TreeDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TreeViewHolder {
        val binding = ItemTreeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TreeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TreeViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TreeViewHolder(private val binding: ItemTreeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        fun bind(tree: Tree) {
            val context = binding.root.context

            // Set tree type name
            binding.textViewTreeName.text = tree.type.displayName

            // Set creation date
            binding.textViewTreeDate.text = dateFormat.format(tree.createdAt)

            // Set sessions count
            val sessionsText = context.resources.getQuantityString(
                R.plurals.focus_sessions_count,
                tree.sessionsCompleted,
                tree.sessionsCompleted
            )
            binding.textViewTreeSessions.text = sessionsText

            // Set tree image based on growth stage
            val imageResource = when {
                tree.growthStage >= 75 -> R.drawable.tree_mature
                tree.growthStage >= 50 -> R.drawable.tree_growing
                tree.growthStage >= 25 -> R.drawable.tree_sapling
                else -> R.drawable.tree_seed
            }
            binding.imageViewTree.setImageResource(imageResource)
        }
    }

    class TreeDiffCallback : DiffUtil.ItemCallback<Tree>() {
        override fun areItemsTheSame(oldItem: Tree, newItem: Tree): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tree, newItem: Tree): Boolean {
            return oldItem == newItem
        }
    }
}
