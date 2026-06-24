package com.example.cityexplorerchallenge.ui.history

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.cityexplorerchallenge.data.local.ChallengeEntity
import com.example.cityexplorerchallenge.databinding.ItemChallengeHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : ListAdapter<ChallengeEntity, HistoryAdapter.HistoryViewHolder>(ChallengeDiffCallback()) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val binding = ItemChallengeHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.apply {
            tvTitle.text = item.title
            tvCategory.text = "Category: ${item.category}"
            tvDistance.text = "Distance: ${item.distanceMeters} m"
            val eventTime = when (item.status) {
                "COMPLETED" -> item.completedAt ?: item.generatedAt
                "EXPIRED" -> item.expiredAt ?: item.generatedAt
                else -> item.generatedAt
            }
            val eventLabel = when (item.status) {
                "COMPLETED" -> "Completed"
                "EXPIRED" -> "Expired"
                else -> "Generated"
            }
            tvDate.text = "$eventLabel: ${dateFormat.format(Date(eventTime))}"

            // Set status indicator color dynamically
            val statusColorRes = when (item.status) {
                "COMPLETED" -> com.example.cityexplorerchallenge.R.color.status_completed
                "EXPIRED" -> com.example.cityexplorerchallenge.R.color.status_expired
                else -> com.example.cityexplorerchallenge.R.color.status_active
            }
            statusIndicator.setBackgroundColor(
                androidx.core.content.ContextCompat.getColor(root.context, statusColorRes)
            )
        }
    }

    class HistoryViewHolder(val binding: ItemChallengeHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    class ChallengeDiffCallback : DiffUtil.ItemCallback<ChallengeEntity>() {
        override fun areItemsTheSame(oldItem: ChallengeEntity, newItem: ChallengeEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChallengeEntity, newItem: ChallengeEntity): Boolean {
            return oldItem == newItem
        }
    }
}
