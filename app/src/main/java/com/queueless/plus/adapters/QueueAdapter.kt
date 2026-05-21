package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.R
import com.queueless.plus.databinding.ItemQueueBinding
import com.queueless.plus.models.Queue
import com.queueless.plus.utils.formatWaitTime

class QueueAdapter(
    private val onClick: (Queue) -> Unit
) : ListAdapter<Queue, QueueAdapter.QueueViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueueViewHolder {
        val binding = ItemQueueBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return QueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QueueViewHolder, position: Int) {
        holder.bind(getItem(position))
        // Add fade-in animation
        holder.itemView.startAnimation(
            AnimationUtils.loadAnimation(holder.itemView.context, R.anim.fade_in)
        )
    }

    inner class QueueViewHolder(
        private val binding: ItemQueueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(queue: Queue) {
            binding.tvQueueName.text    = queue.queueName
            val pausedLabel = if (queue.isPaused) " (Paused)" else ""
            binding.tvDescription.text  = queue.description + pausedLabel
            binding.tvLocation.text     = queue.location
            binding.tvServiceTime.text  = "~${queue.avgServiceTime} min/person"
            binding.tvCount.text        = "${queue.currentCount} waiting"
            val estimatedWait = queue.currentCount * queue.avgServiceTime
            binding.tvEstimatedWait.text = "Est. wait: ${estimatedWait.formatWaitTime()}"
            binding.root.setOnClickListener { onClick(queue) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Queue>() {
        override fun areItemsTheSame(old: Queue, new: Queue) = old.queueId == new.queueId
        override fun areContentsTheSame(old: Queue, new: Queue) = old == new
    }
}
