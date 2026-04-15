package com.queueless.plus.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.queueless.plus.databinding.ItemQueueEntryBinding
import com.queueless.plus.models.QueueEntry

class QueueEntryAdapter : ListAdapter<QueueEntry, QueueEntryAdapter.EntryViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryViewHolder {
        val binding = ItemQueueEntryBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EntryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EntryViewHolder, position: Int) {
        holder.bind(getItem(position), position + 1)
    }

    inner class EntryViewHolder(
        private val binding: ItemQueueEntryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(entry: QueueEntry, position: Int) {
            binding.tvPosition.text = "#$position"
            binding.tvUserName.text = entry.userName
            binding.tvStatus.text   = entry.status.replaceFirstChar { it.uppercase() }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<QueueEntry>() {
        override fun areItemsTheSame(old: QueueEntry, new: QueueEntry) = old.entryId == new.entryId
        override fun areContentsTheSame(old: QueueEntry, new: QueueEntry) = old == new
    }
}
