package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemAdminQueueBinding
import com.queueless.plus.models.Queue

class AdminQueueAdapter(
    private val onManage: (Queue) -> Unit,
    private val onDelete: (Queue) -> Unit
) : ListAdapter<Queue, AdminQueueAdapter.AdminQueueViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdminQueueViewHolder {
        val binding = ItemAdminQueueBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return AdminQueueViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AdminQueueViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class AdminQueueViewHolder(
        private val binding: ItemAdminQueueBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(queue: Queue) {
            binding.tvQueueName.text   = queue.queueName
            binding.tvLocation.text    = queue.location
            binding.tvServiceTime.text = "${queue.avgServiceTime} min/person"
            binding.btnManage.setOnClickListener { onManage(queue) }
            binding.btnDelete.setOnClickListener { onDelete(queue) }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Queue>() {
        override fun areItemsTheSame(old: Queue, new: Queue) = old.queueId == new.queueId
        override fun areContentsTheSame(old: Queue, new: Queue) = old == new
    }
}
