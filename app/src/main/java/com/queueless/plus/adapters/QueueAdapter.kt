package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemQueueBinding
import com.queueless.plus.models.Queue

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
    }

    inner class QueueViewHolder(
        private val binding: ItemQueueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(queue: Queue) {
            binding.tvQueueName.text    = queue.queueName
            binding.tvDescription.text  = queue.description
            binding.tvLocation.text     = queue.location
            binding.tvServiceTime.text  = "~${queue.avgServiceTime} min/person"
            binding.tvCount.text        = "${queue.currentCount} waiting"
            binding.root.setOnClickListener { onClick(queue) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Queue>() {
        override fun areItemsTheSame(old: Queue, new: Queue) = old.queueId == new.queueId
        override fun areContentsTheSame(old: Queue, new: Queue) = old == new
    }
}
